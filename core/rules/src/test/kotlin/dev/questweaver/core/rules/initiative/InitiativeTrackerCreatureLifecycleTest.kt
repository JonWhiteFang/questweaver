package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Helper to unwrap InitiativeResult for testing.
 * Throws if result is InvalidState.
 */
private fun <T> InitiativeResult<T>.unwrap(): T = when (this) {
    is InitiativeResult.Success -> this.value
    is InitiativeResult.InvalidState -> throw AssertionError("Expected Success but got InvalidState: $reason")
}

/**
 * Unit tests for InitiativeTracker creature lifecycle.
 *
 * Tests verify that:
 * - Add creature inserts at correct position
 * - Remove creature maintains turn order
 * - Remove active creature advances turn
 * - Remove creature before current turn adjusts index
 */
class InitiativeTrackerCreatureLifecycleTest : FunSpec({

    context("Adding creatures mid-combat") {
        test("add creature inserts at correct position based on initiative") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 3L, roll = 8, modifier = 0, total = 8)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Add creature with initiative 17 (should go between 1 and 2)
            val newEntry = InitiativeEntry(creatureId = 4L, roll = 15, modifier = 2, total = 17)
            val newState = tracker.addCreature(state, newEntry).unwrap()
            
            newState.initiativeOrder shouldHaveSize 4
            newState.initiativeOrder[0].creatureId shouldBe 1L  // 21
            newState.initiativeOrder[1].creatureId shouldBe 4L  // 17
            newState.initiativeOrder[2].creatureId shouldBe 2L  // 13
            newState.initiativeOrder[3].creatureId shouldBe 3L  // 8
        }

        test("add creature at beginning of initiative order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 2L, roll = 10, modifier = 1, total = 11)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Add creature with highest initiative
            val newEntry = InitiativeEntry(creatureId = 3L, roll = 19, modifier = 3, total = 22)
            val newState = tracker.addCreature(state, newEntry).unwrap()
            
            newState.initiativeOrder[0].creatureId shouldBe 3L
            newState.initiativeOrder shouldHaveSize 3
        }

        test("add creature at end of initiative order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Add creature with lowest initiative
            val newEntry = InitiativeEntry(creatureId = 3L, roll = 5, modifier = 0, total = 5)
            val newState = tracker.addCreature(state, newEntry).unwrap()
            
            newState.initiativeOrder[2].creatureId shouldBe 3L
            newState.initiativeOrder shouldHaveSize 3
        }

        test("add creature adjusts turn index when inserted before current turn") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 3L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 2 (index 1)
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 1
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            // Add creature with initiative 17 (inserts at index 1, before current turn)
            val newEntry = InitiativeEntry(creatureId = 4L, roll = 15, modifier = 2, total = 17)
            val newState = tracker.addCreature(state, newEntry).unwrap()
            
            // Turn index should be adjusted to maintain current creature
            newState.currentTurn!!.turnIndex shouldBe 2
            newState.currentTurn!!.activeCreatureId shouldBe 2L
        }

        test("add creature doesn't adjust turn index when inserted after current turn") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 3L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state.currentTurn!!.turnIndex shouldBe 0
            
            // Add creature with initiative 10 (inserts after current turn)
            val newEntry = InitiativeEntry(creatureId = 4L, roll = 9, modifier = 1, total = 10)
            val newState = tracker.addCreature(state, newEntry).unwrap()
            
            // Turn index should remain the same
            newState.currentTurn!!.turnIndex shouldBe 0
            newState.currentTurn!!.activeCreatureId shouldBe 1L
        }
    }

    context("Removing creatures from combat") {
        test("remove creature maintains turn order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Remove creature 2
            val newState = tracker.removeCreature(state, 2L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 3
            newState.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 3L, 4L)
            newState.initiativeOrder shouldNotContain InitiativeEntry(2L, 15, 2, 17)
        }

        test("remove active creature advances turn") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 1L
            
            // Remove active creature
            val newState = tracker.removeCreature(state, 1L).unwrap()
            
            // Should advance to next creature
            newState.currentTurn!!.activeCreatureId shouldBe 2L
            newState.currentTurn!!.turnIndex shouldBe 0
            newState.initiativeOrder shouldHaveSize 2
        }

        test("remove creature before current turn adjusts index") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 3 (index 2)
            state = tracker.advanceTurn(state).unwrap()
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.turnIndex shouldBe 2
            state.currentTurn!!.activeCreatureId shouldBe 3L
            
            // Remove creature 1 (before current turn)
            val newState = tracker.removeCreature(state, 1L).unwrap()
            
            // Turn index should be decremented
            newState.currentTurn!!.turnIndex shouldBe 1
            newState.currentTurn!!.activeCreatureId shouldBe 3L
        }

        test("remove creature after current turn doesn't adjust index") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13),
                InitiativeEntry(creatureId = 4L, roll = 8, modifier = 0, total = 8)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            state.currentTurn!!.turnIndex shouldBe 0
            
            // Remove creature 4 (after current turn)
            val newState = tracker.removeCreature(state, 4L).unwrap()
            
            // Turn index should remain the same
            newState.currentTurn!!.turnIndex shouldBe 0
            newState.currentTurn!!.activeCreatureId shouldBe 1L
        }

        test("remove last creature in order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Remove last creature
            val newState = tracker.removeCreature(state, 3L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 2
            newState.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 2L)
        }

        test("remove first creature in order") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 1L
            
            // Remove first creature (which is active)
            val newState = tracker.removeCreature(state, 1L).unwrap()
            
            newState.initiativeOrder shouldHaveSize 2
            newState.currentTurn!!.activeCreatureId shouldBe 2L
        }
    }

    context("Complex lifecycle scenarios") {
        test("add and remove creatures in same combat") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Add creature
            val newEntry = InitiativeEntry(creatureId = 3L, roll = 15, modifier = 2, total = 17)
            state = tracker.addCreature(state, newEntry).unwrap()
            state.initiativeOrder shouldHaveSize 3
            
            // Remove creature
            state = tracker.removeCreature(state, 2L).unwrap()
            state.initiativeOrder shouldHaveSize 2
            state.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 3L)
        }

        test("remove multiple creatures") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 20, modifier = 3, total = 23),
                InitiativeEntry(creatureId = 2L, roll = 18, modifier = 2, total = 20),
                InitiativeEntry(creatureId = 3L, roll = 15, modifier = 1, total = 16),
                InitiativeEntry(creatureId = 4L, roll = 12, modifier = 0, total = 12),
                InitiativeEntry(creatureId = 5L, roll = 8, modifier = -1, total = 7)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Remove creatures 2 and 4
            state = tracker.removeCreature(state, 2L).unwrap()
            state = tracker.removeCreature(state, 4L).unwrap()
            
            state.initiativeOrder shouldHaveSize 3
            state.initiativeOrder.map { it.creatureId } shouldBe listOf(1L, 3L, 5L)
        }

        test("add creature during active creature's turn") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Advance to creature 2
            state = tracker.advanceTurn(state).unwrap()
            state.currentTurn!!.activeCreatureId shouldBe 2L
            
            // Add creature with higher initiative (should go before current)
            val newEntry = InitiativeEntry(creatureId = 3L, roll = 15, modifier = 2, total = 17)
            state = tracker.addCreature(state, newEntry).unwrap()
            
            // Current creature should still be creature 2
            state.currentTurn!!.activeCreatureId shouldBe 2L
            state.initiativeOrder shouldHaveSize 3
        }

        test("remove all creatures except one") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntry(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Remove creatures 2 and 3
            state = tracker.removeCreature(state, 2L).unwrap()
            state = tracker.removeCreature(state, 3L).unwrap()
            
            state.initiativeOrder shouldHaveSize 1
            state.initiativeOrder[0].creatureId shouldBe 1L
            state.currentTurn!!.activeCreatureId shouldBe 1L
        }
    }

    context("Edge cases") {
        test("add creature to empty initiative order") {
            val tracker = InitiativeTracker()
            
            // This should fail validation - empty order not allowed
            val emptyResult = tracker.initialize(emptyList())
            emptyResult.shouldBeInstanceOf<InitiativeResult.InvalidState>()
        }

        test("add multiple creatures with same initiative") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 15, modifier = 2, total = 17)
            )
            
            var state = tracker.initialize(initialOrder).unwrap()
            
            // Add creatures with same total but different modifiers
            val entry2 = InitiativeEntry(creatureId = 2L, roll = 16, modifier = 1, total = 17)
            state = tracker.addCreature(state, entry2).unwrap()
            
            val entry3 = InitiativeEntry(creatureId = 3L, roll = 14, modifier = 3, total = 17)
            state = tracker.addCreature(state, entry3).unwrap()
            
            state.initiativeOrder shouldHaveSize 3
            // Should be sorted by modifier tiebreaker
        }

        test("remove non-existent creature returns error") {
            val tracker = InitiativeTracker()
            
            val initialOrder = listOf(
                InitiativeEntry(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntry(creatureId = 2L, roll = 15, modifier = 2, total = 17)
            )
            
            val state = tracker.initialize(initialOrder).unwrap()
            
            // Try to remove creature that doesn't exist - should return InvalidState
            val result = tracker.removeCreature(state, 999L)
            result.shouldBeInstanceOf<InitiativeResult.InvalidState>()
        }
    }
})
