package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.ActionType
import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import dev.questweaver.domain.dice.DiceRoller
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll

/**
 * Property-based tests for initiative system invariants.
 * 
 * Tests that critical invariants hold across all possible states:
 * - Turn index always within bounds
 * - Active creature always exists in order
 * - Round number never decreases
 * - Movement remaining never negative
 * - Actions can only be consumed once per turn
 */
class InitiativeInvariantPropertyTest : FunSpec({
    
    test("turn index is always within bounds of initiative order") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 2..10)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            // Create creatures with random modifiers
            val creatures = creatureIds.associateWith { 
                (it % 10).toInt() - 5 // Modifiers from -5 to +4
            }
            
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            val state = (initResult as InitiativeResult.Success<RoundState>).value
            
            // Verify initial state
            val currentTurn = state.currentTurn!!
            currentTurn.turnIndex shouldBeInRange (0 until state.initiativeOrder.size)
            
            // Advance through multiple rounds
            var currentState = state
            repeat(state.initiativeOrder.size * 3) {
                val advanceResult = tracker.advanceTurn(currentState)
                advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                currentState = (advanceResult as InitiativeResult.Success<RoundState>).value
                val turn = currentState.currentTurn!!
                turn.turnIndex shouldBeInRange (0 until currentState.initiativeOrder.size)
            }
        }
    }
    
    test("active creature always exists in initiative order") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 2..10)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            val creatures = creatureIds.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            val state = (initResult as InitiativeResult.Success<RoundState>).value
            
            // Verify initial state
            val initialTurn = state.currentTurn!!
            val activeCreature = state.initiativeOrder[initialTurn.turnIndex]
            activeCreature.creatureId shouldBe initialTurn.activeCreatureId
            
            // Advance through multiple turns
            var currentState = state
            repeat(state.initiativeOrder.size * 2) {
                val advanceResult = tracker.advanceTurn(currentState)
                advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                currentState = (advanceResult as InitiativeResult.Success<RoundState>).value
                val turn = currentState.currentTurn!!
                val active = currentState.initiativeOrder[turn.turnIndex]
                active.creatureId shouldBe turn.activeCreatureId
            }
        }
    }
    
    test("round number never decreases") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 2..10)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            val creatures = creatureIds.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            
            var currentState = (initResult as InitiativeResult.Success<RoundState>).value
            var previousRound = currentState.roundNumber
            
            // Advance through multiple rounds
            repeat(currentState.initiativeOrder.size * 5) {
                val advanceResult = tracker.advanceTurn(currentState)
                advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                currentState = (advanceResult as InitiativeResult.Success<RoundState>).value
                currentState.roundNumber shouldBeGreaterThanOrEqualTo previousRound
                previousRound = currentState.roundNumber
            }
        }
    }

    
    test("movement remaining never goes negative") {
        checkAll(
            Arb.int(30..60), // Base movement speed
            Arb.list(Arb.int(1..10), 1..20) // Movement consumption amounts
        ) { baseMovement: Int, consumptions: List<Int> ->
            val phaseManager = TurnPhaseManager()
            var phase = phaseManager.startTurn(1L, baseMovement)
            
            // Consume movement multiple times
            consumptions.forEach { amount ->
                phase = phaseManager.consumeMovement(phase, amount)
                phase.movementRemaining shouldBeGreaterThanOrEqualTo 0
            }
        }
    }
    
    test("action can only be consumed once per turn") {
        checkAll(Arb.int(30..60)) { baseMovement: Int ->
            val phaseManager = TurnPhaseManager()
            val phase = phaseManager.startTurn(1L, baseMovement)
            
            // Action available at start
            phase.actionAvailable.shouldBeTrue()
            phaseManager.isActionAvailable(phase, ActionType.Action).shouldBeTrue()
            
            // Consume action
            val afterAction = phaseManager.consumeAction(phase)
            afterAction.actionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterAction, ActionType.Action).shouldBeFalse()
            
            // Consuming again should have no effect (still false)
            val afterSecond = phaseManager.consumeAction(afterAction)
            afterSecond.actionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterSecond, ActionType.Action).shouldBeFalse()
        }
    }
    
    test("bonus action can only be consumed once per turn") {
        checkAll(Arb.int(30..60)) { baseMovement: Int ->
            val phaseManager = TurnPhaseManager()
            val phase = phaseManager.startTurn(1L, baseMovement)
            
            // Bonus action available at start
            phase.bonusActionAvailable.shouldBeTrue()
            phaseManager.isActionAvailable(phase, ActionType.BonusAction).shouldBeTrue()
            
            // Consume bonus action
            val afterBonus = phaseManager.consumeBonusAction(phase)
            afterBonus.bonusActionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterBonus, ActionType.BonusAction).shouldBeFalse()
            
            // Consuming again should have no effect
            val afterSecond = phaseManager.consumeBonusAction(afterBonus)
            afterSecond.bonusActionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterSecond, ActionType.BonusAction).shouldBeFalse()
        }
    }
    
    test("reaction can only be consumed once per turn") {
        checkAll(Arb.int(30..60)) { baseMovement: Int ->
            val phaseManager = TurnPhaseManager()
            val phase = phaseManager.startTurn(1L, baseMovement)
            
            // Reaction available at start
            phase.reactionAvailable.shouldBeTrue()
            phaseManager.isActionAvailable(phase, ActionType.Reaction).shouldBeTrue()
            
            // Consume reaction
            val afterReaction = phaseManager.consumeReaction(phase)
            afterReaction.reactionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterReaction, ActionType.Reaction).shouldBeFalse()
            
            // Consuming again should have no effect
            val afterSecond = phaseManager.consumeReaction(afterReaction)
            afterSecond.reactionAvailable.shouldBeFalse()
            phaseManager.isActionAvailable(afterSecond, ActionType.Reaction).shouldBeFalse()
        }
    }
    
    test("turn index remains valid after adding creatures") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 3..8),
            Arb.list(Arb.long(101L..200L), 1..5)
        ) { seed: Long, initialCreatures: Set<Long>, newCreatures: List<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            // Initialize with initial creatures
            val creatures = initialCreatures.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            var state = (initResult as InitiativeResult.Success<RoundState>).value
            
            // Add new creatures one by one
            newCreatures.forEach { creatureId ->
                val newEntry = initiativeRoller.rollInitiative(creatureId, (creatureId % 10).toInt())
                val addResult = tracker.addCreature(state, newEntry)
                addResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                state = (addResult as InitiativeResult.Success<RoundState>).value
                
                // Verify invariants after each addition
                val turn = state.currentTurn!!
                turn.turnIndex shouldBeInRange (0 until state.initiativeOrder.size)
                val activeCreature = state.initiativeOrder[turn.turnIndex]
                activeCreature.creatureId shouldBe turn.activeCreatureId
            }
        }
    }
    
    test("turn index remains valid after removing creatures") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 5..10)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            val creatures = creatureIds.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            var state = (initResult as InitiativeResult.Success<RoundState>).value
            
            // Remove creatures one by one (but keep at least 1)
            val toRemove = creatureIds.take(creatureIds.size - 1)
            toRemove.forEach { creatureId ->
                val removeResult = tracker.removeCreature(state, creatureId)
                removeResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                state = (removeResult as InitiativeResult.Success<RoundState>).value
                
                // Verify invariants after each removal
                if (state.initiativeOrder.isNotEmpty() && state.currentTurn != null) {
                    val turn = state.currentTurn!!
                    turn.turnIndex shouldBeInRange (0 until state.initiativeOrder.size)
                    val activeCreature = state.initiativeOrder[turn.turnIndex]
                    activeCreature.creatureId shouldBe turn.activeCreatureId
                }
            }
        }
    }
    
    test("round number increments correctly across multiple full rounds") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 2..6)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            val creatures = creatureIds.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            val state = (initResult as InitiativeResult.Success<RoundState>).value
            
            var currentState = state
            val orderSize = state.initiativeOrder.size
            
            // Advance through exactly 3 full rounds
            repeat(orderSize * 3) { turnCount: Int ->
                val expectedRound = state.roundNumber + (turnCount / orderSize)
                currentState.roundNumber shouldBe expectedRound
                val advanceResult = tracker.advanceTurn(currentState)
                advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                currentState = (advanceResult as InitiativeResult.Success<RoundState>).value
            }
            
            // After 3 full rounds, should be in round 4 (or 3 if started at 0)
            currentState.roundNumber shouldBe state.roundNumber + 3
        }
    }
    
    test("active creature ID always matches creature at turn index") {
        checkAll(
            Arb.long(),
            Arb.set(Arb.long(1L..100L), 2..10)
        ) { seed: Long, creatureIds: Set<Long> ->
            val roller = DiceRoller(seed)
            val initiativeRoller = InitiativeRoller(roller)
            val tracker = InitiativeTracker()
            
            val creatures = creatureIds.associateWith { (it % 10).toInt() }
            val entries = initiativeRoller.rollInitiativeForAll(creatures)
            val initResult = tracker.initialize(entries, emptySet())
            initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
            var state = (initResult as InitiativeResult.Success<RoundState>).value
            
            // Test through multiple operations
            repeat(20) {
                // Verify invariant
                val turn = state.currentTurn!!
                val expectedCreature = state.initiativeOrder[turn.turnIndex]
                turn.activeCreatureId shouldBe expectedCreature.creatureId
                
                // Perform random operation
                val result = when (it % 3) {
                    0 -> tracker.advanceTurn(state)
                    1 -> {
                        // Add a creature
                        val newId = 200L + it
                        val newEntry = initiativeRoller.rollInitiative(newId, 0)
                        tracker.addCreature(state, newEntry)
                    }
                    else -> InitiativeResult.Success(state)
                }
                
                if (result is InitiativeResult.Success) {
                    state = result.value
                }
            }
        }
    }
})
