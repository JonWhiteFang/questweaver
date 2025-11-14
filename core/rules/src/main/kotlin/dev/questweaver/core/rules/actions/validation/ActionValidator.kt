package dev.questweaver.core.rules.actions.validation

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.ActionOption
import dev.questweaver.core.rules.actions.models.Attack
import dev.questweaver.core.rules.actions.models.CastSpell
import dev.questweaver.core.rules.actions.models.CombatAction
import dev.questweaver.core.rules.actions.models.Dash
import dev.questweaver.core.rules.actions.models.Disengage
import dev.questweaver.core.rules.actions.models.Dodge
import dev.questweaver.core.rules.actions.models.Help
import dev.questweaver.core.rules.actions.models.Move
import dev.questweaver.core.rules.actions.models.Reaction
import dev.questweaver.core.rules.actions.models.Ready

/**
 * Sealed interface representing validation results.
 */
sealed interface ValidationResult {
    /**
     * Action is valid and can be executed.
     */
    object Valid : ValidationResult
    
    /**
     * Action is invalid and cannot be executed.
     */
    data class Invalid(val reason: String) : ValidationResult
    
    /**
     * Action requires a choice from the player before execution.
     */
    data class RequiresChoice(val options: List<ActionOption>) : ValidationResult
}

/**
 * Placeholder interface for the Action Validation System.
 * TODO: Implement as part of 06-action-validation spec.
 */
interface ActionValidationSystem {
    fun validateAction(action: CombatAction, context: ActionContext): ValidationResult
}

/**
 * Pre-execution validation using the Action Validation System.
 */
class ActionValidator(
    private val actionValidationSystem: ActionValidationSystem
) {
    /**
     * Validates an action before execution.
     *
     * @param action The action to validate
     * @param context Current action context
     * @return ValidationResult (Valid, Invalid, RequiresChoice)
     */
    fun validate(
        action: CombatAction,
        context: ActionContext
    ): ValidationResult {
        // Delegate to the Action Validation System
        return actionValidationSystem.validateAction(action, context)
    }
}
