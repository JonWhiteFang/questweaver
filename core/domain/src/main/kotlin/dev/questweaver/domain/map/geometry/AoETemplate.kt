package dev.questweaver.domain.map.geometry

/**
 * Represents an area-of-effect template for spells and abilities.
 * Templates calculate which grid positions are affected by an AoE effect.
 */
sealed interface AoETemplate {
    /**
     * Calculates all positions affected by this AoE template.
     *
     * @param origin The origin point of the effect
     * @param grid The map grid (used for bounds checking)
     * @return A set of all affected positions within grid bounds
     */
    fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos>
}
