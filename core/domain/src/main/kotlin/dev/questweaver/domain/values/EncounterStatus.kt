package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents the status of a combat encounter.
 */
@Serializable
enum class EncounterStatus {
    /** The encounter is currently in progress */
    IN_PROGRESS,
    
    /** The encounter ended in victory for the player */
    VICTORY,
    
    /** The encounter ended in defeat for the player */
    DEFEAT,
    
    /** The player fled from the encounter */
    FLED
}
