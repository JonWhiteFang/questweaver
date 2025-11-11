package dev.questweaver.rules.outcomes

import dev.questweaver.rules.modifiers.DamageModifier
import dev.questweaver.rules.modifiers.DamageType

/**
 * Represents the outcome of a damage calculation.
 *
 * Contains all details about damage dealt, including individual die rolls,
 * modifiers applied, damage type, and the final damage after resistances,
 * vulnerabilities, and immunities.
 *
 * @property diceRolls The individual die results from the damage roll
 * @property diceTotal The sum of all dice rolled
 * @property damageModifier The flat modifier added to the damage
 * @property baseDamage The total damage before resistances (diceTotal + damageModifier)
 * @property damageType The type of damage dealt
 * @property isCritical Whether this damage was from a critical hit (dice doubled)
 * @property appliedModifiers The damage modifiers applied (resistances/vulnerabilities/immunities)
 * @property finalDamage The final damage after all modifiers
 */
data class DamageOutcome(
    val diceRolls: List<Int>,
    val diceTotal: Int,
    val damageModifier: Int,
    val baseDamage: Int,
    val damageType: DamageType,
    val isCritical: Boolean,
    val appliedModifiers: Set<DamageModifier>,
    val finalDamage: Int
)
