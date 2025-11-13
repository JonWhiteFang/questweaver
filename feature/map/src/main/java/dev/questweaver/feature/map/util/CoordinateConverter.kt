package dev.questweaver.feature.map.util

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.GridPos
import dev.questweaver.feature.map.ui.MapRenderState

/**
 * Utility object for converting between grid coordinates and screen coordinates.
 */
object CoordinateConverter {
    /**
     * Converts a grid position to screen coordinates.
     *
     * @param gridPos The grid position to convert
     * @param cellSize The size of each cell in pixels
     * @param cameraOffset The camera offset for panning
     * @param zoomLevel The current zoom level
     * @return The screen offset for the top-left corner of the cell
     */
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
    
    /**
     * Converts screen coordinates to a grid position.
     * Returns null if the position is out of bounds.
     *
     * @param screenPos The screen position to convert
     * @param state The current map render state
     * @param cellSize The size of each cell in pixels
     * @return The grid position, or null if out of bounds
     */
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
    
    /**
     * Calculates the center point of a cell in screen coordinates.
     *
     * @param gridPos The grid position
     * @param cellSize The size of each cell in pixels
     * @param cameraOffset The camera offset for panning
     * @param zoomLevel The current zoom level
     * @return The screen offset for the center of the cell
     */
    fun gridToScreenCenter(
        gridPos: GridPos,
        cellSize: Float,
        cameraOffset: Offset,
        zoomLevel: Float
    ): Offset {
        val topLeft = gridToScreen(gridPos, cellSize, cameraOffset, zoomLevel)
        val scaledCellSize = cellSize * zoomLevel
        return topLeft + Offset(scaledCellSize / 2, scaledCellSize / 2)
    }
}
