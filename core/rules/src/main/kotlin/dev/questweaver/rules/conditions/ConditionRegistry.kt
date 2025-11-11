package dev.questweaver.rules.conditions

import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.modifiers.SaveEffect

/**
 * Registry for looking up condition effects on various game mechanics.
 *
 * This object provides methods to determine how conditions affect attack rolls,
 * saving throws, ability checks, and actions. All condition effects are pre-computed
 * and stored as static data for performance.
 */
object ConditionRegistry {
    /**
     * Gets the effect of a condition on attack rolls.
     *
     * @param condition The condition to check
     * @param isAttacker Whether this is for the attacker (true) or defender (false)
     * @return RollModifier to apply, or null if the condition has no effect
     */
    fun getAttackRollEffect(condition: Condition, isAttacker: Boolean): RollModifier? {
        return if (isAttacker) {
            getAttackerEffect(condition)
        } else {
            getDefenderEffect(condition)
        }
    }
    
    /**
     * Gets the effect of a condition on the attacker's attack rolls.
     */
    private fun getAttackerEffect(condition: Condition): RollModifier? {
        return when (condition) {
            is Condition.Prone -> RollModifier.Disadvantage
            is Condition.Poisoned -> RollModifier.Disadvantage
            is Condition.Blinded -> RollModifier.Disadvantage
            is Condition.Restrained -> RollModifier.Disadvantage
            is Condition.Stunned,
            is Condition.Paralyzed,
            is Condition.Unconscious,
            is Condition.Incapacitated -> null
        }
    }
    
    /**
     * Gets the effect of a condition on attacks against the defender.
     */
    private fun getDefenderEffect(condition: Condition): RollModifier? {
        return when (condition) {
            is Condition.Prone -> RollModifier.Advantage // Melee attacks against prone have advantage
            is Condition.Blinded -> RollModifier.Advantage // Attacks against blinded have advantage
            is Condition.Restrained -> RollModifier.Advantage // Attacks against restrained have advantage
            is Condition.Stunned -> RollModifier.Advantage // Attacks against stunned have advantage
            is Condition.Paralyzed -> RollModifier.Advantage // Attacks against paralyzed have advantage
            is Condition.Unconscious -> RollModifier.Advantage // Attacks against unconscious have advantage
            is Condition.Poisoned,
            is Condition.Incapacitated -> null
        }
    }
    
    /**
     * Gets the effect of a condition on saving throws.
     *
     * @param condition The condition to check
     * @param abilityType The ability being saved (STR, DEX, etc.)
     * @return SaveEffect to apply, or null if the condition has no effect
     */
    fun getSavingThrowEffect(condition: Condition, abilityType: AbilityType): SaveEffect? {
        return when (condition) {
            is Condition.Stunned -> {
                // Stunned auto-fails STR and DEX saves
                when (abilityType) {
                    AbilityType.Strength, AbilityType.Dexterity -> SaveEffect.AutoFail
                    else -> null
                }
            }
            is Condition.Paralyzed -> {
                // Paralyzed auto-fails STR and DEX saves
                when (abilityType) {
                    AbilityType.Strength, AbilityType.Dexterity -> SaveEffect.AutoFail
                    else -> null
                }
            }
            is Condition.Unconscious -> {
                // Unconscious auto-fails STR and DEX saves
                when (abilityType) {
                    AbilityType.Strength, AbilityType.Dexterity -> SaveEffect.AutoFail
                    else -> null
                }
            }
            is Condition.Restrained -> {
                // Restrained has disadvantage on DEX saves
                when (abilityType) {
                    AbilityType.Dexterity -> SaveEffect.Disadvantage
                    else -> null
                }
            }
            is Condition.Prone,
            is Condition.Poisoned,
            is Condition.Blinded,
            is Condition.Incapacitated -> null // No effect on saving throws
        }
    }
    
    /**
     * Gets the effect of a condition on ability checks.
     *
     * @param condition The condition to check
     * @return RollModifier to apply, or null if the condition has no effect
     */
    fun getAbilityCheckEffect(condition: Condition): RollModifier? {
        return when (condition) {
            is Condition.Poisoned -> RollModifier.Disadvantage
            is Condition.Prone,
            is Condition.Stunned,
            is Condition.Blinded,
            is Condition.Restrained,
            is Condition.Incapacitated,
            is Condition.Paralyzed,
            is Condition.Unconscious -> null // No direct effect on ability checks
        }
    }
    
    /**
     * Checks if a condition prevents a creature from taking actions.
     *
     * @param condition The condition to check
     * @return True if the creature cannot take actions or reactions
     */
    fun preventsActions(condition: Condition): Boolean {
        return when (condition) {
            is Condition.Stunned,
            is Condition.Incapacitated,
            is Condition.Paralyzed,
            is Condition.Unconscious -> true
            is Condition.Prone,
            is Condition.Poisoned,
            is Condition.Blinded,
            is Condition.Restrained -> false
        }
    }
}
