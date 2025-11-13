package dev.questweaver.core.rules.initiative.models

/**
 * Turn phase tracking for action economy.
 *
 * Tracks the availability of different action types during a creature's turn.
 * Immutable - all modifications return new instances.
 *
 * @property creatureId The creature whose turn this represents
 * @property movementRemaining The amount of movement remaining (in feet)
 * @property actionAvailable Whether the creature can still take an action
 * @property bonusActionAvailable Whether the creature can still take a bonus action
 * @property reactionAvailable Whether the creature has a reaction available
 */
data class TurnPhase(
    val creatureId: Long,
    val movementRemaining: Int,
    val actionAvailable: Boolean,
    val bonusActionAvailable: Boolean,
    val reactionAvailable: Boolean
)
