# Design Document

## Overview

The Grid System & Geometry module provides the foundational data structures and algorithms for tactical combat on a square grid. It implements D&D 5e distance rules, line-of-effect calculations, and area-of-effect templates. The design prioritizes immutability, determinism, and performance to support real-time tactical map rendering.

## Architecture

The module resides in `core/domain/map/geometry` and consists of:

1. **Data Structures**: Immutable representations of grid positions and map grids
2. **Distance Calculations**: Chebyshev distance and range queries
3. **Line-of-Effect**: Bresenham's line algorithm for path tracing
4. **AoE Templates**: Geometric calculations for cone, sphere, and cube effects
5. **Grid Operations**: Neighbor queries, bounds checking, and cell properties

All components are pure Kotlin with no Android dependencies, enabling use across all modules.

## Components and Interfaces

### Core Data Structures

```kotlin
// core/domain/map/geometry/GridPos.kt
@Serializable
data class GridPos(
    val x: Int,
    val y: Int
) {
    fun distanceTo(other: GridPos): Int
    fun distanceToInFeet(other: GridPos): Int
    fun isWithinRange(other: GridPos, rangeInFeet: Int): Boolean
    fun neighbors(): List<GridPos>
    fun neighborsInDirection(direction: Direction): GridPos?
}

// core/domain/map/geometry/MapGrid.kt
@Serializable
data class MapGrid(
    val width: Int,
    val height: Int,
    val cells: Map<GridPos, CellProperties> = emptyMap()
) {
    init {
        require(width in 10..100) { "Width must be 10-100" }
        require(height in 10..100) { "Height must be 10-100" }
    }
    
    fun isInBounds(pos: GridPos): Boolean
    fun getCellProperties(pos: GridPos): CellProperties
    fun withCellProperties(pos: GridPos, properties: CellProperties): MapGrid
    fun allPositions(): Sequence<GridPos>
}

// core/domain/map/geometry/CellProperties.kt
@Serializable
data class CellProperties(
    val terrainType: TerrainType = TerrainType.NORMAL,
    val hasObstacle: Boolean = false,
    val occupiedBy: Long? = null // Creature ID
)

enum class TerrainType {
    NORMAL,
    DIFFICULT,
    IMPASSABLE
}

enum class Direction {
    NORTH, NORTHEAST, EAST, SOUTHEAST,
    SOUTH, SOUTHWEST, WEST, NORTHWEST
}
```

### Distance Calculations

```kotlin
// core/domain/map/geometry/DistanceCalculator.kt
object DistanceCalculator {
    /**
     * Calculate Chebyshev distance (D&D 5e diagonal movement)
     * Returns distance in grid squares
     */
    fun chebyshevDistance(from: GridPos, to: GridPos): Int {
        return maxOf(
            kotlin.math.abs(to.x - from.x),
            kotlin.math.abs(to.y - from.y)
        )
    }
    
    /**
     * Calculate distance in feet (5ft per square)
     */
    fun distanceInFeet(from: GridPos, to: GridPos): Int {
        return chebyshevDistance(from, to) * 5
    }
    
    /**
     * Get all positions within range (feet)
     */
    fun positionsWithinRange(
        center: GridPos,
        rangeInFeet: Int,
        grid: MapGrid
    ): Set<GridPos> {
        val rangeInSquares = rangeInFeet / 5
        return grid.allPositions()
            .filter { pos ->
                grid.isInBounds(pos) &&
                chebyshevDistance(center, pos) <= rangeInSquares
            }
            .toSet()
    }
}
```

### Line-of-Effect

```kotlin
// core/domain/map/geometry/LineOfEffect.kt
object LineOfEffect {
    /**
     * Check if there's an unobstructed line between two positions
     * Uses Bresenham's line algorithm
     */
    fun hasLineOfEffect(
        from: GridPos,
        to: GridPos,
        grid: MapGrid
    ): Boolean {
        val path = bresenhamLine(from, to)
        
        // Check each cell in path (excluding start and end)
        return path.drop(1).dropLast(1).all { pos ->
            grid.isInBounds(pos) && !grid.getCellProperties(pos).hasObstacle
        }
    }
    
    /**
     * Get all cells along the line between two positions
     */
    fun bresenhamLine(from: GridPos, to: GridPos): List<GridPos> {
        val result = mutableListOf<GridPos>()
        
        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y
        
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        
        while (true) {
            result.add(GridPos(x0, y0))
            
            if (x0 == x1 && y0 == y1) break
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        
        return result
    }
    
    /**
     * Get positions within range that have line-of-effect
     */
    fun positionsWithinRangeAndLOS(
        from: GridPos,
        rangeInFeet: Int,
        grid: MapGrid
    ): Set<GridPos> {
        return DistanceCalculator.positionsWithinRange(from, rangeInFeet, grid)
            .filter { pos -> hasLineOfEffect(from, pos, grid) }
            .toSet()
    }
}
```

### Area-of-Effect Templates

```kotlin
// core/domain/map/geometry/AoETemplate.kt
sealed interface AoETemplate {
    fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos>
}

// core/domain/map/geometry/SphereTemplate.kt
data class SphereTemplate(
    val radiusInFeet: Int
) : AoETemplate {
    init {
        require(radiusInFeet > 0 && radiusInFeet % 5 == 0) {
            "Radius must be positive multiple of 5"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        return DistanceCalculator.positionsWithinRange(origin, radiusInFeet, grid)
    }
}

// core/domain/map/geometry/CubeTemplate.kt
data class CubeTemplate(
    val sideLengthInFeet: Int
) : AoETemplate {
    init {
        require(sideLengthInFeet > 0 && sideLengthInFeet % 5 == 0) {
            "Side length must be positive multiple of 5"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        val halfSide = sideLengthInFeet / 10 // Convert to squares, then half
        val positions = mutableSetOf<GridPos>()
        
        for (dx in -halfSide..halfSide) {
            for (dy in -halfSide..halfSide) {
                val pos = GridPos(origin.x + dx, origin.y + dy)
                if (grid.isInBounds(pos)) {
                    positions.add(pos)
                }
            }
        }
        
        return positions
    }
}

// core/domain/map/geometry/ConeTemplate.kt
data class ConeTemplate(
    val lengthInFeet: Int,
    val direction: Direction
) : AoETemplate {
    init {
        require(lengthInFeet > 0 && lengthInFeet % 5 == 0) {
            "Length must be positive multiple of 5"
        }
    }
    
    override fun affectedPositions(origin: GridPos, grid: MapGrid): Set<GridPos> {
        val lengthInSquares = lengthInFeet / 5
        val positions = mutableSetOf<GridPos>()
        
        // Get direction vector
        val (dx, dy) = direction.toVector()
        
        // 53-degree cone approximation on grid
        // Width at distance d is approximately d squares
        for (distance in 1..lengthInSquares) {
            val width = distance
            
            // Center line position at this distance
            val centerX = origin.x + dx * distance
            val centerY = origin.y + dy * distance
            
            // Add positions in perpendicular direction
            val (perpX, perpY) = direction.perpendicular()
            
            for (offset in -width / 2..width / 2) {
                val pos = GridPos(
                    centerX + perpX * offset,
                    centerY + perpY * offset
                )
                if (grid.isInBounds(pos)) {
                    positions.add(pos)
                }
            }
        }
        
        return positions
    }
}

// Extension functions for Direction
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

fun Direction.perpendicular(): Pair<Int, Int> = when (this) {
    Direction.NORTH, Direction.SOUTH -> Pair(1, 0)
    Direction.EAST, Direction.WEST -> Pair(0, 1)
    Direction.NORTHEAST, Direction.SOUTHWEST -> Pair(1, 1)
    Direction.NORTHWEST, Direction.SOUTHEAST -> Pair(-1, 1)
}
```

## Data Models

All data models are immutable Kotlin data classes with kotlinx-serialization support:

- **GridPos**: x, y coordinates (Int)
- **MapGrid**: width, height (Int), cells (Map<GridPos, CellProperties>)
- **CellProperties**: terrainType, hasObstacle, occupiedBy
- **AoETemplate**: Sealed interface with Sphere, Cube, Cone implementations

## Error Handling

- **Bounds Validation**: All operations validate positions are within grid bounds
- **Input Validation**: Constructors use `require()` for dimension and range constraints
- **Graceful Degradation**: Out-of-bounds positions are filtered from results, not thrown as exceptions
- **Deterministic**: All calculations are pure functions with consistent results

## Testing Strategy

### Unit Tests (kotest)

1. **GridPos Operations**
   - Distance calculations (orthogonal, diagonal, mixed)
   - Range checks (within/outside range)
   - Neighbor queries (interior, edge, corner positions)

2. **MapGrid Operations**
   - Bounds checking (valid, invalid positions)
   - Cell property management (get, set, immutability)
   - Grid initialization (valid/invalid dimensions)

3. **Line-of-Effect**
   - Bresenham line algorithm (horizontal, vertical, diagonal, arbitrary)
   - Obstacle blocking (single, multiple, edge cases)
   - Clear line-of-effect (no obstacles)

4. **AoE Templates**
   - Sphere: various radii, edge positions
   - Cube: various sizes, origin positions
   - Cone: all 8 directions, various lengths

### Property-Based Tests

```kotlin
test("distance is symmetric") {
    checkAll(Arb.gridPos(), Arb.gridPos()) { pos1, pos2 ->
        pos1.distanceTo(pos2) shouldBe pos2.distanceTo(pos1)
    }
}

test("distance satisfies triangle inequality") {
    checkAll(Arb.gridPos(), Arb.gridPos(), Arb.gridPos()) { a, b, c ->
        a.distanceTo(c) shouldBeLessThanOrEqual 
            a.distanceTo(b) + b.distanceTo(c)
    }
}

test("all positions within range are reachable") {
    checkAll(Arb.gridPos(), Arb.int(5..60)) { center, range ->
        val positions = DistanceCalculator.positionsWithinRange(center, range, testGrid)
        positions.all { pos ->
            DistanceCalculator.distanceInFeet(center, pos) <= range
        }
    }
}
```

### Performance Tests

- Distance calculation: <1μs per operation
- Range query (30ft on 50x50 grid): <1ms
- Line-of-effect check: <100μs per check
- AoE template calculation: <1ms for typical sizes

### Coverage Target

90%+ coverage (core:domain requirement)

## Performance Considerations

1. **Lazy Sequences**: Use `Sequence` for grid iteration to avoid materializing large collections
2. **Caching**: MapGrid stores cell properties in a Map for O(1) lookup
3. **Early Termination**: Range queries filter by bounds before distance calculation
4. **Immutable Collections**: Use persistent data structures to avoid defensive copying
5. **Inline Functions**: Mark small utility functions as `inline` for zero-cost abstractions

## Dependencies

- **Kotlin Standard Library**: Core collections, math functions
- **kotlinx-serialization**: For GridPos, MapGrid, CellProperties serialization
- **No Android Dependencies**: Pure Kotlin for use in core:domain

## Integration Points

- **core/domain/entities**: Creature positions reference GridPos
- **core/domain/events**: Movement events include GridPos
- **feature/map**: Rendering uses GridPos for token placement
- **core/rules**: Combat rules use distance and line-of-effect calculations
- **feature/encounter**: Turn engine uses grid for movement validation
