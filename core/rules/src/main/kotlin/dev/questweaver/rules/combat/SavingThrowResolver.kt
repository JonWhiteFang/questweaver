package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.dice.RollType
import dev.questweaver.rules.conditions.Condition
import dev.questweaver.rules.conditions.ConditionRegistry
import dev.questweaver.rules.modifiers.AbilityType
import dev.questweaver.rules.modifiers.RollModifier
import dev.questweaver.rules.modifiers.SaveEffect
import dev.questweaver.rules.outcomes.SavingThrowOutcome

/**
 * Resolves saving throws against a DC, handling advantage/disadvantage and proficiency.
 *
 * This class is responsible for:
 * - Rolling d20 (or 2d20 for advantage/disadvantage)
 * - Applying ability modifiers and proficiency bonuses
 * - Handling automatic success (natural 20)
 * - Handling automatic failure from conditions (e.g., Stunned)
 * - Applying condition effects (disadvantage, auto-fail)
 *
 * All calculations are deterministic based on the seeded DiceRoller.
 *
 * ## Usage Examples
 *
 * ### Basic Saving Throw
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 * val resolver = SavingThrowResolver(roller)
 *
 * val outcome = resolver.resolveSavingThrow(
 *     abilityModifier = 2,
 *     proficiencyBonus = 3,
 *     dc = 15,
 *     rollModifier = RollModifier.Normal,
 *     isProficient = true,
 *     abilityType = AbilityType.Dexterity,
 *     conditions = emptySet()
 * )
 * // outcome.d20Roll = 14
 * // outcome.totalRoll = 19 (14 + 2 + 3)
 * // outcome.success = true (19 >= 15)
 * ```
 *
 * ### With Advantage
 * ```kotlin
 * val outcome = resolver.resolveSavingThrow(
 *     abilityModifier = 1,
 *     proficiencyBonus = 2,
 *     dc = 12,
 *     rollModifier = RollModifier.Advantage,
 *     isProficient = true,
 *     abilityType = AbilityType.Wisdom,
 *     conditions = emptySet()
 * )
 * // Rolls 2d20, takes higher
 * ```
 *
 * ### Stunned Auto-Fail
 * ```kotlin
 * val outcome = resolver.resolveSavingThrow(
 *     abilityModifier = 3,
 *     proficiencyBonus = 2,
 *     dc = 10,
 *     rollModifier = RollModifier.Normal,
 *     isProficient = false,
 *     abilityType = AbilityType.Dexterity,
 *     conditions = setOf(Condition.Stunned)
 * )
 * // outcome.success = false (auto-fail from Stunned)
 * ```
 *
 * @param diceRoller The seeded dice roller for deterministic rolls
 */
class SavingThrowResolver(private val diceRoller: DiceRoller) {

    /**
     * Resolves a saving throw.
     *
     * @param abilityModifier The creature's ability modifier for this save
     * @param proficiencyBonus The creature's proficiency bonus (if proficient)
     * @param dc The Difficulty Class to meet or exceed
     * @param rollModifier Advantage, disadvantage, or normal roll
     * @param isProficient Whether the creature is proficient in this save
     * @param abilityType The ability type being saved (for condition effects)
     * @param conditions Active conditions on the creature
     * @return SavingThrowOutcome with roll details and success status
     */
    @Suppress("LongParameterList") // Resolver functions need all parameters for complete resolution
    fun resolveSavingThrow(
        abilityModifier: Int,
        proficiencyBonus: Int,
        dc: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        isProficient: Boolean = false,
        abilityType: AbilityType,
        conditions: Set<Condition> = emptySet()
    ): SavingThrowOutcome {
        // Check for auto-fail conditions first
        val saveEffects = conditions.mapNotNull { condition ->
            ConditionRegistry.getSavingThrowEffect(condition, abilityType)
        }
        
        val hasAutoFail = saveEffects.any { it is SaveEffect.AutoFail }
        
        // Determine effective roll modifier
        val effectiveRollModifier = determineEffectiveRollModifier(rollModifier, saveEffects)
        
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
        
        // Check for natural 20 (automatic success, overrides auto-fail)
        @Suppress("MagicNumber") // 20 is the natural 20 constant in D&D
        val isNatural20 = d20Roll == 20
        
        // Calculate total roll
        val appliedProficiency = if (isProficient) proficiencyBonus else 0
        val totalRoll = d20Roll + abilityModifier + appliedProficiency
        
        // Determine success
        val success = when {
            isNatural20 -> true // Natural 20 always succeeds
            hasAutoFail -> false // Auto-fail from conditions
            else -> totalRoll >= dc // Normal comparison
        }
        
        return SavingThrowOutcome(
            d20Roll = d20Roll,
            abilityModifier = abilityModifier,
            proficiencyBonus = appliedProficiency,
            totalRoll = totalRoll,
            dc = dc,
            success = success,
            isAutoSuccess = isNatural20,
            rollModifier = effectiveRollModifier,
            appliedConditions = conditions
        )
    }

    /**
     * Determines the effective roll modifier by combining base modifier with condition effects.
     *
     * Rules:
     * - If any condition causes disadvantage, apply disadvantage
     * - Otherwise, use the base roll modifier
     * - Multiple sources of advantage/disadvantage don't stack (one of each cancels out)
     *
     * @param baseModifier The base roll modifier (from spell, feature, etc.)
     * @param saveEffects The save effects from conditions
     * @return The effective roll modifier to use
     */
    private fun determineEffectiveRollModifier(
        baseModifier: RollModifier,
        saveEffects: List<SaveEffect>
    ): RollModifier {
        val hasDisadvantage = saveEffects.any { it is SaveEffect.Disadvantage }
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
