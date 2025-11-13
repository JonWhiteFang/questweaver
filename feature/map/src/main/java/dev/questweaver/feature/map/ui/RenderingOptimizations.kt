package dev.questweaver.feature.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import dev.questweaver.domain.map.geometry.GridPos

/**
 * Utility functions for optimizing rendering performance.
 */

private const val TOKEN_SIZE_RATIO = 0.8f
private const val VIEWPORT_PADDING = 1

/**
 * Data class containing pre-computed rendering parameters to avoid repeated calculations.
 */
data class RenderingParams(
    val scaledCellSize: Float,
    val tokenRadius: Float,
    val visibleTokens: List<TokenRenderData>,
    val visibleRangePositions: Set<GridPos>?,
    val visibleAoEPositions: Set<GridPos>?
)

/**
 * Creates a derived state for rendering parameters that only recomputes when dependencies change.
 * This optimization reduces unnecessary calculations during recomposition.
 *
 * @param state The current map render state
 * @param cellSizePx The cell size in pixels
 * @param canvasSize The canvas size for viewport calculations
 * @return Derived rendering parameters
 */
@Composable
fun rememberRenderingParams(
    state: MapRenderState,
    cellSizePx: Float,
    canvasSize: Size
): RenderingParams {
    return remember(state, cellSizePx, canvasSize) {
        derivedStateOf {
            computeRenderingParams(state, cellSizePx, canvasSize)
        }
    }.value
}

/**
 * Computes rendering parameters from state.
 */
private fun computeRenderingParams(
    state: MapRenderState,
    cellSizePx: Float,
    canvasSize: Size
): RenderingParams {
    val scaledCellSize = cellSizePx * state.zoomLevel
    val tokenRadius = scaledCellSize * TOKEN_SIZE_RATIO / 2f
    val hasValidCanvas = canvasSize.width > 0 && canvasSize.height > 0
    
    val visibleTokens = if (hasValidCanvas) {
        filterVisibleTokens(state.tokens, state, scaledCellSize, canvasSize)
    } else {
        state.tokens
    }
    
    val visibleRangePositions = state.rangeOverlay?.let { overlay ->
        if (hasValidCanvas) {
            filterVisiblePositions(overlay.positions, state, scaledCellSize, canvasSize)
        } else {
            overlay.positions
        }
    }
    
    val visibleAoEPositions = state.aoeOverlay?.let { overlay ->
        if (hasValidCanvas) {
            filterVisiblePositions(overlay.affectedPositions, state, scaledCellSize, canvasSize)
        } else {
            overlay.affectedPositions
        }
    }
    
    return RenderingParams(
        scaledCellSize = scaledCellSize,
        tokenRadius = tokenRadius,
        visibleTokens = visibleTokens,
        visibleRangePositions = visibleRangePositions,
        visibleAoEPositions = visibleAoEPositions
    )
}

/**
 * Calculates viewport bounds for filtering.
 */
private data class ViewportBounds(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
)

/**
 * Calculates viewport bounds from state and canvas size.
 */
private fun calculateViewportBounds(
    state: MapRenderState,
    scaledCellSize: Float,
    canvasSize: Size
): ViewportBounds {
    val startX = ((-state.cameraOffset.x / scaledCellSize).toInt() - VIEWPORT_PADDING).coerceAtLeast(0)
    val endX = (((canvasSize.width - state.cameraOffset.x) / scaledCellSize).toInt() + VIEWPORT_PADDING)
    val startY = ((-state.cameraOffset.y / scaledCellSize).toInt() - VIEWPORT_PADDING).coerceAtLeast(0)
    val endY = (((canvasSize.height - state.cameraOffset.y) / scaledCellSize).toInt() + VIEWPORT_PADDING)
    
    return ViewportBounds(startX, endX, startY, endY)
}

/**
 * Filters tokens to only those visible in the current viewport.
 */
private fun filterVisibleTokens(
    tokens: List<TokenRenderData>,
    state: MapRenderState,
    scaledCellSize: Float,
    canvasSize: Size
): List<TokenRenderData> {
    val viewport = calculateViewportBounds(state, scaledCellSize, canvasSize)
    
    return tokens.filter { token ->
        token.position.x >= viewport.startX && token.position.x < viewport.endX &&
        token.position.y >= viewport.startY && token.position.y < viewport.endY
    }
}

/**
 * Filters grid positions to only those visible in the current viewport.
 */
private fun filterVisiblePositions(
    positions: Set<GridPos>,
    state: MapRenderState,
    scaledCellSize: Float,
    canvasSize: Size
): Set<GridPos> {
    val viewport = calculateViewportBounds(state, scaledCellSize, canvasSize)
    
    return positions.filter { pos ->
        pos.x >= viewport.startX && pos.x < viewport.endX &&
        pos.y >= viewport.startY && pos.y < viewport.endY
    }.toSet()
}
