package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A cubic area-of-effect template (e.g., Thunderwave).
 * Affects all positions within a square area centered on the origin.
 *
 * @property sideLengthInFeet The length of each side in feet (must be positive multiple of 5)
 */
@Serializable
@SerialName("cube_template")
data class CubeTemplate(
    @SerialName("side_length_in_feet")
    val sideLengthInFeet: Int
) : AoETemplate {
    init {
        require(sideLengthInFeet > 0 && sideLengthInFeet % FEET_PER_SQUARE == 0) {
            "Side length must be positive multiple of $FEET_PER_SQUARE, got $sideLengthInFeet"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        val halfSide = sideLengthInFeet / FEET_TO_HALF_SQUARES // Convert to squares, then half
        val positions = mutableSetOf<GridPos>()
        
        for (dx in -halfSide..halfSide) {
            for (dy in -halfSide..halfSide) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
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
        
        /** Conversion factor from feet to half-squares (5 feet = 1 square, so 10 feet = 2 half-squares) */
        private const val FEET_TO_HALF_SQUARES = 10
    }
}
