package dev.questweaver.core.rules.validation.state

/**
 * Represents a consumable or limited resource that can be used by creatures.
 */
sealed interface Resource {
    /**
     * A spell slot of a specific level (1-9).
     */
    data class SpellSlot(val level: Int) : Resource {
        init {
            require(level in MIN_SPELL_LEVEL..MAX_SPELL_LEVEL) { 
                "Spell slot level must be between $MIN_SPELL_LEVEL and $MAX_SPELL_LEVEL" 
            }
        }
        
        companion object {
            const val MIN_SPELL_LEVEL = 1
            const val MAX_SPELL_LEVEL = 9
        }
    }
    
    /**
     * A limited-use class feature.
     */
    data class ClassFeature(val featureId: String, val uses: Int) : Resource {
        init {
            require(uses > 0) { "Class feature uses must be positive" }
        }
    }
    
    /**
     * Charges from a magical item.
     */
    data class ItemCharge(val itemId: Long, val charges: Int) : Resource {
        init {
            require(charges > 0) { "Item charges must be positive" }
        }
    }
    
    /**
     * Hit dice for healing during short rests.
     */
    data class HitDice(val diceType: String, val count: Int) : Resource {
        init {
            require(count > 0) { "Hit dice count must be positive" }
        }
    }
}
