package dev.questweaver.feature.map.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.feature.map.ui.AoEOverlayData
import dev.questweaver.feature.map.ui.RangeOverlayData
import dev.questweaver.feature.map.ui.RangeType
import dev.questweaver.feature.map.util.CoordinateConverter

/**
 * Extension functions for rendering overlays (range and AoE) on a DrawScope.
 */

private const val OVERLAY_ALPHA = 0.3f
private const val AOE_ALPHA = 0.4f
private const val SELECTION_STROKE_WIDTH = 3f

/**
 * Checks if a grid position is within the visible viewport.
 */
private fun isPositionVisible(
    pos: GridPos,
    viewport: ViewportBounds
): Boolean {
    return pos.x >= viewport.startX && pos.x < viewport.endX &&
           pos.y >= viewport.startY && pos.y < viewport.endY
}

/**
 * Renders a range overlay highlighting positions within range.
 * Uses viewport culling to only render visible overlay cells.
 *
 * @param overlay Range overlay data with positions and type
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 * @param canvasSize The size of the canvas for viewport culling
 */
fun DrawScope.drawRangeOverlay(
    overlay: RangeOverlayData,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float,
    canvasSize: Size
) {
    val scaledCellSize = cellSize * zoomLevel
    
    // Calculate viewport for culling
    val viewport = calculateViewportBounds(
        canvasSize = canvasSize,
        cameraOffset = cameraOffset,
        scaledCellSize = scaledCellSize,
        gridWidth = Int.MAX_VALUE, // Use large values since we're filtering a set
        gridHeight = Int.MAX_VALUE
    )
    
    val overlayColor = when (overlay.rangeType) {
        RangeType.MOVEMENT -> Color.Blue.copy(alpha = OVERLAY_ALPHA)
        RangeType.WEAPON -> Color.Red.copy(alpha = OVERLAY_ALPHA)
        RangeType.SPELL -> Color.Magenta.copy(alpha = OVERLAY_ALPHA)
    }
    
    // Only render visible positions
    overlay.positions.forEach { pos ->
        if (isPositionVisible(pos, viewport)) {
            val screenPos = CoordinateConverter.gridToScreen(
                pos,
                cellSize,
                cameraOffset,
                zoomLevel
            )
            drawRect(
                color = overlayColor,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize)
            )
        }
    }
}


/**
 * Renders an area-of-effect overlay showing affected positions.
 * Uses viewport culling to only render visible overlay cells.
 *
 * @param overlay AoE overlay data with template and affected positions
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 * @param canvasSize The size of the canvas for viewport culling
 */
fun DrawScope.drawAoEOverlay(
    overlay: AoEOverlayData,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float,
    canvasSize: Size
) {
    val scaledCellSize = cellSize * zoomLevel
    
    // Calculate viewport for culling
    val viewport = calculateViewportBounds(
        canvasSize = canvasSize,
        cameraOffset = cameraOffset,
        scaledCellSize = scaledCellSize,
        gridWidth = Int.MAX_VALUE, // Use large values since we're filtering a set
        gridHeight = Int.MAX_VALUE
    )
    
    val aoeColor = Color.Red.copy(alpha = AOE_ALPHA)
    
    // Only render visible positions
    overlay.affectedPositions.forEach { pos ->
        if (isPositionVisible(pos, viewport)) {
            val screenPos = CoordinateConverter.gridToScreen(
                pos,
                cellSize,
                cameraOffset,
                zoomLevel
            )
            drawRect(
                color = aoeColor,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize)
            )
        }
    }
}

/**
 * Renders a selection highlight around the selected cell.
 *
 * @param position The grid position to highlight
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 */
fun DrawScope.drawSelectionHighlight(
    position: GridPos,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    val screenPos = CoordinateConverter.gridToScreen(
        position,
        cellSize,
        cameraOffset,
        zoomLevel
    )
    
    drawRect(
        color = Color.White,
        topLeft = screenPos,
        size = Size(scaledCellSize, scaledCellSize),
        style = Stroke(width = SELECTION_STROKE_WIDTH)
    )
}
