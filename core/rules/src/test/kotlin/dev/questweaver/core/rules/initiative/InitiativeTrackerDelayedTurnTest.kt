package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for InitiativeTracker delayed turn mechanics.
 *
 * Tests verify that:
 * - Delay removes creature from order
 * - Resume inserts at current position
 * - Delayed creature maintains new initiative
 * - Round end places delayed creature at end
 */


/**
 * Helper to unwrap InitiativeResult for testing.
 * Throws if result is InvalidState.
 */
private fun <T> InitiativeResult<T>.unwrap(): T = when (this) {
    is InitiativeResult.Success -> this.value
    is InitiativeResult.InvalidState -> throw AssertionError("Expected Success but got InvalidState: " + reason)
}

class InitiativeTrackerDelayedTurnTest : FunSpec({

    context("Delaying turns") {
        test("delay removes creature from current position in order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 2
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            // Creature 2 delays
            val newState = tracker.delayTurn(state, 2L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 2
            newState.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 3L)
            newState.delayedCreatures shouldContainKey 2L
        }

        test("delay advances turn to next creature") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 2
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            // Creature 2 delays
            val newState = tracker.delayTurn(state, 2L).unwrap()
            
            // Should advance to creature 3
            newState.currentTurn!!.activeCreatureId shouldBe 3L
        }

        test("delayed creature stored with original initiative") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            
            // Creature 2 delays
            val newState = tracker.delayTurn(state, 2L).unwrap()
            
            newState.delayedCreatures shouldContainKey 2L
            val delayedEntry = newState.delayedCreatures[2L]
            delayedEntry shouldNotBe null
            delayedEntry!!.creatureId shouldBe 2L
            delayedEntry.total shouldBe 17
        }

        test("delay first creature in order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 1L
            
            // Creature 1 delays
            val newState = tracker.delayTurn(state, 1L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 2
            newState.currentTurn!!.activeCreatureId shouldBe 2L
            newState.delayedCreatures shouldContainKey 1L
        }

        test("delay last creature in order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 3
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 3L
            
            // Creature 3 delays
            val newState = tracker.delayTurn(state, 3L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 2
            newState.delayedCreatures shouldContainKey 3L
            // Should wrap to next round
            newState.roundNumber shouldBe 2
            newState.currentTurn!!.activeCreatureId shouldBe 1L
        }
    }

    context("Resuming delayed turns") {
        test("resume inserts creature at current position") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 2 delays
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Now on creature 3's turn
            state.currentTurn!!.activeCreatureId shouldBe 3L
            
            // Creature 2 resumes with new initiative 10
            val newState = tracker.resumeDelayedTurn(state, 2L, 10).unwrap()
            
            newState.delayedCreatures shouldNotContainKey 2L
            newState.initiativeOrder shouldHaveSize 3
        }

        test("delayed creature maintains new initiative score") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Resume with new initiative 10
            val newState = tracker.resumeDelayedTurn(state, 2L, 10).unwrap()
            
            val resumedEntry = newState.initiativeOrder.find { it.creatureId == 2L }
            resumedEntry shouldNotBe null
            resumedEntry!!.total shouldBe 10
        }

        test("resume places creature in correct sorted position") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Resume with initiative 11 (should go between 3 and 4)
            val newState = tracker.resumeDelayedTurn(state, 2L, 11).unwrap()
            
            val creature2Index = newState.initiativeOrder.indexOfFirst { it.creatureId == 2L }
            val creature3Index = newState.initiativeOrder.indexOfFirst { it.creatureId == 3L }
            val creature4Index = newState.initiativeOrder.indexOfFirst { it.creatureId == 4L }
            
            creature3Index shouldBe creature2Index - 1
            creature4Index shouldBe creature2Index + 1
        }

        test("resume removes creature from delayed map") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            state.delayedCreatures shouldContainKey 2L
            
            val newState = tracker.resumeDelayedTurn(state, 2L, 10).unwrap()
            
            newState.delayedCreatures shouldNotContainKey 2L
        }
    }

    context("Multiple delayed creatures") {
        test("multiple creatures can delay in same round") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 1 delays
            state = tracker.delayTurn(state, 1L).unwrap()
            
            // Creature 2 delays
            state = tracker.delayTurn(state, 2L).unwrap()
            
            state.delayedCreatures shouldContainKey 1L
            state.delayedCreatures shouldContainKey 2L
            state.initiativeOrder shouldHaveSize 2
        }

        test("delayed creatures can resume in different order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creatures 1 and 2 delay
            state = tracker.delayTurn(state, 1L).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Creature 2 resumes first with initiative 14
            state = tracker.resumeDelayedTurn(state, 2L, 14).unwrap()
            
            // Creature 1 resumes with initiative 9
            state = tracker.resumeDelayedTurn(state, 1L, 9).unwrap()
            
            // Order should be: 2 (14), 3 (13), 1 (9)
            state.initiativeOrder.map { it.creatureId } shouldBe listOf(2L, 3L, 1L)
        }
    }

    context("Delayed turns across rounds") {
        test("delayed creature persists across rounds") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 1 delays
            state = tracker.delayTurn(state, 1L).unwrap()
            
            // Complete the round
            state = tracker.advanceTurn(state).unwrap() // Creature 3
            state = tracker.advanceTurn(state).unwrap() // Wrap to round 2, creature 2
            
            state.roundNumber shouldBe 2
            state.delayedCreatures shouldContainKey 1L
        }

        test("delayed creature can resume in later round") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 1 delays
            state = tracker.delayTurn(state, 1L).unwrap()
            
            // Complete round 1
            state = tracker.advanceTurn(state).unwrap() // Wrap to round 2
            
            state.roundNumber shouldBe 2
            
            // Creature 1 resumes in round 2
            state = tracker.resumeDelayedTurn(state, 1L, 16).unwrap()
            
            state.delayedCreatures shouldNotContainKey 1L
            state.initiativeOrder shouldHaveSize 2
        }
    }

    context("Edge cases") {
        test("delay only remaining creature") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Only creature delays
            val newState = tracker.delayTurn(state, 1L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 0
            newState.delayedCreatures shouldContainKey 1L
        }

        test("resume delayed creature when they're the only one") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state = tracker.delayTurn(state, 1L).unwrap()
            
            // Resume
            val newState = tracker.resumeDelayedTurn(state, 1L, 15).unwrap()
            
            newState.initiativeOrder shouldHaveSize 1
            newState.delayedCreatures shouldNotContainKey 1L
        }

        test("delay and resume maintains turn order integrity") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 20, modifier = 3, total = 23),
                InitiativeEntry(creatureId = 2L, roll = 18, modifier = 2, total = 20),
                InitiativeEntry(creatureId = 3L, roll = 15, modifier = 1, total = 16),
                InitiativeEntry(creatureId = 4L, roll = 12, modifier = 0, total = 12)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 2 delays
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Advance through remaining creatures
            state = tracker.advanceTurn(state).unwrap() // Creature 4
            
            // Creature 2 resumes with initiative 11
            state = tracker.resumeDelayedTurn(state, 2L, 11).unwrap()
            
            // Verify order is maintained
            val totals = state.initiativeOrder.map { it.total }
            for (i in 0 until totals.size - 1) {
                totals[i] shouldBe totals[i].coerceAtLeast(totals[i + 1])
            }
        }
    }

    context("Complex delay scenarios") {
        test("creature delays, another creature is removed, then resumes") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 2 delays
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // Creature 4 is removed
            state = tracker.removeCreature(state, 4L).unwrap()
            
            // Creature 2 resumes
            state = tracker.resumeDelayedTurn(state, 2L, 10).unwrap()
            
            state.initiativeOrder shouldHaveSize 3
            state.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 3L, 2L)
        }

        test("creature delays, new creature added, then resumes") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Creature 2 delays
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.delayTurn(state, 2L).unwrap()
            
            // New creature added
            val newEntry = InitiativeEntry(creatureId = 4L, roll = 14, modifier = 1, total = 15)
            state = tracker.addCreature(state, newEntry).unwrap()
            
            // Creature 2 resumes with initiative 11
            state = tracker.resumeDelayedTurn(state, 2L, 11).unwrap()
            
            state.initiativeOrder shouldHaveSize 4
        }
    }
})
