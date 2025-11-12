package dev.questweaver.core.rules.validation.validators

import dev.questweaver.core.rules.validation.actions.ActionType
import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ResourceCost
import dev.questweaver.core.rules.validation.results.ValidationFailure
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry

/**
 * Validates whether conditions prevent actions.
 *
 * In D&D 5e, certain conditions (Stunned, Incapacitated, Paralyzed, Unconscious)
 * prevent creatures from taking actions or reactions. This validator checks if
 * any active conditions block the attempted action.
 */
class ConditionValidator(
    private val conditionRegistry: ConditionRegistry = ConditionRegistry
) {
    /**
     * Validates whether the actor's conditions allow the action.
     *
     * @param action The action to validate
     * @param actorConditions Active conditions on the actor
     * @return ValidationResult indicating success or failure with blocking condition
     */
    fun validateConditions(
        action: GameAction,
        actorConditions: Set<Condition>
    ): ValidationResult {
        // Check if any condition blocks this type of action
        val blockingCondition = when (action.actionType) {
            ActionType.Action -> conditionRegistry.getBlockingCondition(actorConditions)
            ActionType.BonusAction -> conditionRegistry.getBlockingCondition(actorConditions)
            ActionType.Reaction -> conditionRegistry.getBlockingReactionCondition(actorConditions)
            ActionType.Movement -> conditionRegistry.getBlockingMovementCondition(actorConditions)
            ActionType.FreeAction -> null  // Free actions are never blocked
        }

        return if (blockingCondition != null) {
            ValidationResult.Failure(
                ValidationFailure.ConditionPreventsAction(
                    condition = blockingCondition,
                    reason = conditionRegistry.getPreventionReason(
                        blockingCondition,
                        action.actionType.name.lowercase()
                    )
                )
            )
        } else {
            ValidationResult.Success(ResourceCost.None)
        }
    }

    /**
     * Checks if any conditions prevent all actions.
     *
     * @param conditions Active conditions on the creature
     * @return Blocking condition, if any
     */
    fun getBlockingCondition(conditions: Set<Condition>): Condition? {
        return conditionRegistry.getBlockingCondition(conditions)
    }
}
