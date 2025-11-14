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

/**
 * Main coordinator that routes actions to appropriate handlers.
 * Validates actions before execution and returns results with generated events.
 */
class ActionProcessor(
    private val attackHandler: AttackActionHandler,
    private val movementHandler: MovementActionHandler,
    private val spellHandler: SpellActionHandler,
    private val specialHandler: SpecialActionHandler,
    private val reactionHandler: ReactionHandler,
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
        
        // If validation fails, return ActionResult.Failure
        when (validationResult) {
            is ValidationResult.Invalid -> {
                return ActionResult.Failure(validationResult.reason)
            }
            is ValidationResult.RequiresChoice -> {
                return ActionResult.RequiresChoice(validationResult.options)
            }
            is ValidationResult.Valid -> {
                // Continue to action execution
            }
        }
        
        // Route to appropriate handler based on action type
        val events = try {
            when (action) {
                is Attack -> attackHandler.handleAttack(action, context)
                is Move -> movementHandler.handleMovement(action, context)
                is Dash -> movementHandler.handleDash(action, context)
                is CastSpell -> spellHandler.handleSpellCast(action, context)
                is Dodge -> specialHandler.handleDodge(action, context)
                is Disengage -> specialHandler.handleDisengage(action, context)
                is Help -> specialHandler.handleHelp(action, context)
                is Ready -> specialHandler.handleReady(action, context)
                is Reaction -> {
                    // Reactions require a trigger, which should be provided separately
                    // For now, return an error
                    return ActionResult.Failure("Reactions must be processed with a trigger")
                }
            }
        } catch (e: IllegalArgumentException) {
            return ActionResult.Failure(e.message ?: "Invalid action")
        } catch (e: Exception) {
            return ActionResult.Failure("Action processing failed: ${e.message}")
        }
        
        // Return ActionResult.Success with events
        return ActionResult.Success(events)
    }
}
