package dev.questweaver.feature.map.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.geometry.TerrainType
import dev.questweaver.feature.map.util.CoordinateConverter

/**
 * Extension functions for rendering the grid on a DrawScope.
 */

private const val CELL_BORDER_WIDTH = 1f
private const val VIEWPORT_PADDING = 1

@Suppress("MagicNumber")
private val DIFFICULT_TERRAIN_COLOR = Color(0xFF8B4513) // Brown

/**
 * Data class representing the visible viewport bounds for culling.
 */
internal data class ViewportBounds(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
)

/**
 * Calculates the visible viewport bounds for efficient culling.
 */
internal fun calculateViewportBounds(
    canvasSize: Size,
    cameraOffset: Offset,
    scaledCellSize: Float,
    gridWidth: Int,
    gridHeight: Int
): ViewportBounds {
    val startX = ((-cameraOffset.x / scaledCellSize).toInt() - VIEWPORT_PADDING).coerceAtLeast(0)
    val endX = (((canvasSize.width - cameraOffset.x) / scaledCellSize).toInt() + VIEWPORT_PADDING)
        .coerceAtMost(gridWidth)
    val startY = ((-cameraOffset.y / scaledCellSize).toInt() - VIEWPORT_PADDING).coerceAtLeast(0)
    val endY = (((canvasSize.height - cameraOffset.y) / scaledCellSize).toInt() + VIEWPORT_PADDING)
        .coerceAtMost(gridHeight)
    
    return ViewportBounds(startX, endX, startY, endY)
}

/**
 * Renders the tactical grid with terrain colors and cell borders.
 * Uses viewport culling to only render visible cells for optimal performance.
 *
 * @param grid The map grid to render
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 * @param canvasSize The size of the canvas for viewport culling
 */
fun DrawScope.drawGrid(
    grid: MapGrid,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float,
    canvasSize: Size
) {
    val scaledCellSize = cellSize * zoomLevel
    
    // Calculate visible range for viewport culling
    val viewport = calculateViewportBounds(
        canvasSize = canvasSize,
        cameraOffset = cameraOffset,
        scaledCellSize = scaledCellSize,
        gridWidth = grid.width,
        gridHeight = grid.height
    )
    
    // Batch draw cells with terrain colors
    for (x in viewport.startX until viewport.endX) {
        for (y in viewport.startY until viewport.endY) {
            val pos = GridPos(x, y)
            val cell = grid.getCellProperties(pos)
            val screenPos = CoordinateConverter.gridToScreen(pos, cellSize, cameraOffset, zoomLevel)
            
            // Cell background color based on terrain and obstacles
            val color = when {
                cell.hasObstacle -> Color.DarkGray
                cell.terrainType == TerrainType.IMPASSABLE -> Color.Black
                cell.terrainType == TerrainType.DIFFICULT -> DIFFICULT_TERRAIN_COLOR
                else -> Color.LightGray
            }
            
            // Draw cell background
            drawRect(
                color = color,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize)
            )
            
            // Draw cell border
            drawRect(
                color = Color.Gray,
                topLeft = screenPos,
                size = Size(scaledCellSize, scaledCellSize),
                style = Stroke(width = CELL_BORDER_WIDTH)
            )
        }
    }
}
