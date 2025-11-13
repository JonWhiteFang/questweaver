package dev.questweaver.core.rules.initiative.models

/**
 * Result type for initiative operations that can fail.
 *
 * Provides type-safe error handling for initiative tracker operations.
 * Use exhaustive when expressions to handle all cases.
 *
 * Example usage:
 * ```
 * when (val result = tracker.advanceTurn(state)) {
 *     is InitiativeResult.Success -> applyState(result.value)
 *     is InitiativeResult.InvalidState -> showError(result.reason)
 * }
 * ```
 */
sealed interface InitiativeResult<out T> {
    /**
     * Operation succeeded with a value.
     *
     * @property value The successful result value
     */
    data class Success<T>(val value: T) : InitiativeResult<T>
    
    /**
     * Operation failed due to invalid state.
     *
     * @property reason Human-readable explanation of why the operation failed
     */
    data class InvalidState(val reason: String) : InitiativeResult<Nothing>
}
