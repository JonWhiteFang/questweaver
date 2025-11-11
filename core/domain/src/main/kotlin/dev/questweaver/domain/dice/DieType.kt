package dev.questweaver.domain.dice

import kotlin.random.Random

/**
 * Standard D&D dice types.
 *
 * Represents the common polyhedral dice used in D&D 5e SRD mechanics.
 * Each die type knows its number of sides and can roll itself using a
 * provided random number generator.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 *
 * // Use die types directly with roll()
 * val d6Roll = roller.roll(die = DieType.D6, modifier = 2)
 * val multiRoll = roller.roll(count = 3, die = DieType.D8)
 *
 * // Or use convenience methods
 * val d20Roll = roller.d20(modifier = 5)
 * ```
 *
 * ## Die Types and Common Uses
 *
 * - **D4**: Small weapons (dagger), healing spells
 * - **D6**: Medium weapons (shortsword), ability scores
 * - **D8**: Martial weapons (longsword), hit dice
 * - **D10**: Heavy weapons (pike), cantrips
 * - **D12**: Great weapons (greataxe), barbarian hit dice
 * - **D20**: Attack rolls, ability checks, saving throws
 * - **D100**: Percentile rolls, random tables
 */
@Suppress("MagicNumber") // Die sides are domain constants
enum class DieType(val sides: Int) {
    /** 4-sided die. Range: 1-4. Common for daggers and small weapons. */
    D4(4),

    /** 6-sided die. Range: 1-6. Common for shortswords and ability score generation. */
    D6(6),

    /** 8-sided die. Range: 1-8. Common for longswords and many class hit dice. */
    D8(8),

    /** 10-sided die. Range: 1-10. Common for pikes and cantrip damage scaling. */
    D10(10),

    /** 12-sided die. Range: 1-12. Common for greataxes and barbarian hit dice. */
    D12(12),

    /** 20-sided die. Range: 1-20. Used for all attack rolls, ability checks, and saving throws. */
    D20(20),

    /** 100-sided die (percentile). Range: 1-100. Used for random tables and percentage checks. */
    D100(100);

    /**
     * Roll this die type using the provided random generator.
     *
     * @param random The seeded random number generator to use
     * @return A value between 1 and [sides] inclusive
     */
    internal fun roll(random: Random): Int = random.nextInt(1, sides + 1)
}
