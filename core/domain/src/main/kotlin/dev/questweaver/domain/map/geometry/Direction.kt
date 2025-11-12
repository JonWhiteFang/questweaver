package dev.questweaver.domain.map.geometry

import kotlinx.serialization.Serializable

/**
 * Represents the 8 cardinal and diagonal directions on a grid.
 */
@Serializable
enum class Direction {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST
}
