package dev.questweaver.feature.map.ui

import dev.questweaver.domain.map.geometry.AoETemplate
import dev.questweaver.domain.map.geometry.GridPos

/**
 * Data for rendering area-of-effect overlays on the map.
 *
 * @property template The AoE template (sphere, cube, cone)
 * @property origin The origin position of the AoE
 * @property affectedPositions Set of positions affected by the AoE
 */
data class AoEOverlayData(
    val template: AoETemplate,
    val origin: GridPos,
    val affectedPositions: Set<GridPos>
)
