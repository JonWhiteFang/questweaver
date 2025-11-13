package dev.questweaver.feature.map.ui

import androidx.compose.ui.geometry.Offset
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.values.GridPos

/**
 * Immutable state for tactical map rendering.
 *
 * @property grid The map grid with terrain and cell properties
 * @property tokens List of creature tokens to render
 * @property movementPath Optional movement path to visualize
 * @property rangeOverlay Optional range overlay data
 * @property aoeOverlay Optional AoE overlay data
 * @property selectedPosition Currently selected grid position
 * @property cameraOffset Camera offset for panning
 * @property zoomLevel Current zoom level (0.5x to 3.0x)
 */
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
