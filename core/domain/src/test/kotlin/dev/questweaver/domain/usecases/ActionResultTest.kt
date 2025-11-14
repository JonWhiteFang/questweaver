package dev.questweaver.domain.usecases

import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.TurnEnded
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ActionResultTest : FunSpec({
    val json = Json { prettyPrint = true }

    context("ActionResult subtypes") {
        test("Success contains list of GameEvent instances") {
            val events = listOf<GameEvent>(
                EncounterStarted(
                    sessionId = 1L,
                    timestamp = 1000L,
                    encounterId = 10L,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17))
                ),
                TurnEnded(
                    sessionId = 1L,
                    timestamp = 2000L,
                    encounterId = 10L,
                    creatureId = 1L
                )
            )
            
            val result = ActionResult.Success(events)
            
            result.events.size shouldBe 2
            result.events[0].shouldBeInstanceOf<EncounterStarted>()
            result.events[1].shouldBeInstanceOf<TurnEnded>()
        }

        test("Failure contains reason string") {
            val result = ActionResult.Failure("Target is out of range")
            
            result.reason shouldBe "Target is out of range"
        }

        test("RequiresChoice contains list of ActionOption instances") {
            val options = listOf(
                ActionOption(
                    id = "spell_slot_1",
                    description = "Cast using 1st level spell slot",
                    metadata = mapOf("level" to "1")
                ),
                ActionOption(
                    id = "spell_slot_2",
                    description = "Cast using 2nd level spell slot",
                    metadata = mapOf("level" to "2")
                )
            )
            
            val result = ActionResult.RequiresChoice(options)
            
            result.options.size shouldBe 2
            result.options[0].id shouldBe "spell_slot_1"
            result.options[1].description shouldBe "Cast using 2nd level spell slot"
        }
    }

    context("exhaustive when expressions for ActionResult") {
        test("sealed interface enables exhaustive when without else") {
            val results: List<ActionResult> = listOf(
                ActionResult.Success(
                    listOf(
                        EncounterStarted(1L, 1000L, 10L, listOf(1L), listOf(InitiativeEntryData(1L, 15, 2, 17)))
                    )
                ),
                ActionResult.Failure("Invalid action"),
                ActionResult.RequiresChoice(
                    listOf(
                        ActionOption("option1", "First option"),
                        ActionOption("option2", "Second option")
                    )
                )
            )
            
            // This when expression is exhaustive - compiler enforces all cases
            val outcomes = results.map { result ->
                when (result) {
                    is ActionResult.Success -> "success"
                    is ActionResult.Failure -> "failure"
                    is ActionResult.RequiresChoice -> "requires_choice"
                    // No else branch needed - exhaustive
                }
            }
            
            outcomes shouldBe listOf("success", "failure", "requires_choice")
        }

        test("exhaustive when can extract values from each subtype") {
            val successResult: ActionResult = ActionResult.Success(
                listOf(
                    EncounterStarted(1L, 1000L, 10L, listOf(1L), listOf(InitiativeEntryData(1L, 15, 2, 17)))
                )
            )
            val failureResult: ActionResult = ActionResult.Failure("Action failed")
            val choiceResult: ActionResult = ActionResult.RequiresChoice(
                listOf(ActionOption("opt1", "Option 1"))
            )
            
            val successValue = when (successResult) {
                is ActionResult.Success -> successResult.events.size
                is ActionResult.Failure -> -1
                is ActionResult.RequiresChoice -> -2
            }
            
            val failureValue = when (failureResult) {
                is ActionResult.Success -> "no error"
                is ActionResult.Failure -> failureResult.reason
                is ActionResult.RequiresChoice -> "no error"
            }
            
            val choiceValue = when (choiceResult) {
                is ActionResult.Success -> 0
                is ActionResult.Failure -> 0
                is ActionResult.RequiresChoice -> choiceResult.options.size
            }
            
            successValue shouldBe 1
            failureValue shouldBe "Action failed"
            choiceValue shouldBe 1
        }
    }

    context("ActionOption creation and serialization") {
        test("ActionOption can be created with minimal properties") {
            val option = ActionOption(
                id = "attack_melee",
                description = "Attack with melee weapon"
            )
            
            option.id shouldBe "attack_melee"
            option.description shouldBe "Attack with melee weapon"
            option.metadata shouldBe emptyMap()
        }

        test("ActionOption can be created with metadata") {
            val option = ActionOption(
                id = "cast_spell",
                description = "Cast Fireball",
                metadata = mapOf(
                    "spell_name" to "Fireball",
                    "spell_level" to "3",
                    "damage_type" to "fire"
                )
            )
            
            option.metadata["spell_name"] shouldBe "Fireball"
            option.metadata["spell_level"] shouldBe "3"
            option.metadata["damage_type"] shouldBe "fire"
        }

        test("ActionOption serializes and deserializes correctly") {
            val original = ActionOption(
                id = "move_to_position",
                description = "Move to grid position (5, 5)",
                metadata = mapOf(
                    "x" to "5",
                    "y" to "5",
                    "cost" to "15"
                )
            )
            
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ActionOption>(serialized)
            
            deserialized shouldBe original
            deserialized.id shouldBe "move_to_position"
            deserialized.metadata["x"] shouldBe "5"
        }

        test("ActionOption with empty metadata serializes correctly") {
            val original = ActionOption(
                id = "end_turn",
                description = "End your turn"
            )
            
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ActionOption>(serialized)
            
            deserialized shouldBe original
            deserialized.metadata shouldBe emptyMap()
        }
    }
})
