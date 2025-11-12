package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A cone-shaped area-of-effect template (e.g., Burning Hands).
 * Affects positions in a 53-degree cone extending from the origin in a specified direction.
 * The cone width increases with distance (approximately 1 square per square of distance).
 *
 * @property lengthInFeet The length of the cone in feet (must be positive multiple of 5)
 * @property direction The direction the cone extends from the origin
 */
@Serializable
@SerialName("cone_template")
data class ConeTemplate(
    @SerialName("length_in_feet")
    val lengthInFeet: Int,
    val direction: Direction
) : AoETemplate {
    init {
        require(lengthInFeet > 0 && lengthInFeet % FEET_PER_SQUARE == 0) {
            "Length must be positive multiple of $FEET_PER_SQUARE, got $lengthInFeet"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        val lengthInSquares = lengthInFeet / FEET_PER_SQUARE
        val positions = mutableSetOf<GridPos>()
        
        // Get direction vector
        val (dx, dy) = direction.toVector()
        
        // 53-degree cone approximation on grid
        // Width at distance d is approximately d squares
        for (distance in 1..lengthInSquares) {
            val width = distance
            
            // Center line position at this distance
            val centerX = origin.x + dx * distance
            val centerY = origin.y + dy * distance
            
            // Add positions in perpendicular direction
            val (perpX, perpY) = direction.perpendicular()
            
            for (offset in -width / CONE_WIDTH_DIVISOR..width / CONE_WIDTH_DIVISOR) {
                val pos = GridPos(
                    centerX + perpX * offset,
                    centerY + perpY * offset
                )
                if (grid.isInBounds(pos)) {
                    positions.add(pos)
                }
            }
        }
        
        return positions
    }
    
    companion object {
        /** Number of feet per grid square */
        private const val FEET_PER_SQUARE = 5
        
        /** Divisor for calculating cone width at each distance */
        private const val CONE_WIDTH_DIVISOR = 2
    }
}
