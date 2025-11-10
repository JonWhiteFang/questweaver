package dev.questweaver.domain.events

import kotlinx.serialization.Serializable

/**
 * Base sealed interface for all game events in the event sourcing architecture.
 * All state mutations in the game produce immutable GameEvent instances.
 */
@Serializable
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}
