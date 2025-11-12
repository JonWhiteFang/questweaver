package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.ActionType
import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ActionEconomyResource
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.GridPos
import dev.questweaver.core.rules.validation.state.TurnState

/**
 * Validator for action economy constraints in D&D 5e.
 *
 * Validates whether a creature has the required action economy resources available
 * to perform an action. In D&D 5e, creatures are limited to:
 * - One Action per turn
 * - One Bonus Action per turn (if available)
 * - One Reaction per round (resets at start of creature's turn)
 * - Movement up to their speed per turn
 *
 * Special cases:
 * - Dash action doubles movement speed for the turn
 * - Movement can be split before and after actions
 * - Reactions are consumed until the start of the creature's next turn
 */
class ActionEconomyValidator {

    /**
     * Validates whether the action can be taken given current action economy.
     *
     * Checks if the creature has the required action economy resources available
     * (action, bonus action, reaction, or movement) to perform the specified action.
     *
     * @param action The action to validate
     * @param turnState Current turn state with action economy tracking
     * @return ValidationResult indicating success or failure with reason
     */
    fun validateActionEconomy(
        action: GameAction,
        turnState: TurnState
    ): ValidationResult {
        // Verify the action is for the current creature
        if (action.actorId != turnState.creatureId) {
            return ValidationResult.Failure(
                ValidationFailure.InvalidTarget("Action actor does not match current turn creature")
            )
        }

        // Check action economy based on action type
        return when (action.actionType) {
            ActionType.Action -> validateAction(turnState)
            ActionType.BonusAction -> validateBonusAction(turnState)
            ActionType.Reaction -> validateReaction(turnState)
            ActionType.Movement -> validateMovement(action, turnState)
            ActionType.FreeAction -> validateFreeAction()
        }
    }

    /**
     * Determines which action economy resources would be consumed by an action.
     *
     * @param action The action being validated
     * @return Set of action economy resources consumed
     */
    fun getActionCost(action: GameAction): Set<ActionEconomyResource> {
        return when (action.actionType) {
            ActionType.Action -> setOf(ActionEconomyResource.Action)
            ActionType.BonusAction -> setOf(ActionEconomyResource.BonusAction)
            ActionType.Reaction -> setOf(ActionEconomyResource.Reaction)
            ActionType.Movement -> setOf(ActionEconomyResource.Movement)
            ActionType.FreeAction -> setOf(ActionEconomyResource.FreeAction)
        }
    }

    private fun validateAction(turnState: TurnState): ValidationResult {
        return if (turnState.actionUsed) {
            ValidationResult.Failure(
                ValidationFailure.ActionEconomyExhausted(
                    required = ActionEconomyResource.Action,
                    alreadyUsed = true
                )
            )
        } else {
            ValidationResult.Success(createResourceCost(ActionEconomyResource.Action))
        }
    }

    private fun validateBonusAction(turnState: TurnState): ValidationResult {
        return if (turnState.bonusActionUsed) {
            ValidationResult.Failure(
                ValidationFailure.ActionEconomyExhausted(
                    required = ActionEconomyResource.BonusAction,
                    alreadyUsed = true
                )
            )
        } else {
            ValidationResult.Success(createResourceCost(ActionEconomyResource.BonusAction))
        }
    }

    private fun validateReaction(turnState: TurnState): ValidationResult {
        return if (turnState.reactionUsed) {
            ValidationResult.Failure(
                ValidationFailure.ActionEconomyExhausted(
                    required = ActionEconomyResource.Reaction,
                    alreadyUsed = true
                )
            )
        } else {
            ValidationResult.Success(createResourceCost(ActionEconomyResource.Reaction))
        }
    }

    private fun validateMovement(action: GameAction, turnState: TurnState): ValidationResult {
        val movementCost = when (action) {
            is GameAction.Move -> calculateMovementCost(action.path)
            else -> 0
        }

        return if (movementCost > turnState.remainingMovement()) {
            ValidationResult.Failure(
                ValidationFailure.ActionEconomyExhausted(
                    required = ActionEconomyResource.Movement,
                    alreadyUsed = false
                )
            )
        } else {
            ValidationResult.Success(
                ResourceCost(
                    actionEconomy = setOf(ActionEconomyResource.Movement),
                    resources = emptySet(),
                    movementCost = movementCost,
                    breaksConcentration = false
                )
            )
        }
    }

    private fun validateFreeAction(): ValidationResult {
        return ValidationResult.Success(createResourceCost(ActionEconomyResource.FreeAction))
    }

    private fun createResourceCost(resource: ActionEconomyResource): ResourceCost {
        return ResourceCost(
            actionEconomy = setOf(resource),
            resources = emptySet(),
            movementCost = 0,
            breaksConcentration = false
        )
    }

    /**
     * Calculates the movement cost in feet for a path.
     *
     * In D&D 5e, each grid square is 5 feet. Diagonal movement costs the same
     * as orthogonal movement (simplified rule, not the alternating 5/10 variant).
     *
     * @param path The sequence of grid positions to move through
     * @return The total movement cost in feet
     */
    private fun calculateMovementCost(path: List<GridPos>): Int {
        if (path.isEmpty()) return 0
        
        // Each step in the path costs 5 feet (one grid square)
        // Path includes starting position, so actual moves = path.size - 1
        return (path.size - 1) * FEET_PER_GRID_SQUARE
    }

    companion object {
        /**
         * The number of feet represented by one grid square in D&D 5e.
         */
        private const val FEET_PER_GRID_SQUARE = 5
    }
}
