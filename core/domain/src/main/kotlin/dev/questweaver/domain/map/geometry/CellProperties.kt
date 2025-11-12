package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the properties of a single grid cell.
 *
 * @property terrainType The type of terrain in this cell
 * @property hasObstacle Whether this cell contains an obstacle that blocks line-of-effect
 * @property occupiedBy The ID of the creature occupying this cell, or null if empty
 */
@Serializable
@SerialName("cell_properties")
data class CellProperties(
    @SerialName("terrain_type")
    val terrainType: TerrainType = TerrainType.NORMAL,
    @SerialName("has_obstacle")
    val hasObstacle: Boolean = false,
    @SerialName("occupied_by")
    val occupiedBy: Long? = null
)
