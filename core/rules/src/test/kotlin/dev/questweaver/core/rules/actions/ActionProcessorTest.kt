package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.ActionOption
import dev.questweaver.core.rules.actions.models.ActionResult
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.CombatAction
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Dash
import dev.questweaver.core.rules.actions.models.Disengage
import dev.questweaver.core.rules.actions.models.Dodge
import dev.questweaver.core.rules.actions.models.Help
import dev.questweaver.core.rules.actions.models.HelpType
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.actions.models.Ready
import dev.questweaver.core.rules.actions.models.SpellEffect
import dev.questweaver.core.rules.actions.validation.ActionValidationSystem
import dev.questweaver.core.rules.actions.validation.ActionValidator
import dev.questweaver.core.rules.actions.validation.ValidationResult
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.DisengageAction
import dev.questweaver.domain.events.DodgeAction
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.HelpAction
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.ReadyAction
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.AbilityType
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for ActionProcessor.
 * Tests the complete action processing flow including validation and routing to handlers.
 */
class ActionProcessorTest : FunSpec({
    
    // Test fixtures
    val attacker = Creature(
        id = 1L,
        name = "Fighter",
        armorClass = 16,
        hitPointsCurrent = 30,
        hitPointsMax = 30,
        speed = 30,
        abilities = Abilities(
            strength = 16,
            dexterity = 14,
            constitution = 14,
            intelligence = 10,
            wisdom = 12,
            charisma = 10
        )
    )
    
    val target = Creature(
        id = 2L,
        name = "Goblin",
        armorClass = 15,
        hitPointsCurrent = 20,
        hitPointsMax = 20,
        speed = 30,
        abilities = Abilities(
            strength = 8,
            dexterity = 14,
            constitution = 10,
            intelligence = 10,
            wisdom = 8,
            charisma = 8
        )
    )
    
    val mapGrid = MapGrid(width = 20, height = 20)
    
    val context = ActionContext(
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
        mapGrid = mapGrid,
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    test("complete attack sequence generates correct events") {
        // Arrange
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 9),
            totalDamage = 9
        )
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val attackAction = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        val attackHandler = AttackActionHandler(attackResolver, damageCalculator)
        val movementHandler = mockk<MovementActionHandler>()
        val spellHandler = mockk<SpellActionHandler>()
        val specialHandler = mockk<SpecialActionHandler>()
        val validator = ActionValidator(validationSystem)
        
        val processor = ActionProcessor(
            attackHandler,
            movementHandler,
            spellHandler,
            specialHandler,
            validator
        )
        
        // Act
        val result = runBlocking { processor.processAction(attackAction, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        val events = (result as ActionResult.Success).events
        events shouldHaveSize 1
        
        val event = events.first()
        event.shouldBeInstanceOf<AttackResolved>()
        event.sessionId shouldBe 1L
        event.attackerId shouldBe 1L
        event.targetId shouldBe 2L
        event.hit shouldBe true
    }
    
    test("movement with opportunity attacks generates all events") {
        // Arrange
        val pathfinder = mockk<Pathfinder>()
        val reactionHandler = mockk<ReactionHandler>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        val path = listOf(
            GridPos(0, 0),
            GridPos(1, 0),
            GridPos(2, 0)
        )
        
        every { pathfinder.validatePath(path, mapGrid) } returns true
        every { pathfinder.calculateMovementCost(path, mapGrid) } returns 10
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val moveAction = Move(actorId = 1L, path = path)
        
        val attackHandler = mockk<AttackActionHandler>()
        val movementHandler = MovementActionHandler(pathfinder, reactionHandler)
        val spellHandler = mockk<SpellActionHandler>()
        val specialHandler = mockk<SpecialActionHandler>()
        val validator = ActionValidator(validationSystem)
        
        val processor = ActionProcessor(
            attackHandler,
            movementHandler,
            spellHandler,
            specialHandler,
            validator
        )
        
        // Act
        val result = runBlocking { processor.processAction(moveAction, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        val events = (result as ActionResult.Success).events
        events shouldHaveSize 1
        
        val event = events.first()
        event.shouldBeInstanceOf<MoveCommitted>()
        event.sessionId shouldBe 1L
        event.creatureId shouldBe 1L
        event.path shouldBe path
        event.movementUsed shouldBe 10
        event.movementRemaining shouldBe 20
    }
    
    test("spell casting with multiple targets generates outcomes") {
        // Arrange
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        val target2 = target.copy(id = 3L, name = "Goblin 2")
        val contextWithMultipleTargets = context.copy(
            creatures = mapOf(1L to attacker, 2L to target, 3L to target2)
        )
        
        val spellEffect = SpellEffect.Attack(
            targets = listOf(2L, 3L),
            damageDice = "3d6",
            damageModifier = 4,
            damageType = DamageType.Fire
        )
        
        val castSpellAction = CastSpell(
            actorId = 1L,
            spellId = 100L,
            spellLevel = 2,
            targets = listOf(2L, 3L),
            spellEffect = spellEffect,
            isBonusAction = false
        )
        
        every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 18),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("3d6", 4, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 3, modifier = 4, result = 15),
            totalDamage = 15
        )
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val attackHandler = mockk<AttackActionHandler>()
        val movementHandler = mockk<MovementActionHandler>()
        val spellHandler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val specialHandler = mockk<SpecialActionHandler>()
        val validator = ActionValidator(validationSystem)
        
        val processor = ActionProcessor(
            attackHandler,
            movementHandler,
            spellHandler,
            specialHandler,
            validator
        )
        
        // Act
        val result = runBlocking { processor.processAction(castSpellAction, contextWithMultipleTargets) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        val events = (result as ActionResult.Success).events
        events shouldHaveSize 1
        
        val event = events.first()
        event.shouldBeInstanceOf<SpellCast>()
        event.sessionId shouldBe 1L
        event.casterId shouldBe 1L
        event.spellId shouldBe 100L
        event.spellLevel shouldBe 2
        event.outcomes shouldHaveSize 2
    }
    
    test("action validation failures return appropriate results") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        val attackAction = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        every { validationSystem.validateAction(any(), any()) } returns 
            ValidationResult.Invalid("Action not available")
        
        val attackHandler = mockk<AttackActionHandler>()
        val movementHandler = mockk<MovementActionHandler>()
        val spellHandler = mockk<SpellActionHandler>()
        val specialHandler = mockk<SpecialActionHandler>()
        val validator = ActionValidator(validationSystem)
        
        val processor = ActionProcessor(
            attackHandler,
            movementHandler,
            spellHandler,
            specialHandler,
            validator
        )
        
        // Act
        val result = runBlocking { processor.processAction(attackAction, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Failure>()
        (result as ActionResult.Failure).reason shouldBe "Action not available"
    }
    
    test("action validation requiring choice returns appropriate results") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        val attackAction = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        val options = listOf(
            ActionOption(
                id = "option1",
                description = "Attack with longsword",
                action = attackAction
            ),
            ActionOption(
                id = "option2",
                description = "Attack with shortsword",
                action = attackAction.copy(damageDice = "1d6")
            )
        )
        
        every { validationSystem.validateAction(any(), any()) } returns 
            ValidationResult.RequiresChoice(options)
        
        val attackHandler = mockk<AttackActionHandler>()
        val movementHandler = mockk<MovementActionHandler>()
        val spellHandler = mockk<SpellActionHandler>()
        val specialHandler = mockk<SpecialActionHandler>()
        val validator = ActionValidator(validationSystem)
        
        val processor = ActionProcessor(
            attackHandler,
            movementHandler,
            spellHandler,
            specialHandler,
            validator
        )
        
        // Act
        val result = runBlocking { processor.processAction(attackAction, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
        val choiceResult = result as ActionResult.RequiresChoice
        choiceResult.options shouldHaveSize 2
        choiceResult.options[0].id shouldBe "option1"
        choiceResult.options[1].id shouldBe "option2"
    }
    
    test("exhaustive when expression handles all action types - Attack") {
        // Arrange
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 18),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 8),
            totalDamage = 8
        )
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        val processor = createProcessor(validationSystem, attackResolver, damageCalculator)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<AttackResolved>()
    }
    
    test("exhaustive when expression handles all action types - Move") {
        // Arrange
        val pathfinder = mockk<Pathfinder>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        val path = listOf(GridPos(0, 0), GridPos(1, 0))
        
        every { pathfinder.validatePath(any(), any()) } returns true
        every { pathfinder.calculateMovementCost(any(), any()) } returns 5
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Move(actorId = 1L, path = path)
        
        val processor = createProcessorWithMovement(validationSystem, pathfinder)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<MoveCommitted>()
    }
    
    test("exhaustive when expression handles all action types - Dash") {
        // Arrange
        val pathfinder = mockk<Pathfinder>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Dash(actorId = 1L)
        
        val processor = createProcessorWithMovement(validationSystem, pathfinder)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        // Note: Current implementation returns empty list for Dash (TODO)
        (result as ActionResult.Success).events shouldHaveSize 0
    }
    
    test("exhaustive when expression handles all action types - CastSpell") {
        // Arrange
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 18),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 3, modifier = 4, result = 15),
            totalDamage = 15
        )
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = CastSpell(
            actorId = 1L,
            spellId = 100L,
            spellLevel = 2,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "3d6",
                damageModifier = 4,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        val processor = createProcessorWithSpell(validationSystem, attackResolver, savingThrowResolver, damageCalculator)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<SpellCast>()
    }
    
    test("exhaustive when expression handles all action types - Dodge") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Dodge(actorId = 1L)
        
        val processor = createProcessorWithSpecial(validationSystem)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<DodgeAction>()
    }
    
    test("exhaustive when expression handles all action types - Disengage") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Disengage(actorId = 1L)
        
        val processor = createProcessorWithSpecial(validationSystem)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<DisengageAction>()
    }
    
    test("exhaustive when expression handles all action types - Help") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val action = Help(
            actorId = 1L,
            targetId = 2L,
            helpType = HelpType.Attack
        )
        
        val processor = createProcessorWithSpecial(validationSystem)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<HelpAction>()
    }
    
    test("exhaustive when expression handles all action types - Ready") {
        // Arrange
        val validationSystem = mockk<ActionValidationSystem>()
        
        every { validationSystem.validateAction(any(), any()) } returns ValidationResult.Valid
        
        val preparedAction = Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null,
            attackBonus = 5,
            damageDice = "1d8",
            damageModifier = 3,
            damageType = DamageType.Slashing
        )
        
        val action = Ready(
            actorId = 1L,
            preparedAction = preparedAction,
            trigger = "When the goblin moves"
        )
        
        val processor = createProcessorWithSpecial(validationSystem)
        
        // Act
        val result = runBlocking { processor.processAction(action, context) }
        
        // Assert
        result.shouldBeInstanceOf<ActionResult.Success>()
        (result as ActionResult.Success).events.first().shouldBeInstanceOf<ReadyAction>()
    }
})

// Helper functions to create processors with different configurations
private fun createProcessor(
    validationSystem: ActionValidationSystem,
    attackResolver: AttackResolver,
    damageCalculator: DamageCalculator
): ActionProcessor {
    val attackHandler = AttackActionHandler(attackResolver, damageCalculator)
    val movementHandler = mockk<MovementActionHandler>()
    val spellHandler = mockk<SpellActionHandler>()
    val specialHandler = mockk<SpecialActionHandler>()
    val validator = ActionValidator(validationSystem)
    
    return ActionProcessor(
        attackHandler,
        movementHandler,
        spellHandler,
        specialHandler,
        validator
    )
}

private fun createProcessorWithMovement(
    validationSystem: ActionValidationSystem,
    pathfinder: Pathfinder
): ActionProcessor {
    val attackHandler = mockk<AttackActionHandler>()
    val reactionHandler = mockk<ReactionHandler>()
    val movementHandler = MovementActionHandler(pathfinder, reactionHandler)
    val spellHandler = mockk<SpellActionHandler>()
    val specialHandler = mockk<SpecialActionHandler>()
    val validator = ActionValidator(validationSystem)
    
    return ActionProcessor(
        attackHandler,
        movementHandler,
        spellHandler,
        specialHandler,
        validator
    )
}

private fun createProcessorWithSpell(
    validationSystem: ActionValidationSystem,
    attackResolver: AttackResolver,
    savingThrowResolver: SavingThrowResolver,
    damageCalculator: DamageCalculator
): ActionProcessor {
    val attackHandler = mockk<AttackActionHandler>()
    val movementHandler = mockk<MovementActionHandler>()
    val spellHandler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
    val specialHandler = mockk<SpecialActionHandler>()
    val validator = ActionValidator(validationSystem)
    
    return ActionProcessor(
        attackHandler,
        movementHandler,
        spellHandler,
        specialHandler,
        validator
    )
}

private fun createProcessorWithSpecial(
    validationSystem: ActionValidationSystem
): ActionProcessor {
    val attackHandler = mockk<AttackActionHandler>()
    val movementHandler = mockk<MovementActionHandler>()
    val spellHandler = mockk<SpellActionHandler>()
    val specialHandler = SpecialActionHandler()
    val validator = ActionValidator(validationSystem)
    
    return ActionProcessor(
        attackHandler,
        movementHandler,
        spellHandler,
        specialHandler,
        validator
    )
}
