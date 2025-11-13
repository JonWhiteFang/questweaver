package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.GridPos
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the result of a pathfinding operation.
 */
@Serializable
sealed interface PathResult {
    /**
     * A valid path was found within the movement budget.
     *
     * @property path The ordered list of positions from start to destination (inclusive)
     * @property totalCost The total movement cost of the path
     */
    @Serializable
    @SerialName("path_success")
    data class Success(
        val path: List<GridPos>,
        @SerialName("total_cost")
        val totalCost: Int
    ) : PathResult
    
    /**
     * No valid path exists between the start and destination.
     *
     * @property reason A human-readable explanation of why no path was found
     */
    @Serializable
    @SerialName("path_no_path_found")
    data class NoPathFound(
        val reason: String
    ) : PathResult
    
    /**
     * A path exists but exceeds the available movement budget.
     *
     * @property requiredCost The movement cost required to reach the destination
     * @property availableCost The movement budget that was available
     */
    @Serializable
    @SerialName("path_exceeds_budget")
    data class ExceedsMovementBudget(
        @SerialName("required_cost")
        val requiredCost: Int,
        @SerialName("available_cost")
        val availableCost: Int
    ) : PathResult
}
