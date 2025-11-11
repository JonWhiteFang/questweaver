package dev.questweaver.domain.dice

import kotlinx.serialization.Serializable

/**
 * Immutable result of a dice roll operation.
 *
 * Contains all information about the roll including individual die results,
 * modifiers, and roll type (normal/advantage/disadvantage). This value object
 * is serializable for event sourcing persistence.
 *
 * @property dieType The type of die that was rolled
 * @property rolls The individual die results (e.g., [3, 5, 2] for 3d6)
 * @property modifier The modifier applied to the roll (e.g., +3 for ability bonus)
 * @property rollType Whether this was a normal, advantage, or disadvantage roll
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
     */
    val naturalTotal: Int = rolls.sum()

    /**
     * The final result including modifier.
     * For normal rolls and multiple dice, this is naturalTotal + modifier.
     */
    val total: Int = naturalTotal + modifier

    /**
     * For advantage/disadvantage, the value that was selected.
     * For normal rolls, same as naturalTotal.
     */
    val selectedValue: Int = when (rollType) {
        RollType.ADVANTAGE -> rolls.maxOrNull() ?: 0
        RollType.DISADVANTAGE -> rolls.minOrNull() ?: 0
        RollType.NORMAL -> naturalTotal
    }

    /**
     * The final result for advantage/disadvantage rolls.
     * For advantage/disadvantage: selectedValue + modifier
     * For normal rolls: same as total
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
