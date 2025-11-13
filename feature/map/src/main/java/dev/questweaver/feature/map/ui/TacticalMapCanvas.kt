package dev.questweaver.feature.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.feature.map.render.drawAoEOverlay
import dev.questweaver.feature.map.render.drawGrid
import dev.questweaver.feature.map.render.drawMovementPath
import dev.questweaver.feature.map.render.drawRangeOverlay
import dev.questweaver.feature.map.render.drawSelectionHighlight
import dev.questweaver.feature.map.render.drawTokens
import dev.questweaver.feature.map.util.CoordinateConverter

/**
 * Main composable for rendering the tactical map using Canvas.
 * 
 * This composable is stateless and receives all rendering data through [state].
 * User interactions are emitted through the [onIntent] callback.
 * 
 * Rendering layers (in order):
 * 1. Grid and terrain
 * 2. Range overlay
 * 3. AoE overlay
 * 4. Movement path
 * 5. Tokens
 * 6. Selection highlight
 * 
 * @param state The current map rendering state
 * @param onIntent Callback for user interactions
 * @param modifier Modifier for the canvas
 */
@Composable
fun TacticalMapCanvas(
    state: MapRenderState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = remember { 60.dp }
    val density = LocalDensity.current
    val cellSizePx = remember(density) { with(density) { cellSize.toPx() } }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .handleTapGestures(state, cellSizePx, onIntent)
            .handleTransformGestures(onIntent)
    ) {
        renderMapLayers(state, cellSizePx)
    }
}

/**
 * Renders all map layers in the correct order.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderMapLayers(
    state: MapRenderState,
    cellSizePx: Float
) {
    // Layer 1: Grid and terrain
    drawGrid(
        grid = state.grid,
        cellSize = cellSizePx,
        cameraOffset = state.cameraOffset,
        zoomLevel = state.zoomLevel,
        canvasSize = size
    )
    
    // Layer 2: Range overlay
    state.rangeOverlay?.let { overlay ->
        drawRangeOverlay(
            overlay = overlay,
            cellSize = cellSizePx,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel,
            canvasSize = size
        )
    }
    
    // Layer 3: AoE overlay
    state.aoeOverlay?.let { overlay ->
        drawAoEOverlay(
            overlay = overlay,
            cellSize = cellSizePx,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel,
            canvasSize = size
        )
    }
    
    // Layer 4: Movement path
    state.movementPath?.let { path ->
        drawMovementPath(
            path = path,
            cellSize = cellSizePx,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel
        )
    }
    
    // Layer 5: Tokens
    drawTokens(
        tokens = state.tokens,
        cellSize = cellSizePx,
        cameraOffset = state.cameraOffset,
        zoomLevel = state.zoomLevel,
        canvasSize = size
    )
    
    // Layer 6: Selection highlight
    state.selectedPosition?.let { position ->
        drawSelectionHighlight(
            position = position,
            cellSize = cellSizePx,
            cameraOffset = state.cameraOffset,
            zoomLevel = state.zoomLevel
        )
    }
}


/**
 * Handles tap gestures on the map, converting screen coordinates to grid positions
 * and emitting appropriate intents for cell or token taps.
 */
private fun Modifier.handleTapGestures(
    state: MapRenderState,
    cellSizePx: Float,
    onIntent: (MapIntent) -> Unit
): Modifier = pointerInput(state, cellSizePx) {
    detectTapGestures { offset ->
        handleTapAtPosition(offset, state, cellSizePx, onIntent)
    }
}

/**
 * Handles transform gestures (pan and zoom) on the map.
 */
private fun Modifier.handleTransformGestures(
    onIntent: (MapIntent) -> Unit
): Modifier = pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, _ ->
        handlePanGesture(pan, onIntent)
        handleZoomGesture(zoom, centroid, onIntent)
    }
}

/**
 * Processes a tap at the given screen position, determining if it's on a token or empty cell.
 */
private fun handleTapAtPosition(
    offset: Offset,
    state: MapRenderState,
    cellSizePx: Float,
    onIntent: (MapIntent) -> Unit
) {
    val gridPos = CoordinateConverter.screenToGrid(
        screenPos = offset,
        state = state,
        cellSize = cellSizePx
    ) ?: return
    
    val tappedToken = state.tokens.find { it.position == gridPos }
    
    if (tappedToken != null) {
        onIntent(MapIntent.TokenTapped(tappedToken.creatureId))
    } else {
        onIntent(MapIntent.CellTapped(gridPos))
    }
}

/**
 * Emits pan intent if there's actual movement.
 */
private fun handlePanGesture(pan: Offset, onIntent: (MapIntent) -> Unit) {
    if (pan.x != 0f || pan.y != 0f) {
        onIntent(MapIntent.Pan(pan))
    }
}

/**
 * Emits zoom intent if there's actual scaling.
 */
private fun handleZoomGesture(zoom: Float, centroid: Offset, onIntent: (MapIntent) -> Unit) {
    if (zoom != 1f) {
        onIntent(MapIntent.Zoom(zoom, centroid))
    }
}
