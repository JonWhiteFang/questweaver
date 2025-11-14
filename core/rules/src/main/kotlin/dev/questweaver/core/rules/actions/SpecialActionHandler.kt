package dev.questweaver.core.rules.actions

import dev.questweaver.core.rules.actions.models.ActionContext
import dev.questweaver.core.rules.actions.models.Disengage
import dev.questweaver.core.rules.actions.models.Dodge
import dev.questweaver.core.rules.actions.models.Help
import dev.questweaver.core.rules.actions.models.HelpType
import dev.questweaver.core.rules.actions.models.Ready
import dev.questweaver.core.rules.actions.models.ReadiedAction
import dev.questweaver.domain.events.DisengageAction
import dev.questweaver.domain.events.DodgeAction
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.HelpAction
import dev.questweaver.domain.events.ReadyAction

/**
 * Processes special actions (Dodge, Disengage, Help, Ready).
 */
class SpecialActionHandler {
    /**
     * Processes a Dodge action.
     * Applies the Dodging condition until the start of the creature's next turn.
     *
     * @param action Dodge action details
     * @param context Current action context
     * @return DodgeAction event
     */
    suspend fun handleDodge(
        action: Dodge,
        context: ActionContext
    ): List<GameEvent> {
        // Apply Dodging condition until start of next turn
        // TODO: Track condition application in encounter state
        
        // Mark action phase as consumed
        // TODO: Update turn phase manager
        
        // Generate DodgeAction event
        return listOf(
            DodgeAction(
                sessionId = context.sessionId,
                timestamp = System.currentTimeMillis(),
                creatureId = action.actorId
            )
        )
    }
    
    /**
     * Processes a Disengage action.
     * Prevents opportunity attacks against the creature for the remainder of the turn.
     *
     * @param action Disengage action details
     * @param context Current action context
     * @return DisengageAction event
     */
    suspend fun handleDisengage(
        action: Disengage,
        context: ActionContext
    ): List<GameEvent> {
        // Apply Disengaged condition for remainder of turn
        // TODO: Track condition application in encounter state
        
        // Mark action phase as consumed
        // TODO: Update turn phase manager
        
        // Generate DisengageAction event
        return listOf(
            DisengageAction(
                sessionId = context.sessionId,
                timestamp = System.currentTimeMillis(),
                creatureId = action.actorId
            )
        )
    }
    
    /**
     * Processes a Help action.
     * Grants advantage on the next ability check or attack roll made by the target ally.
     *
     * @param action Help action details
     * @param context Current action context
     * @return HelpAction event
     */
    suspend fun handleHelp(
        action: Help,
        context: ActionContext
    ): List<GameEvent> {
        // Validate target exists
        require(context.creatures.containsKey(action.targetId)) {
            "Target not found: ${action.targetId}"
        }
        
        // Grant advantage on next roll
        // TODO: Track advantage grant in encounter state
        
        // Mark action phase as consumed
        // TODO: Update turn phase manager
        
        // Generate HelpAction event
        // Convert from models.HelpType to events.HelpType
        val eventHelpType = when (action.helpType) {
            HelpType.Attack -> dev.questweaver.domain.events.HelpType.Attack
            HelpType.AbilityCheck -> dev.questweaver.domain.events.HelpType.AbilityCheck
        }
        
        return listOf(
            HelpAction(
                sessionId = context.sessionId,
                timestamp = System.currentTimeMillis(),
                helperId = action.actorId,
                targetId = action.targetId,
                helpType = eventHelpType
            )
        )
    }
    
    /**
     * Processes a Ready action.
     * Stores the prepared action and trigger condition for later execution.
     *
     * @param action Ready action details
     * @param context Current action context
     * @return ReadyAction event
     */
    suspend fun handleReady(
        action: Ready,
        context: ActionContext
    ): List<GameEvent> {
        // Store prepared action and trigger condition
        // This will be tracked in ActionContext.readiedActions
        // When the trigger occurs, the prepared action will be executed using the creature's reaction
        
        // Mark action phase as consumed
        // TODO: Update turn phase manager
        
        // Generate ReadyAction event
        return listOf(
            ReadyAction(
                sessionId = context.sessionId,
                timestamp = System.currentTimeMillis(),
                creatureId = action.actorId,
                preparedActionDescription = action.preparedAction.toString(),
                trigger = action.trigger
            )
        )
    }
}
