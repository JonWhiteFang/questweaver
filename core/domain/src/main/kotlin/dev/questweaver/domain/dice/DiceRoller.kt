package dev.questweaver.domain.dice

import kotlin.random.Random

/**
 * Deterministic dice roller using seeded random number generation.
 *
 * All rolls with the same seed will produce identical results, enabling
 * event replay and deterministic game state reconstruction. This is critical
 * for event sourcing where the same sequence of events must always produce
 * the same outcomes.
 *
 * ## Usage Examples
 *
 * ### Basic Rolling
 * ```kotlin
 * // Initialize with session seed for deterministic behavior
 * val roller = DiceRoller(seed = 12345L)
 *
 * // Roll a single d20
 * val attackRoll = roller.d20(modifier = 5)
 * println("Attack: ${attackRoll.total}") // e.g., "Attack: 19" (14 + 5)
 *
 * // Roll multiple dice (e.g., 2d6 for damage)
 * val damageRoll = roller.roll(count = 2, die = DieType.D6, modifier = 3)
 * println("Damage: ${damageRoll.total}") // e.g., "Damage: 12" (4 + 5 + 3)
 * ```
 *
 * ### Advantage and Disadvantage
 * ```kotlin
 * // Roll with advantage (take higher of two d20s)
 * val advantageRoll = roller.rollWithAdvantage(modifier = 2)
 * println("Rolls: ${advantageRoll.rolls}") // e.g., [8, 15]
 * println("Selected: ${advantageRoll.selectedValue}") // 15
 * println("Result: ${advantageRoll.result}") // 17 (15 + 2)
 *
 * // Roll with disadvantage (take lower of two d20s)
 * val disadvantageRoll = roller.rollWithDisadvantage(modifier = 2)
 * println("Rolls: ${disadvantageRoll.rolls}") // e.g., [12, 7]
 * println("Selected: ${disadvantageRoll.selectedValue}") // 7
 * println("Result: ${disadvantageRoll.result}") // 9 (7 + 2)
 * ```
 *
 * ### Deterministic Replay
 * ```kotlin
 * // Same seed produces identical sequences
 * val roller1 = DiceRoller(seed = 42L)
 * val roller2 = DiceRoller(seed = 42L)
 *
 * val roll1 = roller1.d20()
 * val roll2 = roller2.d20()
 *
 * assert(roll1.rolls == roll2.rolls) // Always true
 * ```
 *
 * ### Convenience Methods
 * ```kotlin
 * val roller = DiceRoller(seed = System.currentTimeMillis())
 *
 * // All standard D&D dice types
 * val d4Roll = roller.d4()      // 1-4
 * val d6Roll = roller.d6()      // 1-6
 * val d8Roll = roller.d8()      // 1-8
 * val d10Roll = roller.d10()    // 1-10
 * val d12Roll = roller.d12()    // 1-12
 * val d20Roll = roller.d20()    // 1-20
 * val d100Roll = roller.d100()  // 1-100 (percentile)
 * ```
 *
 * @param seed The seed value for the random number generator. Use the same seed
 *             to reproduce identical roll sequences for event replay.
 */
@Suppress("TooManyFunctions") // Convenience methods for each die type are intentional
class DiceRoller(seed: Long) {
    private val random = Random(seed)

    /**
     * Roll a single die of the specified type.
     *
     * @param die The type of die to roll (d4, d6, d8, d10, d12, d20, d100)
     * @param modifier The modifier to apply to the roll result (default 0)
     * @return A DiceRoll containing the result and metadata
     */
    fun roll(die: DieType, modifier: Int = 0): DiceRoll {
        val result = die.roll(random)
        return DiceRoll(
            dieType = die,
            rolls = listOf(result),
            modifier = modifier,
            rollType = RollType.NORMAL
        )
    }

    /**
     * Roll multiple dice of the same type.
     *
     * @param count The number of dice to roll (must be positive)
     * @param die The type of die to roll
     * @param modifier The modifier to apply to the total result (default 0)
     * @return A DiceRoll containing all individual results and the total
     * @throws IllegalArgumentException if count is less than 1
     */
    fun roll(count: Int, die: DieType, modifier: Int = 0): DiceRoll {
        require(count > 0) { "Dice count must be positive, got $count" }

        val results = List(count) { die.roll(random) }
        return DiceRoll(
            dieType = die,
            rolls = results,
            modifier = modifier,
            rollType = RollType.NORMAL
        )
    }

    /**
     * Roll a d20 with advantage (take higher of two rolls).
     *
     * Rolls two d20s and selects the higher value. This implements the D&D 5e
     * advantage mechanic. Both individual roll values are preserved in the result.
     *
     * @param modifier The modifier to apply to the selected roll (default 0)
     * @return A DiceRoll with both rolls and the higher value selected
     */
    fun rollWithAdvantage(modifier: Int = 0): DiceRoll {
        val roll1 = DieType.D20.roll(random)
        val roll2 = DieType.D20.roll(random)
        return DiceRoll(
            dieType = DieType.D20,
            rolls = listOf(roll1, roll2),
            modifier = modifier,
            rollType = RollType.ADVANTAGE
        )
    }

    /**
     * Roll a d20 with disadvantage (take lower of two rolls).
     *
     * Rolls two d20s and selects the lower value. This implements the D&D 5e
     * disadvantage mechanic. Both individual roll values are preserved in the result.
     *
     * @param modifier The modifier to apply to the selected roll (default 0)
     * @return A DiceRoll with both rolls and the lower value selected
     */
    fun rollWithDisadvantage(modifier: Int = 0): DiceRoll {
        val roll1 = DieType.D20.roll(random)
        val roll2 = DieType.D20.roll(random)
        return DiceRoll(
            dieType = DieType.D20,
            rolls = listOf(roll1, roll2),
            modifier = modifier,
            rollType = RollType.DISADVANTAGE
        )
    }

    // Convenience methods for common dice types

    /**
     * Roll a d4 (4-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d4(modifier: Int = 0): DiceRoll = roll(DieType.D4, modifier)

    /**
     * Roll a d6 (6-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d6(modifier: Int = 0): DiceRoll = roll(DieType.D6, modifier)

    /**
     * Roll a d8 (8-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d8(modifier: Int = 0): DiceRoll = roll(DieType.D8, modifier)

    /**
     * Roll a d10 (10-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d10(modifier: Int = 0): DiceRoll = roll(DieType.D10, modifier)

    /**
     * Roll a d12 (12-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d12(modifier: Int = 0): DiceRoll = roll(DieType.D12, modifier)

    /**
     * Roll a d20 (20-sided die).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d20(modifier: Int = 0): DiceRoll = roll(DieType.D20, modifier)

    /**
     * Roll a d100 (100-sided die, percentile dice).
     *
     * @param modifier The modifier to apply to the roll (default 0)
     * @return A DiceRoll containing the result
     */
    fun d100(modifier: Int = 0): DiceRoll = roll(DieType.D100, modifier)
}
