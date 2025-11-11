package dev.questweaver.rules.outcomes

import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.modifiers.ProficiencyLevel
import dev.questweaver.rules.modifiers.RollModifier

/**
 * Represents the outcome of an ability check resolution.
 *
 * Contains all details about the ability check, including the natural d20 roll,
 * modifiers applied, proficiency level, and whether the check succeeded.
 *
 * @property d20Roll The natural d20 roll (1-20) before any modifiers
 * @property abilityModifier The ability modifier applied to the roll
 * @property proficiencyBonus The proficiency bonus applied (multiplied by proficiency level)
 * @property totalRoll The final roll result (d20 + abilityModifier + proficiency)
 * @property dc The Difficulty Class that must be met or exceeded
 * @property success Whether the ability check succeeded
 * @property rollModifier The roll modifier applied (advantage/disadvantage/normal)
 * @property proficiencyLevel The proficiency level applied (none/proficient/expertise)
 * @property appliedConditions Conditions that affected the ability check
 */
data class AbilityCheckOutcome(
    val d20Roll: Int,
    val abilityModifier: Int,
    val proficiencyBonus: Int,
    val totalRoll: Int,
    val dc: Int,
    val success: Boolean,
    val rollModifier: RollModifier,
    val proficiencyLevel: ProficiencyLevel,
    val appliedConditions: Set<Condition>
)
