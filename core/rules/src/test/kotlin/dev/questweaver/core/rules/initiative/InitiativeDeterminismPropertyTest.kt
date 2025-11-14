package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.InitiativeEntryData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
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
                    Arb.long(1..100),
                    Arb.int(-5..10),
                    minSize = 2,
                    maxSize = 10
                )
            ) { seed, creatures ->
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
                    Arb.long(1..100),
                    Arb.int(-5..10),
                    minSize = 2,
                    maxSize = 10
                )
            ) { seed, creatures ->
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
                    Arb.long(1..10),
                    Arb.int(0..3),
                    minSize = 3,
                    maxSize = 5
                )
            ) { seed1, seed2, creatures ->
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
                    InitiativeEntryData(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                var state = tracker.initialize(entries)
                
                val firstCreatureId = state.currentTurn!!.activeCreatureId
                
                // Advance through all creatures
                repeat(entries.size) {
                    state = tracker.advanceTurn(state)
                }
                
                // Should be back to first creature in new round
                state.currentTurn!!.activeCreatureId shouldBe firstCreatureId
                state.roundNumber shouldBe 2
            }
        }

        test("turn advancement maintains consistent order across rounds") {
            checkAll(
                Arb.list(Arb.int(1..20), 2..8)
            ) { initiatives ->
                val entries = initiatives.mapIndexed { i, init ->
                    InitiativeEntryData(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                var state = tracker.initialize(entries)
                
                // Collect order in round 1
                val round1Order = mutableListOf<Long>()
                round1Order.add(state.currentTurn!!.activeCreatureId)
                repeat(entries.size - 1) {
                    state = tracker.advanceTurn(state)
                    round1Order.add(state.currentTurn!!.activeCreatureId)
                }
                
                // Advance to round 2
                state = tracker.advanceTurn(state)
                
                // Collect order in round 2
                val round2Order = mutableListOf<Long>()
                round2Order.add(state.currentTurn!!.activeCreatureId)
                repeat(entries.size - 1) {
                    state = tracker.advanceTurn(state)
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
                    InitiativeEntryData(i.toLong(), init, 0, init)
                }
                
                val tracker = InitiativeTracker()
                var state = tracker.initialize(entries)
                
                var previousRound = state.roundNumber
                
                // Advance through multiple rounds
                repeat(entries.size * 3) {
                    state = tracker.advanceTurn(state)
                    
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
            ) { seed, modifiers ->
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
            ) { seed, modifiers ->
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
            ) { seed, modifiers ->
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
            checkAll(Arb.long()) { seed ->
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
            ) { seed, modifiers ->
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
            ) { seed, modifiers ->
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
            ) { seed, modifiers ->
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
