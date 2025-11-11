package dev.questweaver.domain.dice

import kotlinx.serialization.Serializable

/**
 * Immutable result of a dice roll operation.
 *
 * Contains all information about the roll including individual die results,
 * modifiers, and roll type (normal/advantage/disadvantage). This value object
 * is serializable for event sourcing persistence.
 *
 * ## Usage Examples
 *
 * ### Accessing Roll Results
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 *
 * // Single die roll
 * val roll = roller.d20(modifier = 5)
 * println("Die type: ${roll.dieType}")           // D20
 * println("Individual rolls: ${roll.rolls}")     // [14]
 * println("Natural total: ${roll.naturalTotal}") // 14
 * println("Modifier: ${roll.modifier}")          // 5
 * println("Final total: ${roll.total}")          // 19
 * ```
 *
 * ### Multiple Dice
 * ```kotlin
 * // Roll 3d6 for ability scores
 * val abilityRoll = roller.roll(count = 3, die = DieType.D6)
 * println("Rolls: ${abilityRoll.rolls}")         // e.g., [4, 5, 3]
 * println("Total: ${abilityRoll.naturalTotal}")  // 12
 * ```
 *
 * ### Advantage/Disadvantage
 * ```kotlin
 * // Advantage: higher of two rolls is selected
 * val advRoll = roller.rollWithAdvantage(modifier = 3)
 * println("Both rolls: ${advRoll.rolls}")        // e.g., [8, 15]
 * println("Selected: ${advRoll.selectedValue}")  // 15 (higher)
 * println("Result: ${advRoll.result}")           // 18 (15 + 3)
 *
 * // Disadvantage: lower of two rolls is selected
 * val disRoll = roller.rollWithDisadvantage(modifier = 3)
 * println("Both rolls: ${disRoll.rolls}")        // e.g., [12, 7]
 * println("Selected: ${disRoll.selectedValue}")  // 7 (lower)
 * println("Result: ${disRoll.result}")           // 10 (7 + 3)
 * ```
 *
 * ### Event Sourcing
 * ```kotlin
 * @Serializable
 * data class AttackResolved(
 *     val attackerId: Long,
 *     val targetId: Long,
 *     val attackRoll: DiceRoll,  // Embedded in events
 *     val damageRoll: DiceRoll?,
 *     val hit: Boolean
 * ) : GameEvent
 * ```
 *
 * @property dieType The type of die that was rolled (D4, D6, D8, D10, D12, D20, D100)
 * @property rolls The individual die results (e.g., [3, 5, 2] for 3d6). For advantage/disadvantage,
 *                 contains both d20 rolls.
 * @property modifier The modifier applied to the roll (e.g., +3 for ability bonus, -1 for penalty).
 *                    Default is 0.
 * @property rollType Whether this was a NORMAL, ADVANTAGE, or DISADVANTAGE roll. Default is NORMAL.
 */
@Serializable
data class DiceRoll(
    val dieType: DieType,
    val rolls: List<Int>,
    val modifier: Int = 0,
    val rollType: RollType = RollType.NORMAL
) {
    /**
     * The sum of all individual die rolls (before modifier).
     *
     * For a single d20 roll of 14, this is 14.
     * For 3d6 rolling [4, 5, 3], this is 12.
     * For advantage rolling [8, 15], this is 23 (sum of both, not the selected value).
     *
     * Use [selectedValue] for advantage/disadvantage to get the chosen roll.
     */
    val naturalTotal: Int = rolls.sum()

    /**
     * The final result including modifier.
     *
     * For normal rolls and multiple dice, this is `naturalTotal + modifier`.
     * For advantage/disadvantage, use [result] instead as it applies the modifier
     * to the selected value, not the sum of both rolls.
     *
     * Example: d20 rolls 14 with +5 modifier → total = 19
     * Example: 2d6 rolls [4, 5] with +3 modifier → total = 12
     */
    val total: Int = naturalTotal + modifier

    /**
     * For advantage/disadvantage, the value that was selected.
     * For normal rolls, same as naturalTotal.
     *
     * - ADVANTAGE: The higher of the two d20 rolls
     * - DISADVANTAGE: The lower of the two d20 rolls
     * - NORMAL: The sum of all dice (same as naturalTotal)
     *
     * Example: Advantage rolls [8, 15] → selectedValue = 15
     * Example: Disadvantage rolls [12, 7] → selectedValue = 7
     * Example: Normal 2d6 rolls [4, 5] → selectedValue = 9
     */
    val selectedValue: Int = when (rollType) {
        RollType.ADVANTAGE -> rolls.maxOrNull() ?: 0
        RollType.DISADVANTAGE -> rolls.minOrNull() ?: 0
        RollType.NORMAL -> naturalTotal
    }

    /**
     * The final result for advantage/disadvantage rolls.
     *
     * - For ADVANTAGE/DISADVANTAGE: `selectedValue + modifier`
     * - For NORMAL: Same as [total]
     *
     * This is the value you should use for advantage/disadvantage mechanics.
     * For normal rolls, use [total] instead for clarity.
     *
     * Example: Advantage rolls [8, 15] with +2 modifier → result = 17 (15 + 2)
     * Example: Disadvantage rolls [12, 7] with +2 modifier → result = 9 (7 + 2)
     */
    val result: Int = when (rollType) {
        RollType.ADVANTAGE, RollType.DISADVANTAGE -> selectedValue + modifier
        RollType.NORMAL -> total
    }

    init {
        require(rolls.isNotEmpty()) { "Dice roll must contain at least one result" }
        require(rolls.all { it in 1..dieType.sides }) {
            "All roll values must be between 1 and ${dieType.sides}"
        }
    }
}
