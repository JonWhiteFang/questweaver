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
    val strength: Int = DEFAULT_ABILITY_SCORE,
    val dexterity: Int = DEFAULT_ABILITY_SCORE,
    val constitution: Int = DEFAULT_ABILITY_SCORE,
    val intelligence: Int = DEFAULT_ABILITY_SCORE,
    val wisdom: Int = DEFAULT_ABILITY_SCORE,
    val charisma: Int = DEFAULT_ABILITY_SCORE
) {
    init {
        require(strength in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Strength must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
        require(dexterity in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Dexterity must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
        require(constitution in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Constitution must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
        require(intelligence in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Intelligence must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
        require(wisdom in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Wisdom must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
        require(charisma in MIN_ABILITY_SCORE..MAX_ABILITY_SCORE) { 
            "Charisma must be between $MIN_ABILITY_SCORE and $MAX_ABILITY_SCORE" 
        }
    }
    
    /**
     * Strength modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val strModifier: Int get() = Math.floorDiv(strength - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    /**
     * Dexterity modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val dexModifier: Int get() = Math.floorDiv(dexterity - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    /**
     * Constitution modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val conModifier: Int get() = Math.floorDiv(constitution - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    /**
     * Intelligence modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val intModifier: Int get() = Math.floorDiv(intelligence - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    /**
     * Wisdom modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val wisModifier: Int get() = Math.floorDiv(wisdom - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    /**
     * Charisma modifier following D&D 5e rules: (score - 10) / 2 (rounded down)
     */
    val chaModifier: Int get() = Math.floorDiv(charisma - MODIFIER_BASE, MODIFIER_DIVISOR)
    
    companion object {
        /** Default ability score for average creatures (D&D 5e standard) */
        const val DEFAULT_ABILITY_SCORE = 10
        
        /** Minimum valid ability score */
        const val MIN_ABILITY_SCORE = 1
        
        /** Maximum valid ability score (includes magical enhancements) */
        const val MAX_ABILITY_SCORE = 30
        
        /** Base value for modifier calculation (D&D 5e: (score - 10) / 2) */
        const val MODIFIER_BASE = 10
        
        /** Divisor for modifier calculation (D&D 5e: (score - 10) / 2) */
        const val MODIFIER_DIVISOR = 2
    }
}
