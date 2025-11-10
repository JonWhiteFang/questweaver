package dev.questweaver.domain.events

import dev.questweaver.domain.entities.InitiativeEntry
import dev.questweaver.domain.values.Condition
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.EncounterStatus
import dev.questweaver.domain.values.GridPos
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameEventTest : FunSpec({
    val json = Json { prettyPrint = true }

    context("event immutability") {
        test("EncounterStarted is immutable") {
            val event = EncounterStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 10L,
                participants = listOf(1L, 2L),
                initiativeOrder = listOf(
                    InitiativeEntry(1L, 15),
                    InitiativeEntry(2L, 12)
                )
            )
            
            // All properties are val - immutability enforced by compiler
            event.sessionId shouldBe 1L
            event.timestamp shouldBe 1000L
        }

        test("AttackResolved is immutable") {
            val event = AttackResolved(
                sessionId = 1L,
                timestamp = 2000L,
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 18),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            event.hit shouldBe true
            event.critical shouldBe false
        }

        test("MoveCommitted is immutable") {
            val event = MoveCommitted(
                sessionId = 1L,
                timestamp = 3000L,
                creatureId = 1L,
                fromPos = GridPos(0, 0),
                toPos = GridPos(5, 5),
                path = listOf(GridPos(0, 0), GridPos(1, 1), GridPos(5, 5)),
                movementCost = 15
            )
            
            event.movementCost shouldBe 15
        }
    }

    context("event serialization") {
        test("EncounterStarted serializes and deserializes correctly") {
            val original = EncounterStarted(
                sessionId = 1L,
                timestamp = 1000L,
                encounterId = 10L,
                participants = listOf(1L, 2L, 3L),
                initiativeOrder = listOf(
                    InitiativeEntry(1L, 18),
                    InitiativeEntry(2L, 15),
                    InitiativeEntry(3L, 12)
                )
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
            deserialized.shouldBeInstanceOf<EncounterStarted>()
        }

        test("RoundStarted serializes and deserializes correctly") {
            val original = RoundStarted(
                sessionId = 1L,
                timestamp = 2000L,
                encounterId = 10L,
                roundNumber = 3
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("TurnStarted serializes and deserializes correctly") {
            val original = TurnStarted(
                sessionId = 1L,
                timestamp = 3000L,
                encounterId = 10L,
                creatureId = 5L
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("TurnEnded serializes and deserializes correctly") {
            val original = TurnEnded(
                sessionId = 1L,
                timestamp = 4000L,
                encounterId = 10L,
                creatureId = 5L
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("EncounterEnded serializes and deserializes correctly") {
            val original = EncounterEnded(
                sessionId = 1L,
                timestamp = 5000L,
                encounterId = 10L,
                status = EncounterStatus.VICTORY
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("AttackResolved serializes and deserializes correctly") {
            val original = AttackResolved(
                sessionId = 1L,
                timestamp = 6000L,
                attackerId = 1L,
                targetId = 2L,
                attackRoll = DiceRoll(20, 1, 5, 18),
                targetAC = 15,
                hit = true,
                critical = false
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("DamageApplied serializes and deserializes correctly") {
            val original = DamageApplied(
                sessionId = 1L,
                timestamp = 7000L,
                targetId = 2L,
                damageRoll = DiceRoll(8, 2, 3, 15),
                damageAmount = 15,
                hpBefore = 30,
                hpAfter = 15
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("ConditionApplied serializes and deserializes correctly") {
            val original = ConditionApplied(
                sessionId = 1L,
                timestamp = 8000L,
                targetId = 2L,
                condition = Condition.POISONED,
                duration = 3
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("ConditionRemoved serializes and deserializes correctly") {
            val original = ConditionRemoved(
                sessionId = 1L,
                timestamp = 9000L,
                targetId = 2L,
                condition = Condition.POISONED
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }

        test("MoveCommitted serializes and deserializes correctly") {
            val original = MoveCommitted(
                sessionId = 1L,
                timestamp = 10000L,
                creatureId = 1L,
                fromPos = GridPos(0, 0),
                toPos = GridPos(5, 5),
                path = listOf(
                    GridPos(0, 0),
                    GridPos(1, 1),
                    GridPos(2, 2),
                    GridPos(5, 5)
                ),
                movementCost = 20
            )
            
            val serialized = json.encodeToString<GameEvent>(original)
            val deserialized = json.decodeFromString<GameEvent>(serialized)
            
            deserialized shouldBe original
        }
    }

    context("exhaustive when expressions") {
        test("sealed interface enables exhaustive when without else") {
            val events: List<GameEvent> = listOf(
                EncounterStarted(1L, 1000L, 10L, listOf(1L), listOf(InitiativeEntry(1L, 15))),
                RoundStarted(1L, 2000L, 10L, 1),
                TurnStarted(1L, 3000L, 10L, 1L),
                TurnEnded(1L, 4000L, 10L, 1L),
                EncounterEnded(1L, 5000L, 10L, EncounterStatus.VICTORY),
                AttackResolved(1L, 6000L, 1L, 2L, DiceRoll(20, 1, 0, 15), 15, true, false),
                DamageApplied(1L, 7000L, 2L, DiceRoll(8, 1, 0, 5), 5, 20, 15),
                ConditionApplied(1L, 8000L, 2L, Condition.STUNNED, null),
                ConditionRemoved(1L, 9000L, 2L, Condition.STUNNED),
                MoveCommitted(1L, 10000L, 1L, GridPos(0, 0), GridPos(5, 5), listOf(), 15)
            )
            
            // This when expression is exhaustive - compiler enforces all cases
            val results = events.map { event ->
                when (event) {
                    is EncounterStarted -> "encounter_started"
                    is RoundStarted -> "round_started"
                    is TurnStarted -> "turn_started"
                    is TurnEnded -> "turn_ended"
                    is EncounterEnded -> "encounter_ended"
                    is AttackResolved -> "attack_resolved"
                    is DamageApplied -> "damage_applied"
                    is ConditionApplied -> "condition_applied"
                    is ConditionRemoved -> "condition_removed"
                    is MoveCommitted -> "move_committed"
                    // No else branch needed - exhaustive
                }
            }
            
            results.size shouldBe 10
        }
    }
})
