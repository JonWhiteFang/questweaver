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

@Suppress("MagicNumber")
private val DIFFICULT_TERRAIN_COLOR = Color(0xFF8B4513) // Brown

/**
 * Renders the tactical grid with terrain colors and cell borders.
 *
 * @param grid The map grid to render
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 */
fun DrawScope.drawGrid(
    grid: MapGrid,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    
    // Calculate visible range for viewport culling
    val visibleStartX = ((-cameraOffset.x / scaledCellSize).toInt() - 1).coerceAtLeast(0)
    val visibleEndX = (((size.width - cameraOffset.x) / scaledCellSize).toInt() + 1).coerceAtMost(grid.width)
    val visibleStartY = ((-cameraOffset.y / scaledCellSize).toInt() - 1).coerceAtLeast(0)
    val visibleEndY = (((size.height - cameraOffset.y) / scaledCellSize).toInt() + 1).coerceAtMost(grid.height)
    
    // Draw cells with terrain colors
    for (x in visibleStartX until visibleEndX) {
        for (y in visibleStartY until visibleEndY) {
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
