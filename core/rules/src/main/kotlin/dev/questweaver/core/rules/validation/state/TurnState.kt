package dev.questweaver.core.rules.validation.state

/**
 * Data class representing the current state of a creature's turn.
 *
 * Tracks action economy resources (action, bonus action, reaction, movement)
 * and other turn-specific state that resets at the start of each turn.
 *
 * @property creatureId The unique identifier of the creature whose turn this is
 * @property round The current combat round number
 * @property actionUsed Whether the creature has used their action this turn
 * @property bonusActionUsed Whether the creature has used their bonus action this turn
 * @property reactionUsed Whether the creature has used their reaction this round
 * @property movementUsed The amount of movement used in feet
 * @property movementTotal The total movement speed in feet
 */
data class TurnState(
    val creatureId: Long,
    val round: Int,
    val actionUsed: Boolean = false,
    val bonusActionUsed: Boolean = false,
    val reactionUsed: Boolean = false,
    val movementUsed: Int = 0,
    val movementTotal: Int
) {
    /**
     * Returns the remaining movement in feet.
     */
    fun remainingMovement(): Int = movementTotal - movementUsed

    /**
     * Returns true if the creature has an action available.
     */
    fun hasActionAvailable(): Boolean = !actionUsed

    /**
     * Returns true if the creature has a bonus action available.
     */
    fun hasBonusActionAvailable(): Boolean = !bonusActionUsed

    /**
     * Returns true if the creature has a reaction available.
     */
    fun hasReactionAvailable(): Boolean = !reactionUsed

    /**
     * Returns a new TurnState with the action marked as used.
     */
    fun useAction(): TurnState = copy(actionUsed = true)

    /**
     * Returns a new TurnState with the bonus action marked as used.
     */
    fun useBonusAction(): TurnState = copy(bonusActionUsed = true)

    /**
     * Returns a new TurnState with the reaction marked as used.
     */
    fun useReaction(): TurnState = copy(reactionUsed = true)

    /**
     * Returns a new TurnState with the specified amount of movement used.
     *
     * @param feet The amount of movement to use in feet
     */
    fun useMovement(feet: Int): TurnState = copy(movementUsed = movementUsed + feet)
}
