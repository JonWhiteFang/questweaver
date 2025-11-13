package dev.questweaver.feature.map.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import dev.questweaver.feature.map.ui.Allegiance
import dev.questweaver.feature.map.ui.TokenRenderData
import dev.questweaver.feature.map.util.CoordinateConverter

/**
 * Extension functions for rendering creature tokens on a DrawScope.
 */

private const val TOKEN_SIZE_RATIO = 0.8f
private const val HP_RING_WIDTH = 3f
private const val HP_TEXT_SIZE_RATIO = 0.3f
private const val BLOODIED_INDICATOR_SIZE_RATIO = 0.3f
private const val BLOODIED_INDICATOR_OFFSET_RATIO = 0.6f
private const val HP_TEXT_VERTICAL_OFFSET_RATIO = 3f
private const val TOKEN_RADIUS_MULTIPLIER = 2f
private const val HP_RING_START_ANGLE = -90f
private const val HP_RING_FULL_CIRCLE = 360f

/**
 * Renders creature tokens on the map.
 *
 * @param tokens List of token data to render
 * @param cellSize The size of each cell in pixels
 * @param cameraOffset The camera offset for panning
 * @param zoomLevel The current zoom level
 */
fun DrawScope.drawTokens(
    tokens: List<TokenRenderData>,
    cellSize: Float,
    cameraOffset: Offset,
    zoomLevel: Float
) {
    val scaledCellSize = cellSize * zoomLevel
    val tokenRadius = scaledCellSize * TOKEN_SIZE_RATIO / 2f
    
    tokens.forEach { token ->
        val center = CoordinateConverter.gridToScreenCenter(
            token.position,
            cellSize,
            cameraOffset,
            zoomLevel
        )
        
        drawTokenCircle(token, tokenRadius, center)
        drawHPIndicator(token, scaledCellSize, center)
        drawBloodiedIndicator(token, tokenRadius, center)
        drawHPRing(token, tokenRadius, center)
    }
}

/**
 * Draws the main token circle with allegiance color.
 */
private fun DrawScope.drawTokenCircle(
    token: TokenRenderData,
    tokenRadius: Float,
    center: Offset
) {
    val tokenColor = when (token.allegiance) {
        Allegiance.FRIENDLY -> Color.Blue
        Allegiance.ENEMY -> Color.Red
        Allegiance.NEUTRAL -> Color.Yellow
    }
    
    drawCircle(
        color = tokenColor,
        radius = tokenRadius,
        center = center
    )
}

/**
 * Draws HP text indicator for friendly creatures.
 */
private fun DrawScope.drawHPIndicator(
    token: TokenRenderData,
    scaledCellSize: Float,
    center: Offset
) {
    if (!token.showHP) return
    
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = scaledCellSize * HP_TEXT_SIZE_RATIO
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawText(
            "${token.currentHP}",
            center.x,
            center.y + paint.textSize / HP_TEXT_VERTICAL_OFFSET_RATIO,
            paint
        )
    }
}

/**
 * Draws bloodied indicator for non-friendly creatures below half HP.
 */
private fun DrawScope.drawBloodiedIndicator(
    token: TokenRenderData,
    tokenRadius: Float,
    center: Offset
) {
    if (token.showHP || !token.isBloodied) return
    
    val bloodiedRadius = tokenRadius * BLOODIED_INDICATOR_SIZE_RATIO
    val bloodiedOffset = tokenRadius * BLOODIED_INDICATOR_OFFSET_RATIO
    drawCircle(
        color = Color.Red,
        radius = bloodiedRadius,
        center = center + Offset(bloodiedOffset, -bloodiedOffset)
    )
}

/**
 * Draws HP ring showing health percentage.
 */
private fun DrawScope.drawHPRing(
    token: TokenRenderData,
    tokenRadius: Float,
    center: Offset
) {
    val sweepAngle = HP_RING_FULL_CIRCLE * token.hpPercentage
    drawArc(
        color = Color.Green,
        startAngle = HP_RING_START_ANGLE,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = center - Offset(tokenRadius, tokenRadius),
        size = Size(tokenRadius * TOKEN_RADIUS_MULTIPLIER, tokenRadius * TOKEN_RADIUS_MULTIPLIER),
        style = Stroke(width = HP_RING_WIDTH)
    )
}
