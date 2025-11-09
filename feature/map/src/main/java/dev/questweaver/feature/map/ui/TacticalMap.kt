package dev.questweaver.feature.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
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
        drawLine(Offset(x*state.tileSize, 0f), Offset(x*state.tileSize, size.height))
    }
    for (y in 0..state.h) {
        drawLine(Offset(0f, y*state.tileSize), Offset(size.width, y*state.tileSize))
    }
}

private fun DrawScope.drawObstacles(state: MapState) {
    state.blocked.forEach { (x,y) ->
        drawRect(topLeft = Offset(x*state.tileSize, y*state.tileSize),
            size = Size(state.tileSize, state.tileSize), alpha = 0.2f)
    }
}

private fun DrawScope.drawTokens(state: MapState) {
    state.tokens.forEach { t ->
        val cx = t.pos.x*state.tileSize + state.tileSize/2
        val cy = t.pos.y*state.tileSize + state.tileSize/2
        drawCircle(center = Offset(cx, cy), radius = state.tileSize*0.35f)
        // hp bar
        drawRect(
            topLeft = Offset(cx - state.tileSize*0.4f, cy + state.tileSize*0.45f),
            size = Size(state.tileSize*0.8f * t.hpPct, state.tileSize*0.1f)
        )
    }
}
