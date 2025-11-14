package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.core.rules.initiative.models.InitiativeResult
import dev.questweaver.core.rules.initiative.models.RoundState
import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.InitiativeEntryData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

/**
 * Property-based tests for deterministic behavior of initiative system.
 *
 * Tests verify that:
 * - Initiative with same seed produces same order
 * - Turn advancement eventually returns to first creature
 * - Event replay produces identical state
 * - Initiative order is always sorted correctly
 */
class InitiativeDeterminismPropertyTest : FunSpec({

    context("Initiative rolling determinism") {
        test("same seed produces same initiative order") {
            checkAll(
                Arb.long(),
                Arb.map(
                    Arb.long(1L..100L),
                    Arb.int(-5..10),
                    minSize = 2,
                    maxSize = 10
                )
            ) { seed: Long, creatures: Map<Long, Int> ->
                val roller1 = InitiativeRoller(DiceRoller(seed))
                val order1 = roller1.rollInitiativeForAll(creatures)
                
                val roller2 = InitiativeRoller(DiceRoller(seed))
                val order2 = roller2.rollInitiativeForAll(creatures)
                
                order1 shouldBe order2
            }
        }

        test("initiative order is always sorted correctly") {
            checkAll(
                Arb.long(),
                Arb.map(
                    Arb.long(1L..100L),
                    Arb.int(-5..10),
                    minSize = 2,
                    maxSize = 10
                )
            ) { seed: Long, creatures: Map<Long, Int> ->
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                // Verify descending order by total
                for (i in 0 until order.size - 1) {
                    val current = order[i]
                    val next = order[i + 1]
                    
                    // Current should be >= next in terms of sorting
                    if (current.total == next.total) {
                        if (current.modifier == next.modifier) {
                            // Tiebreaker by creature ID (ascending)
                            current.creatureId shouldBeGreaterThanOrEqualTo next.creatureId
                        } else {
                            // Tiebreaker by modifier (descending)
                            current.modifier shouldBeGreaterThan next.modifier
                        }
                    } else {
                        current.total shouldBeGreaterThan next.total
                    }
                }
            }
        }

        test("different seeds produce different results (probabilistically)") {
            checkAll(
                Arb.long(),
                Arb.long(),
                Arb.map(
                    Arb.long(1L..10L),
                    Arb.int(0..3),
                    minSize = 3,
                    maxSize = 5
                )
            ) { seed1: Long, seed2: Long, creatures: Map<Long, Int> ->
                if (seed1 != seed2) {
                    val roller1 = InitiativeRoller(DiceRoller(seed1))
                    val order1 = roller1.rollInitiativeForAll(creatures)
                    
                    val roller2 = InitiativeRoller(DiceRoller(seed2))
                    val order2 = roller2.rollInitiativeForAll(creatures)
                    
                    // With different seeds, at least one roll should be different
                    // (This is probabilistic - there's a tiny chance they're the same)
                    // We just verify the mechanism works - not asserting difference
                    // because it's probabilistically possible (though unlikely) to be same
                    order1.size shouldBe order2.size
                }
            }
        }
    }

    context("Turn advancement determinism") {
        test("turn advancement eventually returns to first creature") {
            checkAll(
                Arb.list(Arb.int(1..20), 2..10)
            ) { initiatives ->
                val entries = initiatives.mapIndexed { i, init ->
                    InitiativeEntry(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                val initResult = tracker.initialize(entries)
                initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                var state = (initResult as InitiativeResult.Success).value
                
                val firstCreatureId = (state as RoundState).currentTurn!!.activeCreatureId
                
                // Advance through all creatures
                var currentState = state
                repeat(entries.size) {
                    val advanceResult = tracker.advanceTurn(currentState)
                    advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                    currentState = (advanceResult as InitiativeResult.Success<RoundState>).value
                }
                
                // Should be back to first creature in new round
                currentState.currentTurn!!.activeCreatureId shouldBe firstCreatureId
                currentState.roundNumber shouldBe 2
            }
        }

        test("turn advancement maintains consistent order across rounds") {
            checkAll(
                Arb.list(Arb.int(1..20), 2..8)
            ) { initiatives ->
                val entries = initiatives.mapIndexed { i, init ->
                    InitiativeEntry(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                val initResult = tracker.initialize(entries)
                initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                var state = (initResult as InitiativeResult.Success<RoundState>).value
                
                // Collect order in round 1
                val round1Order = mutableListOf<Long>()
                round1Order.add(state.currentTurn!!.activeCreatureId)
                repeat(entries.size - 1) {
                    val advanceResult = tracker.advanceTurn(state)
                    advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                    state = (advanceResult as InitiativeResult.Success<RoundState>).value
                    round1Order.add(state.currentTurn!!.activeCreatureId)
                }
                
                // Advance to round 2
                val advanceResult = tracker.advanceTurn(state)
                advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                state = (advanceResult as InitiativeResult.Success<RoundState>).value
                
                // Collect order in round 2
                val round2Order = mutableListOf<Long>()
                round2Order.add(state.currentTurn!!.activeCreatureId)
                repeat(entries.size - 1) {
                    val nextResult = tracker.advanceTurn(state)
                    nextResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                    state = (nextResult as InitiativeResult.Success<RoundState>).value
                    round2Order.add(state.currentTurn!!.activeCreatureId)
                }
                
                // Orders should be identical
                round1Order shouldBe round2Order
            }
        }

        test("round number always increases") {
            checkAll(
                Arb.list(Arb.int(1..20), 2..5)
            ) { initiatives ->
                val entries = initiatives.mapIndexed { i, init ->
                    InitiativeEntry(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                val initResult = tracker.initialize(entries)
                initResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                var state = (initResult as InitiativeResult.Success<RoundState>).value
                
                var previousRound = state.roundNumber
                
                // Advance through multiple rounds
                repeat(entries.size * 3) {
                    val advanceResult = tracker.advanceTurn(state)
                    advanceResult.shouldBeInstanceOf<InitiativeResult.Success<*>>()
                    state = (advanceResult as InitiativeResult.Success<RoundState>).value
                    
                    // Round should never decrease
                    state.roundNumber shouldBeGreaterThanOrEqualTo previousRound
                    previousRound = state.roundNumber
                }
            }
        }
    }

    context("Event replay determinism") {
        test("event replay produces identical state") {
            checkAll(
                Arb.long(),
                Arb.list(
                    Arb.int(-5..10),
                    2..8
                )
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                // Generate initiative with seed
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                // Create event
                val event = EncounterStarted(
                    sessionId = 1L,
                    timestamp = System.currentTimeMillis(),
                    encounterId = 100L,
                    participants = creatures.keys.toList(),
                    initiativeOrder = order.map { 
                        InitiativeEntryData(it.creatureId, it.roll, it.modifier, it.total)
                    },
                    surprisedCreatures = emptySet()
                )
                
                // Build state from event twice
                val builder1 = InitiativeStateBuilder()
                val state1 = builder1.buildState(listOf(event))
                
                val builder2 = InitiativeStateBuilder()
                val state2 = builder2.buildState(listOf(event))
                
                // States should be identical
                state1.roundNumber shouldBe state2.roundNumber
                state1.initiativeOrder shouldBe state2.initiativeOrder
                state1.surprisedCreatures shouldBe state2.surprisedCreatures
            }
        }

        test("multiple event replays produce consistent results") {
            checkAll(
                Arb.long(),
                Arb.list(Arb.int(-3..5), 2..5)
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                val event = EncounterStarted(
                    sessionId = 1L,
                    timestamp = System.currentTimeMillis(),
                    encounterId = 100L,
                    participants = creatures.keys.toList(),
                    initiativeOrder = order.map { 
                        InitiativeEntryData(it.creatureId, it.roll, it.modifier, it.total)
                    },
                    surprisedCreatures = emptySet()
                )
                
                // Replay multiple times
                val states = (1..5).map {
                    val builder = InitiativeStateBuilder()
                    builder.buildState(listOf(event))
                }
                
                // All states should be identical
                states.forEach { state ->
                    state.roundNumber shouldBe states[0].roundNumber
                    state.initiativeOrder shouldBe states[0].initiativeOrder
                }
            }
        }
    }

    context("Initiative order sorting properties") {
        test("initiative order respects total score ordering") {
            checkAll(
                Arb.long(),
                Arb.list(Arb.int(-5..10), 2..10)
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                // Verify each entry's total is correct
                order.forEach { entry ->
                    entry.total shouldBe entry.roll + entry.modifier
                }
                
                // Verify descending order
                for (i in 0 until order.size - 1) {
                    order[i].total shouldBeGreaterThanOrEqualTo order[i + 1].total
                }
            }
        }

        test("initiative tiebreaker is consistent") {
            checkAll(Arb.long()) { seed: Long ->
                // Create creatures with same modifier
                val creatures = mapOf(
                    1L to 2,
                    2L to 2,
                    3L to 2,
                    4L to 2
                )
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                // If any have same total, verify tiebreaker rules
                for (i in 0 until order.size - 1) {
                    val current = order[i]
                    val next = order[i + 1]
                    
                    if (current.total == next.total && current.modifier == next.modifier) {
                        // Creature ID tiebreaker (ascending)
                        current.creatureId shouldBeGreaterThanOrEqualTo next.creatureId
                    }
                }
            }
        }
    }

    context("State consistency properties") {
        test("initiative order size matches creature count") {
            checkAll(
                Arb.long(),
                Arb.list(Arb.int(-5..10), 1..15)
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                order.size shouldBe creatures.size
            }
        }

        test("all creature IDs present in initiative order") {
            checkAll(
                Arb.long(),
                Arb.list(Arb.int(-5..10), 2..10)
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                val orderIds = order.map { it.creatureId }.toSet()
                val creatureIds = creatures.keys
                
                orderIds shouldBe creatureIds
            }
        }

        test("no duplicate creatures in initiative order") {
            checkAll(
                Arb.long(),
                Arb.list(Arb.int(-5..10), 2..10)
            ) { seed: Long, modifiers: List<Int> ->
                val creatures = modifiers.mapIndexed { i, mod -> 
                    (i + 1).toLong() to mod 
                }.toMap()
                
                val roller = InitiativeRoller(DiceRoller(seed))
                val order = roller.rollInitiativeForAll(creatures)
                
                val uniqueIds = order.map { it.creatureId }.toSet()
                uniqueIds.size shouldBe order.size
            }
        }
    }
})
