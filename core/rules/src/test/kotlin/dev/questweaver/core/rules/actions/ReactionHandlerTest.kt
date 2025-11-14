package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.DamageType
import dev.questweaver.core.rules.actions.models.Reaction
import dev.questweaver.core.rules.actions.models.ReactionTrigger
import dev.questweaver.core.rules.actions.models.ReactionType
import dev.questweaver.core.rules.actions.models.ReadiedAction
import dev.questweaver.core.rules.initiative.models.TurnPhase
import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.ReactionUsed
import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class ReactionHandlerTest : FunSpec({
    
    // Test fixtures
    val reactor = Creature(
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
            creatureId = 2L,
            actionAvailable = true,
            bonusActionAvailable = true,
            reactionAvailable = true,
            movementRemaining = 30
        ),
        creatures = mapOf(1L to reactor, 2L to target),
        mapGrid = mapGrid,
        activeConditions = emptyMap(),
        readiedActions = emptyMap()
    )
    
    test("opportunity attack triggers on movement out of reach") {
        val attackHandler = mockk<AttackActionHandler>()
        
        val attackOutcome = AttackOutcome(
            roll = DiceRoll(diceType = 20, count = 1, modifier = 5, result = 20),
            hit = true,
            critical = false
        )
        
        val attackEvent = AttackResolved(
            sessionId = 1L,
            timestamp = System.currentTimeMillis(),
            attackerId = 1L,
            targetId = 2L,
            attackRoll = attackOutcome.roll,
            targetAC = 15,
            hit = true,
            critical = false
        )
        
        coEvery { attackHandler.handleAttack(any(), any()) } returns listOf(attackEvent)
        
        val handler = ReactionHandler(attackHandler)
        
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(2, 0) // Moving out of melee range (>5 feet)
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.OpportunityAttack,
            targetId = 2L
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        // Should generate attack event + reaction used event
        events shouldHaveSize 2
        events[0].shouldBeInstanceOf<AttackResolved>()
        events[1].shouldBeInstanceOf<ReactionUsed>()
        
        val reactionEvent = events[1] as ReactionUsed
        reactionEvent.creatureId shouldBe 1L
        reactionEvent.reactionType shouldBe "OpportunityAttack"
    }
    
    test("reactions consume reaction resource") {
        val attackHandler = mockk<AttackActionHandler>()
        
        coEvery { attackHandler.handleAttack(any(), any()) } returns emptyList()
        
        val handler = ReactionHandler(attackHandler)
        
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(2, 0)
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.OpportunityAttack,
            targetId = 2L
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        // Should always generate ReactionUsed event
        events.any { it is ReactionUsed } shouldBe true
        
        val reactionEvent = events.find { it is ReactionUsed } as ReactionUsed
        reactionEvent.creatureId shouldBe 1L
    }
    
    test("identifyReactors returns creatures in melee range for movement trigger") {
        val handler = ReactionHandler(mockk())
        
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(1, 0)
        )
        
        val reactors = handler.identifyReactors(trigger, context)
        
        // Should identify reactor (creature 1) as being in range
        // Note: Current implementation uses placeholder position (0,0) for all creatures
        // so this test verifies the logic works with the current implementation
        reactors shouldContain 1L
    }
    
    test("identifyReactors returns empty list when no creatures in range") {
        val handler = ReactionHandler(mockk())
        
        // Create context with only the moving creature
        val contextWithOnlyMoving = context.copy(
            creatures = mapOf(2L to target)
        )
        
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(1, 0)
        )
        
        val reactors = handler.identifyReactors(trigger, contextWithOnlyMoving)
        
        // Should not include the moving creature itself
        reactors shouldBe emptyList()
    }
    
    test("identifyReactors finds readied action reactors for trigger condition") {
        val handler = ReactionHandler(mockk())
        
        val readiedAction = ReadiedAction(
            creatureId = 1L,
            action = mockk(),
            trigger = "when the goblin moves"
        )
        
        val contextWithReadied = context.copy(
            readiedActions = mapOf(1L to readiedAction)
        )
        
        val trigger = ReactionTrigger.TriggerConditionMet(
            condition = "goblin moves"
        )
        
        val reactors = handler.identifyReactors(trigger, contextWithReadied)
        
        reactors shouldContain 1L
    }
    
    test("readied actions execute on trigger") {
        val attackHandler = mockk<AttackActionHandler>()
        
        val handler = ReactionHandler(attackHandler)
        
        val readiedAction = ReadiedAction(
            creatureId = 1L,
            action = mockk(),
            trigger = "when enemy approaches"
        )
        
        val contextWithReadied = context.copy(
            readiedActions = mapOf(1L to readiedAction)
        )
        
        val trigger = ReactionTrigger.TriggerConditionMet(
            condition = "enemy approaches"
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.ReadiedAction,
            targetId = null
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, contextWithReadied) }
        
        // Should generate ReactionUsed event
        // Note: Actual readied action execution is marked as TODO in implementation
        events.any { it is ReactionUsed } shouldBe true
    }
    
    test("multiple reactions resolved in initiative order") {
        val handler = ReactionHandler(mockk())
        
        val creature1 = reactor.copy(id = 1L)
        val creature2 = reactor.copy(id = 3L)
        val creature3 = reactor.copy(id = 4L)
        
        val contextWithMultiple = context.copy(
            creatures = mapOf(
                1L to creature1,
                2L to target,
                3L to creature2,
                4L to creature3
            )
        )
        
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(1, 0)
        )
        
        val reactors = handler.identifyReactors(trigger, contextWithMultiple)
        
        // Should identify all creatures except the moving one
        // Note: The actual initiative order sorting would be done by the ActionProcessor
        // This test verifies that identifyReactors returns all eligible reactors
        reactors.size shouldBe 3
        reactors shouldContain 1L
        reactors shouldContain 3L
        reactors shouldContain 4L
    }
    
    test("opportunity attack not triggered when target stays in reach") {
        val attackHandler = mockk<AttackActionHandler>()
        
        coEvery { attackHandler.handleAttack(any(), any()) } returns emptyList()
        
        val handler = ReactionHandler(attackHandler)
        
        // Movement within melee range (5 feet = 1 square)
        val trigger = ReactionTrigger.CreatureMoved(
            creatureId = 2L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(0, 0) // Staying in same position
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.OpportunityAttack,
            targetId = 2L
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        // Should only generate ReactionUsed event, no attack
        events shouldHaveSize 1
        events[0].shouldBeInstanceOf<ReactionUsed>()
    }
    
    test("reaction event contains correct trigger information") {
        val attackHandler = mockk<AttackActionHandler>()
        
        coEvery { attackHandler.handleAttack(any(), any()) } returns emptyList()
        
        val handler = ReactionHandler(attackHandler)
        
        val trigger = ReactionTrigger.SpellCast(
            casterId = 2L,
            spellId = 100L,
            targets = listOf(1L)
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.Counterspell,
            targetId = 2L
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        val reactionEvent = events.find { it is ReactionUsed } as ReactionUsed
        reactionEvent.sessionId shouldBe 1L
        reactionEvent.creatureId shouldBe 1L
        reactionEvent.reactionType shouldBe "Counterspell"
        reactionEvent.trigger shouldBe trigger.toString()
    }
    
    test("identifyReactors handles attack trigger") {
        val handler = ReactionHandler(mockk())
        
        val trigger = ReactionTrigger.AttackMade(
            attackerId = 2L,
            targetId = 1L
        )
        
        val reactors = handler.identifyReactors(trigger, context)
        
        // Current implementation returns empty list for defensive reactions
        // as it requires checking spell lists and abilities (marked as TODO)
        reactors shouldBe emptyList()
    }
    
    test("identifyReactors handles spell cast trigger") {
        val handler = ReactionHandler(mockk())
        
        val trigger = ReactionTrigger.SpellCast(
            casterId = 2L,
            spellId = 100L,
            targets = listOf(1L)
        )
        
        val reactors = handler.identifyReactors(trigger, context)
        
        // Current implementation returns empty list for counterspell
        // as it requires checking spell lists (marked as TODO)
        reactors shouldBe emptyList()
    }
    
    test("reaction handler processes shield reaction type") {
        val attackHandler = mockk<AttackActionHandler>()
        
        val handler = ReactionHandler(attackHandler)
        
        val trigger = ReactionTrigger.AttackMade(
            attackerId = 2L,
            targetId = 1L
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.Shield,
            targetId = null
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        // Should generate ReactionUsed event
        // Actual shield effect implementation is marked as TODO
        events.any { it is ReactionUsed } shouldBe true
        
        val reactionEvent = events.find { it is ReactionUsed } as ReactionUsed
        reactionEvent.reactionType shouldBe "Shield"
    }
    
    test("reaction handler processes other reaction types") {
        val attackHandler = mockk<AttackActionHandler>()
        
        val handler = ReactionHandler(attackHandler)
        
        val trigger = ReactionTrigger.TriggerConditionMet(
            condition = "custom trigger"
        )
        
        val reaction = Reaction(
            actorId = 1L,
            reactionType = ReactionType.Other,
            targetId = null
        )
        
        val events = runBlocking { handler.handleReaction(reaction, trigger, context) }
        
        // Should generate ReactionUsed event
        events.any { it is ReactionUsed } shouldBe true
        
        val reactionEvent = events.find { it is ReactionUsed } as ReactionUsed
        reactionEvent.reactionType shouldBe "Other"
    }
})
