package dev.questweaver.rules.outcomes

import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.modifiers.RollModifier

/**
 * Represents the outcome of an attack roll resolution.
 *
 * Contains all details about the attack roll, including the natural d20 roll,
 * modifiers applied, whether the attack hit, and any special circumstances
 * like critical hits or automatic misses.
 *
 * @property d20Roll The natural d20 roll (1-20) before any modifiers
 * @property attackBonus The attack bonus applied to the roll
 * @property totalRoll The final roll result (d20 + attackBonus)
 * @property targetAC The target's Armor Class
 * @property hit Whether the attack successfully hit the target
 * @property isCritical Whether this was a critical hit (natural 20)
 * @property isAutoMiss Whether this was an automatic miss (natural 1)
 * @property rollModifier The roll modifier applied (advantage/disadvantage/normal)
 * @property appliedConditions Conditions that affected the attack roll
 */
data class AttackOutcome(
    val d20Roll: Int,
    val attackBonus: Int,
    val totalRoll: Int,
    val targetAC: Int,
    val hit: Boolean,
    val isCritical: Boolean,
    val isAutoMiss: Boolean,
    val rollModifier: RollModifier,
    val appliedConditions: Set<Condition>
)
