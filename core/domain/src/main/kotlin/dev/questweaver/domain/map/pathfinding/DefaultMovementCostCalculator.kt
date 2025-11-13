package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType

/**
 * Default implementation of movement cost calculation based on terrain type.
 *
 * Movement costs:
 * - NORMAL terrain: 1 movement point per cell
 * - DIFFICULT terrain: 2 movement points per cell
 * - IMPASSABLE terrain: Int.MAX_VALUE (cannot be traversed)
 *
 * This is a pure function with no side effects.
 */
class DefaultMovementCostCalculator : MovementCostCalculator {
    /**
     * Calculates the movement cost to enter a specific grid cell.
     *
     * @param position The position to calculate cost for
     * @param grid The map grid containing terrain information
     * @return The movement cost in movement points. Returns Int.MAX_VALUE for impassable cells.
     * @throws IllegalArgumentException if position is out of grid bounds
     */
    override fun calculateCost(position: GridPos, grid: MapGrid): Int {
        // Validate grid bounds
        require(grid.isInBounds(position)) {
            "Position $position is outside grid bounds (0,0) to (${grid.width-1},${grid.height-1})"
        }
        
        // Get cell properties at position
        val cellProperties = grid.getCellProperties(position)
        
        // Calculate cost based on terrain type
        return when (cellProperties.terrainType) {
            TerrainType.NORMAL -> 1
            TerrainType.DIFFICULT -> 2
            TerrainType.IMPASSABLE -> Int.MAX_VALUE
        }
    }
}
