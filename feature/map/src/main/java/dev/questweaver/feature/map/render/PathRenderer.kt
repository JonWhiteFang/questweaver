package dev.questweaver.feature.map.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.feature.map.util.CoordinateConverter

/**
 * Extension functions for rendering movement paths on a DrawScope.
 */

private const val PATH_STROKE_WIDTH = 4f
private const val PATH_ALPHA = 0.7f

/**
 * Renders a movement path connecting grid positions.
 *
 * @param path List of grid positions forming the path
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 */
fun DrawScope.drawMovementPath(
    path: List<GridPos>,
    cellSize: Float,
    cameraOffset: androidx.compose.ui.geometry.Offset,
    zoomLevel: Float
) {
    if (path.size < 2) return
    
    val pathColor = Color.Yellow.copy(alpha = PATH_ALPHA)
    
    for (i in 0 until path.size - 1) {
        val start = path[i]
        val end = path[i + 1]
        
        val startCenter = CoordinateConverter.gridToScreenCenter(
            start,
            cellSize,
            cameraOffset,
            zoomLevel
        )
        val endCenter = CoordinateConverter.gridToScreenCenter(
            end,
            cellSize,
            cameraOffset,
            zoomLevel
        )
        
        drawLine(
            color = pathColor,
            start = startCenter,
            end = endCenter,
            strokeWidth = PATH_STROKE_WIDTH
        )
    }
}
