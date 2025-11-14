package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for InitiativeTracker turn advancement.
 *
 * Tests verify that:
 * - Turn advances to next creature
 * - Last creature wraps to first and increments round
 * - Round counter increments correctly
 * - Turn index resets to 0 at round start
 */


/**
 * Helper to unwrap InitiativeResult for testing.
 * Throws if result is InvalidState.
 */
private fun <T> InitiativeResult<T>.unwrap(): T = when (this) {
    is InitiativeResult.Success -> this.value
    is InitiativeResult.InvalidState -> throw AssertionError("Expected Success but got InvalidState: " + reason)
}

class InitiativeTrackerTurnAdvancementTest : FunSpec({

    context("Basic turn advancement") {
        test("turn advances to next creature in order") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val initialState = tracker.initialize(initiativeOrder).unwrap()
            
            initialState.currentTurn shouldNotBe null
            initialState.currentTurn!!.activeCreatureId shouldBe 1L
            initialState.currentTurn!!.turnIndex shouldBe 0
            
            // Advance to second creature
            val state2 = tracker.advanceTurn(initialState).unwrap()
            state2.currentTurn!!.activeCreatureId shouldBe 2L
            state2.currentTurn!!.turnIndex shouldBe 1
            state2.roundNumber shouldBe 1
            
            // Advance to third creature
            val state3 = tracker.advanceTurn(state2).unwrap()
            state3.currentTurn!!.activeCreatureId shouldBe 3L
            state3.currentTurn!!.turnIndex shouldBe 2
            state3.roundNumber shouldBe 1
        }

        test("turn index increments correctly") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 20, modifier = 2, total = 22),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 1, total = 16),
                InitiativeEntry(creatureId = 3L, roll = 10, modifier = 0, total = 10),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = -1, total = 7)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.currentTurn!!.turnIndex shouldBe 0
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 1
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 2
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 3
        }
    }

    context("Round wrapping") {
        test("last creature wraps to first and increments round") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.roundNumber shouldBe 1
            
            // Advance through all creatures
            state = tracker.advanceTurn(state).unwrap() // Creature 2
            state = tracker.advanceTurn(state).unwrap() // Creature 3
            state = tracker.advanceTurn(state).unwrap() // Should wrap to Creature 1, round 2
            
            state.currentTurn!!.activeCreatureId shouldBe 1L
            state.currentTurn!!.turnIndex shouldBe 0
            state.roundNumber shouldBe 2
        }

        test("turn index resets to 0 at round start") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 20, modifier = 2, total = 22),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 1, total = 16)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.currentTurn!!.turnIndex shouldBe 0
            state.roundNumber shouldBe 1
            
            // Advance to creature 2
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 1
            
            // Advance to new round
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 0
            state.roundNumber shouldBe 2
        }

        test("round counter increments correctly over multiple rounds") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 2, total = 20),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.roundNumber shouldBe 1
            
            // Complete round 1
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 2
            
            // Complete round 2
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 3
            
            // Complete round 3
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 4
        }
    }

    context("Single creature edge case") {
        test("single creature advances to itself and increments round") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 15, modifier = 2, total = 17)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 1L
            state.roundNumber shouldBe 1
            
            // Advance turn (should wrap to same creature, new round)
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 1L
            state.currentTurn!!.turnIndex shouldBe 0
            state.roundNumber shouldBe 2
        }
    }

    context("Multiple rounds") {
        test("creatures cycle through multiple rounds correctly") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 20, modifier = 3, total = 23),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 10, modifier = 1, total = 11)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            
            // Round 1
            state.roundNumber shouldBe 1
            state.currentTurn!!.activeCreatureId shouldBe 1L
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 3L
            
            // Round 2
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 2
            state.currentTurn!!.activeCreatureId shouldBe 1L
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 3L
            
            // Round 3
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 3
            state.currentTurn!!.activeCreatureId shouldBe 1L
        }

        test("turn order remains consistent across rounds") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = listOf(
                InitiativeEntry(creatureId = 5L, roll = 18, modifier = 2, total = 20),
                InitiativeEntry(creatureId = 3L, roll = 14, modifier = 1, total = 15),
                InitiativeEntry(creatureId = 7L, roll = 10, modifier = 0, total = 10)
            )
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            
            // Verify order in round 1
            val round1Order = mutableListOf<Long>()
            round1Order.add(state.currentTurn!!.activeCreatureId)
            state = tracker.advanceTurn(state).unwrap()
            round1Order.add(state.currentTurn!!.activeCreatureId)
            state = tracker.advanceTurn(state).unwrap()
            round1Order.add(state.currentTurn!!.activeCreatureId)
            
            // Verify order in round 2
            state = tracker.advanceTurn(state).unwrap()
            val round2Order = mutableListOf<Long>()
            round2Order.add(state.currentTurn!!.activeCreatureId)
            state = tracker.advanceTurn(state).unwrap()
            round2Order.add(state.currentTurn!!.activeCreatureId)
            state = tracker.advanceTurn(state).unwrap()
            round2Order.add(state.currentTurn!!.activeCreatureId)
            
            // Order should be identical
            round1Order shouldBe round2Order
            round1Order shouldBe listOf(5L, 3L, 7L)
        }
    }

    context("Large initiative order") {
        test("handles many creatures correctly") {
            val tracker = InitiativeTracker()
            
            val initiativeOrder = (1L..10L).map { id ->
                InitiativeEntry(
                    creatureId = id,
                    roll = (20 - id).toInt(),
                    modifier = 2,
                    total = (22 - id).toInt()
                )
            }
            
            var state = tracker.initialize(initiativeOrder).unwrap()
            state.roundNumber shouldBe 1
            
            // Advance through all 10 creatures
            for (i in 1L..10L) {
                state.currentTurn!!.activeCreatureId shouldBe i
                if (i < 10L) {
                    state = tracker.advanceTurn(state).unwrap()
                }
            }
            
            // Advance to new round
            state = tracker.advanceTurn(state).unwrap()
            state.roundNumber shouldBe 2
            state.currentTurn!!.activeCreatureId shouldBe 1L
            state.currentTurn!!.turnIndex shouldBe 0
        }
    }
})
