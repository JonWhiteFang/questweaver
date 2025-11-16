package dev.questweaver.ai.tactical

/**
 * Represents a limited resource that can be consumed by actions.
 */
sealed interface Resource {
    /**
     * A spell slot of a specific level.
     *
     * @property level The spell slot level (1-9)
     */
    data class SpellSlot(val level: Int) : Resource {
        init {
            require(level in MIN_SPELL_LEVEL..MAX_SPELL_LEVEL) { 
                "Spell slot level must be between $MIN_SPELL_LEVEL and $MAX_SPELL_LEVEL" 
            }
        }
        
        companion object {
            private const val MIN_SPELL_LEVEL = 1
            private const val MAX_SPELL_LEVEL = 9
        }
    }
    
    /**
     * A limited-use ability.
     *
     * @property name The name of the ability
     * @property usesRemaining Number of uses remaining
     */
    data class LimitedAbility(
        val name: String,
        val usesRemaining: Int
    ) : Resource {
        init {
            require(name.isNotBlank()) { "Ability name cannot be blank" }
            require(usesRemaining >= 0) { "Uses remaining cannot be negative" }
        }
    }
    
    /**
     * A consumable item.
     *
     * @property name The name of the item
     */
    data class ConsumableItem(val name: String) : Resource {
        init {
            require(name.isNotBlank()) { "Item name cannot be blank" }
        }
    }
}
