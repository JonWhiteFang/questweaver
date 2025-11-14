package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.ActionResult
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
import dev.questweaver.core.rules.actions.validation.ActionValidator
import dev.questweaver.core.rules.actions.validation.ValidationResult
import dev.questweaver.domain.events.GameEvent

/**
 * Main coordinator that routes actions to appropriate handlers.
 * Validates actions before execution and returns results with generated events.
 */
class ActionProcessor(
    private val attackHandler: AttackActionHandler,
    private val movementHandler: MovementActionHandler,
    private val spellHandler: SpellActionHandler,
    private val specialHandler: SpecialActionHandler,
    private val validator: ActionValidator
) {
    /**
     * Processes a combat action.
     *
     * @param action The action to process
     * @param context Current action context (turn state, creatures, map)
     * @return ActionResult with events or error
     */
    suspend fun processAction(
        action: CombatAction,
        context: ActionContext
    ): ActionResult {
        // Validate action using ActionValidator
        val validationResult = validator.validate(action, context)
        if (validationResult !is ValidationResult.Valid) {
            return handleValidationFailure(validationResult)
        }
        
        // Route to appropriate handler based on action type
        return try {
            val events = routeActionToHandler(action, context)
            ActionResult.Success(events)
        } catch (e: IllegalArgumentException) {
            ActionResult.Failure(e.message ?: "Invalid action")
        } catch (e: IllegalStateException) {
            ActionResult.Failure(e.message ?: "Action processing failed")
        }
    }
    
    private fun handleValidationFailure(validationResult: ValidationResult): ActionResult {
        return when (validationResult) {
            is ValidationResult.Invalid -> ActionResult.Failure(validationResult.reason)
            is ValidationResult.RequiresChoice -> ActionResult.RequiresChoice(validationResult.options)
            is ValidationResult.Valid -> error("Should not reach here")
        }
    }
    
    private suspend fun routeActionToHandler(
        action: CombatAction,
        context: ActionContext
    ): List<GameEvent> {
        return when (action) {
            is Attack -> attackHandler.handleAttack(action, context)
            is Move -> movementHandler.handleMovement(action, context)
            is Dash -> movementHandler.handleDash(action, context)
            is CastSpell -> spellHandler.handleSpellCast(action, context)
            is Dodge -> specialHandler.handleDodge(action, context)
            is Disengage -> specialHandler.handleDisengage(action, context)
            is Help -> specialHandler.handleHelp(action, context)
            is Ready -> specialHandler.handleReady(action, context)
            is Reaction -> throw IllegalArgumentException("Reactions must be processed with a trigger")
        }
    }
}
