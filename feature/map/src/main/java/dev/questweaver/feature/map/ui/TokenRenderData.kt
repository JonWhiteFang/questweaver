package dev.questweaver.feature.map.ui

import dev.questweaver.domain.values.GridPos

/**
 * Rendering data for a creature token on the map.
 *
 * @property creatureId Unique identifier for the creature
 * @property position Grid position of the creature
 * @property allegiance Creature's allegiance (friendly, enemy, neutral)
 * @property currentHP Current hit points
 * @property maxHP Maximum hit points
 */
data class TokenRenderData(
    val creatureId: Long,
    val position: GridPos,
    val allegiance: Allegiance,
    val currentHP: Int,
    val maxHP: Int
) {
    /**
     * Whether to show HP numbers on the token.
     * Only friendly creatures show HP numbers.
     */
    val showHP: Boolean = allegiance == Allegiance.FRIENDLY
    
    /**
     * Whether the creature is bloodied (below half HP).
     */
    val isBloodied: Boolean = currentHP < maxHP / 2
    
    /**
     * HP percentage for visual health indicator (0.0 to 1.0).
     */
    val hpPercentage: Float = currentHP.toFloat() / maxHP.toFloat()
}

/**
 * Creature allegiance for color coding.
 */
enum class Allegiance {
    FRIENDLY,
    ENEMY,
    NEUTRAL
}
