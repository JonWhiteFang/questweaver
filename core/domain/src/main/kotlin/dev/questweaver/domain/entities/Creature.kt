package dev.questweaver.domain.entities

import dev.questweaver.domain.values.Abilities
import dev.questweaver.domain.values.Condition
import kotlinx.serialization.Serializable

/**
 * Represents a creature (PC, NPC, or monster) with D&D 5e attributes.
 *
 * @property id Unique identifier for the creature
 * @property name The creature's name
 * @property armorClass The creature's AC (Armor Class)
 * @property hitPointsCurrent The creature's current hit points
 * @property hitPointsMax The creature's maximum hit points
 * @property speed The creature's movement speed in feet
 * @property abilities The creature's six ability scores
 * @property proficiencyBonus The creature's proficiency bonus
 * @property conditions Set of conditions currently affecting the creature
 */
@Serializable
data class Creature(
    val id: Long,
    val name: String,
    val armorClass: Int,
    val hitPointsCurrent: Int,
    val hitPointsMax: Int,
    val speed: Int,
    val abilities: Abilities,
    val proficiencyBonus: Int = 2,
    val conditions: Set<Condition> = emptySet()
) {
    init {
        require(id > 0) { "Creature id must be positive" }
        require(name.isNotBlank()) { "Creature name cannot be blank" }
        require(armorClass > 0) { "Armor class must be positive" }
        require(hitPointsMax > 0) { "Max HP must be positive" }
        require(hitPointsCurrent in 0..hitPointsMax) { 
            "Current HP must be between 0 and max HP ($hitPointsMax)" 
        }
        require(speed >= 0) { "Speed cannot be negative" }
        require(proficiencyBonus >= 0) { "Proficiency bonus cannot be negative" }
    }
    
    /**
     * Returns true if the creature has more than 0 hit points.
     */
    val isAlive: Boolean 
        get() = hitPointsCurrent > 0
    
    /**
     * Returns true if the creature's current HP is at or below half of max HP.
     * Bloodied is a common D&D term indicating a creature is significantly wounded.
     */
    val isBloodied: Boolean 
        get() = hitPointsCurrent <= hitPointsMax / 2
}
