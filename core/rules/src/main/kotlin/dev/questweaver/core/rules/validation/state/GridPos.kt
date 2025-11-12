package dev.questweaver.core.rules.validation.state

/**
 * Data class representing a position on the tactical grid.
 *
 * Uses a standard 2D coordinate system where (0, 0) is the top-left corner.
 * Each grid square represents 5 feet in D&D 5e.
 *
 * @property x The x-coordinate (column)
 * @property y The y-coordinate (row)
 */
data class GridPos(
    val x: Int,
    val y: Int
)
