package dev.questweaver.core.rules.validation.state

/**
 * Represents the current state of an encounter for validation purposes.
 *
 * This is a minimal representation containing only the data needed for action validation.
 * The full encounter state is managed elsewhere in the domain layer.
 */
data class EncounterState(
    /**
     * Map of creature IDs to their current grid positions.
     */
    val positions: Map<Long, GridPos> = emptyMap(),
    
    /**
     * Set of grid positions that contain obstacles blocking line-of-effect.
     */
    val obstacles: Set<GridPos> = emptySet()
)
