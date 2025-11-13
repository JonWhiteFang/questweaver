package dev.questweaver.domain.map.pathfinding

import dev.questweaver.domain.map.geometry.DistanceCalculator
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import java.util.PriorityQueue

/**
 * A* pathfinding algorithm implementation for tactical grid movement.
 *
 * This implementation:
 * - Uses Chebyshev distance heuristic (consistent with D&D 5e diagonal movement)
 * - Accounts for obstacles, terrain costs, and occupied cells
 * - Provides deterministic results through consistent tie-breaking
 * - Supports optional movement budget constraints
 */
@Suppress("TooManyFunctions") // A* algorithm requires multiple helper functions for clarity
class AStarPathfinder(
    private val costCalculator: MovementCostCalculator = DefaultMovementCostCalculator()
) : Pathfinder {
    
    override fun findPath(
        start: GridPos,
        destination: GridPos,
        grid: MapGrid,
        maxCost: Int?
    ): PathResult {
        // Validate inputs
        validateInputs(start, destination, grid)?.let { return it }
        
        // Initialize A* data structures
        val searchState = initializeSearch(start, destination)
        
        // Main A* search loop
        return executeSearch(searchState, destination, grid, maxCost)
    }
    
    /**
     * Validates input parameters for pathfinding.
     * Returns an error PathResult if validation fails, null if valid.
     */
    private fun validateInputs(
        start: GridPos,
        destination: GridPos,
        grid: MapGrid
    ): PathResult? {
        return when {
            !grid.isInBounds(start) -> 
                PathResult.NoPathFound("Start position $start is out of bounds")
            !grid.isInBounds(destination) -> 
                PathResult.NoPathFound("Destination position $destination is out of bounds")
            !isTraversable(destination, grid, allowDestination = true) -> 
                PathResult.NoPathFound("Destination is blocked by obstacle or impassable terrain")
            else -> null
        }
    }
    
    /**
     * Initializes the search state for A* algorithm.
     */
    private fun initializeSearch(start: GridPos, destination: GridPos): SearchState {
        val openSet = PriorityQueue<PathNode>()
        val closedSet = mutableSetOf<GridPos>()
        val gScore = mutableMapOf<GridPos, Int>()
        val cameFrom = mutableMapOf<GridPos, GridPos>()
        
        gScore[start] = 0
        openSet.add(PathNode(
            position = start,
            gScore = 0,
            fScore = heuristic(start, destination)
        ))
        
        return SearchState(openSet, closedSet, gScore, cameFrom)
    }
    
    /**
     * Executes the main A* search algorithm.
     */
    private fun executeSearch(
        state: SearchState,
        destination: GridPos,
        grid: MapGrid,
        maxCost: Int?
    ): PathResult {
        while (state.openSet.isNotEmpty()) {
            val current = state.openSet.poll()
            
            // Check if we reached the destination
            if (current.position == destination) {
                return buildSuccessResult(state, destination, maxCost)
            }
            
            // Mark current position as visited
            state.closedSet.add(current.position)
            
            // Explore neighbors
            exploreNeighbors(current, destination, grid, maxCost, state)
        }
        
        return PathResult.NoPathFound("No valid path exists between start and destination")
    }
    
    /**
     * Builds the success result after reaching the destination.
     */
    private fun buildSuccessResult(
        state: SearchState,
        destination: GridPos,
        maxCost: Int?
    ): PathResult {
        val path = reconstructPath(state.cameFrom, destination)
        val totalCost = state.gScore[destination]!!
        
        return if (maxCost != null && totalCost > maxCost) {
            PathResult.ExceedsMovementBudget(totalCost, maxCost)
        } else {
            PathResult.Success(path, totalCost)
        }
    }
    
    /**
     * Explores all neighbors of the current position.
     */
    private fun exploreNeighbors(
        current: PathNode,
        destination: GridPos,
        grid: MapGrid,
        maxCost: Int?,
        state: SearchState
    ) {
        current.position.neighbors()
            .filterNot { shouldSkipNeighbor(it, destination, grid, state) }
            .forEach { neighbor ->
                val movementCost = costCalculator.calculateCost(neighbor, grid)
                val tentativeGScore = state.gScore[current.position]!! + movementCost
                
                if (maxCost == null || tentativeGScore <= maxCost) {
                    if (tentativeGScore < state.gScore.getOrDefault(neighbor, Int.MAX_VALUE)) {
                        updateNeighborPath(neighbor, current.position, tentativeGScore, destination, state)
                    }
                }
            }
    }
    
    /**
     * Checks if a neighbor should be skipped during exploration.
     * Returns true if the neighbor is out of bounds, already visited, or not traversable.
     */
    private fun shouldSkipNeighbor(
        neighbor: GridPos,
        destination: GridPos,
        grid: MapGrid,
        state: SearchState
    ): Boolean = !grid.isInBounds(neighbor) ||
                 state.closedSet.contains(neighbor) ||
                 !isTraversable(neighbor, grid, allowDestination = neighbor == destination)
    
    /**
     * Updates the path information for a neighbor and adds it to the open set.
     */
    private fun updateNeighborPath(
        neighbor: GridPos,
        currentPos: GridPos,
        tentativeGScore: Int,
        destination: GridPos,
        state: SearchState
    ) {
        state.cameFrom[neighbor] = currentPos
        state.gScore[neighbor] = tentativeGScore
        state.openSet.add(PathNode(neighbor, tentativeGScore, tentativeGScore + heuristic(neighbor, destination)))
    }
    
    /**
     * Heuristic function for A* algorithm.
     * Uses Chebyshev distance which is consistent with D&D 5e diagonal movement rules.
     *
     * @param from The current position
     * @param to The destination position
     * @return The estimated cost to reach the destination
     */
    private fun heuristic(from: GridPos, to: GridPos): Int {
        return DistanceCalculator.chebyshevDistance(from, to)
    }
    
    /**
     * Reconstructs the path from start to destination using the cameFrom map.
     *
     * @param cameFrom Map of position to previous position in the path
     * @param destination The final destination position
     * @return The complete path from start to destination (inclusive)
     */
    private fun reconstructPath(
        cameFrom: Map<GridPos, GridPos>,
        destination: GridPos
    ): List<GridPos> {
        val path = mutableListOf(destination)
        var current = destination
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom[current]!!
            path.add(0, current)
        }
        
        return path
    }
    
    /**
     * Checks if a position is traversable for pathfinding.
     *
     * A position is traversable if:
     * - It does not have impassable terrain
     * - It does not have an obstacle
     * - It is not occupied by another creature (unless it's the destination)
     *
     * @param pos The position to check
     * @param grid The map grid
     * @param allowDestination Whether to allow the position if it's occupied (for attack movement)
     * @return true if the position can be traversed, false otherwise
     */
    private fun isTraversable(
        pos: GridPos,
        grid: MapGrid,
        allowDestination: Boolean
    ): Boolean {
        val cell = grid.getCellProperties(pos)
        
        return cell.terrainType != dev.questweaver.domain.map.geometry.TerrainType.IMPASSABLE &&
               !cell.hasObstacle &&
               (cell.occupiedBy == null || allowDestination)
    }
    
    /**
     * Internal state for A* search algorithm.
     */
    private data class SearchState(
        val openSet: PriorityQueue<PathNode>,
        val closedSet: MutableSet<GridPos>,
        val gScore: MutableMap<GridPos, Int>,
        val cameFrom: MutableMap<GridPos, GridPos>
    )
}
