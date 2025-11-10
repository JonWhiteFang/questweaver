package dev.questweaver.domain.entities

import dev.questweaver.domain.values.EncounterStatus
import kotlinx.serialization.Serializable

/**
 * Represents a tactical combat encounter with participants and turn tracking.
 *
 * @property id Unique identifier for the encounter
 * @property campaignId ID of the campaign this encounter belongs to
 * @property createdTimestamp Unix timestamp when the encounter was created
 * @property currentRound The current round number (starts at 1)
 * @property activeCreatureId ID of the creature whose turn it currently is (null if no active turn)
 * @property participants List of creature IDs participating in this encounter
 * @property initiativeOrder List of initiative entries determining turn order
 * @property status Current status of the encounter
 */
@Serializable
data class Encounter(
    val id: Long,
    val campaignId: Long,
    val createdTimestamp: Long,
    val currentRound: Int,
    val activeCreatureId: Long?,
    val participants: List<Long>,
    val initiativeOrder: List<InitiativeEntry>,
    val status: EncounterStatus = EncounterStatus.IN_PROGRESS
) {
    init {
        require(id > 0) { "Encounter id must be positive" }
        require(campaignId > 0) { "Campaign id must be positive" }
        require(createdTimestamp > 0) { "Created timestamp must be positive" }
        require(currentRound >= 1) { "Current round must be at least 1" }
        require(participants.isNotEmpty()) { "Encounter must have at least one participant" }
        
        if (activeCreatureId != null) {
            require(activeCreatureId in participants) { 
                "Active creature id $activeCreatureId must be a participant" 
            }
        }
        
        val initiativeCreatureIds = initiativeOrder.map { it.creatureId }.toSet()
        val participantIds = participants.toSet()
        require(initiativeCreatureIds == participantIds) {
            "Initiative order must include all participants exactly once. " +
            "Missing: ${participantIds - initiativeCreatureIds}, " +
            "Extra: ${initiativeCreatureIds - participantIds}"
        }
    }
}

/**
 * Represents a creature's initiative value in an encounter.
 *
 * @property creatureId ID of the creature
 * @property initiative The initiative value rolled for this creature
 */
@Serializable
data class InitiativeEntry(
    val creatureId: Long,
    val initiative: Int
)
