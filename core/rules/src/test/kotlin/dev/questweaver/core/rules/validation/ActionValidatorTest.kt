package dev.questweaver.core.rules.validation

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ActionEconomyResource
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.ConcentrationInfo
import dev.questweaver.core.rules.validation.state.ConcentrationState
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.GridPos
import dev.questweaver.core.rules.validation.state.Resource
import dev.questweaver.core.rules.validation.state.ResourcePool
import dev.questweaver.core.rules.validation.state.TurnState
import dev.questweaver.core.rules.validation.validators.ActionEconomyValidator
import dev.questweaver.core.rules.validation.validators.ConcentrationValidator
import dev.questweaver.core.rules.validation.validators.ConditionValidator
import dev.questweaver.core.rules.validation.validators.RangeValidator
import dev.questweaver.core.rules.validation.validators.ResourceValidator
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Integration tests for ActionValidator.
 *
 * Tests verify that:
 * - Complete validation flow with all checks
 * - Fail-fast behavior: first failure returned
 * - Resource cost aggregation from multiple validators
 * - Turn state updates after validation
 * - Deterministic validation with same inputs
 * - Multiple simultaneous failures prioritized correctly
 */
class ActionValidatorTest : FunSpec({
    val actionEconomyValidator = ActionEconomyValidator()
    val resourceValidator = ResourceValidator()
    val rangeValidator = RangeValidator()
    val concentrationValidator = ConcentrationValidator()
    val conditionValidator = ConditionValidator(ConditionRegistry)
    
    val validator = ActionValidator(
        actionEconomyValidator,
        resourceValidator,
        rangeValidator,
        concentrationValidator,
        conditionValidator
    )

    val actorId = 1L
    val targetId = 2L
    val baseMovementSpeed = 30

    data class TurnStateConfig(
        val actionUsed: Boolean = false,
        val bonusActionUsed: Boolean = false,
        val reactionUsed: Boolean = false,
        val movementUsed: Int = 0,
        val spellSlots: Map<Int, Int> = emptyMap(),
        val concentrating: Boolean = false
    )

    fun createTurnState(config: TurnStateConfig = TurnStateConfig()): TurnState {
        val concentrationState = if (config.concentrating) {
            ConcentrationState(
                activeConcentrations = mapOf(
                    actorId to ConcentrationInfo("bless", 1, 10)
                )
            )
        } else {
            ConcentrationState.Empty
        }

        return TurnState(
            creatureId = actorId,
            round = 1,
            actionUsed = config.actionUsed,
            bonusActionUsed = config.bonusActionUsed,
            reactionUsed = config.reactionUsed,
            movementUsed = config.movementUsed,
            movementTotal = baseMovementSpeed,
            resourcePool = ResourcePool(spellSlots = config.spellSlots),
            concentrationState = concentrationState
        )
    }

    fun createEncounterState(
        actorPos: GridPos,
        targetPos: GridPos? = null,
        obstacles: Set<GridPos> = emptySet()
    ): EncounterState {
        val positions = mutableMapOf(actorId to actorPos)
        if (targetPos != null) {
            positions[targetId] = targetPos
        }
        return EncounterState(
            positions = positions,
            obstacles = obstacles
        )
    }

    context("Complete validation flow") {
        test("all checks pass for valid action") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 2)
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.actionEconomy shouldContain ActionEconomyResource.Action
            cost.resources shouldContain Resource.SpellSlot(1)
        }

        test("attack action passes all checks") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = false))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0) // Adjacent
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Fail-fast behavior") {
        test("condition check fails first") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = false))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0)
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )
            val conditions = setOf(Condition.Stunned)

            val result = validator.validate(
                action,
                conditions,
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
        }

        test("action economy check fails when action used") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = true))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0)
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.ActionEconomyExhausted>()
        }

        test("resource check fails when no spell slots") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 0) // No slots
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.InsufficientResources>()
        }

        test("range check fails when out of range") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = false))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(20, 0) // 100 feet away
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null // Touch range
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            failure.shouldBeInstanceOf<ValidationFailure.OutOfRange>()
        }
    }

    context("Resource cost aggregation") {
        test("aggregates action economy and resources") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 2)
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.actionEconomy shouldContain ActionEconomyResource.Action
            cost.resources shouldContain Resource.SpellSlot(1)
            cost.movementCost shouldBe 0
            cost.breaksConcentration shouldBe false
        }

        test("aggregates concentration breaking flag") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 2),
                    concentrating = true
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "haste", // Concentration spell
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.breaksConcentration shouldBe true
        }

        test("aggregates movement cost") {
            val turnState = createTurnState(TurnStateConfig(movementUsed = 0))
            val encounterState = createEncounterState(actorPos = GridPos(0, 0))
            val path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0)
            )
            val action = GameAction.Move(actorId = actorId, path = path)

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.movementCost shouldBe 10 // 2 moves * 5 feet
        }
    }

    context("Turn state updates") {
        test("consuming resources updates turn state correctly") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 2)
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            val updatedState = turnState.consumeResources(cost)

            updatedState.actionUsed shouldBe true
            updatedState.resourcePool.spellSlots[1] shouldBe 1
        }

        test("consuming movement updates turn state correctly") {
            val turnState = createTurnState(TurnStateConfig(movementUsed = 10))
            val encounterState = createEncounterState(actorPos = GridPos(0, 0))
            val path = listOf(
                GridPos(0, 0),
                GridPos(1, 0),
                GridPos(2, 0)
            )
            val action = GameAction.Move(actorId = actorId, path = path)

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            val updatedState = turnState.consumeResources(cost)

            updatedState.movementUsed shouldBe 20 // 10 + 10
            updatedState.remainingMovement() shouldBe 10
        }

        test("breaking concentration updates turn state correctly") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    spellSlots = mapOf(1 to 2),
                    concentrating = true
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "haste",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            val updatedState = turnState.consumeResources(cost)

            updatedState.concentrationState.isConcentrating(actorId) shouldBe false
        }
    }

    context("Deterministic validation") {
        test("same inputs produce same result") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = false))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0)
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result1 = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )
            val result2 = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result1::class shouldBe result2::class
            if (result1 is ValidationResult.Success && result2 is ValidationResult.Success) {
                result1.resourceCost shouldBe result2.resourceCost
            }
        }
    }

    context("Multiple simultaneous failures") {
        test("condition failure prioritized over action economy") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = true))
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0)
            )
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )
            val conditions = setOf(Condition.Stunned)

            val result = validator.validate(
                action,
                conditions,
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            // Condition check comes first, so it should fail first
            failure.shouldBeInstanceOf<ValidationFailure.ConditionPreventsAction>()
        }

        test("action economy failure prioritized over resources") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = true,
                    spellSlots = mapOf(1 to 0)
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(5, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Failure>()
            val failure = (result as ValidationResult.Failure).reason
            // Action economy check comes before resource check
            failure.shouldBeInstanceOf<ValidationFailure.ActionEconomyExhausted>()
        }
    }

    context("Position unknown handling") {
        test("validation succeeds when actor position unknown") {
            val turnState = createTurnState(TurnStateConfig(actionUsed = false))
            val encounterState = EncounterState() // No positions
            val action = GameAction.Attack(
                actorId = actorId,
                targetId = targetId,
                weaponId = null
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            // Should succeed because range validation is skipped when position unknown
            result.shouldBeInstanceOf<ValidationResult.Success>()
        }
    }

    context("Complex scenarios") {
        test("casting spell while moving") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = false,
                    movementUsed = 15,
                    spellSlots = mapOf(1 to 1)
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(3, 0),
                targetPos = GridPos(8, 0)
            )
            val action = GameAction.CastSpell(
                actorId = actorId,
                spellId = "magic_missile_1st",
                targetIds = listOf(targetId),
                slotLevel = 1
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.actionEconomy shouldContain ActionEconomyResource.Action
            cost.resources shouldContain Resource.SpellSlot(1)
            cost.movementCost shouldBe 0 // Spell doesn't consume movement
        }

        test("opportunity attack as reaction") {
            val turnState = createTurnState(
                TurnStateConfig(
                    actionUsed = true, // Already used action
                    reactionUsed = false
                )
            )
            val encounterState = createEncounterState(
                actorPos = GridPos(0, 0),
                targetPos = GridPos(1, 0)
            )
            val action = GameAction.OpportunityAttack(
                actorId = actorId,
                targetId = targetId
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            result.shouldBeInstanceOf<ValidationResult.Success>()
            val cost = (result as ValidationResult.Success).resourceCost
            cost.actionEconomy shouldContain ActionEconomyResource.Reaction
        }
    }
})
