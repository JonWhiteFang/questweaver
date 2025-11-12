package dev.questweaver.domain.map.geometry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the 8 cardinal and diagonal directions on a grid.
 */
@Serializable
enum class Direction {
    @SerialName("north")
    NORTH,
    @SerialName("northeast")
    NORTHEAST,
    @SerialName("east")
    EAST,
    @SerialName("southeast")
    SOUTHEAST,
    @SerialName("south")
    SOUTH,
    @SerialName("southwest")
    SOUTHWEST,
    @SerialName("west")
    WEST,
    @SerialName("northwest")
    NORTHWEST
}
