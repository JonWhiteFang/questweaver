# feature:map

**Tactical grid-based map rendering with Compose Canvas**

## Purpose

The `feature:map` module provides tactical map visualization and interaction for grid-based combat. It renders the battlefield using Jetpack Compose Canvas, handles token placement, movement visualization, and provides geometry utilities for range calculations and line-of-effect.

## Responsibilities

- Render tactical grid map with Compose Canvas
- Display creature tokens on grid
- Handle touch/click interactions for movement and targeting
- Implement A* pathfinding for movement
- Calculate line-of-effect and range
- Visualize movement paths and attack ranges
- Maintain 60fps rendering performance (≤4ms per frame)

## Key Classes and Interfaces

### UI Components (Placeholder)

- `TacticalMapScreen`: Main Composable for map display
- `MapViewModel`: MVI ViewModel for map state
- `GridRenderer`: Canvas drawing logic for grid
- `TokenRenderer`: Renders creature tokens

### State (Placeholder)

- `MapState`: Immutable map state (grid, tokens, selection)
- `MapIntent`: Sealed interface for user interactions
- `GridPos`: Grid position data class

### Pathfinding (Placeholder)

- `PathFinder`: A* pathfinding implementation
- `PathNode`: Node for pathfinding graph

### Geometry (Placeholder)

- `LineOfEffect`: Calculates line-of-effect between positions
- `RangeCalculator`: Calculates distances and ranges
- `GridGeometry`: Grid coordinate conversions

## Dependencies

### Production

- `core:domain`: Domain entities (Creature, MapGrid, Encounter)
- `compose-ui`: Jetpack Compose UI
- `compose-material3`: Material3 components
- `compose-ui-tooling-preview`: Compose preview support
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Compose UI and Canvas rendering
- Touch/click event handling
- Pathfinding and geometry calculations
- Dependencies on `core:domain` and `core:rules`

### ❌ Forbidden

- Dependencies on other feature modules (except from `feature:encounter`)
- Business logic (belongs in `core:domain` or `core:rules`)
- Direct database access (use repositories)

## Architecture Patterns

### MVI Pattern

```kotlin
data class MapState(
    val grid: MapGrid,
    val tokens: List<TokenState>,
    val selectedPos: GridPos? = null,
    val highlightedPath: List<GridPos> = emptyList()
)

sealed interface MapIntent {
    data class SelectPosition(val pos: GridPos) : MapIntent
    data class MoveToken(val tokenId: Long, val to: GridPos) : MapIntent
    object ClearSelection : MapIntent
}

class MapViewModel(
    private val pathFinder: PathFinder
) : ViewModel() {
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()
    
    fun handle(intent: MapIntent) {
        viewModelScope.launch {
            when (intent) {
                is MapIntent.SelectPosition -> handleSelect(intent.pos)
                is MapIntent.MoveToken -> handleMove(intent.tokenId, intent.to)
                is MapIntent.ClearSelection -> clearSelection()
            }
        }
    }
}
```

### State Hoisting

Keep Composables stateless:

```kotlin
@Composable
fun TacticalMapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawGrid(state.grid)
        drawTokens(state.tokens)
        drawHighlights(state.highlightedPath)
    }
}
```

### Canvas Rendering

Efficient drawing with batched operations:

```kotlin
fun DrawScope.drawGrid(grid: MapGrid) {
    val cellSize = size.width / grid.width
    
    // Draw grid lines
    for (x in 0..grid.width) {
        drawLine(
            color = Color.Gray,
            start = Offset(x * cellSize, 0f),
            end = Offset(x * cellSize, size.height),
            strokeWidth = 1f
        )
    }
    
    for (y in 0..grid.height) {
        drawLine(
            color = Color.Gray,
            start = Offset(0f, y * cellSize),
            end = Offset(size.width, y * cellSize),
            strokeWidth = 1f
        )
    }
}
```

## Testing Approach

### Unit Tests

- Test pathfinding algorithms
- Test geometry calculations
- Test ViewModel state transitions
- Test coordinate conversions

### Coverage Target

**60%+** code coverage (focus on logic, not rendering)

### Example Test

```kotlin
class PathFinderTest : FunSpec({
    test("finds shortest path between two points") {
        val grid = MapGrid(width = 10, height = 10)
        val pathFinder = PathFinder(grid)
        
        val start = GridPos(0, 0)
        val end = GridPos(5, 5)
        
        val path = pathFinder.findPath(start, end)
        
        path shouldNotBe null
        path?.first() shouldBe start
        path?.last() shouldBe end
        path?.size shouldBe 11 // Manhattan distance + 1
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :feature:map:build

# Run tests
./gradlew :feature:map:test

# Run tests with coverage
./gradlew :feature:map:test koverHtmlReport
```

## Performance Optimization

### Target: ≤4ms per frame (60fps)

**Strategies:**

1. **Batch Draw Calls**: Group similar drawing operations
2. **Caching**: Cache computed paths and ranges
3. **Lazy Evaluation**: Only compute visible area
4. **Efficient Data Structures**: Use spatial indexing for token lookup

### Performance Testing

```kotlin
test("map rendering completes within budget") {
    val state = MapState(grid = MapGrid(20, 20), tokens = List(10) { mockToken() })
    
    val duration = measureTimeMillis {
        repeat(100) { renderMap(state) }
    }
    
    (duration / 100) shouldBeLessThan 4
}
```

## Package Structure

```
dev.questweaver.feature.map/
├── ui/
│   ├── TacticalMapScreen.kt
│   ├── GridRenderer.kt
│   └── TokenRenderer.kt
├── viewmodel/
│   └── MapViewModel.kt
├── pathfinding/
│   ├── PathFinder.kt
│   └── PathNode.kt
├── geometry/
│   ├── LineOfEffect.kt
│   ├── RangeCalculator.kt
│   └── GridGeometry.kt
└── di/
    └── MapModule.kt
```

## Integration Points

### Consumed By

- `feature:encounter` (displays map during combat)
- `app` (navigation to map screen)

### Depends On

- `core:domain` (MapGrid, Creature entities)

## Features

### Current (Placeholder)

- Grid rendering with Compose Canvas
- Token placement and display
- Click/touch interaction

### Planned

- A* pathfinding with obstacles
- Line-of-effect calculation
- Range visualization (movement, attack, spell)
- Fog of war
- Terrain types and difficult terrain
- Token animations

## UI/UX Considerations

- **Touch Targets**: Minimum 48dp for touch targets
- **Visual Feedback**: Highlight selected cells and valid moves
- **Performance**: Maintain 60fps even with many tokens
- **Accessibility**: Consider color-blind friendly colors

## Notes

- Keep rendering logic separate from state management
- Use state hoisting - keep Composables stateless
- Optimize Canvas drawing for performance
- Test pathfinding and geometry logic thoroughly
- This module can be depended on by `feature:encounter` only

---

**Last Updated**: 2025-11-10
