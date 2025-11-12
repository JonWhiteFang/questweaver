# Design Document

## Overview

The Tactical Map Rendering system provides a high-performance, interactive visualization of the combat grid using Jetpack Compose Canvas. It renders the grid, terrain, creature tokens with appropriate HP visibility, movement paths, range overlays, and AoE templates. The design follows MVI architecture with state hoisting and prioritizes 60fps performance through efficient drawing techniques.

## Architecture

The module resides in `feature/map` and consists of:

1. **TacticalMapCanvas**: Main Composable for rendering the map
2. **MapRenderState**: Immutable state containing all rendering data
3. **MapIntent**: Sealed interface for user interactions
4. **MapViewModel**: State management and intent handling
5. **Rendering Layers**: Grid, overlays, paths, tokens (drawn in order)
6. **Coordinate Conversion**: Screen space ↔ grid space transformations
7. **Gesture Handling**: Tap, pan, zoom interactions

The system depends on `core:domain` for grid geometry and pathfinding.

## Components and Interfaces

### State and Intent (MVI Pattern)

```kotlin
// feature/map/ui/MapRenderState.kt
data class MapRenderState(
    val grid: MapGrid,
    val tokens: List<TokenRenderData> = emptyList(),
    val movementPath: List<GridPos>? = null,
    val rangeOverlay: RangeOverlayData? = null,
    val aoeOverlay: AoEOverlayData? = null,
    val selectedPosition: GridPos? = null,
    val cameraOffset: Offset = Offset.Zero,
    val zoomLevel: Float = 1f
)

// feature/map/ui/TokenRenderData.kt
data class TokenRenderData(
    val creatureId: Long,
    val position: GridPos,
    val allegiance: Allegiance,
    val currentHP: Int,
    val maxHP: Int,
    val isBloodied: Boolean
) {
    val showHP: Boolean = allegiance == Allegiance.FRIENDLY
    val hpPercentage: Float = currentHP.toFloat() / maxHP.toFloat()
}

enum class Allegiance {
    FRIENDLY,
    ENEMY,
    NEUTRAL
}

// feature/map/ui/RangeOverlayData.kt
data class RangeOverlayData(
    val origin: GridPos,
    val positions: Set<GridPos>,
    val rangeType: RangeType
)

enum class RangeType {
    MOVEMENT,
    WEAPON,
    SPELL
}

// feature/map/ui/AoEOverlayData.kt
data class AoEOverlayData(
    val template: AoETemplate,
    val origin: GridPos,
    val affectedPositions: Set<GridPos>
)

// feature/map/ui/MapIntent.kt
sealed interface MapIntent {
    data class CellTapped(val position: GridPos) : MapIntent
    data class TokenTapped(val creatureId: Long) : MapIntent
    data class Pan(val delta: Offset) : MapIntent
    data class Zoom(val scale: Float, val focus: Offset) : MapIntent
}
```

### Main Composable

```kotlin
// feature/map/ui/TacticalMapCanvas.kt
@Composable
fun TacticalMapCanvas(
    state: MapRenderState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = remember { 60.dp }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val gridPos = screenToGrid(offset, state, cellSize.toPx())
                    if (gridPos != null) {
                        onIntent(MapIntent.CellTapped(gridPos))
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onIntent(MapIntent.Pan(pan))
                    onIntent(MapIntent.Zoom(zoom, Offset.Zero))
                }
            }
    ) {
        val cellSizePx = cellSize.toPx()
        
        // Layer 1: Grid and terrain
        drawGrid(state.grid, cellSizePx, state.cameraOffset, state.zoomLevel)
        
        // Layer 2: Range overlay
        state.rangeOverlay?.let { overlay ->
            drawRangeOverlay(overlay, cellSizePx, state.cameraOffset, state.zoomLevel)
        }
        
        // Layer 3: AoE overlay
        state.aoeOverlay?.let { overlay ->
            drawAoEOverlay(overlay, cellSizePx, state.cameraOffset, state.zoomLevel)
        }
        
        // Layer 4: Movement path
        state.movementPath?.let { path ->
            drawMovementPath(path, cellSizePx, state.cameraOffset, state.zoomLevel)
        }
        
        // Layer 5: Tokens
        drawTokens(state.tokens, cellSizePx, state.cameraOffset, state.zoomLevel)
        
        // Layer 6: Selection highlight
        state.selectedPosition?.let { pos ->
            drawSelectionHighlight(pos, cellSizePx, state.cameraOffset, state.zoomLevel)
        }
    }
}
```

### Rendering Functions

```kotlin
// feature/map/render/GridRenderer.kt
fun DrawScope.drawGrid(
    grid: MapGrid,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    
    // Draw cells with terrain colors
    for (x in 0 until grid.width) {
        for (y in 0 until grid.height) {
            val pos = GridPos(x, y)
            val cell = grid.getCellProperties(pos)
            val screenPos = gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
            
            // Cell background
            val color = when {
                cell.hasObstacle -> Color.DarkGray
                cell.terrainType == TerrainType.IMPASSABLE -> Color.Black
                cell.terrainType == TerrainType.DIFFICULT -> Color(0xFF8B4513) // Brown
                else -> Color.LightGray
            }
            
            drawRect(
                color = color,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize)
            )
            
            // Cell border
            drawRect(
                color = Color.Gray,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize),
                style = Stroke(width = 1f)
            )
        }
    }
}

// feature/map/render/TokenRenderer.kt
fun DrawScope.drawTokens(
    tokens: List<TokenRenderData>,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    val tokenRadius = scaledCellSize * 0.4f
    
    tokens.forEach { token ->
        val screenPos = gridToScreen(token.position, cellSize, cameraOffset, zoomLevel)
        val center = screenPos + Offset(scaledCellSize / 2, scaledCellSize / 2)
        
        // Token circle with allegiance color
        val tokenColor = when (token.allegiance) {
            Allegiance.FRIENDLY -> Color.Blue
            Allegiance.ENEMY -> Color.Red
            Allegiance.NEUTRAL -> Color.Yellow
        }
        
        drawCircle(
            color = tokenColor,
            radius = tokenRadius,
            center = center
        )
        
        // HP indicator for friendly creatures
        if (token.showHP) {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = scaledCellSize * 0.3f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    "${token.currentHP}",
                    center.x,
                    center.y + paint.textSize / 3,
                    paint
                )
            }
        }
        
        // Bloodied indicator for non-friendly creatures
        if (!token.showHP && token.isBloodied) {
            drawCircle(
                color = Color.Red,
                radius = tokenRadius * 0.3f,
                center = center + Offset(tokenRadius * 0.6f, -tokenRadius * 0.6f)
            )
        }
        
        // HP ring (visual health indicator)
        val sweepAngle = 360f * token.hpPercentage
        drawArc(
            color = Color.Green,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = center - Offset(tokenRadius, tokenRadius),
            size = Size(tokenRadius * 2, tokenRadius * 2),
            style = Stroke(width = 3f)
        )
    }
}

// feature/map/render/PathRenderer.kt
fun DrawScope.drawMovementPath(
    path: List<GridPos>,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    if (path.size < 2) return
    
    val scaledCellSize = cellSize * zoomLevel
    val pathColor = Color.Yellow.copy(alpha = 0.7f)
    
    for (i in 0 until path.size - 1) {
        val start = path[i]
        val end = path[i + 1]
        
        val startScreen = gridToScreen(start, cellSize, cameraOffset, zoomLevel)
        val endScreen = gridToScreen(end, cellSize, cameraOffset, zoomLevel)
        
        val startCenter = startScreen + Offset(scaledCellSize / 2, scaledCellSize / 2)
        val endCenter = endScreen + Offset(scaledCellSize / 2, scaledCellSize / 2)
        
        drawLine(
            color = pathColor,
            start = startCenter,
            end = endCenter,
            strokeWidth = 4f
        )
    }
}

// feature/map/render/OverlayRenderer.kt
fun DrawScope.drawRangeOverlay(
    overlay: RangeOverlayData,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    
    val overlayColor = when (overlay.rangeType) {
        RangeType.MOVEMENT -> Color.Blue.copy(alpha = 0.3f)
        RangeType.WEAPON -> Color.Red.copy(alpha = 0.3f)
        RangeType.SPELL -> Color.Magenta.copy(alpha = 0.3f)
    }
    
    overlay.positions.forEach { pos ->
        val screenPos = gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
        drawRect(
            color = overlayColor,
            topLeft = screenPos,
            size = Size(scaledCellSize, scaledCellSize)
        )
    }
}

fun DrawScope.drawAoEOverlay(
    overlay: AoEOverlayData,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    val aoeColor = Color.Red.copy(alpha = 0.4f)
    
    overlay.affectedPositions.forEach { pos ->
        val screenPos = gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
        drawRect(
            color = aoeColor,
            topLeft = screenPos,
            size = Size(scaledCellSize, scaledCellSize)
        )
    }
}

fun DrawScope.drawSelectionHighlight(
    position: GridPos,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    val screenPos = gridToScreen(position, cellSize, cameraOffset, zoomLevel)
    
    drawRect(
        color = Color.White,
        topLeft = screenPos,
        size = Size(scaledCellSize, scaledCellSize),
        style = Stroke(width = 3f)
    )
}
```

### Coordinate Conversion

```kotlin
// feature/map/util/CoordinateConverter.kt
object CoordinateConverter {
    fun gridToScreen(
        gridPos: GridPos,
        cellSize: Float,
        cameraOffset: Offset,
        zoomLevel: Float
    ): Offset {
        val scaledCellSize = cellSize * zoomLevel
        return Offset(
            gridPos.x * scaledCellSize + cameraOffset.x,
            gridPos.y * scaledCellSize + cameraOffset.y
        )
    }
    
    fun screenToGrid(
        screenPos: Offset,
        state: MapRenderState,
        cellSize: Float
    ): GridPos? {
        val scaledCellSize = cellSize * state.zoomLevel
        val gridX = ((screenPos.x - state.cameraOffset.x) / scaledCellSize).toInt()
        val gridY = ((screenPos.y - state.cameraOffset.y) / scaledCellSize).toInt()
        
        val pos = GridPos(gridX, gridY)
        return if (state.grid.isInBounds(pos)) pos else null
    }
}
```

### ViewModel

```kotlin
// feature/map/viewmodel/MapViewModel.kt
class MapViewModel(
    private val pathfinder: Pathfinder,
    // other dependencies
) : ViewModel() {
    
    private val _state = MutableStateFlow(MapRenderState(
        grid = MapGrid(width = 20, height = 20)
    ))
    val state: StateFlow<MapRenderState> = _state.asStateFlow()
    
    fun handle(intent: MapIntent) {
        when (intent) {
            is MapIntent.CellTapped -> handleCellTap(intent.position)
            is MapIntent.TokenTapped -> handleTokenTap(intent.creatureId)
            is MapIntent.Pan -> handlePan(intent.delta)
            is MapIntent.Zoom -> handleZoom(intent.scale, intent.focus)
        }
    }
    
    private fun handleCellTap(position: GridPos) {
        _state.update { it.copy(selectedPosition = position) }
        // Additional logic for movement, targeting, etc.
    }
    
    private fun handlePan(delta: Offset) {
        _state.update { 
            it.copy(cameraOffset = it.cameraOffset + delta)
        }
    }
    
    private fun handleZoom(scale: Float, focus: Offset) {
        _state.update {
            val newZoom = (it.zoomLevel * scale).coerceIn(0.5f, 3.0f)
            it.copy(zoomLevel = newZoom)
        }
    }
    
    fun showMovementRange(creatureId: Long, movementBudget: Int) {
        viewModelScope.launch {
            val creature = getCreature(creatureId)
            val reachable = reachabilityCalculator.findReachablePositions(
                creature.position,
                movementBudget,
                _state.value.grid
            )
            
            _state.update {
                it.copy(
                    rangeOverlay = RangeOverlayData(
                        origin = creature.position,
                        positions = reachable,
                        rangeType = RangeType.MOVEMENT
                    )
                )
            }
        }
    }
    
    fun showAoEPreview(template: AoETemplate, origin: GridPos) {
        val affected = template.affectedPositions(origin, _state.value.grid)
        _state.update {
            it.copy(
                aoeOverlay = AoEOverlayData(
                    template = template,
                    origin = origin,
                    affectedPositions = affected
                )
            )
        }
    }
}
```

## Data Models

- **MapRenderState**: Immutable state with grid, tokens, overlays, camera
- **TokenRenderData**: Creature visualization data with HP visibility rules
- **RangeOverlayData**: Range visualization with origin and positions
- **AoEOverlayData**: AoE template visualization
- **MapIntent**: Sealed interface for user interactions
- **Allegiance**: Enum for creature allegiance (friendly, enemy, neutral)
- **RangeType**: Enum for range overlay types

## Error Handling

- **Out of Bounds**: Coordinate conversion returns null for invalid positions
- **Invalid State**: Defensive checks in rendering functions
- **Performance Degradation**: Skip rendering layers if frame budget exceeded
- **Gesture Conflicts**: Prioritize tap over pan for small movements

## Testing Strategy

### Unit Tests (kotest)

1. **Coordinate Conversion**
   - Grid to screen conversion with various zoom levels
   - Screen to grid conversion with camera offset
   - Boundary cases (edges, corners)

2. **State Management**
   - MapIntent handling updates state correctly
   - Camera pan and zoom constraints
   - Overlay state updates

3. **Token Rendering Logic**
   - HP visibility for friendly creatures
   - Bloodied indicator for non-friendly creatures
   - Allegiance color mapping

### UI Tests (Compose Testing)

```kotlin
@Test
fun tacticalMapRendersGrid() {
    composeTestRule.setContent {
        TacticalMapCanvas(
            state = MapRenderState(grid = MapGrid(10, 10)),
            onIntent = {}
        )
    }
    
    // Verify canvas is displayed
    composeTestRule.onNodeWithTag("TacticalMapCanvas").assertExists()
}

@Test
fun tapEmitsCorrectIntent() {
    val intents = mutableListOf<MapIntent>()
    
    composeTestRule.setContent {
        TacticalMapCanvas(
            state = testState,
            onIntent = { intents.add(it) }
        )
    }
    
    composeTestRule.onNodeWithTag("TacticalMapCanvas").performClick()
    
    intents.first() shouldBeInstanceOf MapIntent.CellTapped::class
}
```

### Performance Tests

- Render time for 50x50 grid: ≤4ms
- Frame rate during pan/zoom: 60fps
- Memory allocation per frame: minimal
- Overdraw analysis with Android Profiler

### Coverage Target

60%+ coverage (feature module requirement)

## Performance Considerations

1. **Batched Drawing**: Group similar draw calls (all cells, all tokens)
2. **Culling**: Only render visible cells based on camera viewport
3. **Layer Caching**: Cache static layers (grid) when possible
4. **Efficient State**: Use immutable data structures, avoid unnecessary recomposition
5. **Canvas Optimization**: Use drawRect batch operations, avoid individual draws
6. **Gesture Debouncing**: Throttle pan/zoom updates to reduce recomposition

## Dependencies

- **core/domain/map/geometry**: GridPos, MapGrid, TerrainType, AoETemplate
- **core/domain/map/pathfinding**: Pathfinder, ReachabilityCalculator
- **Jetpack Compose**: Canvas, gestures, state management
- **Compose UI**: Modifier, DrawScope, Offset, Size
- **Kotlin Coroutines**: ViewModel coroutine scope

## Integration Points

- **feature/encounter**: Provides creature data for token rendering
- **core/domain/entities**: Creature allegiance and HP data
- **core/rules**: Movement and range calculations
- **app**: Navigation to map screen
