package dev.questweaver.core.rules.validation.results

/**
 * Sealed interface representing the result of an action validation.
 *
 * Validation results indicate whether an action is legal, illegal with a specific reason,
 * or requires additional choices from the user before validation can complete.
 */
sealed interface ValidationResult {
    /**
     * Validation succeeded. The action is legal and can be executed.
     *
     * @property resourceCost The resources that would be consumed by executing this action
     */
    data class Success(val resourceCost: ResourceCost) : ValidationResult

    /**
     * Validation failed. The action is illegal for a specific reason.
     *
     * @property reason The specific reason why the action failed validation
     */
    data class Failure(val reason: ValidationFailure) : ValidationResult

    /**
     * Validation requires additional choices from the user.
     *
     * @property choices The list of choices that must be made before validation can complete
     */
    data class RequiresChoice(val choices: List<ActionChoice>) : ValidationResult
}
