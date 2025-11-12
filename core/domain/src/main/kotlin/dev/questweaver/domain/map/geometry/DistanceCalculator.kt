package dev.questweaver.domain.map.geometry

import kotlin.math.abs
import kotlin.math.max

/**
 * Utility object for calculating distances on a tactical grid using D&D 5e rules.
 */
object DistanceCalculator {
    
    /** Number of feet per grid square per D&D 5e SRD rules */
    private const val FEET_PER_SQUARE = 5
    
    /**
     * Calculates Chebyshev distance between two positions.
     * This implements D&D 5e diagonal movement rules where diagonal movement
     * costs the same as orthogonal movement.
     *
     * @param from The starting position
     * @param to The ending position
     * @return The distance in grid squares
     */
    fun chebyshevDistance(from: GridPos, to: GridPos): Int {
        return max(
            abs(to.x - from.x),
            abs(to.y - from.y)
        )
    }
    
    /**
     * Calculates distance in feet between two positions.
     * Each grid square represents 5 feet per D&D 5e SRD rules.
     *
     * @param from The starting position
     * @param to The ending position
     * @return The distance in feet
     */
    fun distanceInFeet(from: GridPos, to: GridPos): Int {
        return chebyshevDistance(from, to) * FEET_PER_SQUARE
    }
    
    /**
     * Gets all positions within a specified range of a center position.
     * Only returns positions that are within the grid boundaries.
     *
     * @param center The center position
     * @param rangeInFeet The maximum range in feet
     * @param grid The map grid to check bounds against
     * @return A set of all positions within range
     */
    fun positionsWithinRange(
        center: GridPos,
        rangeInFeet: Int,
        grid: MapGrid
    ): Set<GridPos> {
        val rangeInSquares = rangeInFeet / FEET_PER_SQUARE
        return grid.allPositions()
            .filter { pos ->
                grid.isInBounds(pos) &&
                chebyshevDistance(center, pos) <= rangeInSquares
            }
            .toSet()
    }
}
