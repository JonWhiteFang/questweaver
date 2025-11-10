package dev.questweaver.domain.events

import dev.questweaver.domain.values.GridPos
import kotlinx.serialization.Serializable

/**
 * Event emitted when a creature completes a movement action.
 * Captures the full path taken and movement cost.
 */
@Serializable
data class MoveCommitted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val fromPos: GridPos,
    val toPos: GridPos,
    val path: List<GridPos>,
    val movementCost: Int
) : GameEvent
