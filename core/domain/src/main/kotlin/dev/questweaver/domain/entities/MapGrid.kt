package dev.questweaver.domain.entities

import dev.questweaver.domain.values.GridPos
import dev.questweaver.domain.values.TerrainType
import kotlinx.serialization.Serializable

/**
 * Represents a tactical grid map with terrain.
 *
 * @property width The width of the grid in cells
 * @property height The height of the grid in cells
 * @property terrain Map of grid positions to terrain types (positions not in map are EMPTY)
 * @property creaturePositions Map of creature IDs to their positions on the grid
 */
@Serializable
data class MapGrid(
    val width: Int,
    val height: Int,
    val terrain: Map<GridPos, TerrainType> = emptyMap(),
    val creaturePositions: Map<Long, GridPos> = emptyMap()
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        
        terrain.keys.forEach { pos ->
            require(pos.x in 0 until width && pos.y in 0 until height) {
                "Terrain position $pos is outside grid bounds (0,0) to (${width-1},${height-1})"
            }
        }
        
        creaturePositions.values.forEach { pos ->
            require(pos.x in 0 until width && pos.y in 0 until height) {
                "Creature position $pos is outside grid bounds (0,0) to (${width-1},${height-1})"
            }
        }
    }
    
    /**
     * Checks if a position is within the grid bounds.
     *
     * @param pos The position to check
     * @return true if the position is within bounds, false otherwise
     */
    fun isInBounds(pos: GridPos): Boolean =
        pos.x in 0 until width && pos.y in 0 until height
    
    /**
     * Gets the terrain type at a specific position.
     * Returns EMPTY if no terrain is defined at that position.
     *
     * @param pos The position to query
     * @return The terrain type at that position
     */
    fun getTerrainAt(pos: GridPos): TerrainType =
        terrain[pos] ?: TerrainType.EMPTY
}
