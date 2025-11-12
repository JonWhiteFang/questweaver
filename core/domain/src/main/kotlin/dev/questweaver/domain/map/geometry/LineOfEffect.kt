package dev.questweaver.domain.map.geometry

import kotlin.math.abs

/**
 * Provides line-of-effect calculations for tactical combat.
 * Uses Bresenham's line algorithm to trace paths between positions.
 */
object LineOfEffect {
    /**
     * Checks if there's an unobstructed line-of-effect between two positions.
     * Line-of-effect is blocked by obstacles but not by creatures.
     *
     * @param from The starting position
     * @param to The target position
     * @param grid The map grid containing obstacle information
     * @return true if line-of-effect exists, false if blocked by obstacles
     */
    fun hasLineOfEffect(
        from: GridPos,
        to: GridPos,
        grid: MapGrid
    ): Boolean {
        val path = bresenhamLine(from, to)
        
        // Check each cell in path (excluding start and end)
        return path.drop(1).dropLast(1).all { pos ->
            grid.isInBounds(pos) && !grid.getCellProperties(pos).hasObstacle
        }
    }
    
    /**
     * Gets all cells along the line between two positions using Bresenham's algorithm.
     * The path includes both the start and end positions.
     *
     * @param from The starting position
     * @param to The ending position
     * @return A list of all positions along the line
     */
    fun bresenhamLine(from: GridPos, to: GridPos): List<GridPos> {
        val result = mutableListOf<GridPos>()
        
        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y
        
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        
        while (true) {
            result.add(GridPos(x0, y0))
            
            if (x0 == x1 && y0 == y1) break
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        
        return result
    }
    
    /**
     * Gets all positions within range that also have line-of-effect from the origin.
     * Combines distance and line-of-effect constraints.
     *
     * @param from The origin position
     * @param rangeInFeet The maximum range in feet
     * @param grid The map grid
     * @return A set of positions within range and with clear line-of-effect
     */
    fun positionsWithinRangeAndLOS(
        from: GridPos,
        rangeInFeet: Int,
        grid: MapGrid
    ): Set<GridPos> {
        return DistanceCalculator.positionsWithinRange(from, rangeInFeet, grid)
            .filter { pos -> hasLineOfEffect(from, pos, grid) }
            .toSet()
    }
}
