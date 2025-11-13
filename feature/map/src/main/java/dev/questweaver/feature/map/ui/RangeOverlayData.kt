package dev.questweaver.feature.map.ui

import dev.questweaver.domain.values.GridPos

/**
 * Data for rendering range overlays on the map.
 *
 * @property origin The origin position of the range
 * @property positions Set of positions within range
 * @property rangeType Type of range (movement, weapon, spell)
 */
data class RangeOverlayData(
    val origin: GridPos,
    val positions: Set<GridPos>,
    val rangeType: RangeType
)

/**
 * Type of range overlay for color coding.
 */
enum class RangeType {
    MOVEMENT,
    WEAPON,
    SPELL
}
