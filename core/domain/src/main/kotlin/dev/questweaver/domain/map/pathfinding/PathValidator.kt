package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid

/**
 * Utility object for validating paths and calculating path costs.
 *
 * Provides methods to:
 * - Validate that a path is legal and connected
 * - Calculate total movement cost for a path
 * - Check if a path is within a movement budget
 */
object PathValidator {
    
    /**
     * Validates that a path is legal and connected.
     *
     * A valid path must:
     * - Not be empty
     * - Have all positions within grid bounds
     * - Have each consecutive step be adjacent (including diagonals)
     *
     * @param path The path to validate
     * @param grid The map grid
     * @return true if the path is valid, false otherwise
     */
    fun isValidPath(path: List<GridPos>, grid: MapGrid): Boolean = when {
        path.isEmpty() -> false
        path.size == 1 -> grid.isInBounds(path[0])
        !path.all { grid.isInBounds(it) } -> false
        else -> path.zipWithNext().all { (current, next) ->
            current.neighbors().contains(next)
        }
    }
    
    /**
     * Calculates the total movement cost for a path.
     *
     * The cost is the sum of the movement costs for each cell in the path,
     * excluding the starting position (which has no cost to enter).
     *
     * @param path The path to calculate cost for
     * @param grid The map grid
     * @param costCalculator The cost calculator to use (defaults to DefaultMovementCostCalculator)
     * @return The total movement cost
     */
    fun calculatePathCost(
        path: List<GridPos>,
        grid: MapGrid,
        costCalculator: MovementCostCalculator = DefaultMovementCostCalculator()
    ): Int {
        if (path.isEmpty()) return 0
        
        // Start position has no cost
        return path.drop(1).sumOf { pos ->
            costCalculator.calculateCost(pos, grid)
        }
    }
    
    /**
     * Checks if a path is within a movement budget.
     *
     * @param path The path to check
     * @param movementBudget The maximum allowed movement cost
     * @param grid The map grid
     * @return true if the path cost is within the budget, false otherwise
     */
    fun isWithinBudget(
        path: List<GridPos>,
        movementBudget: Int,
        grid: MapGrid
    ): Boolean {
        return calculatePathCost(path, grid) <= movementBudget
    }
}
