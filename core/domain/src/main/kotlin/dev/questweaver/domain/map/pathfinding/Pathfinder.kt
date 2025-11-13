package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid

/**
 * Interface for pathfinding algorithms that calculate optimal movement paths on the tactical grid.
 *
 * Implementations must be deterministic and produce consistent results for the same inputs.
 */
interface Pathfinder {
    /**
     * Finds an optimal path from start to destination on the grid.
     *
     * The pathfinding algorithm accounts for:
     * - Obstacles and impassable terrain
     * - Occupied cells (except the destination)
     * - Terrain movement costs (normal, difficult)
     * - Optional movement budget constraints
     *
     * @param start The starting position (must be within grid bounds)
     * @param destination The destination position (must be within grid bounds)
     * @param grid The map grid containing terrain and obstacle information
     * @param maxCost Optional maximum movement cost budget. If provided, paths exceeding
     *                this cost will return [PathResult.ExceedsMovementBudget]
     * @return A [PathResult] indicating success with a path, no path found, or budget exceeded
     */
    fun findPath(
        start: GridPos,
        destination: GridPos,
        grid: MapGrid,
        maxCost: Int? = null
    ): PathResult
}
