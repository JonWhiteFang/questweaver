package dev.questweaver.domain.map.geometry

import kotlinx.serialization.Serializable

/**
 * Represents a position on the tactical grid.
 *
 * @property x The x-coordinate (column) on the grid
 * @property y The y-coordinate (row) on the grid
 */
@Serializable
data class GridPos(
    val x: Int,
    val y: Int
) {
    /**
     * Calculates the distance to another position in grid squares.
     * Uses Chebyshev distance (D&D 5e diagonal movement rules).
     *
     * @param other The target position
     * @return The distance in grid squares
     */
    fun distanceTo(other: GridPos): Int {
        return DistanceCalculator.chebyshevDistance(this, other)
    }
    
    /**
     * Calculates the distance to another position in feet.
     * Each grid square represents 5 feet per D&D 5e SRD rules.
     *
     * @param other The target position
     * @return The distance in feet
     */
    fun distanceToInFeet(other: GridPos): Int {
        return DistanceCalculator.distanceInFeet(this, other)
    }
    
    /**
     * Checks if this position is within a specified range of another position.
     *
     * @param other The target position
     * @param rangeInFeet The maximum range in feet
     * @return true if within range, false otherwise
     */
    fun isWithinRange(other: GridPos, rangeInFeet: Int): Boolean {
        return distanceToInFeet(other) <= rangeInFeet
    }
    
    /**
     * Returns all 8 adjacent neighbor positions (including diagonals).
     *
     * @return A list of up to 8 neighboring positions
     */
    fun neighbors(): List<GridPos> {
        return Direction.entries.map { direction ->
            val (dx, dy) = direction.toVector()
            GridPos(x + dx, y + dy)
        }
    }
    
    /**
     * Returns the neighbor position in a specific direction.
     *
     * @param direction The direction to move
     * @return The neighboring position in that direction
     */
    fun neighborsInDirection(direction: Direction): GridPos {
        val (dx, dy) = direction.toVector()
        return GridPos(x + dx, y + dy)
    }
}
