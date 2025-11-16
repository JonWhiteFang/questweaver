package dev.questweaver.ai.tactical.targeting

import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Evaluates threat levels of enemy creatures.
 * Threat assessment helps prioritize targets based on danger level.
 */
class ThreatAssessor {
    /**
     * Calculates threat score for a creature.
     * Higher scores indicate more dangerous creatures that should be prioritized.
     *
     * @param creature The creature to assess
     * @param context Current tactical situation
     * @return Threat score (higher = more dangerous)
     */
    fun assessThreat(
        creature: Creature,
        context: TacticalContext
    ): Float {
        var threat = 0f
        
        // Damage output (weight: 2.0)
        threat += calculateDamageOutput(creature, context) * DAMAGE_WEIGHT
        
        // Healing capability (weight: 3.0)
        threat += calculateHealingCapability(creature, context) * HEALING_WEIGHT
        
        // Control potential (weight: 2.5)
        threat += calculateControlPotential(creature, context) * CONTROL_WEIGHT
        
        // HP remaining (weight: 0.1)
        threat += creature.hitPointsCurrent * HP_WEIGHT
        
        // Concentration bonus
        if (context.concentrationSpells.containsKey(creature.id)) {
            threat += CONCENTRATION_BONUS
        }
        
        // Role bonus
        threat += getRoleBonus(creature, context)
        
        return threat
    }
    
    /**
     * Estimates average damage per round for a creature.
     * Considers multi-attack, spell damage, and abilities.
     */
    private fun calculateDamageOutput(
        creature: Creature,
        context: TacticalContext
    ): Float {
        // Base damage estimate from ability scores
        val strModifier = creature.abilities.strModifier
        val dexModifier = creature.abilities.dexModifier
        val intModifier = creature.abilities.intModifier
        
        // Use highest offensive modifier
        val primaryModifier = maxOf(strModifier, dexModifier, intModifier)
        
        // Estimate: 1d8 + modifier per attack, assume 2 attacks
        val baseWeaponDamage = (AVERAGE_D8_ROLL + primaryModifier) * TYPICAL_ATTACKS_PER_ROUND
        
        // Add spell damage estimate if spellcaster
        val spellDamage = if (hasSpellSlots(creature, context)) {
            // Estimate: 3d6 (10.5) for a typical 1st level spell
            AVERAGE_SPELL_DAMAGE
        } else {
            0f
        }
        
        // Add recent damage as indicator of actual output
        val recentDamage = context.recentDamage[creature.id]?.toFloat() ?: 0f
        val recentDamagePerRound = recentDamage / RECENT_DAMAGE_ROUNDS
        
        // Weight recent actual damage more heavily
        return (baseWeaponDamage + spellDamage) * ESTIMATED_DAMAGE_WEIGHT + 
               recentDamagePerRound * ACTUAL_DAMAGE_WEIGHT
    }
    
    /**
     * Estimates average healing per round for a creature.
     * Healers are high-priority targets.
     */
    private fun calculateHealingCapability(
        creature: Creature,
        context: TacticalContext
    ): Float {
        // Check if creature has healing spells or abilities
        val hasHealingSpells = hasSpellSlots(creature, context)
        val wisModifier = creature.abilities.wisModifier
        
        if (!hasHealingSpells) {
            return 0f
        }
        
        // Estimate: 1d8 + wisdom modifier for typical healing spell
        return AVERAGE_D8_ROLL + wisModifier
    }
    
    /**
     * Estimates control potential (ability to disable allies).
     * Creatures with control spells are high-priority targets.
     */
    private fun calculateControlPotential(
        creature: Creature,
        context: TacticalContext
    ): Float {
        // Check if creature has spell slots (potential for control spells)
        if (!hasSpellSlots(creature, context)) {
            return 0f
        }
        
        // Estimate based on spell attack bonus
        val spellAttackBonus = creature.spellAttackBonus
        
        // Higher spell attack bonus = more likely to land control effects
        return spellAttackBonus.toFloat()
    }
    
    /**
     * Checks if a creature has spell slots available.
     */
    private fun hasSpellSlots(creature: Creature, context: TacticalContext): Boolean {
        val slots = context.availableSpellSlots[creature.id] ?: return false
        return slots.values.any { it > 0 }
    }
    
    /**
     * Returns role-based threat bonus.
     */
    private fun getRoleBonus(creature: Creature, context: TacticalContext): Float {
        // Determine role based on creature characteristics
        val hasHealing = hasSpellSlots(creature, context) && creature.abilities.wisModifier > 0
        val hasSpells = hasSpellSlots(creature, context)
        val highDamage = creature.abilities.strModifier > HIGH_DAMAGE_THRESHOLD || 
                        creature.abilities.dexModifier > HIGH_DAMAGE_THRESHOLD
        val highHP = creature.hitPointsMax > HIGH_HP_THRESHOLD
        
        return when {
            hasHealing -> HEALER_BONUS
            hasSpells -> SPELLCASTER_BONUS
            highDamage -> STRIKER_BONUS
            highHP -> TANK_BONUS
            else -> 0f
        }
    }
    
    companion object {
        // Weights for threat calculation
        private const val DAMAGE_WEIGHT = 2.0f
        private const val HEALING_WEIGHT = 3.0f
        private const val CONTROL_WEIGHT = 2.5f
        private const val HP_WEIGHT = 0.1f
        
        // Bonuses
        private const val CONCENTRATION_BONUS = 50f
        private const val HEALER_BONUS = 30f
        private const val SPELLCASTER_BONUS = 20f
        private const val STRIKER_BONUS = 10f
        private const val TANK_BONUS = 0f
        
        // Damage calculation constants
        private const val AVERAGE_D8_ROLL = 4.5f
        private const val TYPICAL_ATTACKS_PER_ROUND = 2f
        private const val AVERAGE_SPELL_DAMAGE = 10.5f
        private const val RECENT_DAMAGE_ROUNDS = 2f
        private const val ESTIMATED_DAMAGE_WEIGHT = 0.3f
        private const val ACTUAL_DAMAGE_WEIGHT = 0.7f
        
        // Role detection thresholds
        private const val HIGH_DAMAGE_THRESHOLD = 2
        private const val HIGH_HP_THRESHOLD = 50
    }
}
