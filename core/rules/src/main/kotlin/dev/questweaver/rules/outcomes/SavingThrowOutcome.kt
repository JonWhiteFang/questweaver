package dev.questweaver.rules.outcomes

import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.modifiers.RollModifier

/**
 * Represents the outcome of a saving throw resolution.
 *
 * Contains all details about the saving throw, including the natural d20 roll,
 * modifiers applied, whether the save succeeded, and any special circumstances
 * like automatic success or failure.
 *
 * @property d20Roll The natural d20 roll (1-20) before any modifiers
 * @property abilityModifier The ability modifier applied to the roll
 * @property proficiencyBonus The proficiency bonus applied (if proficient)
 * @property totalRoll The final roll result (d20 + abilityModifier + proficiencyBonus)
 * @property dc The Difficulty Class that must be met or exceeded
 * @property success Whether the saving throw succeeded
 * @property isAutoSuccess Whether this was an automatic success (natural 20)
 * @property rollModifier The roll modifier applied (advantage/disadvantage/normal)
 * @property appliedConditions Conditions that affected the saving throw
 */
data class SavingThrowOutcome(
    val d20Roll: Int,
    val abilityModifier: Int,
    val proficiencyBonus: Int,
    val totalRoll: Int,
    val dc: Int,
    val success: Boolean,
    val isAutoSuccess: Boolean,
    val rollModifier: RollModifier,
    val appliedConditions: Set<Condition>
)
