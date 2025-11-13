package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos

/**
 * Represents a node in the A* pathfinding algorithm.
 *
 * This class implements [Comparable] with deterministic tie-breaking to ensure
 * consistent pathfinding results across multiple runs with the same inputs.
 *
 * Comparison order:
 * 1. Primary: fScore (lower is better)
 * 2. Secondary: gScore (lower is better - prefer nodes closer to start)
 * 3. Tertiary: position.x (for deterministic tie-breaking)
 * 4. Final: position.y (for deterministic tie-breaking)
 *
 * @property position The grid position of this node
 * @property gScore The actual cost from start to this node
 * @property fScore The estimated total cost (gScore + heuristic)
 */
internal data class PathNode(
    val position: GridPos,
    val gScore: Int,
    val fScore: Int
) : Comparable<PathNode> {
    
    /**
     * Compares this node to another for priority queue ordering.
     *
     * Lower values have higher priority (will be polled first).
     * Implements deterministic tie-breaking to ensure consistent behavior.
     *
     * @param other The node to compare to
     * @return Negative if this node has higher priority, positive if lower, zero if equal
     */
    override fun compareTo(other: PathNode): Int {
        return compareValuesBy(
            this,
            other,
            { it.fScore },      // Primary: fScore (lower is better)
            { it.gScore },      // Secondary: gScore (prefer closer to start)
            { it.position.x },  // Tertiary: x coordinate (for determinism)
            { it.position.y }   // Final: y coordinate (for determinism)
        )
    }
}
