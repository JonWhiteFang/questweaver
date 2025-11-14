package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.CreatureDefeated
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.DiceRoll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class AttackActionHandlerTest : FunSpec({
    
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
        mapGrid = MapGrid(width = 10, height = 10),
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    val attackAction = Attack(
        actorId = 1L,
        targetId = 2L,
        weaponId = null,
        attackBonus = 5,
        damageDice = "1d8",
        damageModifier = 3,
        damageType = DamageType.Slashing
    )
    
    test("attack hits when roll meets or exceeds AC") {
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 9),
            totalDamage = 9
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleAttack(attackAction, context) }
        
        events shouldHaveSize 1
        val event = events.first()
        event.shouldBeInstanceOf<AttackResolved>()
        event.hit shouldBe true
        event.critical shouldBe false
    }
    
    test("attack misses when roll is below AC") {
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 13),
            hit = false,
            critical = false
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleAttack(attackAction, context) }
        
        events shouldHaveSize 1
        val event = events.first()
        event.shouldBeInstanceOf<AttackResolved>()
        event.hit shouldBe false
    }
    
    test("critical hit is recorded in event") {
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 25),
            hit = true,
            critical = true
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, true, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 2, modifier = 3, result = 19),
            totalDamage = 19
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleAttack(attackAction, context) }
        
        events shouldHaveSize 1
        val event = events.first()
        event.shouldBeInstanceOf<AttackResolved>()
        event.critical shouldBe true
    }
    
    test("CreatureDefeated event generated when target HP reaches 0") {
        val lowHPTarget = target.copy(hitPointsCurrent = 5)
        val contextWithLowHP = context.copy(
            creatures = mapOf(1L to attacker, 2L to lowHPTarget)
        )
        
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 8),
            totalDamage = 8
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleAttack(attackAction, contextWithLowHP) }
        
        events shouldHaveSize 2
        events[0].shouldBeInstanceOf<AttackResolved>()
        events[1].shouldBeInstanceOf<CreatureDefeated>()
        
        val defeatedEvent = events[1] as CreatureDefeated
        defeatedEvent.creatureId shouldBe 2L
        defeatedEvent.defeatedBy shouldBe 1L
    }
    
    test("multiple attacks processed correctly") {
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 8),
            totalDamage = 8
        )
        
        val attacks = listOf(
            attackAction,
            attackAction.copy(actorId = 1L)
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleMultiAttack(attacks, context) }
        
        events shouldHaveSize 2
        events.all { it is AttackResolved } shouldBe true
    }
    
    test("attack event contains all required information") {
        val attackResolver = mockk<AttackResolver>()
        val damageCalculator = mockk<DamageCalculator>()
        
        val expectedRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20)
        
        every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
            roll = expectedRoll,
            hit = true,
            critical = false
        )
        
        every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
            roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 9),
            totalDamage = 9
        )
        
        val handler = AttackActionHandler(attackResolver, damageCalculator)
        val events = runBlocking { handler.handleAttack(attackAction, context) }
        
        val event = events.first() as AttackResolved
        event.sessionId shouldBe 1L
        event.attackerId shouldBe 1L
        event.targetId shouldBe 2L
        event.attackRoll shouldBe expectedRoll
        event.targetAC shouldBe 15
        event.hit shouldBe true
    }
})
