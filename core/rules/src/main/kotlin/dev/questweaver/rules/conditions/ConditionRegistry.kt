package dev.questweaver.rules.conditions

import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.modifiers.SaveEffect

/**
 * Registry for condition effects and their impact on actions and rolls.
 *
 * This object provides a centralized lookup for how conditions affect
 * various game mechanics. It's implemented as an object singleton since
 * condition effects are static and never change.
 */
object ConditionRegistry {
    /**
     * Checks if a condition prevents all actions.
     *
     * @param condition The condition to check
     * @return True if the creature cannot take actions
     */
    fun preventsActions(condition: Condition): Boolean {
        return when (condition) {
            Condition.Stunned -> true
            Condition.Incapacitated -> true
            Condition.Paralyzed -> true
            Condition.Unconscious -> true
            Condition.Prone -> false
            Condition.Poisoned -> false
            Condition.Blinded -> false
            Condition.Restrained -> false
        }
    }

    /**
     * Checks if a condition prevents reactions.
     *
     * @param condition The condition to check
     * @return True if the creature cannot take reactions
     */
    fun preventsReactions(condition: Condition): Boolean {
        return when (condition) {
            Condition.Stunned -> true
            Condition.Incapacitated -> true
            Condition.Paralyzed -> true
            Condition.Unconscious -> true
            Condition.Prone -> false
            Condition.Poisoned -> false
            Condition.Blinded -> false
            Condition.Restrained -> false
        }
    }

    /**
     * Checks if a condition prevents movement.
     *
     * @param condition The condition to check
     * @return True if the creature cannot move
     */
    fun preventsMovement(condition: Condition): Boolean {
        return when (condition) {
            Condition.Stunned -> true
            Condition.Paralyzed -> true
            Condition.Unconscious -> true
            Condition.Restrained -> true  // Speed becomes 0
            Condition.Incapacitated -> false
            Condition.Prone -> false  // Can crawl
            Condition.Poisoned -> false
            Condition.Blinded -> false
        }
    }

    /**
     * Gets a human-readable explanation of why a condition prevents an action.
     *
     * @param condition The condition that prevents the action
     * @param actionType The type of action being prevented
     * @return A human-readable explanation
     */
    fun getPreventionReason(condition: Condition, actionType: String): String {
        return when (condition) {
            Condition.Stunned -> "Stunned creatures cannot take $actionType"
            Condition.Incapacitated -> "Incapacitated creatures cannot take $actionType"
            Condition.Paralyzed -> "Paralyzed creatures cannot take $actionType"
            Condition.Unconscious -> "Unconscious creatures cannot take $actionType"
            Condition.Restrained -> "Restrained creatures cannot move"
            Condition.Prone -> "Prone creatures must crawl or stand up"
            Condition.Poisoned -> "Poisoned creatures have disadvantage on $actionType"
            Condition.Blinded -> "Blinded creatures have disadvantage on $actionType"
        }
    }

    /**
     * Checks if any condition in a set prevents actions.
     *
     * @param conditions The set of active conditions
     * @return The first blocking condition, if any
     */
    fun getBlockingCondition(conditions: Set<Condition>): Condition? {
        return conditions.firstOrNull { preventsActions(it) }
    }

    /**
     * Checks if any condition in a set prevents reactions.
     *
     * @param conditions The set of active conditions
     * @return The first blocking condition, if any
     */
    fun getBlockingReactionCondition(conditions: Set<Condition>): Condition? {
        return conditions.firstOrNull { preventsReactions(it) }
    }

    /**
     * Checks if any condition in a set prevents movement.
     *
     * @param conditions The set of active conditions
     * @return The first blocking condition, if any
     */
    fun getBlockingMovementCondition(conditions: Set<Condition>): Condition? {
        return conditions.firstOrNull { preventsMovement(it) }
    }

    /**
     * Gets the effect of a condition on ability checks.
     *
     * @param condition The condition to check
     * @return The roll modifier effect, or null if no effect
     */
    fun getAbilityCheckEffect(condition: Condition): RollModifier? {
        return when (condition) {
            Condition.Poisoned -> RollModifier.Disadvantage
            Condition.Stunned,
            Condition.Incapacitated,
            Condition.Paralyzed,
            Condition.Unconscious,
            Condition.Prone,
            Condition.Blinded,
            Condition.Restrained -> null
        }
    }

    /**
     * Gets the effect of a condition on attack rolls.
     *
     * @param condition The condition to check
     * @param isAttacker True if checking the attacker's conditions, false for target's conditions
     * @return The roll modifier effect, or null if no effect
     */
    fun getAttackRollEffect(condition: Condition, isAttacker: Boolean): RollModifier? {
        return when (condition) {
            Condition.Poisoned -> if (isAttacker) RollModifier.Disadvantage else null
            Condition.Blinded -> if (isAttacker) RollModifier.Disadvantage else null
            Condition.Prone -> if (isAttacker) null else RollModifier.Advantage
            Condition.Restrained -> if (isAttacker) RollModifier.Disadvantage else RollModifier.Advantage
            Condition.Stunned,
            Condition.Incapacitated,
            Condition.Paralyzed,
            Condition.Unconscious -> null
        }
    }

    /**
     * Gets the effect of a condition on saving throws.
     *
     * @param condition The condition to check
     * @param abilityType The ability type of the saving throw
     * @return The save effect, or null if no effect
     */
    fun getSavingThrowEffect(condition: Condition, abilityType: AbilityType): SaveEffect? {
        return when (condition) {
            Condition.Stunned -> when (abilityType) {
                AbilityType.Strength, AbilityType.Dexterity -> SaveEffect.AutoFail
                else -> null
            }
            Condition.Paralyzed -> when (abilityType) {
                AbilityType.Strength, AbilityType.Dexterity -> SaveEffect.AutoFail
                else -> null
            }
            Condition.Restrained -> when (abilityType) {
                AbilityType.Dexterity -> SaveEffect.Disadvantage
                else -> null
            }
            Condition.Incapacitated,
            Condition.Unconscious,
            Condition.Prone,
            Condition.Poisoned,
            Condition.Blinded -> null
        }
    }
}
