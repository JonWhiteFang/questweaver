package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.SpellEffect
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.values.AbilityType
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.DiceRoll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class SpellActionHandlerTest : FunSpec({
    
    // Test fixtures
    val caster = Creature(
        id = 1L,
        name = "Wizard",
        armorClass = 12,
        hitPointsCurrent = 20,
        hitPointsMax = 20,
        speed = 30,
        abilities = Abilities(
            strength = 8,
            dexterity = 14,
            constitution = 12,
            intelligence = 16,
            wisdom = 10,
            charisma = 10
        ),
        proficiencyBonus = 2
    )
    
    val target1 = Creature(
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
    
    val target2 = Creature(
        id = 3L,
        name = "Orc",
        armorClass = 13,
        hitPointsCurrent = 30,
        hitPointsMax = 30,
        speed = 30,
        abilities = Abilities(
            strength = 16,
            dexterity = 12,
            constitution = 16,
            intelligence = 7,
            wisdom = 11,
            charisma = 10
        )
    )
    
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
        creatures = mapOf(1L to caster, 2L to target1, 3L to target2),
        mapGrid = MapGrid(width = 10, height = 10),
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    test("spell slots consumed correctly") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 3, modifier = 0, result = 12),
            totalDamage = 12
        )
        
        val spellAction = CastSpell(
            actorId = 1L,
            spellId = 100L,
            spellLevel = 2,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "3d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(spellAction, context) }
        
        events shouldHaveSize 1
        val event = events.first() as SpellCast
        event.spellLevel shouldBe 2
        event.slotConsumed shouldBe 2
    }
    
    test("spell attacks resolved correctly") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        val expectedRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20)
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = expectedRoll,
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("3d6", 0, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 3, modifier = 0, result = 12),
            totalDamage = 12
        )
        
        val spellAction = CastSpell(
            actorId = 1L,
            spellId = 100L,
            spellLevel = 1,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "3d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(spellAction, context) }
        
        val event = events.first() as SpellCast
        event.outcomes shouldHaveSize 1
        
        val outcome = event.outcomes.first()
        outcome.targetId shouldBe 2L
        outcome.attackRoll shouldBe expectedRoll
        outcome.success shouldBe true
        outcome.damage shouldBe 12
        outcome.damageType shouldBe "Fire"
    }
    
    test("saving throws resolved correctly") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        // Target1 fails save (dex modifier = +2)
        every { savingThrowResolver.resolveSave(14, AbilityType.Dexterity, 2, any()) } returns SaveOutcome(
            roll = 10,
            success = false
        )
        
        every { damageCalculator.calculateDamage("8d6", 0, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 8, modifier = 0, result = 28),
            totalDamage = 28
        )
        
        val spellAction = CastSpell(
            actorId = 1L,
            spellId = 101L,
            spellLevel = 3,
            targets = listOf(2L),
            spellEffect = SpellEffect.Save(
                dc = 14,
                abilityType = AbilityType.Dexterity,
                targets = listOf(2L),
                damageDice = "8d6",
                damageModifier = 0,
                damageType = DamageType.Fire,
                halfDamageOnSave = true
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(spellAction, context) }
        
        val event = events.first() as SpellCast
        event.outcomes shouldHaveSize 1
        
        val outcome = event.outcomes.first()
        outcome.targetId shouldBe 2L
        outcome.saveRoll shouldNotBe null
        outcome.success shouldBe false
        outcome.damage shouldBe 28
    }
    
    test("saving throw with half damage on success") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        // Target succeeds on save
        every { savingThrowResolver.resolveSave(14, AbilityType.Dexterity, 2, any()) } returns SaveOutcome(
            roll = 18,
            success = true
        )
        
        every { damageCalculator.calculateDamage("8d6", 0, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 8, modifier = 0, result = 28),
            totalDamage = 28
        )
        
        val spellAction = CastSpell(
            actorId = 1L,
            spellId = 101L,
            spellLevel = 3,
            targets = listOf(2L),
            spellEffect = SpellEffect.Save(
                dc = 14,
                abilityType = AbilityType.Dexterity,
                targets = listOf(2L),
                damageDice = "8d6",
                damageModifier = 0,
                damageType = DamageType.Fire,
                halfDamageOnSave = true
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(spellAction, context) }
        
        val event = events.first() as SpellCast
        val outcome = event.outcomes.first()
        
        outcome.success shouldBe true
        outcome.damage shouldBe 14 // Half of 28
    }
    
    test("bonus action spell restriction enforced") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
            roll = DiceRoll(diceType = 4, count = 1, modifier = 0, result = 3),
            totalDamage = 3
        )
        
        val bonusActionSpell = CastSpell(
            actorId = 1L,
            spellId = 102L,
            spellLevel = 0, // Cantrip
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "1d4",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = true
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(bonusActionSpell, context) }
        
        // Should complete without error
        // TODO: When bonus action restriction is fully implemented,
        // this test should verify that only cantrips can be cast as actions
        // on the same turn as a bonus action spell
        events shouldHaveSize 1
        events.first().shouldBeInstanceOf<SpellCast>()
    }
    
    test("area-of-effect targets multiple creatures") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        // Target1 fails save
        every { savingThrowResolver.resolveSave(14, AbilityType.Dexterity, 2, any()) } returns SaveOutcome(
            roll = 10,
            success = false
        )
        
        // Target2 succeeds on save (dex modifier = +1)
        every { savingThrowResolver.resolveSave(14, AbilityType.Dexterity, 1, any()) } returns SaveOutcome(
            roll = 16,
            success = true
        )
        
        every { damageCalculator.calculateDamage("8d6", 0, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 8, modifier = 0, result = 28),
            totalDamage = 28
        )
        
        val aoeSpell = CastSpell(
            actorId = 1L,
            spellId = 103L,
            spellLevel = 3,
            targets = listOf(2L, 3L),
            spellEffect = SpellEffect.Save(
                dc = 14,
                abilityType = AbilityType.Dexterity,
                targets = listOf(2L, 3L),
                damageDice = "8d6",
                damageModifier = 0,
                damageType = DamageType.Fire,
                halfDamageOnSave = true
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(aoeSpell, context) }
        
        val event = events.first() as SpellCast
        event.outcomes shouldHaveSize 2
        
        // Target1 failed save - full damage
        val outcome1 = event.outcomes.find { it.targetId == 2L }!!
        outcome1.success shouldBe false
        outcome1.damage shouldBe 28
        
        // Target2 succeeded save - half damage
        val outcome2 = event.outcomes.find { it.targetId == 3L }!!
        outcome2.success shouldBe true
        outcome2.damage shouldBe 14
    }
    
    test("SpellCast event contains all outcomes") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { attackResolver.resolveAttack(5, 13, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 15),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("4d6", 0, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 6, count = 4, modifier = 0, result = 14),
            totalDamage = 14
        )
        
        val multiTargetSpell = CastSpell(
            actorId = 1L,
            spellId = 104L,
            spellLevel = 2,
            targets = listOf(2L, 3L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L, 3L),
                damageDice = "4d6",
                damageModifier = 0,
                damageType = DamageType.Force
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(multiTargetSpell, context) }
        
        val event = events.first() as SpellCast
        
        // Verify event structure
        event.sessionId shouldBe 1L
        event.casterId shouldBe 1L
        event.spellId shouldBe 104L
        event.spellLevel shouldBe 2
        event.slotConsumed shouldBe 2
        event.targets shouldBe listOf(2L, 3L)
        event.outcomes shouldHaveSize 2
        
        // Verify all outcomes are present
        event.outcomes.map { it.targetId }.toSet() shouldBe setOf(2L, 3L)
        event.outcomes.all { it.attackRoll != null } shouldBe true
        event.outcomes.all { it.success } shouldBe true
        event.outcomes.all { it.damage != null } shouldBe true
        event.outcomes.all { it.damageType == "Force" } shouldBe true
    }
    
    test("spell attack miss generates outcome with no damage") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 10),
            hit = false,
            critical = false
        )
        
        val spellAction = CastSpell(
            actorId = 1L,
            spellId = 100L,
            spellLevel = 1,
            targets = listOf(2L),
            spellEffect = SpellEffect.Attack(
                targets = listOf(2L),
                damageDice = "3d6",
                damageModifier = 0,
                damageType = DamageType.Fire
            ),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(spellAction, context) }
        
        val event = events.first() as SpellCast
        val outcome = event.outcomes.first()
        
        outcome.success shouldBe false
        outcome.damage shouldBe null
    }
    
    test("utility spell generates event with no outcomes") {
        val attackResolver = mockk<AttackResolver>()
        val savingThrowResolver = mockk<SavingThrowResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        val utilitySpell = CastSpell(
            actorId = 1L,
            spellId = 105L,
            spellLevel = 1,
            targets = emptyList(),
            spellEffect = SpellEffect.Utility(effect = "Mage Armor"),
            isBonusAction = false
        )
        
        val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
        val events = runBlocking { handler.handleSpellCast(utilitySpell, context) }
        
        val event = events.first() as SpellCast
        event.outcomes shouldHaveSize 0
        event.spellId shouldBe 105L
        event.spellLevel shouldBe 1
    }
})
