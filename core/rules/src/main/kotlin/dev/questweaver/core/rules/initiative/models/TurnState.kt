package dev.questweaver.core.rules.initiative.models

/**
 * Current turn state.
 *
 * Represents the active creature's turn with phase tracking and position in initiative order.
 * Immutable - all modifications return new instances.
 *
 * @property activeCreatureId The creature whose turn is currently active
 * @property turnPhase The current phase of the turn (action economy tracking)
 * @property turnIndex The index of this turn in the initiative order (0-based)
 */
data class TurnState(
    val activeCreatureId: Long,
    val turnPhase: TurnPhase,
    val turnIndex: Int
)
