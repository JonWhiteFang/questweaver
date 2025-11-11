package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import dev.questweaver.rules.modifiers.ProficiencyLevel
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.outcomes.AbilityCheckOutcome

/**
 * Resolves ability checks against a DC, handling advantage/disadvantage, proficiency, and expertise.
 *
 * This class is responsible for:
 * - Rolling d20 (or 2d20 for advantage/disadvantage)
 * - Applying ability modifiers
 * - Applying proficiency bonuses (1x for proficient, 2x for expertise)
 * - Applying condition effects (disadvantage from Poisoned)
 *
 * All calculations are deterministic based on the seeded DiceRoller.
 *
 * ## Usage Examples
 *
 * ### Basic Ability Check
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 * val resolver = AbilityCheckResolver(roller)
 *
 * val outcome = resolver.resolveAbilityCheck(
 *     abilityModifier = 3,
 *     proficiencyBonus = 2,
 *     dc = 15,
 *     rollModifier = RollModifier.Normal,
 *     proficiencyLevel = ProficiencyLevel.None,
 *     conditions = emptySet()
 * )
 * // outcome.d20Roll = 14
 * // outcome.totalRoll = 17 (14 + 3 + 0)
 * // outcome.success = true (17 >= 15)
 * ```
 *
 * ### With Proficiency
 * ```kotlin
 * val outcome = resolver.resolveAbilityCheck(
 *     abilityModifier = 2,
 *     proficiencyBonus = 3,
 *     dc = 12,
 *     rollModifier = RollModifier.Normal,
 *     proficiencyLevel = ProficiencyLevel.Proficient,
 *     conditions = emptySet()
 * )
 * // Adds +3 proficiency bonus (1x)
 * ```
 *
 * ### With Expertise
 * ```kotlin
 * val outcome = resolver.resolveAbilityCheck(
 *     abilityModifier = 4,
 *     proficiencyBonus = 3,
 *     dc = 18,
 *     rollModifier = RollModifier.Normal,
 *     proficiencyLevel = ProficiencyLevel.Expertise,
 *     conditions = emptySet()
 * )
 * // Adds +6 proficiency bonus (2x)
 * ```
 *
 * ### Poisoned Disadvantage
 * ```kotlin
 * val outcome = resolver.resolveAbilityCheck(
 *     abilityModifier = 2,
 *     proficiencyBonus = 2,
 *     dc = 10,
 *     rollModifier = RollModifier.Normal,
 *     proficiencyLevel = ProficiencyLevel.Proficient,
 *     conditions = setOf(Condition.Poisoned)
 * )
 * // Rolls with disadvantage due to Poisoned condition
 * ```
 *
 * @param diceRoller The seeded dice roller for deterministic rolls
 */
class AbilityCheckResolver(private val diceRoller: DiceRoller) {

    /**
     * Resolves an ability check.
     *
     * @param abilityModifier The creature's ability modifier
     * @param proficiencyBonus The creature's proficiency bonus
     * @param dc The Difficulty Class to meet or exceed
     * @param rollModifier Advantage, disadvantage, or normal roll
     * @param proficiencyLevel None, proficient, or expertise
     * @param conditions Active conditions on the creature
     * @return AbilityCheckOutcome with roll details and success status
     */
    @Suppress("LongParameterList") // Resolver functions need all parameters for complete resolution
    fun resolveAbilityCheck(
        abilityModifier: Int,
        proficiencyBonus: Int,
        dc: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        proficiencyLevel: ProficiencyLevel = ProficiencyLevel.None,
        conditions: Set<Condition> = emptySet()
    ): AbilityCheckOutcome {
        // Determine effective roll modifier from conditions
        val effectiveRollModifier = determineEffectiveRollModifier(rollModifier, conditions)
        
        // Roll d20 (or 2d20 for advantage/disadvantage)
        val diceRoll = when (effectiveRollModifier) {
            RollModifier.Advantage -> diceRoller.rollWithAdvantage()
            RollModifier.Disadvantage -> diceRoller.rollWithDisadvantage()
            RollModifier.Normal -> diceRoller.d20()
        }
        
        val d20Roll = when (effectiveRollModifier) {
            RollModifier.Advantage, RollModifier.Disadvantage -> diceRoll.selectedValue
            RollModifier.Normal -> diceRoll.rolls.first()
        }
        
        // Calculate proficiency multiplier
        val proficiencyMultiplier = when (proficiencyLevel) {
            ProficiencyLevel.None -> 0
            ProficiencyLevel.Proficient -> 1
            ProficiencyLevel.Expertise -> 2
        }
        
        val appliedProficiency = proficiencyBonus * proficiencyMultiplier
        
        // Calculate total roll
        val totalRoll = d20Roll + abilityModifier + appliedProficiency
        
        // Determine success
        val success = totalRoll >= dc
        
        return AbilityCheckOutcome(
            d20Roll = d20Roll,
            abilityModifier = abilityModifier,
            proficiencyBonus = appliedProficiency,
            totalRoll = totalRoll,
            dc = dc,
            success = success,
            rollModifier = effectiveRollModifier,
            proficiencyLevel = proficiencyLevel,
            appliedConditions = conditions
        )
    }

    /**
     * Determines the effective roll modifier by combining base modifier with condition effects.
     *
     * Rules:
     * - Poisoned condition causes disadvantage on ability checks
     * - Multiple sources of advantage/disadvantage don't stack (one of each cancels out)
     *
     * @param baseModifier The base roll modifier (from spell, feature, etc.)
     * @param conditions The active conditions on the creature
     * @return The effective roll modifier to use
     */
    private fun determineEffectiveRollModifier(
        baseModifier: RollModifier,
        conditions: Set<Condition>
    ): RollModifier {
        // Check for condition effects
        val conditionEffects = conditions.mapNotNull { condition ->
            ConditionRegistry.getAbilityCheckEffect(condition)
        }
        
        val hasDisadvantage = conditionEffects.any { it == RollModifier.Disadvantage }
        val baseHasAdvantage = baseModifier == RollModifier.Advantage
        val baseHasDisadvantage = baseModifier == RollModifier.Disadvantage
        
        // Combine sources of advantage/disadvantage
        val totalAdvantage = baseHasAdvantage
        val totalDisadvantage = baseHasDisadvantage || hasDisadvantage
        
        // If both advantage and disadvantage, they cancel out
        return when {
            totalAdvantage && totalDisadvantage -> RollModifier.Normal
            totalAdvantage -> RollModifier.Advantage
            totalDisadvantage -> RollModifier.Disadvantage
            else -> RollModifier.Normal
        }
    }
}
