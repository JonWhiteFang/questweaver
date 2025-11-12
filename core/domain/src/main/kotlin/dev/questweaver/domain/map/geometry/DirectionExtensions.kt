package dev.questweaver.domain.map.geometry

/**
 * Converts a direction to a coordinate offset vector.
 *
 * @return A pair of (dx, dy) representing the direction vector
 */
fun Direction.toVector(): Pair<Int, Int> = when (this) {
    Direction.NORTH -> Pair(0, -1)
    Direction.NORTHEAST -> Pair(1, -1)
    Direction.EAST -> Pair(1, 0)
    Direction.SOUTHEAST -> Pair(1, 1)
    Direction.SOUTH -> Pair(0, 1)
    Direction.SOUTHWEST -> Pair(-1, 1)
    Direction.WEST -> Pair(-1, 0)
    Direction.NORTHWEST -> Pair(-1, -1)
}

/**
 * Gets the perpendicular direction vector for calculating cone width.
 * Used to spread positions perpendicular to the cone's main direction.
 *
 * @return A pair of (dx, dy) representing the perpendicular direction
 */
fun Direction.perpendicular(): Pair<Int, Int> = when (this) {
    Direction.NORTH, Direction.SOUTH -> Pair(1, 0)
    Direction.EAST, Direction.WEST -> Pair(0, 1)
    Direction.NORTHEAST, Direction.SOUTHWEST -> Pair(1, 1)
    Direction.NORTHWEST, Direction.SOUTHEAST -> Pair(-1, 1)
}
