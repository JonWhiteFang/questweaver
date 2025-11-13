package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType

/**
 * Calculator for finding reachable positions within a movement budget.
 *
 * Uses breadth-first search to explore all positions that can be reached
 * from a starting position within a given movement budget, accounting for
 * terrain costs and obstacles.
 *
 * @param pathfinder The pathfinder to use for exact cost calculations (defaults to AStarPathfinder)
 */
class ReachabilityCalculator(
    private val pathfinder: Pathfinder = AStarPathfinder()
) {
    
    /**
     * Finds all positions reachable within a movement budget.
     *
     * Uses BFS to explore positions, accounting for terrain costs.
     * A position is reachable if there exists a valid path from start
     * with total cost <= movementBudget.
     *
     * @param start The starting position
     * @param movementBudget The maximum movement points available
     * @param grid The map grid
     * @return Set of all reachable positions (including start)
     */
    @Suppress("CognitiveComplexMethod", "LoopWithTooManyJumpStatements")
    fun findReachablePositions(
        start: GridPos,
        movementBudget: Int,
        grid: MapGrid
    ): Set<GridPos> {
        val reachable = mutableSetOf<GridPos>()
        val visited = mutableSetOf<GridPos>()
        val queue = ArrayDeque<Pair<GridPos, Int>>()
        
        queue.add(Pair(start, 0))
        visited.add(start)
        
        while (queue.isNotEmpty()) {
            val (current, costSoFar) = queue.removeFirst()
            reachable.add(current)
            
            for (neighbor in current.neighbors()) {
                if (!grid.isInBounds(neighbor)) continue
                if (visited.contains(neighbor)) continue
                if (!isTraversable(neighbor, grid)) continue
                
                val movementCost = calculateMovementCost(neighbor, grid)
                val newCost = costSoFar + movementCost
                
                if (newCost <= movementBudget) {
                    visited.add(neighbor)
                    queue.add(Pair(neighbor, newCost))
                }
            }
        }
        
        return reachable
    }
    
    /**
     * Finds all positions reachable with an exact movement cost.
     *
     * This method filters reachable positions to only those where the
     * actual path cost equals the specified exact cost.
     *
     * @param start The starting position
     * @param exactCost The exact movement cost required
     * @param grid The map grid
     * @return Set of positions reachable at exactly the specified cost
     */
    fun findPositionsAtCost(
        start: GridPos,
        exactCost: Int,
        grid: MapGrid
    ): Set<GridPos> {
        return findReachablePositions(start, exactCost, grid)
            .filter { pos ->
                val result = pathfinder.findPath(start, pos, grid)
                result is PathResult.Success && result.totalCost == exactCost
            }
            .toSet()
    }
    
    /**
     * Checks if a position is traversable for reachability calculation.
     *
     * A position is traversable if:
     * - It does not have impassable terrain
     * - It does not have an obstacle
     * - It is not occupied by another creature
     *
     * @param pos The position to check
     * @param grid The map grid
     * @return true if the position can be traversed, false otherwise
     */
    private fun isTraversable(pos: GridPos, grid: MapGrid): Boolean {
        val cell = grid.getCellProperties(pos)
        return cell.terrainType != TerrainType.IMPASSABLE &&
               !cell.hasObstacle &&
               cell.occupiedBy == null
    }
    
    /**
     * Calculates the movement cost for entering a position.
     *
     * @param pos The position to calculate cost for
     * @param grid The map grid
     * @return The movement cost (1 for normal terrain, 2 for difficult, Int.MAX_VALUE for impassable)
     */
    private fun calculateMovementCost(pos: GridPos, grid: MapGrid): Int {
        val cell = grid.getCellProperties(pos)
        return when (cell.terrainType) {
            TerrainType.NORMAL -> 1
            TerrainType.DIFFICULT -> 2
            TerrainType.IMPASSABLE -> Int.MAX_VALUE
        }
    }
}
