package dev.questweaver.core.rules.actions.validation

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.ActionResult
import dev.questweaver.core.rules.actions.models.CombatAction
import dev.questweaver.core.rules.validation.ActionValidator as CoreActionValidator
import dev.questweaver.core.rules.validation.actions.GameAction
import dev.questweaver.core.rules.validation.results.ValidationResult
import dev.questweaver.core.rules.validation.state.EncounterState
import dev.questweaver.core.rules.validation.state.TurnState
import dev.questweaver.rules.conditions.Condition

/**
 * Adapter that bridges CombatAction validation to the core ActionValidator.
 * 
 * This class converts CombatAction instances to GameAction instances and
 * ActionContext to the validation state objects required by the core validator.
 */
class ActionValidator(
    private val coreValidator: CoreActionValidator
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
    ): ActionResult {
        // Convert CombatAction to GameAction
        val gameAction = convertToGameAction(action)
        
        // Get actor conditions from context
        val actorConditions = context.activeConditions[action.actorId] ?: emptySet()
        
        // Build TurnState from context
        val turnState = buildTurnState(context, action.actorId)
        
        // Build EncounterState from context
        val encounterState = buildEncounterState(context)
        
        // Perform validation
        val result = coreValidator.validate(
            gameAction,
            actorConditions,
            turnState,
            encounterState
        )
        
        // Convert ValidationResult to ActionResult
        return when (result) {
            is ValidationResult.Success -> ActionResult.Success(emptyList()) // Events will be added by handlers
            is ValidationResult.Failure -> ActionResult.Failure(result.failure.message)
            is ValidationResult.RequiresChoice -> ActionResult.RequiresChoice(
                result.choices.map { choice ->
                    dev.questweaver.core.rules.actions.models.ActionOption(
                        id = choice.id,
                        description = choice.description,
                        action = action // Simplified - would need proper conversion
                    )
                }
            )
        }
    }
    
    /**
     * Converts a CombatAction to a GameAction for validation.
     */
    private fun convertToGameAction(action: CombatAction): GameAction {
        return when (action) {
            is dev.questweaver.core.rules.actions.models.Attack -> GameAction.Attack(
                actorId = action.actorId,
                targetId = action.targetId,
                weaponId = action.weaponId,
                attackBonus = action.attackBonus
            )
            is dev.questweaver.core.rules.actions.models.Move -> GameAction.Move(
                actorId = action.actorId,
                path = action.path.map { pos ->
                    dev.questweaver.core.rules.validation.state.GridPos(pos.x, pos.y)
                }
            )
            is dev.questweaver.core.rules.actions.models.CastSpell -> GameAction.CastSpell(
                actorId = action.actorId,
                spellId = action.spellId,
                spellLevel = action.spellLevel,
                targetIds = action.targets,
                targetPos = null, // Would need to extract from context
                isBonusAction = action.isBonusAction
            )
            is dev.questweaver.core.rules.actions.models.Dodge -> GameAction.Dodge(
                actorId = action.actorId
            )
            is dev.questweaver.core.rules.actions.models.Disengage -> GameAction.Disengage(
                actorId = action.actorId
            )
            is dev.questweaver.core.rules.actions.models.Dash -> GameAction.Dash(
                actorId = action.actorId
            )
            is dev.questweaver.core.rules.actions.models.Help,
            is dev.questweaver.core.rules.actions.models.Ready,
            is dev.questweaver.core.rules.actions.models.Reaction -> {
                // These don't have direct GameAction equivalents yet
                // For now, treat as generic action
                GameAction.Dodge(actorId = action.actorId) // Placeholder
            }
        }
    }
    
    /**
     * Builds TurnState from ActionContext.
     */
    private fun buildTurnState(context: ActionContext, actorId: Long): TurnState {
        // This would need to extract turn phase information from context
        // For now, return a basic TurnState
        return TurnState(
            creatureId = actorId,
            actionAvailable = context.turnPhase.actionAvailable,
            bonusActionAvailable = context.turnPhase.bonusActionAvailable,
            reactionAvailable = context.turnPhase.reactionAvailable,
            movementRemaining = context.turnPhase.movementRemaining,
            resourcePool = dev.questweaver.core.rules.validation.state.ResourcePool(emptyMap()),
            concentrationState = null
        )
    }
    
    /**
     * Builds EncounterState from ActionContext.
     */
    private fun buildEncounterState(context: ActionContext): EncounterState {
        // Convert creature positions from context
        val positions = context.creatures.mapValues { (_, creature) ->
            // Would need to extract position from creature or map
            dev.questweaver.core.rules.validation.state.GridPos(0, 0) // Placeholder
        }
        
        return EncounterState(
            positions = positions,
            obstacles = emptySet(), // Would extract from mapGrid
            difficultTerrain = emptySet() // Would extract from mapGrid
        )
    }
}
