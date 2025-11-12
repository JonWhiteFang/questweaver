package dev.questweaver.core.rules.validation.state

import dev.questweaver.core.rules.validation.results.ActionEconomyResource
import dev.questweaver.core.rules.validation.results.ResourceCost

/**
 * Data class representing the current state of a creature's turn.
 *
 * Tracks action economy resources (action, bonus action, reaction, movement),
 * resource pools (spell slots, class features), and concentration state.
 * This state resets at the start of each turn (except reaction and concentration).
 *
 * @property creatureId The unique identifier of the creature whose turn this is
 * @property round The current combat round number
 * @property actionUsed Whether the creature has used their action this turn
 * @property bonusActionUsed Whether the creature has used their bonus action this turn
 * @property reactionUsed Whether the creature has used their reaction this round
 * @property movementUsed The amount of movement used in feet
 * @property movementTotal The total movement speed in feet
 * @property resourcePool The creature's available resources (spell slots, class features, etc.)
 * @property concentrationState The current concentration state for all creatures
 */
data class TurnState(
    val creatureId: Long,
    val round: Int,
    val actionUsed: Boolean = false,
    val bonusActionUsed: Boolean = false,
    val reactionUsed: Boolean = false,
    val movementUsed: Int = 0,
    val movementTotal: Int,
    val resourcePool: ResourcePool,
    val concentrationState: ConcentrationState
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

    /**
     * Returns a new TurnState with the specified resource cost consumed.
     *
     * This method applies all resource costs from a validated action:
     * - Marks action economy resources as used
     * - Consumes resources from the resource pool
     * - Updates movement used
     * - Breaks concentration if required
     *
     * @param cost The resource cost to consume
     * @return A new TurnState with resources consumed
     */
    fun consumeResources(cost: ResourceCost): TurnState {
        return copy(
            actionUsed = actionUsed || ActionEconomyResource.Action in cost.actionEconomy,
            bonusActionUsed = bonusActionUsed || ActionEconomyResource.BonusAction in cost.actionEconomy,
            reactionUsed = reactionUsed || ActionEconomyResource.Reaction in cost.actionEconomy,
            movementUsed = movementUsed + cost.movementCost,
            resourcePool = cost.resources.fold(resourcePool) { pool, resource ->
                pool.consume(resource)
            },
            concentrationState = if (cost.breaksConcentration) {
                concentrationState.breakConcentration(creatureId)
            } else {
                concentrationState
            }
        )
    }
}
