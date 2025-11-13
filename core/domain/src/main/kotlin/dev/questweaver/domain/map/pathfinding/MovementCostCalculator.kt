package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid

/**
 * Interface for calculating movement costs for grid cells.
 *
 * Movement costs are based on terrain type:
 * - NORMAL terrain: 1 movement point per cell
 * - DIFFICULT terrain: 2 movement points per cell
 * - IMPASSABLE terrain: Cannot be traversed (Int.MAX_VALUE cost)
 *
 * Implementations must be pure functions with no side effects.
 */
interface MovementCostCalculator {
    /**
     * Calculates the movement cost to enter a specific grid cell.
     *
     * @param position The position to calculate cost for
     * @param grid The map grid containing terrain information
     * @return The movement cost in movement points. Returns Int.MAX_VALUE for impassable cells.
     */
    fun calculateCost(position: GridPos, grid: MapGrid): Int
}
