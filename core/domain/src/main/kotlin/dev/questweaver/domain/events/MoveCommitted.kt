package dev.questweaver.domain.events

import dev.questweaver.domain.values.GridPos
import kotlinx.serialization.Serializable

/**
 * Event emitted when a creature completes a movement action.
 * Captures the full path taken, movement used, and remaining movement.
 */
@Serializable
data class MoveCommitted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val path: List<GridPos>,
    val movementUsed: Int,
    val movementRemaining: Int
) : GameEvent
