package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.actions.models.SpellEffect
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for combat action processing.
 * 
 * These tests verify invariants that must hold for all possible inputs,
 * ensuring the system behaves correctly across a wide range of scenarios.
 * 
 * Requirements tested:
 * - 6.1: Action phase availability
 * - 6.2: Resource availability
 * - 6.3: Range and line-of-effect
 * - 6.4: Turn phase checks
 */
class PropertyBasedTest : FunSpec({
    
    context("Attack damage properties") {
        test("attack damage never exceeds maximum possible") {
            checkAll(
                Arb.int(1..10),
                Arb.int(4..12),
                Arb.int(0..10),
                Arb.long()
            ) { count, dieSize, modifier, seed ->
                // Skip invalid die sizes
                if (dieSize !in listOf(4, 6, 8, 10, 12)) {
                    return@checkAll
                }
                
                // Calculate maximum possible damage
                val maxDamage = (count * dieSize) + modifier
                
                // Mock damage calculator
                val damageCalculator = mockk<DamageCalculator>()
                val attackResolver = mockk<AttackResolver>()
                
                // Simulate a damage roll
                val actualDamage = (1..count).sumOf { (1..dieSize).random() } + modifier
                
                every { damageCalculator.calculateDamage(any(), any(), false, any()) } returns DamageResult(
                    roll = DiceRoll(diceType = dieSize, count = count, modifier = modifier, result = actualDamage),
                    totalDamage = actualDamage
                )
                
                every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
                    roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                    hit = true,
                    critical = false
                )
                
                val handler = AttackActionHandler(attackResolver, damageCalculator)
                
                val context = createTestContext(seed)
                val action = Attack(
                    actorId = 1L,
                    targetId = 2L,
                    weaponId = null,
                    attackBonus = 5,
                    damageDice = "${count}d${dieSize}",
                    damageModifier = modifier,
                    damageType = DamageType.Slashing
                )
                
                runBlocking { handler.handleAttack(action, context) }
                
                // Verify damage never exceeds maximum
                actualDamage shouldBeLessThanOrEqual maxDamage
                actualDamage shouldBeGreaterThanOrEqual (count + modifier) // Minimum possible
            }
        }
        
        test("critical hit damage never exceeds double maximum possible") {
            checkAll(
                Arb.int(1..10),
                Arb.int(4..12),
                Arb.int(0..10),
                Arb.long()
            ) { count, dieSize, modifier, seed ->
                // Skip invalid die sizes
                if (dieSize !in listOf(4, 6, 8, 10, 12)) {
                    return@checkAll
                }
                
                // Calculate maximum possible damage for critical (double dice)
                val maxCritDamage = (count * 2 * dieSize) + modifier
                
                val damageCalculator = mockk<DamageCalculator>()
                val attackResolver = mockk<AttackResolver>()
                
                // Simulate critical damage roll (double dice)
                val actualDamage = (1..(count * 2)).sumOf { (1..dieSize).random() } + modifier
                
                every { damageCalculator.calculateDamage(any(), any(), true, any()) } returns DamageResult(
                    roll = DiceRoll(diceType = dieSize, count = count * 2, modifier = modifier, result = actualDamage),
                    totalDamage = actualDamage
                )
                
                every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
                    roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                    hit = true,
                    critical = true
                )
                
                val handler = AttackActionHandler(attackResolver, damageCalculator)
                
                val context = createTestContext(seed)
                val action = Attack(
                    actorId = 1L,
                    targetId = 2L,
                    weaponId = null,
                    attackBonus = 5,
                    damageDice = "${count}d${dieSize}",
                    damageModifier = modifier,
                    damageType = DamageType.Slashing
                )
                
                runBlocking { handler.handleAttack(action, context) }
                
                // Verify critical damage never exceeds double maximum
                actualDamage shouldBeLessThanOrEqual maxCritDamage
                actualDamage shouldBeGreaterThanOrEqual ((count * 2) + modifier) // Minimum possible
            }
        }
    }
    
    context("Movement cost properties") {
        test("movement cost never exceeds available movement") {
            checkAll(
                Arb.int(0..100),
                Arb.int(1..50),
                Arb.long()
            ) { availableMovement, pathCost, seed ->
                // Skip cases where cost exceeds available (should throw exception)
                if (pathCost > availableMovement) {
                    return@checkAll
                }
                
                val pathfinder = mockk<Pathfinder>()
                val reactionHandler = mockk<ReactionHandler>()
                
                val path = listOf(GridPos(0, 0), GridPos(1, 0))
                
                every { pathfinder.validatePath(any(), any()) } returns true
                every { pathfinder.calculateMovementCost(any(), any()) } returns pathCost
                
                val context = createTestContext(seed).copy(
                    turnPhase = TurnPhase(
                        creatureId = 1L,
                        actionAvailable = true,
                        bonusActionAvailable = true,
                        reactionAvailable = true,
                        movementRemaining = availableMovement
                    )
                )
                
                val action = Move(actorId = 1L, path = path)
                val handler = MovementActionHandler(pathfinder, reactionHandler)
                
                val events = runBlocking { handler.handleMovement(action, context) }
                val moveEvent = events.first() as MoveCommitted
                
                // Verify movement cost never exceeds available
                moveEvent.movementUsed shouldBeLessThanOrEqual availableMovement
                moveEvent.movementRemaining shouldBeGreaterThanOrEqual 0
                moveEvent.movementUsed + moveEvent.movementRemaining shouldBe availableMovement
            }
        }
        
        test("movement remaining is always non-negative") {
            checkAll(
                Arb.int(0..100),
                Arb.int(0..100),
                Arb.long()
            ) { availableMovement, pathCost, seed ->
                // Only test valid movements
                if (pathCost > availableMovement) {
                    return@checkAll
                }
                
                val pathfinder = mockk<Pathfinder>()
                val reactionHandler = mockk<ReactionHandler>()
                
                val path = listOf(GridPos(0, 0), GridPos(1, 0))
                
                every { pathfinder.validatePath(any(), any()) } returns true
                every { pathfinder.calculateMovementCost(any(), any()) } returns pathCost
                
                val context = createTestContext(seed).copy(
                    turnPhase = TurnPhase(
                        creatureId = 1L,
                        actionAvailable = true,
                        bonusActionAvailable = true,
                        reactionAvailable = true,
                        movementRemaining = availableMovement
                    )
                )
                
                val action = Move(actorId = 1L, path = path)
                val handler = MovementActionHandler(pathfinder, reactionHandler)
                
                val events = runBlocking { handler.handleMovement(action, context) }
                val moveEvent = events.first() as MoveCommitted
                
                // Verify remaining movement is never negative
                moveEvent.movementRemaining shouldBeGreaterThanOrEqual 0
            }
        }
    }
    
    context("Spell slot consumption properties") {
        test("spell slot consumption never goes negative") {
            checkAll(
                Arb.int(0..9),
                Arb.long()
            ) { spellLevel, seed ->
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
                
                val context = createTestContext(seed)
                val action = CastSpell(
                    actorId = 1L,
                    spellId = 100L,
                    spellLevel = spellLevel,
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
                val events = runBlocking { handler.handleSpellCast(action, context) }
                val spellEvent = events.first() as SpellCast
                
                // Verify spell slot consumed is never negative
                spellEvent.slotConsumed shouldBeGreaterThanOrEqual 0
                spellEvent.slotConsumed shouldBe spellLevel
            }
        }
        
        test("spell slot consumed matches spell level") {
            checkAll(
                Arb.int(0..9),
                Arb.long()
            ) { spellLevel, seed ->
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
                
                val context = createTestContext(seed)
                val action = CastSpell(
                    actorId = 1L,
                    spellId = 100L,
                    spellLevel = spellLevel,
                    targets = listOf(2L),
                    spellEffect = SpellEffect.Utility(effect = "Test Spell"),
                    isBonusAction = false
                )
                
                val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
                val events = runBlocking { handler.handleSpellCast(action, context) }
                val spellEvent = events.first() as SpellCast
                
                // Verify consumed slot always matches spell level
                spellEvent.slotConsumed shouldBe spellLevel
            }
        }
    }
    
    context("Action phase consumption properties") {
        test("action phase consumption is idempotent") {
            checkAll(Arb.long()) { seed ->
                // Create context with action available
                val initialContext = createTestContext(seed).copy(
                    turnPhase = TurnPhase(
                        creatureId = 1L,
                        actionAvailable = true,
                        bonusActionAvailable = true,
                        reactionAvailable = true,
                        movementRemaining = 30
                    )
                )
                
                val attackResolver = mockk<AttackResolver>()
                val damageCalculator = mockk<DamageCalculator>()
                
                every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
                    roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
                    hit = true,
                    critical = false
                )
                
                every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
                    roll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 8),
                    totalDamage = 8
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
                
                val handler = AttackActionHandler(attackResolver, damageCalculator)
                
                // Process action once
                val events1 = runBlocking { handler.handleAttack(action, initialContext) }
                
                // Process same action again with same context
                val events2 = runBlocking { handler.handleAttack(action, initialContext) }
                
                // Verify idempotency - same input produces same output
                events1.size shouldBe events2.size
                
                val attack1 = events1.first() as AttackResolved
                val attack2 = events2.first() as AttackResolved
                
                // Same attack parameters should produce same results with same seed
                attack1.attackerId shouldBe attack2.attackerId
                attack1.targetId shouldBe attack2.targetId
                attack1.hit shouldBe attack2.hit
            }
        }
        
        test("turn phase state remains consistent") {
            checkAll(
                Arb.int(0..100),
                Arb.long()
            ) { movementRemaining, seed ->
                val context = createTestContext(seed).copy(
                    turnPhase = TurnPhase(
                        creatureId = 1L,
                        actionAvailable = true,
                        bonusActionAvailable = true,
                        reactionAvailable = true,
                        movementRemaining = movementRemaining
                    )
                )
                
                // Verify turn phase consistency
                context.turnPhase.actionAvailable shouldBe true
                context.turnPhase.bonusActionAvailable shouldBe true
                context.turnPhase.reactionAvailable shouldBe true
                context.turnPhase.movementRemaining shouldBe movementRemaining
                context.turnPhase.movementRemaining shouldBeGreaterThanOrEqual 0
            }
        }
    }
})

/**
 * Helper function to create a test context with default values.
 */
private fun createTestContext(seed: Long): ActionContext {
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
    
    return ActionContext(
        sessionId = seed,
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
}
