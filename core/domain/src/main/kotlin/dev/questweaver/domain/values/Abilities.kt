package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents D&D 5e ability scores.
 *
 * @property strength Physical power and athletic prowess
 * @property dexterity Agility, reflexes, and balance
 * @property constitution Endurance and health
 * @property intelligence Reasoning and memory
 * @property wisdom Awareness and insight
 * @property charisma Force of personality and leadership
 */
@Serializable
data class Abilities(
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10
) {
    init {
        require(strength in 1..30) { "Strength must be between 1 and 30" }
        require(dexterity in 1..30) { "Dexterity must be between 1 and 30" }
        require(constitution in 1..30) { "Constitution must be between 1 and 30" }
        require(intelligence in 1..30) { "Intelligence must be between 1 and 30" }
        require(wisdom in 1..30) { "Wisdom must be between 1 and 30" }
        require(charisma in 1..30) { "Charisma must be between 1 and 30" }
    }
    
    /**
     * Strength modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val strModifier: Int get() = Math.floorDiv(strength - 10, 2)
    
    /**
     * Dexterity modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val dexModifier: Int get() = Math.floorDiv(dexterity - 10, 2)
    
    /**
     * Constitution modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val conModifier: Int get() = Math.floorDiv(constitution - 10, 2)
    
    /**
     * Intelligence modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val intModifier: Int get() = Math.floorDiv(intelligence - 10, 2)
    
    /**
     * Wisdom modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val wisModifier: Int get() = Math.floorDiv(wisdom - 10, 2)
    
    /**
     * Charisma modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val chaModifier: Int get() = Math.floorDiv(charisma - 10, 2)
}
