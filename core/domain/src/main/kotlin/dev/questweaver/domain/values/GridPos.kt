package dev.questweaver.domain.values

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/**
 * Represents a position on a tactical grid.
 *
 * @property x The x-coordinate (column) on the grid
 * @property y The y-coordinate (row) on the grid
 */
@Serializable
data class GridPos(
    val x: Int,
    val y: Int
) {
    init {
        require(x >= 0) { "X coordinate cannot be negative" }
        require(y >= 0) { "Y coordinate cannot be negative" }
    }
    
    /**
     * Calculates the Chebyshev distance to another position.
     * Chebyshev distance is the maximum of the absolute differences in coordinates,
     * representing the number of moves needed on a grid allowing diagonal movement.
     *
     * @param other The target position
     * @return The Chebyshev distance to the target
     */
    fun distanceTo(other: GridPos): Int =
        max(abs(x - other.x), abs(y - other.y))
    
    /**
     * Returns all 8 adjacent positions (including diagonals).
     * Only returns positions with non-negative coordinates.
     *
     * @return List of neighboring positions
     */
    fun neighbors(): List<GridPos> = buildList {
        // Cardinal directions
        if (x > 0) add(GridPos(x - 1, y))
        add(GridPos(x + 1, y))
        if (y > 0) add(GridPos(x, y - 1))
        add(GridPos(x, y + 1))
        
        // Diagonal directions
        if (x > 0 && y > 0) add(GridPos(x - 1, y - 1))
        if (y > 0) add(GridPos(x + 1, y - 1))
        if (x > 0) add(GridPos(x - 1, y + 1))
        add(GridPos(x + 1, y + 1))
    }
}
