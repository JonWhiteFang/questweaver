package dev.questweaver.core.domain.action

import dev.questweaver.core.domain.intent.NLAction

/**
 * Represents the result of processing a player action.
 * 
 * This sealed interface captures the three possible outcomes when
 * validating and executing a player's intended action:
 * - Success: Action is valid and was classified
 * - Failure: Action is invalid or cannot be executed
 * - RequiresChoice: Action is ambiguous and requires player clarification
 */
sealed interface ActionResult {
    /**
     * Action was successfully classified and structured.
     * 
     * @property action The structured action derived from natural language input
     */
    data class Success(val action: NLAction) : ActionResult
    
    /**
     * Action failed validation or execution.
     * 
     * @property reason Human-readable explanation of why the action failed
     */
    data class Failure(val reason: String) : ActionResult
    
    /**
     * Action is ambiguous and requires player to choose from options.
     * 
     * @property options The available choices the player can select from
     * @property prompt The question or prompt to display to the player
     */
    data class RequiresChoice(
        val options: List<ActionOption>,
        val prompt: String
    ) : ActionResult
}
