package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.ActionType
import dev.questweaver.core.rules.initiative.models.TurnPhase

/**
 * Manages turn phase state and action economy for creatures during combat.
 *
 * Tracks the availability of actions, bonus actions, reactions, and movement
 * during a creature's turn. All methods return new immutable instances.
 *
 * Implements D&D 5e SRD action economy rules:
 * - One action per turn
 * - One bonus action per turn (if available)
 * - One reaction per round (restored at start of turn)
 * - Movement up to speed per turn
 */
class TurnPhaseManager {
    
    /**
     * Creates initial turn phase state for a creature's turn.
     *
     * All actions are available at the start of a turn:
     * - Action available
     * - Bonus action available
     * - Reaction available (restored from previous round)
     * - Full movement speed available
     *
     * @param creatureId The creature whose turn is starting
     * @param movementSpeed The creature's movement speed in feet
     * @return TurnPhase with all actions available
     */
    fun startTurn(
        creatureId: Long,
        movementSpeed: Int
    ): TurnPhase {
        return TurnPhase(
            creatureId = creatureId,
            movementRemaining = movementSpeed,
            actionAvailable = true,
            bonusActionAvailable = true,
            reactionAvailable = true
        )
    }
    
    /**
     * Consumes movement from the current turn.
     *
     * Reduces the remaining movement by the specified amount.
     * Movement is clamped to 0 minimum (cannot go negative).
     *
     * @param currentPhase Current turn phase state
     * @param movementUsed Amount of movement consumed in feet
     * @return Updated TurnPhase with reduced movement (minimum 0)
     */
    fun consumeMovement(
        currentPhase: TurnPhase,
        movementUsed: Int
    ): TurnPhase {
        val newMovement = (currentPhase.movementRemaining - movementUsed).coerceAtLeast(0)
        return currentPhase.copy(movementRemaining = newMovement)
    }
    
    /**
     * Marks the action phase as consumed.
     *
     * Once consumed, the action cannot be used again this turn.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with action consumed
     */
    fun consumeAction(currentPhase: TurnPhase): TurnPhase {
        return currentPhase.copy(actionAvailable = false)
    }
    
    /**
     * Marks the bonus action phase as consumed.
     *
     * Once consumed, the bonus action cannot be used again this turn.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with bonus action consumed
     */
    fun consumeBonusAction(currentPhase: TurnPhase): TurnPhase {
        return currentPhase.copy(bonusActionAvailable = false)
    }
    
    /**
     * Marks the reaction as consumed.
     *
     * Once consumed, the reaction cannot be used again until the start
     * of the creature's next turn.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with reaction consumed
     */
    fun consumeReaction(currentPhase: TurnPhase): TurnPhase {
        return currentPhase.copy(reactionAvailable = false)
    }
    
    /**
     * Restores the reaction at the start of a creature's turn.
     *
     * Reactions are restored at the start of each turn, allowing the
     * creature to use their reaction again in the upcoming round.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with reaction restored
     */
    fun restoreReaction(currentPhase: TurnPhase): TurnPhase {
        return currentPhase.copy(reactionAvailable = true)
    }
    
    /**
     * Checks if a specific action type is available.
     *
     * Queries the appropriate field based on the action type:
     * - Action: checks actionAvailable
     * - BonusAction: checks bonusActionAvailable
     * - Reaction: checks reactionAvailable
     * - Movement: checks if movementRemaining > 0
     * - FreeAction: always returns true
     *
     * @param currentPhase Current turn phase state
     * @param actionType Type of action to check
     * @return True if the action is available, false otherwise
     */
    fun isActionAvailable(
        currentPhase: TurnPhase,
        actionType: ActionType
    ): Boolean {
        return when (actionType) {
            ActionType.Action -> currentPhase.actionAvailable
            ActionType.BonusAction -> currentPhase.bonusActionAvailable
            ActionType.Reaction -> currentPhase.reactionAvailable
            ActionType.Movement -> currentPhase.movementRemaining > 0
            ActionType.FreeAction -> true
        }
    }
}
