package dev.questweaver.feature.encounter.usecases

import dev.questweaver.feature.encounter.viewmodel.ActionResult
import dev.questweaver.feature.encounter.viewmodel.CombatAction

/**
 * Use case for processing player actions during combat.
 * Validates and executes actions, generating appropriate events.
 *
 * TODO: Integrate with ActionProcessor from spec 11-combat-actions
 * TODO: Integrate with TurnPhaseManager for turn phase updates
 */
class ProcessPlayerAction {
    /**
     * Processes a player action.
     *
     * @param action The combat action to process
     * @param context The current action context (encounter state, active creature, etc.)
     * @return ActionResult with generated events or error
     */
    suspend operator fun invoke(
        action: CombatAction,
        context: ActionContext
    ): ActionResult {
        // TODO: Implement action processing
        // 1. Build ActionContext from current encounter state
        // 2. Use ActionProcessor to validate and execute action
        // 3. If successful, use TurnPhaseManager to update turn phase
        // 4. Return ActionResult with generated events
        
        return ActionResult.Failure("Action processing not yet implemented")
    }
}

/**
 * Context for processing an action.
 * Contains all necessary state information.
 *
 * TODO: Define proper ActionContext structure based on spec 11
 */
data class ActionContext(
    val sessionId: Long,
    val activeCreatureId: Long,
    val roundNumber: Int
)
