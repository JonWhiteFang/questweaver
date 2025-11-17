package dev.questweaver.ai.tactical.resources

import dev.questweaver.ai.tactical.Resource
import dev.questweaver.ai.tactical.TacticalAction
import dev.questweaver.ai.tactical.TacticalContext

/**
 * Manages limited resources (spell slots, abilities, consumables) for tactical AI decisions.
 * 
 * Implements intelligent resource usage heuristics:
 * - Spell Slots: Reserve high-level slots for critical situations
 * - Limited Abilities: Use powerful abilities when many enemies remain
 * - Consumable Items: Use items when tactical benefit justifies cost
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7
 */
class ResourceManager {
    
    /**
     * Decides whether to use a resource for an action.
     * 
     * Evaluates the current tactical situation to determine if using the resource
     * is justified based on encounter difficulty, remaining enemies, and ally status.
     * 
     * @param resource The resource being considered
     * @param action The action requiring the resource
     * @param context Current tactical situation
     * @return True if the resource should be used
     */
    fun shouldUseResource(
        resource: Resource,
        action: TacticalAction,
        context: TacticalContext
    ): Boolean {
        return when (resource) {
            is Resource.SpellSlot -> shouldUseSpellSlot(resource, action, context)
            is Resource.LimitedAbility -> shouldUseLimitedAbility(resource, action, context)
            is Resource.ConsumableItem -> shouldUseConsumableItem(resource, action, context)
        }
    }
    
    /**
     * Evaluates whether to use a spell slot.
     * 
     * Heuristics:
     * - High-level slots (7-9): Only for critical situations
     * - Mid-level slots (4-6): When 3+ enemies remain
     * - Low-level slots (1-3): Freely in early combat
     * - Prefer cantrips when resources low (<30%)
     * 
     * Requirements: 5.2, 5.4, 5.6
     */
    private fun shouldUseSpellSlot(
        spellSlot: Resource.SpellSlot,
        action: TacticalAction,
        context: TacticalContext
    ): Boolean {
        val level = spellSlot.level
        val enemyCount = context.enemies.size
        val difficulty = estimateEncounterDifficulty(context)
        
        // High-level slots (7-9): Only for critical situations
        if (level >= 7) {
            return difficulty == EncounterDifficulty.CRITICAL
        }
        
        // Mid-level slots (4-6): When 3+ enemies remain
        if (level in 4..6) {
            return enemyCount >= 3 || difficulty >= EncounterDifficulty.CHALLENGING
        }
        
        // Low-level slots (1-3): Use freely in early combat
        if (level in 1..3) {
            // Check if resources are low (<30%)
            val totalSlots = getTotalSpellSlots(context)
            val remainingSlots = getRemainingSpellSlots(context)
            val resourcePercentage = if (totalSlots > 0) {
                remainingSlots.toFloat() / totalSlots.toFloat()
            } else {
                0f
            }
            
            // Prefer cantrips when resources low
            if (resourcePercentage < 0.3f) {
                // Only use if situation is at least moderate
                return difficulty >= EncounterDifficulty.MODERATE
            }
            
            // Otherwise use freely
            return true
        }
        
        // Default: allow usage
        return true
    }
    
    /**
     * Evaluates whether to use a limited ability.
     * 
     * Heuristics:
     * - Powerful abilities: When 4+ enemies remain
     * - Defensive abilities: For HP < 50%
     * - Utility abilities: Use freely
     * 
     * Requirements: 5.3
     */
    private fun shouldUseLimitedAbility(
        ability: Resource.LimitedAbility,
        action: TacticalAction,
        context: TacticalContext
    ): Boolean {
        val enemyCount = context.enemies.size
        val abilityType = classifyAbility(ability.name)
        
        return when (abilityType) {
            AbilityType.POWERFUL -> {
                // Use powerful abilities when 4+ enemies remain
                enemyCount >= 4
            }
            AbilityType.DEFENSIVE -> {
                // Reserve defensive abilities for HP < 50%
                // Check if any ally is below 50% HP
                context.allies.any { ally ->
                    ally.hitPointsCurrent.toFloat() / ally.hitPointsMax.toFloat() < 0.5f
                }
            }
            AbilityType.UTILITY -> {
                // Use utility abilities freely
                true
            }
        }
    }
    
    /**
     * Evaluates whether to use a consumable item.
     * 
     * Heuristics:
     * - Healing potions: When HP < 30% and no healer
     * - Buff potions: Before major encounters
     * - Avoid consumables in trivial encounters
     * 
     * Requirements: 5.5
     */
    private fun shouldUseConsumableItem(
        item: Resource.ConsumableItem,
        action: TacticalAction,
        context: TacticalContext
    ): Boolean {
        val itemType = classifyItem(item.name)
        val difficulty = estimateEncounterDifficulty(context)
        
        // Avoid consumables in trivial encounters
        if (difficulty == EncounterDifficulty.TRIVIAL) {
            return false
        }
        
        return when (itemType) {
            ItemType.HEALING -> {
                // Use healing potions when HP < 30% and no healer
                val hasHealer = context.allies.any { isHealer(it) }
                val anyAllyLowHP = context.allies.any { ally ->
                    ally.hitPointsCurrent.toFloat() / ally.hitPointsMax.toFloat() < 0.3f
                }
                anyAllyLowHP && !hasHealer
            }
            ItemType.BUFF -> {
                // Use buff potions before major encounters
                difficulty >= EncounterDifficulty.CHALLENGING
            }
            ItemType.UTILITY -> {
                // Use utility items when encounter is at least moderate
                difficulty >= EncounterDifficulty.MODERATE
            }
        }
    }
    
    /**
     * Estimates the difficulty of the current encounter.
     * 
     * Considers:
     * - Remaining encounter duration
     * - Count of remaining enemies
     * - Ally HP and resources
     * 
     * Requirements: 5.2, 5.7
     */
    private fun estimateEncounterDifficulty(context: TacticalContext): EncounterDifficulty {
        val enemyCount = context.enemies.size
        val allyCount = context.allies.size
        
        // Calculate average ally HP percentage
        val avgAllyHPPercent = if (context.allies.isNotEmpty()) {
            context.allies.map { ally ->
                ally.hitPointsCurrent.toFloat() / ally.hitPointsMax.toFloat()
            }.average().toFloat()
        } else {
            1.0f
        }
        
        // Calculate resource availability
        val totalSlots = getTotalSpellSlots(context)
        val remainingSlots = getRemainingSpellSlots(context)
        val resourcePercent = if (totalSlots > 0) {
            remainingSlots.toFloat() / totalSlots.toFloat()
        } else {
            1.0f
        }
        
        // Assess threat level
        val threatRatio = if (allyCount > 0) {
            enemyCount.toFloat() / allyCount.toFloat()
        } else {
            Float.MAX_VALUE
        }
        
        // Determine difficulty based on multiple factors
        return when {
            // Critical: Outnumbered 2:1 or more, or allies very low HP
            threatRatio >= 2.0f || avgAllyHPPercent < 0.3f -> EncounterDifficulty.CRITICAL
            
            // Challenging: Outnumbered or allies below 50% HP
            threatRatio >= 1.5f || avgAllyHPPercent < 0.5f -> EncounterDifficulty.CHALLENGING
            
            // Moderate: Even numbers or allies below 70% HP
            threatRatio >= 1.0f || avgAllyHPPercent < 0.7f -> EncounterDifficulty.MODERATE
            
            // Easy: Outnumber enemies and allies healthy
            threatRatio <= 0.5f && avgAllyHPPercent > 0.7f -> EncounterDifficulty.EASY
            
            // Trivial: Greatly outnumber enemies
            enemyCount <= 1 && allyCount >= 3 && avgAllyHPPercent > 0.8f -> EncounterDifficulty.TRIVIAL
            
            // Default to moderate
            else -> EncounterDifficulty.MODERATE
        }
    }
    
    /**
     * Calculates total spell slots available across all allies.
     */
    private fun getTotalSpellSlots(context: TacticalContext): Int {
        return context.allies.sumOf { ally ->
            context.availableSpellSlots[ally.id]?.values?.sum() ?: 0
        }
    }
    
    /**
     * Calculates remaining spell slots across all allies.
     */
    private fun getRemainingSpellSlots(context: TacticalContext): Int {
        return context.allies.sumOf { ally ->
            context.availableSpellSlots[ally.id]?.values?.sum() ?: 0
        }
    }
    
    /**
     * Classifies an ability by type based on its name.
     */
    private fun classifyAbility(abilityName: String): AbilityType {
        val lowerName = abilityName.lowercase()
        
        return when {
            // Powerful offensive abilities
            lowerName.contains("action surge") ||
            lowerName.contains("rage") ||
            lowerName.contains("smite") ||
            lowerName.contains("sneak attack") -> AbilityType.POWERFUL
            
            // Defensive abilities
            lowerName.contains("second wind") ||
            lowerName.contains("lay on hands") ||
            lowerName.contains("shield") ||
            lowerName.contains("dodge") ||
            lowerName.contains("parry") -> AbilityType.DEFENSIVE
            
            // Default to utility
            else -> AbilityType.UTILITY
        }
    }
    
    /**
     * Classifies an item by type based on its name.
     */
    private fun classifyItem(itemName: String): ItemType {
        val lowerName = itemName.lowercase()
        
        return when {
            // Healing items
            lowerName.contains("potion of healing") ||
            lowerName.contains("healing potion") ||
            lowerName.contains("cure") -> ItemType.HEALING
            
            // Buff items
            lowerName.contains("potion of") && !lowerName.contains("healing") -> ItemType.BUFF
            
            // Default to utility
            else -> ItemType.UTILITY
        }
    }
    
    /**
     * Checks if a creature is a healer based on available healing abilities.
     */
    private fun isHealer(creature: dev.questweaver.domain.entities.Creature): Boolean {
        // This is a simplified check - in a full implementation, would check
        // for healing spells, abilities, or class features
        // For now, assume creatures with high Wisdom are potential healers
        return creature.abilities.wisModifier >= 2
    }
    
    /**
     * Represents the difficulty level of an encounter.
     */
    private enum class EncounterDifficulty {
        TRIVIAL,
        EASY,
        MODERATE,
        CHALLENGING,
        CRITICAL
    }
    
    /**
     * Represents the type of a limited ability.
     */
    private enum class AbilityType {
        POWERFUL,
        DEFENSIVE,
        UTILITY
    }
    
    /**
     * Represents the type of a consumable item.
     */
    private enum class ItemType {
        HEALING,
        BUFF,
        UTILITY
    }
}
