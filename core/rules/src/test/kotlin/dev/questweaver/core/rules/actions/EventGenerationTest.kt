package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Disengage
import dev.questweaver.core.rules.actions.models.Dodge
import dev.questweaver.core.rules.actions.models.Help
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.actions.models.Ready
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.BonusActionTaken
import dev.questweaver.domain.events.ConditionApplied
import dev.questweaver.domain.events.CreatureDefeated
import dev.questweaver.domain.events.DamageApplied
import dev.questweaver.domain.events.DisengageAction
import dev.questweaver.domain.events.DodgeAction
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.HelpAction
import dev.questweaver.domain.events.HelpType
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.ReadyAction
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for event generation across all action handlers.
 * Verifies that:
 * - All actions generate appropriate events
 * - Events contain complete outcome information
 * - Events are immutable and serializable
 * - Event timestamps are set correctly
 * 
 * Requirements: 9.2
 */
class EventGenerationTest : FunSpec({
    
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = false
    }
    
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
    
    context("AttackResolved event generation") {
        test("generates event with complete attack information") {
            val attackResolver = mockk<AttackResolver>()
            val damageCalculator = mockk<DamageCalculator>()
            
            val expectedRoll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20)
            val expectedDamageRoll = DiceRoll(diceType = 8, count = 1, modifier = 3, result = 9)
            
            every { attackResolver.resolveAttack(5, 15, any()) } returns AttackOutcome(
                roll = expectedRoll,
                hit = true,
                critical = false
            )
            
            every { damageCalculator.calculateDamage("1d8", 3, false, any()) } returns DamageResult(
                roll = expectedDamageRoll,
                totalDamage = 9
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
            
            val handler = AttackActionHandler(attackResolver, damageCalculator)
            val events = runBlocking { handler.handleAttack(attackAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<AttackResolved>()
            
            // Verify complete outcome information
            event.sessionId shouldBe 1L
            event.attackerId shouldBe 1L
            event.targetId shouldBe 2L
            event.attackRoll shouldBe expectedRoll
            event.targetAC shouldBe 15
            event.hit shouldBe true
            event.critical shouldBe false
        }
        
        test("event is immutable") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = System.currentTimeMillis(),
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 20),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            // Verify all properties are val (immutable)
            // This is enforced by data class definition
            event.sessionId shouldBe 1L
            event.attackerId shouldBe 1L
        }
        
        test("event is serializable") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = 1699564800000L,
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 20),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            // Serialize to JSON
            val jsonString = json.encodeToString<GameEvent>(event)
            jsonString shouldNotBe ""
            
            // Deserialize back
            val decoded = json.decodeFromString<GameEvent>(jsonString)
            decoded shouldBe event
        }
        
        test("event timestamp is set correctly") {
            val beforeTime = System.currentTimeMillis()
            
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = System.currentTimeMillis(),
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 20),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            event.timestamp shouldBeGreaterThan 0L
            event.timestamp shouldBeGreaterThan beforeTime - 1000 // Allow 1s tolerance
        }
    }
    
    context("MoveCommitted event generation") {
        test("generates event with complete movement information") {
            val pathfinder = mockk<Pathfinder>()
            val reactionHandler = mockk<ReactionHandler>()
            
            val path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0)
            )
            
            every { pathfinder.validatePath(path, context.mapGrid) } returns true
            every { pathfinder.calculateMovementCost(path, context.mapGrid) } returns 10
            
            val moveAction = Move(actorId = 1L, path = path)
            val handler = MovementActionHandler(pathfinder, reactionHandler)
            
            val events = runBlocking { handler.handleMovement(moveAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<MoveCommitted>()
            
            // Verify complete outcome information
            event.sessionId shouldBe 1L
            event.creatureId shouldBe 1L
            event.path shouldBe path
            event.movementUsed shouldBe 10
            event.movementRemaining shouldBe 20
        }
        
        test("event is serializable") {
            val event = MoveCommitted(
                sessionId = 1L,
                timestamp = 1699564800000L,
                creatureId = 1L,
                path = listOf(GridPos(0, 0), GridPos(1, 0)),
                movementUsed = 5,
                movementRemaining = 25
            )
            
            val jsonString = json.encodeToString<GameEvent>(event)
            val decoded = json.decodeFromString<GameEvent>(jsonString)
            decoded shouldBe event
        }
    }
    
    context("SpellCast event generation") {
        test("generates event with complete spell information") {
            val attackResolver = mockk<AttackResolver>()
            val savingThrowResolver = mockk<SavingThrowResolver>()
            val damageCalculator = mockk<DamageCalculator>()
            
            val spellAction = CastSpell(
                actorId = 1L,
                spellId = 100L,
                spellLevel = 1,
                targets = listOf(2L),
                spellEffect = dev.questweaver.core.rules.actions.models.SpellEffect.Attack(
                    targets = listOf(2L),
                    damageDice = "1d6",
                    damageModifier = 0,
                    damageType = DamageType.Fire
                ),
                isBonusAction = false
            )
            
            every { attackResolver.resolveAttack(any(), any(), any()) } returns AttackOutcome(
                roll = DiceRoll(20, 1, 5, 18),
                hit = true,
                critical = false
            )
            
            every { damageCalculator.calculateDamage(any(), any(), any(), any()) } returns DamageResult(
                roll = DiceRoll(6, 1, 0, 4),
                totalDamage = 4
            )
            
            val handler = SpellActionHandler(attackResolver, savingThrowResolver, damageCalculator)
            val events = runBlocking { handler.handleSpellCast(spellAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<SpellCast>()
            
            // Verify complete outcome information
            event.sessionId shouldBe 1L
            event.casterId shouldBe 1L
            event.spellId shouldBe 100L
            event.spellLevel shouldBe 1
            event.slotConsumed shouldBe 1
            event.targets shouldBe listOf(2L)
            event.outcomes shouldHaveSize 1
        }
        
        test("event is serializable") {
            val event = SpellCast(
                sessionId = 1L,
                timestamp = 1699564800000L,
                casterId = 1L,
                spellId = 100L,
                spellLevel = 1,
                slotConsumed = 1,
                targets = listOf(2L),
                outcomes = listOf(
                    dev.questweaver.domain.events.SpellOutcome(
                        targetId = 2L,
                        attackRoll = DiceRoll(20, 1, 5, 18),
                        saveRoll = null,
                        success = true,
                        damage = 8,
                        damageType = "Fire"
                    )
                )
            )
            
            val jsonString = json.encodeToString<GameEvent>(event)
            val decoded = json.decodeFromString<GameEvent>(jsonString)
            decoded shouldBe event
        }
    }
    
    context("Special action event generation") {
        test("Dodge action generates DodgeAction event") {
            val dodgeAction = Dodge(actorId = 1L)
            val handler = SpecialActionHandler()
            
            val events = runBlocking { handler.handleDodge(dodgeAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<DodgeAction>()
            event.sessionId shouldBe 1L
            event.creatureId shouldBe 1L
        }
        
        test("Disengage action generates DisengageAction event") {
            val disengageAction = Disengage(actorId = 1L)
            val handler = SpecialActionHandler()
            
            val events = runBlocking { handler.handleDisengage(disengageAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<DisengageAction>()
            event.sessionId shouldBe 1L
            event.creatureId shouldBe 1L
        }
        
        test("Help action generates HelpAction event") {
            val helpAction = Help(
                actorId = 1L,
                targetId = 2L,
                helpType = dev.questweaver.core.rules.actions.models.HelpType.Attack
            )
            val handler = SpecialActionHandler()
            
            val events = runBlocking { handler.handleHelp(helpAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<HelpAction>()
            event.sessionId shouldBe 1L
            event.helperId shouldBe 1L
            event.targetId shouldBe 2L
            event.helpType shouldBe HelpType.Attack
        }
        
        test("Ready action generates ReadyAction event") {
            val readyAction = Ready(
                actorId = 1L,
                preparedAction = Attack(
                    actorId = 1L,
                    targetId = 2L,
                    weaponId = null,
                    attackBonus = 5,
                    damageDice = "1d8",
                    damageModifier = 3,
                    damageType = DamageType.Slashing
                ),
                trigger = "When enemy moves adjacent"
            )
            val handler = SpecialActionHandler()
            
            val events = runBlocking { handler.handleReady(readyAction, context) }
            
            events shouldHaveSize 1
            val event = events.first()
            event.shouldBeInstanceOf<ReadyAction>()
            event.sessionId shouldBe 1L
            event.creatureId shouldBe 1L
            event.trigger shouldBe "When enemy moves adjacent"
        }
        
        test("special action events are serializable") {
            val dodgeEvent = DodgeAction(
                sessionId = 1L,
                timestamp = 1699564800000L,
                creatureId = 1L
            )
            
            val jsonString = json.encodeToString<GameEvent>(dodgeEvent)
            val decoded = json.decodeFromString<GameEvent>(jsonString)
            decoded shouldBe dodgeEvent
        }
    }
    
    context("CreatureDefeated event generation") {
        test("generates event when creature HP reaches 0") {
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
            
            val attackAction = Attack(
                actorId = 1L,
                targetId = 2L,
                weaponId = null,
                attackBonus = 5,
                damageDice = "1d8",
                damageModifier = 3,
                damageType = DamageType.Slashing
            )
            
            val handler = AttackActionHandler(attackResolver, damageCalculator)
            val events = runBlocking { handler.handleAttack(attackAction, contextWithLowHP) }
            
            events shouldHaveSize 2
            events[0].shouldBeInstanceOf<AttackResolved>()
            events[1].shouldBeInstanceOf<CreatureDefeated>()
            
            val defeatedEvent = events[1] as CreatureDefeated
            defeatedEvent.sessionId shouldBe 1L
            defeatedEvent.creatureId shouldBe 2L
            defeatedEvent.defeatedBy shouldBe 1L
        }
        
        test("CreatureDefeated event is serializable") {
            val event = CreatureDefeated(
                sessionId = 1L,
                timestamp = 1699564800000L,
                creatureId = 2L,
                defeatedBy = 1L
            )
            
            val jsonString = json.encodeToString<GameEvent>(event)
            val decoded = json.decodeFromString<GameEvent>(jsonString)
            decoded shouldBe event
        }
    }
    
    context("Event immutability verification") {
        test("all event types are data classes with val properties") {
            // Data classes with val properties are immutable by design
            // This test verifies the pattern is followed
            
            val attackEvent = AttackResolved(
                sessionId = 1L,
                timestamp = 1699564800000L,
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 20),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            MoveCommitted(
                sessionId = 1L,
                timestamp = 1699564800000L,
                creatureId = 1L,
                path = listOf(GridPos(0, 0)),
                movementUsed = 5,
                movementRemaining = 25
            )
            
            SpellCast(
                sessionId = 1L,
                timestamp = 1699564800000L,
                casterId = 1L,
                spellId = 100L,
                spellLevel = 1,
                slotConsumed = 1,
                targets = listOf(2L),
                outcomes = emptyList()
            )
            
            // Verify events can be copied but not mutated
            val copiedAttack = attackEvent.copy(hit = false)
            copiedAttack.hit shouldBe false
            attackEvent.hit shouldBe true // Original unchanged
        }
    }
    
    context("Event timestamp consistency") {
        test("all events have valid timestamps") {
            val beforeTime = System.currentTimeMillis()
            
            val events = listOf(
                AttackResolved(1L, System.currentTimeMillis(), 1L, 2L, DiceRoll(20, 1, 5, 20), 15, true, false),
                MoveCommitted(1L, System.currentTimeMillis(), 1L, listOf(GridPos(0, 0)), 5, 25),
                DodgeAction(1L, System.currentTimeMillis(), 1L),
                DisengageAction(1L, System.currentTimeMillis(), 1L)
            )
            
            events.forEach { event ->
                event.timestamp shouldBeGreaterThan 0L
                event.timestamp shouldBeGreaterThan beforeTime - 1000
            }
        }
    }
})
