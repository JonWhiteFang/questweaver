package dev.questweaver.core.rules.validation.state

/**
 * Tracks available resources for a creature.
 * 
 * This is an immutable snapshot of resource availability at a point in time.
 * All operations return new instances rather than modifying the existing pool.
 */
data class ResourcePool(
    val spellSlots: Map<Int, Int> = emptyMap(),           // Level -> remaining slots
    val classFeatures: Map<String, Int> = emptyMap(),     // Feature ID -> remaining uses
    val itemCharges: Map<Long, Int> = emptyMap(),         // Item ID -> remaining charges
    val hitDice: Map<String, Int> = emptyMap()            // Dice type -> remaining dice
) {
    /**
     * Checks if the specified resource is available.
     */
    fun hasResource(resource: Resource): Boolean = when (resource) {
        is Resource.SpellSlot -> {
            // Check if any slot of this level or higher is available
            spellSlots.entries.any { (level, count) ->
                level >= resource.level && count > 0
            }
        }
        is Resource.ClassFeature -> {
            classFeatures[resource.featureId]?.let { it >= resource.uses } ?: false
        }
        is Resource.ItemCharge -> {
            itemCharges[resource.itemId]?.let { it >= resource.charges } ?: false
        }
        is Resource.HitDice -> {
            hitDice[resource.diceType]?.let { it >= resource.count } ?: false
        }
    }
    
    /**
     * Returns a new ResourcePool with the specified resource consumed.
     * 
     * @throws IllegalStateException if the resource is not available
     */
    fun consume(resource: Resource): ResourcePool = when (resource) {
        is Resource.SpellSlot -> {
            // Find the lowest available slot level that can be used
            val availableLevel = spellSlots.entries
                .filter { (level, count) -> level >= resource.level && count > 0 }
                .minByOrNull { it.key }
                ?.key
                ?: error("No spell slot of level ${resource.level} or higher available")
            
            val currentCount = spellSlots[availableLevel]!!
            copy(spellSlots = spellSlots + (availableLevel to currentCount - 1))
        }
        is Resource.ClassFeature -> {
            val currentUses = classFeatures[resource.featureId]
                ?: error("Class feature ${resource.featureId} not found")
            require(currentUses >= resource.uses) {
                "Insufficient uses for ${resource.featureId}: have $currentUses, need ${resource.uses}"
            }
            copy(classFeatures = classFeatures + (resource.featureId to currentUses - resource.uses))
        }
        is Resource.ItemCharge -> {
            val currentCharges = itemCharges[resource.itemId]
                ?: error("Item ${resource.itemId} not found")
            require(currentCharges >= resource.charges) {
                "Insufficient charges for item ${resource.itemId}: have $currentCharges, need ${resource.charges}"
            }
            copy(itemCharges = itemCharges + (resource.itemId to currentCharges - resource.charges))
        }
        is Resource.HitDice -> {
            val currentDice = hitDice[resource.diceType]
                ?: error("Hit dice ${resource.diceType} not found")
            require(currentDice >= resource.count) {
                "Insufficient hit dice ${resource.diceType}: have $currentDice, need ${resource.count}"
            }
            copy(hitDice = hitDice + (resource.diceType to currentDice - resource.count))
        }
    }
    
    /**
     * Returns the highest spell slot level available for a given minimum level.
     * Returns null if no slots are available.
     */
    fun getAvailableSpellSlotLevel(minLevel: Int): Int? {
        return spellSlots.entries
            .filter { (level, count) -> level >= minLevel && count > 0 }
            .minByOrNull { it.key }
            ?.key
    }
    
    /**
     * Returns all spell slot levels that have remaining slots.
     */
    fun getAvailableSpellSlotLevels(): List<Int> {
        return spellSlots.entries
            .filter { (_, count) -> count > 0 }
            .map { it.key }
            .sorted()
    }
    
    companion object {
        /**
         * Creates an empty resource pool with no resources.
         */
        val Empty = ResourcePool()
    }
}
