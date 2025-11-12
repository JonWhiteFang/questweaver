package dev.questweaver.domain.map.geometry

import kotlinx.serialization.Serializable

/**
 * Represents the type of terrain in a grid cell.
 */
@Serializable
enum class TerrainType {
    /** Normal terrain with no movement penalties */
    NORMAL,
    
    /** Difficult terrain requiring extra movement */
    DIFFICULT,
    
    /** Impassable terrain that blocks movement */
    IMPASSABLE
}
