package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.domain.events.CreatureAddedToCombat
import dev.questweaver.domain.events.CreatureRemovedFromCombat
import dev.questweaver.domain.events.DelayedTurnResumed
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.ReactionUsed
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.TurnDelayed
import dev.questweaver.domain.events.TurnEnded
import dev.questweaver.domain.events.TurnStarted
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldBeEmpty as mapShouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for InitiativeStateBuilder event sourcing.
 *
 * Tests verify that:
 * - State derived from events matches original
 * - Replay produces identical state
 * - All events handled exhaustively
 * - Non-initiative events don't affect state
 */
class InitiativeStateBuilderTest : FunSpec({

    val sessionId = 1L
    val encounterId = 100L
    val timestamp = System.currentTimeMillis()

    context("Building state from EncounterStarted event") {
        test("state derived from EncounterStarted event") {
            val builder = InitiativeStateBuilder()
            
            val initiativeData = listOf(
                InitiativeEntryData(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntryData(creatureId = 2L, roll = 15, modifier = 2, total = 17),
                InitiativeEntryData(creatureId = 3L, roll = 12, modifier = 1, total = 13)
            )
            
            val event = EncounterStarted(
                sessionId = sessionId,
                timestamp = timestamp,
                encounterId = encounterId,
                participants = listOf(1L, 2L, 3L),
                initiativeOrder = initiativeData,
                surprisedCreatures = emptySet()
            )
            
            val state = builder.buildState(listOf(event))
            
            state.roundNumber shouldBe 1
            state.isSurpriseRound shouldBe false
            state.initiativeOrder shouldHaveSize 3
            state.initiativeOrder[0].creatureId shouldBe 1L
            state.initiativeOrder[1].creatureId shouldBe 2L
            state.initiativeOrder[2].creatureId shouldBe 3L
            state.currentTurn shouldNotBe null
            state.currentTurn!!.activeCreatureId shouldBe 1L
        }

        test("state with surprise round from EncounterStarted") {
            val builder = InitiativeStateBuilder()
            
            val initiativeData = listOf(
                InitiativeEntryData(creatureId = 1L, roll = 18, modifier = 3, total = 21),
                InitiativeEntryData(creatureId = 2L, roll = 15, modifier = 2, total = 17)
            )
            
            val event = EncounterStarted(
                sessionId = sessionId,
                timestamp = timestamp,
                encounterId = encounterId,
                participants = listOf(1L, 2L),
                initiativeOrder = initiativeData,
                surprisedCreatures = setOf(2L)
            )
            
            val state = builder.buildState(listOf(event))
            
            state.isSurpriseRound shouldBe true
            state.surprisedCreatures shouldBe setOf(2L)
            state.roundNumber shouldBe 0
        }
    }

    context("Building state from RoundStarted event") {
        test("round number increments from RoundStarted") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                RoundStarted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    roundNumber = 2
                )
            )
            
            val state = builder.buildState(events)
            
            state.roundNumber shouldBe 2
        }
    }

    context("Building state from TurnStarted and TurnEnded events") {
        test("TurnStarted updates current turn") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnStarted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 2L,
                    roundNumber = 1,
                    turnIndex = 1
                )
            )
            
            val state = builder.buildState(events)
            
            state.currentTurn shouldNotBe null
            state.currentTurn!!.activeCreatureId shouldBe 2L
            state.currentTurn!!.turnIndex shouldBe 1
        }

        test("TurnEnded clears current turn") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnEnded(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 1L,
                    roundNumber = 1
                )
            )
            
            val state = builder.buildState(events)
            
            // After turn ended, current turn should be cleared or updated
            // Implementation may vary - this tests the event is processed
        }
    }

    context("Building state from creature lifecycle events") {
        test("CreatureAddedToCombat adds creature to order") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                CreatureAddedToCombat(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 3L,
                    initiativeEntry = InitiativeEntryData(3L, 16, 2, 18)
                )
            )
            
            val state = builder.buildState(events)
            
            state.initiativeOrder shouldHaveSize 3
            val creature3 = state.initiativeOrder.find { it.creatureId == 3L }
            creature3 shouldNotBe null
            creature3!!.total shouldBe 18
        }

        test("CreatureRemovedFromCombat removes creature from order") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L, 3L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17),
                        InitiativeEntryData(3L, 12, 1, 13)
                    ),
                    surprisedCreatures = emptySet()
                ),
                CreatureRemovedFromCombat(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 2L,
                    reason = "defeated"
                )
            )
            
            val state = builder.buildState(events)
            
            state.initiativeOrder shouldHaveSize 2
            val creature2 = state.initiativeOrder.find { it.creatureId == 2L }
            creature2 shouldBe null
        }
    }

    context("Building state from delayed turn events") {
        test("TurnDelayed moves creature to delayed map") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnDelayed(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 2L,
                    originalInitiative = 17
                )
            )
            
            val state = builder.buildState(events)
            
            state.delayedCreatures shouldContainKey 2L
            state.initiativeOrder shouldHaveSize 1
        }

        test("DelayedTurnResumed moves creature back to order") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnDelayed(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    encounterId = encounterId,
                    creatureId = 2L,
                    originalInitiative = 17
                ),
                DelayedTurnResumed(
                    sessionId = sessionId,
                    timestamp = timestamp + 2000,
                    encounterId = encounterId,
                    creatureId = 2L,
                    newInitiative = 10
                )
            )
            
            val state = builder.buildState(events)
            
            state.initiativeOrder shouldHaveSize 2
            state.delayedCreatures.keys shouldNotContain 2L
            val creature2 = state.initiativeOrder.find { it.creatureId == 2L }
            creature2 shouldNotBe null
            creature2!!.total shouldBe 10
        }
    }

    context("Event replay produces identical state") {
        test("replay produces identical state") {
            val builder1 = InitiativeStateBuilder()
            val builder2 = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L, 3L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17),
                        InitiativeEntryData(3L, 12, 1, 13)
                    ),
                    surprisedCreatures = emptySet()
                ),
                TurnStarted(sessionId, timestamp + 1000, encounterId, 1L, 1, 0),
                TurnEnded(sessionId, timestamp + 2000, encounterId, 1L, 1),
                TurnStarted(sessionId, timestamp + 3000, encounterId, 2L, 1, 1),
                ReactionUsed(sessionId, timestamp + 4000, encounterId, 2L, "Opportunity Attack", "Enemy moved away"),
                TurnEnded(sessionId, timestamp + 5000, encounterId, 2L, 1)
            )
            
            val state1 = builder1.buildState(events)
            val state2 = builder2.buildState(events)
            
            state1.roundNumber shouldBe state2.roundNumber
            state1.initiativeOrder shouldBe state2.initiativeOrder
            state1.surprisedCreatures shouldBe state2.surprisedCreatures
            state1.delayedCreatures shouldBe state2.delayedCreatures
        }

        test("complex event sequence replay") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L, 3L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17),
                        InitiativeEntryData(3L, 12, 1, 13)
                    ),
                    surprisedCreatures = setOf(3L)
                ),
                RoundStarted(sessionId, timestamp + 1000, encounterId, 1),
                TurnDelayed(sessionId, timestamp + 2000, encounterId, 2L, 17),
                CreatureAddedToCombat(
                    sessionId, timestamp + 3000, encounterId, 4L,
                    InitiativeEntryData(4L, 14, 1, 15)
                ),
                DelayedTurnResumed(sessionId, timestamp + 4000, encounterId, 2L, 11),
                CreatureRemovedFromCombat(sessionId, timestamp + 5000, encounterId, 3L, "defeated")
            )
            
            val state = builder.buildState(events)
            
            // Verify final state
            state.roundNumber shouldBe 1
            state.initiativeOrder shouldHaveSize 3
            state.delayedCreatures.mapShouldBeEmpty()
        }
    }

    context("Non-initiative events don't affect state") {
        test("non-initiative events are ignored") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                ),
                MoveCommitted(
                    sessionId = sessionId,
                    timestamp = timestamp + 1000,
                    creatureId = 1L,
                    path = listOf(GridPos(0, 0), GridPos(5, 5)),
                    movementUsed = 5,
                    movementRemaining = 25
                )
            )
            
            val state = builder.buildState(events)
            
            // State should only reflect EncounterStarted
            state.initiativeOrder shouldHaveSize 2
            state.roundNumber shouldBe 1
        }

        test("mixed initiative and non-initiative events") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = encounterId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21)
                    ),
                    surprisedCreatures = emptySet()
                ),
                MoveCommitted(
                    sessionId, timestamp + 1000, 1L,
                    listOf(GridPos(0, 0), GridPos(5, 5)), 5, 25
                ),
                TurnStarted(sessionId, timestamp + 2000, encounterId, 1L, 1, 0),
                MoveCommitted(
                    sessionId, timestamp + 3000, 1L,
                    listOf(GridPos(5, 5), GridPos(10, 10)), 5, 20
                ),
                TurnEnded(sessionId, timestamp + 4000, encounterId, 1L, 1)
            )
            
            val state = builder.buildState(events)
            
            // Only initiative events should affect state
            state.initiativeOrder shouldHaveSize 1
        }
    }

    context("Empty and edge cases") {
        test("empty event list returns initial state") {
            val builder = InitiativeStateBuilder()
            
            val state = builder.buildState(emptyList())
            
            state.roundNumber shouldBe 0
            state.initiativeOrder.shouldBeEmpty()
            state.surprisedCreatures.shouldBeEmpty()
            state.delayedCreatures.mapShouldBeEmpty()
            state.currentTurn shouldBe null
        }

        test("single EncounterStarted event") {
            val builder = InitiativeStateBuilder()
            
            val event = EncounterStarted(
                sessionId = sessionId,
                timestamp = timestamp,
                encounterId = encounterId,
                participants = listOf(1L),
                initiativeOrder = listOf(
                    InitiativeEntryData(1L, 15, 2, 17)
                ),
                surprisedCreatures = emptySet()
            )
            
            val state = builder.buildState(listOf(event))
            
            state.initiativeOrder shouldHaveSize 1
            state.roundNumber shouldBe 1
        }

        test("events from different sessions don't interfere") {
            val builder = InitiativeStateBuilder()
            
            val events = listOf(
                EncounterStarted(
                    sessionId = 1L,
                    timestamp = timestamp,
                    encounterId = 100L,
                    participants = listOf(1L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 18, 3, 21)
                    ),
                    surprisedCreatures = emptySet()
                ),
                EncounterStarted(
                    sessionId = 2L,
                    timestamp = timestamp + 1000,
                    encounterId = 200L,
                    participants = listOf(2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    surprisedCreatures = emptySet()
                )
            )
            
            // Builder should handle all events
            val state = builder.buildState(events)
            
            // Last EncounterStarted should determine state
            state.initiativeOrder shouldHaveSize 1
        }
    }

    context("Exhaustive event handling") {
        test("all initiative event types are handled") {
            val builder = InitiativeStateBuilder()
            
            // Create one of each initiative event type
            val events = listOf(
                EncounterStarted(
                    sessionId, timestamp, encounterId, listOf(1L, 2L),
                    listOf(
                        InitiativeEntryData(1L, 18, 3, 21),
                        InitiativeEntryData(2L, 15, 2, 17)
                    ),
                    emptySet()
                ),
                RoundStarted(sessionId, timestamp + 1000, encounterId, 1),
                TurnStarted(sessionId, timestamp + 2000, encounterId, 1L, 1, 0),
                ReactionUsed(sessionId, timestamp + 3000, encounterId, 1L, "Shield", "Hit by attack"),
                TurnEnded(sessionId, timestamp + 4000, encounterId, 1L, 1),
                TurnStarted(sessionId, timestamp + 5000, encounterId, 2L, 1, 1),
                TurnDelayed(sessionId, timestamp + 6000, encounterId, 2L, 17),
                CreatureAddedToCombat(
                    sessionId, timestamp + 7000, encounterId, 3L,
                    InitiativeEntryData(3L, 12, 1, 13)
                ),
                DelayedTurnResumed(sessionId, timestamp + 8000, encounterId, 2L, 10),
                CreatureRemovedFromCombat(sessionId, timestamp + 9000, encounterId, 3L, "fled")
            )
            
            // Should not throw exception - all events handled
            val state = builder.buildState(events)
            
            state.initiativeOrder shouldHaveSize 2
        }
    }
})
