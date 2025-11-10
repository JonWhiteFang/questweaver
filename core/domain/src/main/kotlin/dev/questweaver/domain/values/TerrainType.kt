package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents terrain types on a tactical map.
 */
@Serializable
enum class TerrainType {
    /** Empty, passable terrain with no movement penalty */
    EMPTY,
    
    /** Difficult terrain that costs extra movement */
    DIFFICULT,
    
    /** Impassable terrain that blocks movement */
    IMPASSABLE,
    
    /** Terrain occupied by a creature */
    OCCUPIED
}
