# feature:map

**Tactical map rendering with grid-based combat visualization**

## Responsibilities

- Render tactical grid with terrain types and obstacles
- Display creature tokens with allegiance colors and HP indicators
- Visualize movement paths and range overlays
- Provide coordinate conversion between grid and screen space
- Support camera panning and zoom for map navigation
- Maintain 60fps performance (≤4ms render time per frame)

## Module Rules

✅ **Allowed Dependencies**:
- `core:domain` - For `MapGrid`, `GridPos`, terrain types, and geometry

❌ **Forbidden**:
- Other feature modules (except when `feature:encounter` depends on this module)
- Direct database access (use repositories from `core:domain`)
- AI modules (rendering is deterministic)
- Android dependencies outside Compose UI

## Key Classes

### Rendering (feature/map/render/)

- **`GridRenderer.kt`**: Renders tactical grid with terrain colors and cell borders. Implements viewport culling for performance.
- **`TokenRenderer.kt`**: Renders creature tokens with allegiance colors (blue/red/yellow), HP text for friendly creatures, bloodied indicators for enemies, and HP ring visualization.
- **`PathRenderer.kt`**: Visualizes movement paths as connected line segments between grid positions.
- **`OverlayRenderer.kt`**: Renders range overlays (movement/weapon/spell) and AoE templates with semi-transparent highlighting.

### State Management (feature/map/ui/)

- **`MapRenderState.kt`**: Immutable state containing grid, tokens, overlays, camera offset, and zoom level.
- **`TokenRenderData.kt`**: Token display data with position, allegiance, HP, and bloodied status.
- **`RangeOverlayData.kt`**: Range overlay with positions and type (movement/weapon/spell).
- **`AoEOverlayData.kt`**: Area-of-effect overlay with template and affected positions.
- **`Allegiance.kt`**: Enum for creature allegiance (FRIENDLY, ENEMY, NEUTRAL).

### Utilities (feature/map/util/)

- **`CoordinateConverter.kt`**: Converts between grid coordinates and screen coordinates, accounting for camera offset and zoom level.

## Dependencies

**Production**:
- `core:domain` - Grid geometry, terrain types, `GridPos`
- Compose BOM 2024.06.00 - UI framework
- Compose Canvas - Low-level drawing API
- Kotlin Coroutines - Async operations
- Koin - Dependency injection

**Test**:
- kotest - Testing framework
- MockK - Mocking library

## Performance Optimization

### Target: ≤4ms per frame (60fps)

**Viewport Culling**: `GridRenderer` only draws visible cells based on camera offset and canvas size.

**Batched Drawing**: Grid lines and cell borders drawn in batches to minimize draw calls.

**Canvas Phase Rendering**: All state reads happen in Canvas draw block to skip composition and layout phases (see `compose-performance.md`).

**Immutable State**: `MapRenderState` is immutable to enable efficient recomposition skipping.

**Stable Keys**: When used in lists, tokens should use stable keys (creature ID) to prevent unnecessary recomposition.

## Testing Approach

**Coverage Target**: 60%+ (focus on logic, not rendering)

**Key Patterns**:
- Unit tests for `CoordinateConverter` with various zoom levels and offsets
- Property-based tests for coordinate round-trip conversion
- Performance tests to verify ≤4ms render budget
- Mock `MapGrid` and `GridPos` from `core:domain` for isolated testing

**Test Structure**:
```kotlin
class CoordinateConverterTest : FunSpec({
    context("grid to screen conversion") {
        test("converts position with zoom and offset") {
            val result = CoordinateConverter.gridToScreen(
                GridPos(5, 5),
                cellSize = 50f,
                cameraOffset = Offset(10f, 20f),
                zoomLevel = 2f
            )
            result shouldBe Offset(510f, 520f)
        }
    }
})
```

## Package Structure

```
feature/map/src/main/java/dev/questweaver/feature/map/
├── render/              # DrawScope extension functions
│   ├── GridRenderer.kt
│   ├── TokenRenderer.kt
│   ├── PathRenderer.kt
│   └── OverlayRenderer.kt
├── ui/                  # State and data classes
│   ├── MapRenderState.kt
│   ├── TokenRenderData.kt
│   ├── RangeOverlayData.kt
│   ├── AoEOverlayData.kt
│   └── Allegiance.kt
└── util/                # Coordinate conversion utilities
    └── CoordinateConverter.kt
```

## Usage Example

```kotlin
@Composable
fun TacticalMapScreen(state: MapRenderState, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // Read state in draw phase for optimal performance
        drawGrid(
            grid = state.grid,
            cellSize = 50f,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel
        )
        
        state.rangeOverlay?.let { overlay ->
            drawRangeOverlay(overlay, 50f, state.cameraOffset, state.zoomLevel)
        }
        
        state.movementPath?.let { path ->
            drawMovementPath(path, 50f, state.cameraOffset, state.zoomLevel)
        }
        
        drawTokens(
            tokens = state.tokens,
            cellSize = 50f,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel
        )
        
        state.aoeOverlay?.let { overlay ->
            drawAoEOverlay(overlay, 50f, state.cameraOffset, state.zoomLevel)
        }
    }
}
```

## Future Enhancements

- Interactive gesture handling (pan, zoom, tap)
- ViewModel with MVI pattern for state management
- Animation support for token movement
- Line-of-sight visualization
- Fog of war rendering
- Custom token images/sprites

---

**Last Updated**: 2025-11-13
