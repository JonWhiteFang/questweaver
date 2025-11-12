package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A spherical area-of-effect template (e.g., Fireball).
 * Affects all positions within a specified radius using Chebyshev distance.
 *
 * @property radiusInFeet The radius of the sphere in feet (must be positive multiple of 5)
 */
@Serializable
@SerialName("sphere_template")
data class SphereTemplate(
    @SerialName("radius_in_feet")
    val radiusInFeet: Int
) : AoETemplate {
    init {
        require(radiusInFeet > 0 && radiusInFeet % FEET_PER_SQUARE == 0) {
            "Radius must be positive multiple of $FEET_PER_SQUARE, got $radiusInFeet"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        return DistanceCalculator.positionsWithinRange(origin, radiusInFeet, grid)
    }
    
    companion object {
        /** Number of feet per grid square */
        private const val FEET_PER_SQUARE = 5
    }
}
