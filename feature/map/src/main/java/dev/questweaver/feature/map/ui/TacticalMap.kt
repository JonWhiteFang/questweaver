package dev.questweaver.feature.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun TacticalMap(state: MapState, onTap: (GridPos) -> Unit, modifier: Modifier = Modifier) {
    Canvas(modifier.pointerInput(Unit) {
        detectTapGestures { offset ->
            val gx = (offset.x / state.tileSize).toInt()
            val gy = (offset.y / state.tileSize).toInt()
            onTap(GridPos(gx, gy))
        }
    }) {
        drawGrid(state)
        drawObstacles(state)
        drawTokens(state)
    }
}

private fun DrawScope.drawGrid(state: MapState) {
    for (x in 0..state.w) {
        drawLine(Color.Gray, Offset(x*state.tileSize, 0f), Offset(x*state.tileSize, size.height), strokeWidth = 1f)
    }
    for (y in 0..state.h) {
        drawLine(Color.Gray, Offset(0f, y*state.tileSize), Offset(size.width, y*state.tileSize), strokeWidth = 1f)
    }
}

private fun DrawScope.drawObstacles(state: MapState) {
    state.blocked.forEach { (x,y) ->
        drawRect(Color.Black, topLeft = Offset(x*state.tileSize, y*state.tileSize),
            size = Size(state.tileSize, state.tileSize), alpha = 0.2f)
    }
}

private fun DrawScope.drawTokens(state: MapState) {
    state.tokens.forEach { t ->
        val cx = t.pos.x * state.tileSize + state.tileSize / HALF_DIVISOR
        val cy = t.pos.y * state.tileSize + state.tileSize / HALF_DIVISOR
        drawCircle(Color.Blue, center = Offset(cx, cy), radius = state.tileSize * TOKEN_RADIUS_FACTOR)
        // hp bar
        drawRect(
            Color.Red,
            topLeft = Offset(cx - state.tileSize * HP_BAR_OFFSET, cy + state.tileSize * HP_BAR_Y_OFFSET),
            size = Size(state.tileSize * HP_BAR_WIDTH * t.hpPct, state.tileSize * HP_BAR_HEIGHT)
        )
    }
}

private const val HALF_DIVISOR = 2f
private const val TOKEN_RADIUS_FACTOR = 0.35f
private const val HP_BAR_OFFSET = 0.4f
private const val HP_BAR_Y_OFFSET = 0.45f
private const val HP_BAR_WIDTH = 0.8f
private const val HP_BAR_HEIGHT = 0.1f
