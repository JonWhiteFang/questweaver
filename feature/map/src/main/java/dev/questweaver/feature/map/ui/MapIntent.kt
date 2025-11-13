package dev.questweaver.feature.map.ui

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.values.GridPos

/**
 * Sealed interface for map user interactions following MVI pattern.
 */
sealed interface MapIntent {
    /**
     * User tapped a grid cell.
     *
     * @property position The grid position that was tapped
     */
    data class CellTapped(val position: GridPos) : MapIntent
    
    /**
     * User tapped a creature token.
     *
     * @property creatureId The ID of the creature that was tapped
     */
    data class TokenTapped(val creatureId: Long) : MapIntent
    
    /**
     * User panned the map.
     *
     * @property delta The pan offset delta
     */
    data class Pan(val delta: Offset) : MapIntent
    
    /**
     * User zoomed the map.
     *
     * @property scale The zoom scale factor
     * @property focus The focus point for the zoom
     */
    data class Zoom(val scale: Float, val focus: Offset) : MapIntent
}
