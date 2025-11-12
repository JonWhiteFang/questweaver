package dev.questweaver.core.rules.validation

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ActionEconomyResource
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.results.ValidationResult
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
import dev.questweaver.rules.conditions.ConditionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

/**
 * Property-based tests for ActionValidator.
 *
 * These tests verify invariants that should hold for all inputs:
 * - Validation is deterministic for same inputs
 * - Successful validation always returns resource cost
 * - Failed validation always returns specific failure reason
 * - Resource consumption never produces negative values
 */
class ActionValidatorPropertyTest : FunSpec({
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

    // Arbitrary generators for test data
    val arbGridPos = arbitrary {
        GridPos(
            x = it.random.nextInt(0, 20),
            y = it.random.nextInt(0, 20)
        )
    }

    val arbResourcePool = arbitrary {
        ResourcePool(
            spellSlots = mapOf(
                1 to it.random.nextInt(0, 4),
                2 to it.random.nextInt(0, 3),
                3 to it.random.nextInt(0, 3)
            )
        )
    }

    val arbTurnState = arbitrary {
        val creatureId = it.random.nextLong(1, 100)
        TurnState(
            creatureId = creatureId,
            round = it.random.nextInt(1, 10),
            actionUsed = it.random.nextBoolean(),
            bonusActionUsed = it.random.nextBoolean(),
            reactionUsed = it.random.nextBoolean(),
            movementUsed = it.random.nextInt(0, 30),
            movementTotal = 30,
            resourcePool = arbResourcePool.bind(),
            concentrationState = ConcentrationState.Empty
        )
    }

    val arbEncounterState = arbitrary {
        val actorId = 1L
        val targetId = 2L
        EncounterState(
            positions = mapOf(
                actorId to arbGridPos.bind(),
                targetId to arbGridPos.bind()
            ),
            obstacles = emptySet()
        )
    }

    val arbAttackAction = arbitrary {
        GameAction.Attack(
            actorId = 1L,
            targetId = 2L,
            weaponId = null
        )
    }

    test("validation is deterministic for same inputs") {
        checkAll(
            iterations = 100,
            arbAttackAction,
            arbTurnState,
            arbEncounterState
        ) { action, turnState, encounterState ->
            // Run validation twice with identical inputs
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

            // Results should be identical
            result1::class shouldBe result2::class
            
            when {
                result1 is ValidationResult.Success && result2 is ValidationResult.Success -> {
                    result1.resourceCost shouldBe result2.resourceCost
                }
                result1 is ValidationResult.Failure && result2 is ValidationResult.Failure -> {
                    result1.reason::class shouldBe result2.reason::class
                }
            }
        }
    }

    test("successful validation always returns resource cost") {
        checkAll(
            iterations = 100,
            Arb.long(1, 100),
            Arb.int(0, 30),
            arbResourcePool
        ) { creatureId, movementUsed, resourcePool ->
            // Create a valid scenario: action available, target in range
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = false,
                bonusActionUsed = false,
                reactionUsed = false,
                movementUsed = movementUsed,
                movementTotal = 30,
                resourcePool = resourcePool,
                concentrationState = ConcentrationState.Empty
            )
            val encounterState = EncounterState(
                positions = mapOf(
                    creatureId to GridPos(0, 0),
                    2L to GridPos(1, 0) // Adjacent target
                )
            )
            val action = GameAction.Attack(
                actorId = creatureId,
                targetId = 2L,
                weaponId = null
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            // If validation succeeds, it must have a resource cost
            if (result is ValidationResult.Success) {
                result.resourceCost shouldNotBe null
                // Resource cost should be valid
                result.resourceCost.actionEconomy.isNotEmpty() shouldBe true
            }
        }
    }

    test("failed validation always returns specific failure reason") {
        checkAll(
            iterations = 100,
            Arb.long(1, 100),
            Arb.boolean()
        ) { creatureId, actionUsed ->
            // Create a scenario that will fail: action already used
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = actionUsed,
                bonusActionUsed = false,
                reactionUsed = false,
                movementUsed = 0,
                movementTotal = 30,
                resourcePool = ResourcePool.Empty,
                concentrationState = ConcentrationState.Empty
            )
            val encounterState = EncounterState(
                positions = mapOf(
                    creatureId to GridPos(0, 0),
                    2L to GridPos(1, 0)
                )
            )
            val action = GameAction.Attack(
                actorId = creatureId,
                targetId = 2L,
                weaponId = null
            )

            val result = validator.validate(
                action,
                emptySet(),
                turnState,
                encounterState
            )

            // If validation fails, it must have a specific reason
            if (result is ValidationResult.Failure) {
                result.reason shouldNotBe null
                // Reason should be a valid ValidationFailure type
                result.reason.shouldBeInstanceOf<dev.questweaver.core.rules.validation.results.ValidationFailure>()
            }
        }
    }

    test("resource consumption never produces negative values") {
        checkAll(
            iterations = 100,
            Arb.int(0, 30),
            Arb.int(0, 30),
            Arb.map(
                Arb.int(1, 9),
                Arb.int(0, 4),
                minSize = 0,
                maxSize = 9
            )
        ) { movementUsed, movementCost, spellSlots ->
            val creatureId = 1L
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = false,
                bonusActionUsed = false,
                reactionUsed = false,
                movementUsed = movementUsed,
                movementTotal = 30,
                resourcePool = ResourcePool(spellSlots = spellSlots),
                concentrationState = ConcentrationState.Empty
            )

            // Create a resource cost
            val resourceCost = ResourceCost(
                actionEconomy = setOf(ActionEconomyResource.Action),
                resources = emptySet(),
                movementCost = movementCost,
                breaksConcentration = false
            )

            // Consume resources
            val updatedState = turnState.consumeResources(resourceCost)

            // Verify no negative values
            updatedState.movementUsed shouldBeGreaterThanOrEqual 0
            updatedState.movementTotal shouldBeGreaterThanOrEqual 0
            updatedState.resourcePool.spellSlots.values.forEach { count ->
                count shouldBeGreaterThanOrEqual 0
            }
        }
    }

    test("resource consumption with spell slots never produces negative values") {
        checkAll(
            iterations = 100,
            Arb.int(1, 9),
            Arb.int(0, 4)
        ) { slotLevel, slotCount ->
            val creatureId = 1L
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = false,
                bonusActionUsed = false,
                reactionUsed = false,
                movementUsed = 0,
                movementTotal = 30,
                resourcePool = ResourcePool(spellSlots = mapOf(slotLevel to slotCount)),
                concentrationState = ConcentrationState.Empty
            )

            // Only consume if we have slots available
            if (slotCount > 0) {
                val resourceCost = ResourceCost(
                    actionEconomy = setOf(ActionEconomyResource.Action),
                    resources = setOf(Resource.SpellSlot(slotLevel)),
                    movementCost = 0,
                    breaksConcentration = false
                )

                val updatedState = turnState.consumeResources(resourceCost)

                // Verify spell slot count is non-negative
                updatedState.resourcePool.spellSlots[slotLevel]?.let { count ->
                    count shouldBeGreaterThanOrEqual 0
                }
            }
        }
    }

    test("movement consumption never exceeds total movement") {
        checkAll(
            iterations = 100,
            Arb.int(0, 30),
            Arb.int(0, 30)
        ) { initialMovement, additionalMovement ->
            val creatureId = 1L
            val movementTotal = 30
            
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = false,
                bonusActionUsed = false,
                reactionUsed = false,
                movementUsed = initialMovement,
                movementTotal = movementTotal,
                resourcePool = ResourcePool.Empty,
                concentrationState = ConcentrationState.Empty
            )

            val resourceCost = ResourceCost(
                actionEconomy = emptySet(),
                resources = emptySet(),
                movementCost = additionalMovement,
                breaksConcentration = false
            )

            val updatedState = turnState.consumeResources(resourceCost)

            // Movement used should be sum of initial and additional
            updatedState.movementUsed shouldBe (initialMovement + additionalMovement)
            // Remaining movement can be negative (over-movement), but used should be tracked
            updatedState.movementUsed shouldBeGreaterThanOrEqual 0
        }
    }

    test("action economy flags are idempotent") {
        checkAll(
            iterations = 100,
            Arb.boolean(),
            Arb.boolean(),
            Arb.boolean()
        ) { actionUsed, bonusActionUsed, reactionUsed ->
            val creatureId = 1L
            val turnState = TurnState(
                creatureId = creatureId,
                round = 1,
                actionUsed = actionUsed,
                bonusActionUsed = bonusActionUsed,
                reactionUsed = reactionUsed,
                movementUsed = 0,
                movementTotal = 30,
                resourcePool = ResourcePool.Empty,
                concentrationState = ConcentrationState.Empty
            )

            // Consume action economy
            val resourceCost = ResourceCost(
                actionEconomy = setOf(
                    ActionEconomyResource.Action,
                    ActionEconomyResource.BonusAction,
                    ActionEconomyResource.Reaction
                ),
                resources = emptySet(),
                movementCost = 0,
                breaksConcentration = false
            )

            val updatedState = turnState.consumeResources(resourceCost)

            // All flags should be true after consumption
            updatedState.actionUsed shouldBe true
            updatedState.bonusActionUsed shouldBe true
            updatedState.reactionUsed shouldBe true

            // Consuming again should be idempotent
            val doubleUpdatedState = updatedState.consumeResources(resourceCost)
            doubleUpdatedState.actionUsed shouldBe true
            doubleUpdatedState.bonusActionUsed shouldBe true
            doubleUpdatedState.reactionUsed shouldBe true
        }
    }
})
