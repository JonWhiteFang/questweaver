package dev.questweaver.core.rules.initiative.models

/**
 * Current round state.
 *
 * Represents the complete state of a combat round including initiative order,
 * surprise mechanics, delayed creatures, and the current turn.
 * Immutable - all modifications return new instances.
 *
 * @property roundNumber The current round number (0 for surprise round, 1+ for normal rounds)
 * @property isSurpriseRound Whether this is a surprise round
 * @property initiativeOrder The sorted list of creatures in initiative order (highest first)
 * @property surprisedCreatures Set of creature IDs that are surprised (cannot act in surprise round)
 * @property delayedCreatures Map of creature IDs to their original initiative entries for creatures that delayed their turn
 * @property currentTurn The current turn state, or null if no turn is active
 */
data class RoundState(
    val roundNumber: Int,
    val isSurpriseRound: Boolean,
    val initiativeOrder: List<InitiativeEntry>,
    val surprisedCreatures: Set<Long>,
    val delayedCreatures: Map<Long, InitiativeEntry>,
    val currentTurn: TurnState?
)
