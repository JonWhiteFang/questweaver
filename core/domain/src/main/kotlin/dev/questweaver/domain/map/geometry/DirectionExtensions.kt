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
