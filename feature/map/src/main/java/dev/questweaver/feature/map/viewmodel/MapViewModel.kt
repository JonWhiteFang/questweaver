package dev.questweaver.feature.map.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.questweaver.domain.map.geometry.AoETemplate
import dev.questweaver.domain.map.geometry.DistanceCalculator
import dev.questweaver.domain.map.geometry.MapGrid
import dev.questweaver.domain.map.pathfinding.Pathfinder
import dev.questweaver.domain.map.pathfinding.ReachabilityCalculator
import dev.questweaver.domain.values.GridPos
import dev.questweaver.feature.map.ui.AoEOverlayData
import dev.questweaver.feature.map.ui.MapIntent
import dev.questweaver.feature.map.ui.MapRenderState
import dev.questweaver.feature.map.ui.RangeOverlayData
import dev.questweaver.feature.map.ui.RangeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for tactical map rendering following MVI pattern.
 *
 * Manages map state including camera position, zoom level, overlays, and user interactions.
 * All state updates are immutable and processed through the [handle] method.
 *
 * @param pathfinder Pathfinder for calculating movement paths (reserved for future use)
 * @param reachabilityCalculator Calculator for finding reachable positions
 */
@Suppress("TooManyFunctions") // ViewModel requires multiple public methods for different overlay types
class MapViewModel(
    @Suppress("UnusedPrivateProperty") // Reserved for future pathfinding features
    private val pathfinder: Pathfinder,
    private val reachabilityCalculator: ReachabilityCalculator
) : ViewModel() {
    
    companion object {
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 3.0f
    }
    
    private val _state = MutableStateFlow(
        MapRenderState(
            grid = MapGrid(width = 20, height = 20)
        )
    )
    val state: StateFlow<MapRenderState> = _state.asStateFlow()
    
    /**
     * Handles user intents and updates state accordingly.
     *
     * @param intent The user intent to process
     */
    fun handle(intent: MapIntent) {
        when (intent) {
            is MapIntent.CellTapped -> handleCellTap(intent.position)
            is MapIntent.TokenTapped -> handleTokenTap(intent.creatureId)
            is MapIntent.Pan -> handlePan(intent.delta)
            is MapIntent.Zoom -> handleZoom(intent.scale, intent.focus)
        }
    }
    
    /**
     * Handles cell tap events by updating the selected position.
     */
    private fun handleCellTap(position: GridPos) {
        _state.update { it.copy(selectedPosition = position) }
    }
    
    /**
     * Handles token tap events.
     * Currently updates selected position to the token's position.
     * Can be extended to show creature details or actions.
     */
    private fun handleTokenTap(creatureId: Long) {
        val token = _state.value.tokens.find { it.creatureId == creatureId }
        if (token != null) {
            _state.update { it.copy(selectedPosition = token.position) }
        }
    }
    
    /**
     * Handles pan gestures by updating the camera offset.
     */
    private fun handlePan(delta: Offset) {
        _state.update { 
            it.copy(cameraOffset = it.cameraOffset + delta)
        }
    }
    
    /**
     * Handles zoom gestures by updating the zoom level.
     * Clamps zoom level between MIN_ZOOM and MAX_ZOOM.
     *
     * @param scale The zoom scale factor
     * @param focus The focus point for the zoom (reserved for future focal point zoom)
     */
    @Suppress("UnusedParameter") // Focus parameter reserved for future focal point zoom implementation
    private fun handleZoom(scale: Float, focus: Offset) {
        _state.update {
            val newZoom = (it.zoomLevel * scale).coerceIn(MIN_ZOOM, MAX_ZOOM)
            it.copy(zoomLevel = newZoom)
        }
    }
    
    /**
     * Shows movement range overlay for a creature.
     * Uses ReachabilityCalculator to find all positions reachable within the movement budget.
     *
     * @param origin The creature's current position
     * @param movementBudget The creature's available movement points
     */
    fun showMovementRange(origin: GridPos, movementBudget: Int) {
        viewModelScope.launch {
            val geometryOrigin = origin.toGeometryGridPos()
            val reachable = reachabilityCalculator.findReachablePositions(
                start = geometryOrigin,
                movementBudget = movementBudget,
                grid = _state.value.grid
            )
            
            _state.update {
                it.copy(
                    rangeOverlay = RangeOverlayData(
                        origin = origin,
                        positions = reachable.map { it.toValuesGridPos() }.toSet(),
                        rangeType = RangeType.MOVEMENT
                    )
                )
            }
        }
    }
    
    /**
     * Shows weapon range overlay for a creature.
     * Uses DistanceCalculator to find all positions within weapon range.
     *
     * @param origin The creature's current position
     * @param rangeInFeet The weapon's range in feet
     */
    fun showWeaponRange(origin: GridPos, rangeInFeet: Int) {
        viewModelScope.launch {
            val geometryOrigin = origin.toGeometryGridPos()
            val positions = DistanceCalculator.positionsWithinRange(
                center = geometryOrigin,
                rangeInFeet = rangeInFeet,
                grid = _state.value.grid
            )
            
            _state.update {
                it.copy(
                    rangeOverlay = RangeOverlayData(
                        origin = origin,
                        positions = positions.map { it.toValuesGridPos() }.toSet(),
                        rangeType = RangeType.WEAPON
                    )
                )
            }
        }
    }
    
    /**
     * Shows spell range overlay for a creature.
     * Uses DistanceCalculator to find all positions within spell range.
     * Note: Line-of-effect checking can be added in future iterations.
     *
     * @param origin The creature's current position
     * @param rangeInFeet The spell's range in feet
     */
    fun showSpellRange(origin: GridPos, rangeInFeet: Int) {
        viewModelScope.launch {
            val geometryOrigin = origin.toGeometryGridPos()
            val positions = DistanceCalculator.positionsWithinRange(
                center = geometryOrigin,
                rangeInFeet = rangeInFeet,
                grid = _state.value.grid
            )
            
            _state.update {
                it.copy(
                    rangeOverlay = RangeOverlayData(
                        origin = origin,
                        positions = positions.map { it.toValuesGridPos() }.toSet(),
                        rangeType = RangeType.SPELL
                    )
                )
            }
        }
    }
    
    /**
     * Shows AoE preview overlay for a spell or ability.
     * Calculates affected positions using the template's affectedPositions method.
     *
     * @param template The AoE template (sphere, cube, cone, etc.)
     * @param origin The origin point of the AoE effect
     */
    fun showAoEPreview(template: AoETemplate, origin: GridPos) {
        viewModelScope.launch {
            val geometryOrigin = origin.toGeometryGridPos()
            val affected = template.affectedPositions(
                origin = geometryOrigin,
                grid = _state.value.grid
            )
            
            _state.update {
                it.copy(
                    aoeOverlay = AoEOverlayData(
                        template = template,
                        origin = origin,
                        affectedPositions = affected.map { it.toValuesGridPos() }.toSet()
                    )
                )
            }
        }
    }
    
    /**
     * Clears the range overlay from the map.
     */
    fun clearRangeOverlay() {
        _state.update { it.copy(rangeOverlay = null) }
    }
    
    /**
     * Clears the AoE overlay from the map.
     */
    fun clearAoEOverlay() {
        _state.update { it.copy(aoeOverlay = null) }
    }
    
    /**
     * Updates the map grid.
     * Useful for loading a new map or updating terrain.
     *
     * @param grid The new map grid
     */
    fun updateGrid(grid: MapGrid) {
        _state.update { it.copy(grid = grid) }
    }
}

/**
 * Converts a values.GridPos to a map.geometry.GridPos for use with domain geometry functions.
 */
private fun GridPos.toGeometryGridPos(): dev.questweaver.domain.map.geometry.GridPos {
    return dev.questweaver.domain.map.geometry.GridPos(x, y)
}

/**
 * Converts a map.geometry.GridPos to a values.GridPos for use with UI state.
 */
private fun dev.questweaver.domain.map.geometry.GridPos.toValuesGridPos(): GridPos {
    return GridPos(x, y)
}
