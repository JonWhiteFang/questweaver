package dev.questweaver.feature.map.ui

import dev.questweaver.domain.map.geometry.GridPos

data class Token(val id: String, val pos: GridPos, val isEnemy: Boolean, val hpPct: Float)
data class MapState(
    val w: Int, val h: Int, val tileSize: Float,
    val blocked: Set<GridPos>,
    val difficult: Set<GridPos>,
    val tokens: List<Token>
)
