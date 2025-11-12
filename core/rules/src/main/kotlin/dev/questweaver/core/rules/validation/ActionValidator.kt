package dev.questweaver.core.rules.validation

import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.GridPos
import dev.questweaver.core.rules.validation.state.TurnState
import dev.questweaver.core.rules.validation.validators.ActionEconomyValidator
import dev.questweaver.core.rules.validation.validators.ConditionValidator
import dev.questweaver.core.rules.validation.validators.ConcentrationValidator
import dev.questweaver.core.rules.validation.validators.RangeValidator
import dev.questweaver.core.rules.validation.validators.ResourceValidator
import dev.questweaver.rules.conditions.Condition

/**
 * Main orchestrator for action validation.
 *
 * Coordinates all validation checks to determine if an action is legal given
 * the current game state. Validation is performed in a fail-fast manner:
 * the first failure encountered is returned immediately.
 *
 * Validation order:
 * 1. Conditions - Check if conditions prevent the action
 * 2. Action Economy - Check if action/bonus/reaction/movement available
 * 3. Resources - Check if required resources (spell slots, etc.) available
 * 4. Range - Check if target is within range and line-of-effect
 * 5. Concentration - Check if casting concentration spell while concentrating
 */
class ActionValidator(
    private val actionEconomyValidator: ActionEconomyValidator,
    private val resourceValidator: ResourceValidator,
    private val rangeValidator: RangeValidator,
    private val concentrationValidator: ConcentrationValidator,
    private val conditionValidator: ConditionValidator
) {
    /**
     * Validates whether an action can be performed given current game state.
     *
     * Performs all validation checks in order, returning the first failure encountered.
     * If all checks pass, returns Success with the aggregated resource cost.
     *
     * @param action The action to validate
     * @param actorConditions Active conditions on the actor
     * @param turnState Current turn phase and resource availability
     * @param encounterState Current encounter state (positions, obstacles)
     * @return ValidationResult indicating success, failure, or required choices
     */
    fun validate(
        action: GameAction,
        actorConditions: Set<Condition>,
        turnState: TurnState,
        encounterState: EncounterState
    ): ValidationResult {
        // Perform all validation checks in sequence
        val validationResults = listOf(
            conditionValidator.validateConditions(action, actorConditions),
            actionEconomyValidator.validateActionEconomy(action, turnState),
            resourceValidator.validateResources(action, turnState.resourcePool),
            validateRangeIfPositionKnown(action, encounterState),
            concentrationValidator.validateConcentration(
                action,
                action.actorId,
                turnState.concentrationState
            )
        )

        // Return first failure, or aggregate costs if all passed
        return validationResults.firstOrNull { it is ValidationResult.Failure }
            ?: ValidationResult.Success(aggregateResourceCosts(validationResults))
    }

    /**
     * Validates range and line-of-effect if actor position is known.
     *
     * @param action The action to validate
     * @param encounterState Current encounter state with positions
     * @return ValidationResult from range validation, or Success if position unknown
     */
    private fun validateRangeIfPositionKnown(
        action: GameAction,
        encounterState: EncounterState
    ): ValidationResult {
        val actorPos = encounterState.positions[action.actorId]
        return if (actorPos != null) {
            val targetPos = getTargetPosition(action, encounterState)
            rangeValidator.validateRange(action, actorPos, targetPos, encounterState)
        } else {
            ValidationResult.Success(ResourceCost.None)
        }
    }

    /**
     * Extracts the target position from an action.
     *
     * @param action The action to extract target position from
     * @param encounterState Current encounter state with positions
     * @return The target position, or null if action has no target or target position unknown
     */
    private fun getTargetPosition(
        action: GameAction,
        encounterState: EncounterState
    ): GridPos? {
        return when (action) {
            is GameAction.Attack -> encounterState.positions[action.targetId]
            is GameAction.CastSpell -> action.targetPos ?: action.targetIds.firstOrNull()?.let { 
                encounterState.positions[it] 
            }
            is GameAction.OpportunityAttack -> encounterState.positions[action.targetId]
            is GameAction.UseClassFeature -> action.targetId?.let { encounterState.positions[it] }
            is GameAction.Move,
            is GameAction.Dash,
            is GameAction.Disengage,
            is GameAction.Dodge -> null
        }
    }

    /**
     * Aggregates resource costs from all successful validation results.
     *
     * Combines action economy resources, consumable resources, movement costs,
     * and concentration breaking flags from all validators.
     *
     * @param results The validation results to aggregate
     * @return A single ResourceCost with all costs combined
     */
    private fun aggregateResourceCosts(results: List<ValidationResult>): ResourceCost {
        val costs = results.filterIsInstance<ValidationResult.Success>()
            .map { it.resourceCost }

        return ResourceCost(
            actionEconomy = costs.flatMap { it.actionEconomy }.toSet(),
            resources = costs.flatMap { it.resources }.toSet(),
            movementCost = costs.sumOf { it.movementCost },
            breaksConcentration = costs.any { it.breaksConcentration }
        )
    }
}
