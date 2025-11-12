package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of terrain in a grid cell.
 */
@Serializable
enum class TerrainType {
    /** Normal terrain with no movement penalties */
    @SerialName("normal")
    NORMAL,
    
    /** Difficult terrain requiring extra movement */
    @SerialName("difficult")
    DIFFICULT,
    
    /** Impassable terrain that blocks movement */
    @SerialName("impassable")
    IMPASSABLE
}
