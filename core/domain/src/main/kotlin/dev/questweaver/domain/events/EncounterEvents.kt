package dev.questweaver.domain.events

import dev.questweaver.domain.entities.InitiativeEntry
import dev.questweaver.domain.values.EncounterStatus
import kotlinx.serialization.Serializable

/**
 * Event emitted when an encounter begins.
 */
@Serializable
data class EncounterStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val participants: List<Long>,
    val initiativeOrder: List<InitiativeEntry>
) : GameEvent

/**
 * Event emitted when a new round starts in an encounter.
 */
@Serializable
data class RoundStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val roundNumber: Int
) : GameEvent

/**
 * Event emitted when a creature's turn begins.
 */
@Serializable
data class TurnStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long
) : GameEvent

/**
 * Event emitted when a creature's turn ends.
 */
@Serializable
data class TurnEnded(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long
) : GameEvent

/**
 * Event emitted when an encounter concludes.
 */
@Serializable
data class EncounterEnded(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val status: EncounterStatus
) : GameEvent
