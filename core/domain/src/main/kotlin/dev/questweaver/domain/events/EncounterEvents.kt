package dev.questweaver.domain.events

import dev.questweaver.domain.values.EncounterStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Serializable representation of an initiative entry.
 *
 * @property creatureId The creature's unique identifier
 * @property roll The d20 roll result (1-20)
 * @property modifier The Dexterity modifier applied to the roll
 * @property total The final initiative score (roll + modifier)
 */
@Serializable
data class InitiativeEntryData(
    val creatureId: Long,
    val roll: Int,
    val modifier: Int,
    val total: Int
)

/**
 * Event emitted when an encounter begins with initiative rolled.
 */
@Serializable
@SerialName("encounter_started")
data class EncounterStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val participants: List<Long>,
    val initiativeOrder: List<InitiativeEntryData>,
    val surprisedCreatures: Set<Long> = emptySet()
) : GameEvent

/**
 * Event emitted when a new round starts in an encounter.
 */
@Serializable
@SerialName("round_started")
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
@SerialName("turn_started")
data class TurnStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val roundNumber: Int = 1,
    val turnIndex: Int = 0
) : GameEvent

/**
 * Event emitted when a creature's turn ends.
 */
@Serializable
@SerialName("turn_ended")
data class TurnEnded(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val roundNumber: Int = 1
) : GameEvent

/**
 * Event emitted when an encounter concludes.
 */
@Serializable
@SerialName("encounter_ended")
data class EncounterEnded(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val status: EncounterStatus
) : GameEvent

/**
 * Event emitted when a creature uses their reaction.
 */
@Serializable
@SerialName("reaction_used")
data class ReactionUsed(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val reactionType: String,
    val trigger: String
) : GameEvent

/**
 * Event emitted when a creature delays their turn.
 */
@Serializable
@SerialName("turn_delayed")
data class TurnDelayed(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val originalInitiative: Int
) : GameEvent

/**
 * Event emitted when a delayed creature resumes their turn.
 */
@Serializable
@SerialName("delayed_turn_resumed")
data class DelayedTurnResumed(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val newInitiative: Int
) : GameEvent

/**
 * Event emitted when a creature is added to combat mid-encounter.
 */
@Serializable
@SerialName("creature_added_to_combat")
data class CreatureAddedToCombat(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val initiativeEntry: InitiativeEntryData
) : GameEvent

/**
 * Event emitted when a creature is removed from combat.
 */
@Serializable
@SerialName("creature_removed_from_combat")
data class CreatureRemovedFromCombat(
    override val sessionId: Long,
    override val timestamp: Long,
    val encounterId: Long,
    val creatureId: Long,
    val reason: String
) : GameEvent
