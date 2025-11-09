package dev.questweaver.feature.map.ui
data class GridPos(val x: Int, val y: Int)
data class Token(val id: String, val pos: GridPos, val isEnemy: Boolean, val hpPct: Float)
data class MapState(
    val w: Int, val h: Int, val tileSize: Float,
    val blocked: Set<GridPos>,
    val difficult: Set<GridPos>,
    val tokens: List<Token>
)
