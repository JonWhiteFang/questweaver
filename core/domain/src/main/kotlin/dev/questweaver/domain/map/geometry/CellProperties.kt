package dev.questweaver.domain.map.geometry

import kotlinx.serialization.Serializable

/**
 * Represents the properties of a single grid cell.
 *
 * @property terrainType The type of terrain in this cell
 * @property hasObstacle Whether this cell contains an obstacle that blocks line-of-effect
 * @property occupiedBy The ID of the creature occupying this cell, or null if empty
 */
@Serializable
data class CellProperties(
    val terrainType: TerrainType = TerrainType.NORMAL,
    val hasObstacle: Boolean = false,
    val occupiedBy: Long? = null
)
