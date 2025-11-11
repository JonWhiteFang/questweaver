package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.outcomes.AttackOutcome

/**
 * Resolves attack rolls against targets according to D&D 5e SRD rules.
 *
 * Handles advantage/disadvantage, critical hits (natural 20), automatic misses
 * (natural 1), and condition effects on attack rolls. All resolution is deterministic
 * based on the seeded DiceRoller.
 *
 * ## Usage Examples
 *
 * ### Basic Attack
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 * val resolver = AttackResolver(roller)
 *
 * val outcome = resolver.resolveAttack(
 *     attackBonus = 5,
 *     targetAC = 15
 * )
 * println("Hit: ${outcome.hit}, Roll: ${outcome.d20Roll}, Total: ${outcome.totalRoll}")
 * ```
 *
 * ### Attack with Advantage
 * ```kotlin
 * val outcome = resolver.resolveAttack(
 *     attackBonus = 5,
 *     targetAC = 15,
 *     rollModifier = RollModifier.Advantage
 * )
 * ```
 *
 * ### Attack with Conditions
 * ```kotlin
 * val outcome = resolver.resolveAttack(
 *     attackBonus = 5,
 *     targetAC = 15,
 *     attackerConditions = setOf(Condition.Poisoned), // Disadvantage on attacks
 *     targetConditions = setOf(Condition.Prone)       // Advantage for melee attacks
 * )
 * ```
 *
 * @param diceRoller The seeded dice roller for deterministic results
 */
class AttackResolver(private val diceRoller: DiceRoller) {
    
    companion object {
        private const val NATURAL_20 = 20
        private const val NATURAL_1 = 1
    }

    /**
     * Resolves an attack roll against a target.
     *
     * Algorithm:
     * 1. Determine effective roll modifier by combining base modifier with condition effects
     * 2. Roll d20 (or 2d20 for advantage/disadvantage) using DiceRoller
     * 3. Check for natural 20 (critical hit) or natural 1 (automatic miss)
     * 4. Calculate total: d20 result + attack bonus
     * 5. Compare total to target AC
     * 6. Return AttackOutcome with all details
     *
     * @param attackBonus The attacker's attack bonus (ability modifier + proficiency)
     * @param targetAC The target's Armor Class
     * @param rollModifier Base roll modifier (advantage, disadvantage, or normal)
     * @param attackerConditions Active conditions on the attacker
     * @param targetConditions Active conditions on the target
     * @return AttackOutcome with roll details and hit status
     */
    fun resolveAttack(
        attackBonus: Int,
        targetAC: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        attackerConditions: Set<Condition> = emptySet(),
        targetConditions: Set<Condition> = emptySet()
    ): AttackOutcome {
        // Determine effective roll modifier from conditions
        val effectiveModifier = determineEffectiveRollModifier(
            baseModifier = rollModifier,
            attackerConditions = attackerConditions,
            targetConditions = targetConditions
        )
        
        // Roll d20 based on effective modifier
        val diceRoll = when (effectiveModifier) {
            RollModifier.Advantage -> diceRoller.rollWithAdvantage()
            RollModifier.Disadvantage -> diceRoller.rollWithDisadvantage()
            RollModifier.Normal -> diceRoller.d20()
        }
        
        // Get the natural d20 roll (for advantage/disadvantage, this is the selected value)
        val d20Roll = diceRoll.selectedValue
        
        // Check for critical hit (natural 20) or automatic miss (natural 1)
        val isCritical = d20Roll == NATURAL_20
        val isAutoMiss = d20Roll == NATURAL_1
        
        // Calculate total roll
        val totalRoll = d20Roll + attackBonus
        
        // Determine if attack hits
        val hit = when {
            isAutoMiss -> false  // Natural 1 always misses
            isCritical -> true   // Natural 20 always hits
            else -> totalRoll >= targetAC
        }
        
        // Collect all conditions that affected the roll
        val appliedConditions = (attackerConditions + targetConditions).toSet()
        
        return AttackOutcome(
            d20Roll = d20Roll,
            attackBonus = attackBonus,
            totalRoll = totalRoll,
            targetAC = targetAC,
            hit = hit,
            isCritical = isCritical,
            isAutoMiss = isAutoMiss,
            rollModifier = effectiveModifier,
            appliedConditions = appliedConditions
        )
    }
    
    /**
     * Determines the effective roll modifier by combining base modifier with condition effects.
     *
     * Rules for combining advantage/disadvantage:
     * - If any source grants advantage and any source grants disadvantage, they cancel out (normal roll)
     * - Multiple sources of advantage don't stack (still just advantage)
     * - Multiple sources of disadvantage don't stack (still just disadvantage)
     *
     * @param baseModifier The base roll modifier
     * @param attackerConditions Conditions on the attacker
     * @param targetConditions Conditions on the target
     * @return The effective roll modifier after considering all conditions
     */
    private fun determineEffectiveRollModifier(
        baseModifier: RollModifier,
        attackerConditions: Set<Condition>,
        targetConditions: Set<Condition>
    ): RollModifier {
        var hasAdvantage = baseModifier == RollModifier.Advantage
        var hasDisadvantage = baseModifier == RollModifier.Disadvantage
        
        // Check attacker conditions
        for (condition in attackerConditions) {
            when (ConditionRegistry.getAttackRollEffect(condition, isAttacker = true)) {
                RollModifier.Advantage -> hasAdvantage = true
                RollModifier.Disadvantage -> hasDisadvantage = true
                RollModifier.Normal, null -> {} // No effect
            }
        }
        
        // Check target conditions (affects attacks against the target)
        for (condition in targetConditions) {
            when (ConditionRegistry.getAttackRollEffect(condition, isAttacker = false)) {
                RollModifier.Advantage -> hasAdvantage = true
                RollModifier.Disadvantage -> hasDisadvantage = true
                RollModifier.Normal, null -> {} // No effect
            }
        }
        
        // Advantage and disadvantage cancel each other out
        return when {
            hasAdvantage && hasDisadvantage -> RollModifier.Normal
            hasAdvantage -> RollModifier.Advantage
            hasDisadvantage -> RollModifier.Disadvantage
            else -> RollModifier.Normal
        }
    }
}
