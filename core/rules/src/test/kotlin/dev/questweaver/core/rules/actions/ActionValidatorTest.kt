package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.ActionOption
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.CombatAction
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.actions.models.SpellEffect
import dev.questweaver.core.rules.actions.validation.ActionValidationSystem
import dev.questweaver.core.rules.actions.validation.ActionValidator
import dev.questweaver.core.rules.actions.validation.ValidationResult
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ActionValidatorTest : FunSpec({
    
    // Test fixtures
    val attacker = Creature(
        id = 1L,
        name = "Fighter",
        armorClass = 16,
        hitPointsCurrent = 30,
        hitPointsMax = 30,
        speed = 30,
        abilities = Abilities(strength = 16, dexterity = 14, constitution = 14, intelligence = 10, wisdom = 12, charisma = 10)
    )
    
    val target = Creature(
        id = 2L,
        name = "Goblin",
        armorClass = 15,
        hitPointsCurrent = 20,
        hitPointsMax = 20,
        speed = 30,
        abilities = Abilities(strength = 8, dexterity = 14, constitution = 10, intelligence = 10, wisdom = 8, charisma = 8)
    )
    
    val baseContext = ActionContext(
        sessionId = 1L,
        roundNumber = 1,
        turnPhase = TurnPhase(
            creatureId = 1L,
            actionAvailable = true,
            bonusActionAvailable = true,
            reactionAvailable = true,
            movementRemaining = 30
        ),
        creatures = mapOf(1L to attacker, 2L to target),
        mapGrid = MapGrid(width = 10, height = 10),
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    test("valid action passes validation") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns ValidationResult.Valid
        
        val result = validator.validate(action, baseContext)
        
        result shouldBe ValidationResult.Valid
        verify { validationSystem.validateAction(action, baseContext) }
    }
    
    test("invalid action rejected with reason") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        val expectedReason = "Target is not a valid creature"
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid(expectedReason)
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe expectedReason
    }
    
    test("action without available action phase rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val contextNoAction = baseContext.copy(
            turnPhase = baseContext.turnPhase.copy(actionAvailable = false)
        )
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, contextNoAction) } returns 
            ValidationResult.Invalid("Action phase not available")
        
        val result = validator.validate(action, contextNoAction)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Action phase not available"
    }
    
    test("action without available bonus action phase rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val contextNoBonusAction = baseContext.copy(
            turnPhase = baseContext.turnPhase.copy(bonusActionAvailable = false)
        )
        
        val action = CastSpell(
            actorId = 1L,
            spellId = 1L,
            spellLevel = 1,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "1d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = true
        )
        
        every { validationSystem.validateAction(action, contextNoBonusAction) } returns 
            ValidationResult.Invalid("Bonus action phase not available")
        
        val result = validator.validate(action, contextNoBonusAction)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Bonus action phase not available"
    }
    
    test("action without available reaction rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val contextNoReaction = baseContext.copy(
            turnPhase = baseContext.turnPhase.copy(reactionAvailable = false)
        )
        
        val action = dev.questweaver.core.rules.actions.models.Reaction(
            actorId = 1L,
            reactionType = dev.questweaver.core.rules.actions.models.ReactionType.OpportunityAttack,
            targetId = 2L
        )
        
        every { validationSystem.validateAction(action, contextNoReaction) } returns 
            ValidationResult.Invalid("Reaction not available")
        
        val result = validator.validate(action, contextNoReaction)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Reaction not available"
    }
    
    test("movement exceeding available movement rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Move(
            actorId = 1L,
            path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0),
                GridPos(3, 0),
                GridPos(4, 0),
                GridPos(5, 0),
                GridPos(6, 0),
                GridPos(7, 0)
            )
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("Insufficient movement: requires 35 feet, only 30 available")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Insufficient movement: requires 35 feet, only 30 available"
    }
    
    test("spell without available spell slot rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = CastSpell(
            actorId = 1L,
            spellId = 1L,
            spellLevel = 3,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "3d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("No spell slots available for level 3")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "No spell slots available for level 3"
    }
    
    test("attack out of range rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("Target out of range: distance 60 feet, max range 30 feet")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Target out of range: distance 60 feet, max range 30 feet"
    }
    
    test("action with incapacitated condition rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val contextWithCondition = baseContext.copy(
            activeConditions = mapOf(1L to setOf("Incapacitated"))
        )
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, contextWithCondition) } returns 
            ValidationResult.Invalid("Cannot take actions while Incapacitated")
        
        val result = validator.validate(action, contextWithCondition)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Cannot take actions while Incapacitated"
    }
    
    test("action with stunned condition rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val contextWithCondition = baseContext.copy(
            activeConditions = mapOf(1L to setOf("Stunned"))
        )
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, contextWithCondition) } returns 
            ValidationResult.Invalid("Cannot take actions while Stunned")
        
        val result = validator.validate(action, contextWithCondition)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Cannot take actions while Stunned"
    }
    
    test("action requiring choice returns options") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = CastSpell(
            actorId = 1L,
            spellId = 1L,
            spellLevel = 0,
            targets = emptyList(),
            spellEffect = SpellEffect.Attack(
                targets = emptyList(),
                damageDice = "1d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        val options = listOf(
            ActionOption(
                id = "target_1",
                description = "Target Goblin",
                action = action.copy(targets = listOf(2L))
            ),
            ActionOption(
                id = "target_2",
                description = "Target Orc",
                action = action.copy(targets = listOf(3L))
            )
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.RequiresChoice(options)
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.RequiresChoice>()
        (result as ValidationResult.RequiresChoice).options shouldBe options
    }
    
    test("movement with no line of effect rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Move(
            actorId = 1L,
            path = listOf(GridPos(0, 0), GridPos(5, 5))
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("Path blocked at position (2, 2)")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Path blocked at position (2, 2)"
    }
    
    test("action targeting invalid creature rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Attack(
            actorId = 1L,
            targetId = 999L, // Non-existent creature
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("Target creature does not exist")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Target creature does not exist"
    }
    
    test("action by non-existent actor rejected") {
        val validationSystem = mockk<ActionValidationSystem>()
        val validator = ActionValidator(validationSystem)
        
        val action = Attack(
            actorId = 999L, // Non-existent creature
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(action, baseContext) } returns 
            ValidationResult.Invalid("Actor creature does not exist")
        
        val result = validator.validate(action, baseContext)
        
        result.shouldBeInstanceOf<ValidationResult.Invalid>()
        (result as ValidationResult.Invalid).reason shouldBe "Actor creature does not exist"
    }
})
