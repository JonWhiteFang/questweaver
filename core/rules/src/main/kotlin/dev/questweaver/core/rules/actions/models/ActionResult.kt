package dev.questweaver.core.rules.actions.models

import dev.questweaver.domain.events.GameEvent

/**
 * Sealed interface representing the result of processing a combat action.
 */
sealed interface ActionResult {
    /**
     * Action was successfully processed and generated events.
     */
    data class Success(val events: List<GameEvent>) : ActionResult
    
    /**
     * Action failed validation or execution.
     */
    data class Failure(val reason: String) : ActionResult
    
    /**
     * Action requires additional choice from the player.
     */
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}

/**
 * Represents an option the player can choose when an action requires clarification.
 */
data class ActionOption(
    val id: String,
    val description: String,
    val action: CombatAction
)

/**
 * Sealed interface for action processing errors.
 */
sealed interface ActionError {
    data class InvalidAction(val reason: String) : ActionError
    data class InsufficientResources(val resource: String) : ActionError
    data class OutOfRange(val distance: Int, val maxRange: Int) : ActionError
    data class NoLineOfEffect(val blockedBy: dev.questweaver.domain.values.GridPos) : ActionError
    data class ActionNotAvailable(val actionType: String) : ActionError
}
