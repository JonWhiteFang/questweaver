# Design Document

## Overview

The Pathfinding System implements A* algorithm for optimal path calculation on the tactical grid. It accounts for obstacles, terrain costs, and movement budgets to provide efficient and deterministic pathfinding. The design prioritizes performance, determinism, and integration with the grid geometry system from spec 07.

## Architecture

The module resides in `core/domain/map/pathfinding` and consists of:

1. **A* Pathfinder**: Core pathfinding algorithm implementation
2. **Movement Cost Calculator**: Terrain-based cost calculation
3. **Path Validator**: Path feasibility and budget checking
4. **Reachability Calculator**: Finding all positions within movement budget
5. **Data Structures**: Priority queue, path nodes, and result types

All components are pure Kotlin with deterministic behavior, suitable for core:domain module.

## Components and Interfaces

### Core Pathfinding Interface

```kotlin
// core/domain/map/pathfinding/Pathfinder.kt
interface Pathfinder {
    /**
     * Find optimal path from start to destination
     * Returns empty list if no path exists
     */
    fun findPath(
        start: GridPos,
        destination: GridPos,
        grid: MapGrid,
        maxCost: Int? = null
    ): PathResult
}

// core/domain/map/pathfinding/PathResult.kt
sealed interface PathResult {
    data class Success(
        val path: List<GridPos>,
        val totalCost: Int
    ) : PathResult
    
    data class NoPathFound(
        val reason: String
    ) : PathResult
    
    data class ExceedsMovementBudget(
        val requiredCost: Int,
        val availableCost: Int
    ) : PathResult
}
```

### A* Implementation

```kotlin
// core/domain/map/pathfinding/AStarPathfinder.kt
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
        if (!grid.isInBounds(start) || !grid.isInBounds(destination)) {
            return PathResult.NoPathFound("Start or destination out of bounds")
        }
        
        if (!isTraversable(destination, grid, allowDestination = true)) {
            return PathResult.NoPathFound("Destination is blocked")
        }
        
        // A* algorithm
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
        
        while (openSet.isNotEmpty()) {
            val current = openSet.poll()
            
            // Check if we reached destination
            if (current.position == destination) {
                val path = reconstructPath(cameFrom, current.position)
                val totalCost = gScore[destination]!!
                
                return if (maxCost != null && totalCost > maxCost) {
                    PathResult.ExceedsMovementBudget(totalCost, maxCost)
                } else {
                    PathResult.Success(path, totalCost)
                }
            }
            
            closedSet.add(current.position)
            
            // Explore neighbors
            for (neighbor in current.position.neighbors()) {
                if (!grid.isInBounds(neighbor)) continue
                if (closedSet.contains(neighbor)) continue
                if (!isTraversable(neighbor, grid, allowDestination = neighbor == destination)) continue
                
                val movementCost = costCalculator.calculateCost(neighbor, grid)
                val tentativeGScore = gScore[current.position]!! + movementCost
                
                // Check movement budget
                if (maxCost != null && tentativeGScore > maxCost) continue
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current.position
                    gScore[neighbor] = tentativeGScore
                    
                    val fScore = tentativeGScore + heuristic(neighbor, destination)
                    openSet.add(PathNode(neighbor, tentativeGScore, fScore))
                }
            }
        }
        
        return PathResult.NoPathFound("No valid path exists")
    }
    
    private fun heuristic(from: GridPos, to: GridPos): Int {
        // Chebyshev distance (consistent with D&D 5e diagonal movement)
        return DistanceCalculator.chebyshevDistance(from, to)
    }
    
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
    
    private fun isTraversable(
        pos: GridPos,
        grid: MapGrid,
        allowDestination: Boolean
    ): Boolean {
        val cell = grid.getCellProperties(pos)
        
        // Check terrain
        if (cell.terrainType == TerrainType.IMPASSABLE) return false
        if (cell.hasObstacle) return false
        
        // Check occupancy (allow destination to be occupied for attack movement)
        if (cell.occupiedBy != null && !allowDestination) return false
        
        return true
    }
}

// core/domain/map/pathfinding/PathNode.kt
private data class PathNode(
    val position: GridPos,
    val gScore: Int,
    val fScore: Int
) : Comparable<PathNode> {
    override fun compareTo(other: PathNode): Int {
        // Primary: compare fScore
        val fCompare = fScore.compareTo(other.fScore)
        if (fCompare != 0) return fCompare
        
        // Tie-breaker: prefer lower gScore (closer to start)
        val gCompare = gScore.compareTo(other.gScore)
        if (gCompare != 0) return gCompare
        
        // Final tie-breaker: position (for determinism)
        val xCompare = position.x.compareTo(other.position.x)
        if (xCompare != 0) return xCompare
        
        return position.y.compareTo(other.position.y)
    }
}
```

### Movement Cost Calculator

```kotlin
// core/domain/map/pathfinding/MovementCostCalculator.kt
interface MovementCostCalculator {
    fun calculateCost(position: GridPos, grid: MapGrid): Int
}

// core/domain/map/pathfinding/DefaultMovementCostCalculator.kt
class DefaultMovementCostCalculator : MovementCostCalculator {
    override fun calculateCost(position: GridPos, grid: MapGrid): Int {
        val cell = grid.getCellProperties(position)
        
        return when (cell.terrainType) {
            TerrainType.NORMAL -> 1
            TerrainType.DIFFICULT -> 2
            TerrainType.IMPASSABLE -> Int.MAX_VALUE // Should not be traversed
        }
    }
}
```

### Reachability Calculator

```kotlin
// core/domain/map/pathfinding/ReachabilityCalculator.kt
class ReachabilityCalculator(
    private val pathfinder: Pathfinder = AStarPathfinder()
) {
    /**
     * Find all positions reachable within movement budget
     */
    fun findReachablePositions(
        start: GridPos,
        movementBudget: Int,
        grid: MapGrid
    ): Set<GridPos> {
        val reachable = mutableSetOf<GridPos>()
        val visited = mutableSetOf<GridPos>()
        val queue = ArrayDeque<Pair<GridPos, Int>>()
        
        queue.add(Pair(start, 0))
        visited.add(start)
        
        while (queue.isNotEmpty()) {
            val (current, costSoFar) = queue.removeFirst()
            reachable.add(current)
            
            for (neighbor in current.neighbors()) {
                if (!grid.isInBounds(neighbor)) continue
                if (visited.contains(neighbor)) continue
                if (!isTraversable(neighbor, grid)) continue
                
                val movementCost = calculateMovementCost(neighbor, grid)
                val newCost = costSoFar + movementCost
                
                if (newCost <= movementBudget) {
                    visited.add(neighbor)
                    queue.add(Pair(neighbor, newCost))
                }
            }
        }
        
        return reachable
    }
    
    /**
     * Find all positions reachable with exact movement cost
     */
    fun findPositionsAtCost(
        start: GridPos,
        exactCost: Int,
        grid: MapGrid
    ): Set<GridPos> {
        return findReachablePositions(start, exactCost, grid)
            .filter { pos ->
                val result = pathfinder.findPath(start, pos, grid)
                result is PathResult.Success && result.totalCost == exactCost
            }
            .toSet()
    }
    
    private fun isTraversable(pos: GridPos, grid: MapGrid): Boolean {
        val cell = grid.getCellProperties(pos)
        return cell.terrainType != TerrainType.IMPASSABLE &&
               !cell.hasObstacle &&
               cell.occupiedBy == null
    }
    
    private fun calculateMovementCost(pos: GridPos, grid: MapGrid): Int {
        val cell = grid.getCellProperties(pos)
        return when (cell.terrainType) {
            TerrainType.NORMAL -> 1
            TerrainType.DIFFICULT -> 2
            TerrainType.IMPASSABLE -> Int.MAX_VALUE
        }
    }
}
```

### Path Validator

```kotlin
// core/domain/map/pathfinding/PathValidator.kt
object PathValidator {
    /**
     * Validate that a path is legal and connected
     */
    fun isValidPath(path: List<GridPos>, grid: MapGrid): Boolean {
        if (path.isEmpty()) return false
        if (path.size == 1) return grid.isInBounds(path[0])
        
        // Check all positions are in bounds
        if (!path.all { grid.isInBounds(it) }) return false
        
        // Check each step is adjacent
        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]
            
            if (!current.neighbors().contains(next)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Calculate total movement cost for a path
     */
    fun calculatePathCost(
        path: List<GridPos>,
        grid: MapGrid,
        costCalculator: MovementCostCalculator = DefaultMovementCostCalculator()
    ): Int {
        if (path.isEmpty()) return 0
        
        // Start position has no cost
        return path.drop(1).sumOf { pos ->
            costCalculator.calculateCost(pos, grid)
        }
    }
    
    /**
     * Check if path is within movement budget
     */
    fun isWithinBudget(
        path: List<GridPos>,
        movementBudget: Int,
        grid: MapGrid
    ): Boolean {
        return calculatePathCost(path, grid) <= movementBudget
    }
}
```

## Data Models

- **PathResult**: Sealed interface with Success, NoPathFound, ExceedsMovementBudget variants
- **PathNode**: Internal data class for A* priority queue (position, gScore, fScore)
- **MovementCostCalculator**: Interface for terrain-based cost calculation

All models use immutable data structures and pure functions.

## Error Handling

- **Out of Bounds**: Return NoPathFound result with descriptive reason
- **Blocked Destination**: Return NoPathFound result
- **No Valid Path**: Return NoPathFound result after exhausting search
- **Exceeds Budget**: Return ExceedsMovementBudget with required and available costs
- **Invalid Inputs**: Validate early and return appropriate PathResult

## Testing Strategy

### Unit Tests (kotest)

1. **Basic Pathfinding**
   - Straight line paths (horizontal, vertical, diagonal)
   - Paths around single obstacle
   - Paths around multiple obstacles
   - No path exists (completely blocked)

2. **Movement Costs**
   - Normal terrain (cost 1 per cell)
   - Difficult terrain (cost 2 per cell)
   - Mixed terrain paths
   - Path selection prefers lower cost

3. **Movement Budget**
   - Path within budget (success)
   - Path exceeds budget (failure)
   - Exact budget match
   - Reachable positions calculation

4. **Edge Cases**
   - Start equals destination
   - Destination occupied (allowed)
   - Intermediate cells occupied (blocked)
   - Grid boundaries
   - Maximum grid size (100x100)

5. **Determinism**
   - Same inputs produce same path
   - Tie-breaking is consistent
   - No random behavior

### Property-Based Tests

```kotlin
test("path is always connected") {
    checkAll(Arb.gridPos(), Arb.gridPos()) { start, dest ->
        val result = pathfinder.findPath(start, dest, testGrid)
        if (result is PathResult.Success) {
            PathValidator.isValidPath(result.path, testGrid) shouldBe true
        }
    }
}

test("path cost matches sum of cell costs") {
    checkAll(Arb.gridPos(), Arb.gridPos()) { start, dest ->
        val result = pathfinder.findPath(start, dest, testGrid)
        if (result is PathResult.Success) {
            val calculatedCost = PathValidator.calculatePathCost(result.path, testGrid)
            result.totalCost shouldBe calculatedCost
        }
    }
}

test("reachable positions are all within budget") {
    checkAll(Arb.gridPos(), Arb.int(1..20)) { start, budget ->
        val reachable = calculator.findReachablePositions(start, budget, testGrid)
        reachable.all { pos ->
            val result = pathfinder.findPath(start, pos, testGrid)
            result is PathResult.Success && result.totalCost <= budget
        }
    }
}
```

### Performance Tests

- Pathfinding on 50x50 grid: <10ms
- Reachability calculation (30ft movement): <5ms
- Path validation: <1ms
- Memory allocation: minimal (reuse data structures)

### Coverage Target

90%+ coverage (core:domain requirement)

## Performance Considerations

1. **Priority Queue**: Use efficient heap-based implementation
2. **Early Termination**: Stop search when destination reached
3. **Visited Set**: Use HashSet for O(1) lookup
4. **Heuristic**: Chebyshev distance provides good A* guidance
5. **Memory**: Reuse data structures where possible
6. **Bounds Checking**: Filter neighbors early to reduce iterations

## Dependencies

- **core/domain/map/geometry**: GridPos, MapGrid, Direction, TerrainType, CellProperties, DistanceCalculator
- **Kotlin Standard Library**: Collections, comparisons
- **No Android Dependencies**: Pure Kotlin for core:domain

## Integration Points

- **core/domain/entities**: Creature movement uses pathfinding
- **core/domain/events**: Movement events include calculated paths
- **feature/encounter**: Turn engine validates movement with pathfinding
- **feature/map**: UI visualizes paths from pathfinding results
- **core/rules**: Movement rules use pathfinding for validation
