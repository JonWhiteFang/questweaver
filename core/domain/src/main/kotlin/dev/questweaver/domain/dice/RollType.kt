package dev.questweaver.domain.dice

import kotlinx.serialization.Serializable

/**
 * Type of dice roll (normal, advantage, or disadvantage).
 *
 * Used to distinguish between standard rolls and D&D 5e advantage/disadvantage
 * mechanics where two d20s are rolled and the higher or lower value is selected.
 *
 * ## Usage Examples
 *
 * ### Normal Rolls
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 * val roll = roller.d20(modifier = 3)
 * assert(roll.rollType == RollType.NORMAL)
 * println("Result: ${roll.total}") // e.g., 17 (14 + 3)
 * ```
 *
 * ### Advantage
 * ```kotlin
 * // When the player has advantage (e.g., attacking prone enemy)
 * val advRoll = roller.rollWithAdvantage(modifier = 5)
 * assert(advRoll.rollType == RollType.ADVANTAGE)
 * println("Rolls: ${advRoll.rolls}")        // e.g., [8, 15]
 * println("Selected: ${advRoll.selectedValue}") // 15 (higher)
 * println("Result: ${advRoll.result}")      // 20 (15 + 5)
 * ```
 *
 * ### Disadvantage
 * ```kotlin
 * // When the player has disadvantage (e.g., attacking while prone)
 * val disRoll = roller.rollWithDisadvantage(modifier = 5)
 * assert(disRoll.rollType == RollType.DISADVANTAGE)
 * println("Rolls: ${disRoll.rolls}")        // e.g., [12, 7]
 * println("Selected: ${disRoll.selectedValue}") // 7 (lower)
 * println("Result: ${disRoll.result}")      // 12 (7 + 5)
 * ```
 *
 * ### Checking Roll Type
 * ```kotlin
 * fun displayRoll(roll: DiceRoll) {
 *     when (roll.rollType) {
 *         RollType.NORMAL -> println("Rolled ${roll.total}")
 *         RollType.ADVANTAGE -> println("Advantage: ${roll.rolls} → ${roll.result}")
 *         RollType.DISADVANTAGE -> println("Disadvantage: ${roll.rolls} → ${roll.result}")
 *     }
 * }
 * ```
 */
@Serializable
enum class RollType {
    /**
     * Standard roll with no special mechanics.
     *
     * Used for most dice rolls including damage, ability checks without
     * advantage/disadvantage, and multiple dice rolls (e.g., 2d6).
     */
    NORMAL,

    /**
     * Roll two d20s and take the higher result.
     *
     * Used when the player has advantage on an attack roll, ability check,
     * or saving throw. Common situations include:
     * - Attacking a prone enemy
     * - Attacking an enemy you can't see but who can't see you
     * - Using the Help action
     * - Various class features and spells
     */
    ADVANTAGE,

    /**
     * Roll two d20s and take the lower result.
     *
     * Used when the player has disadvantage on an attack roll, ability check,
     * or saving throw. Common situations include:
     * - Attacking while prone
     * - Attacking a target you can't see
     * - Making a ranged attack while an enemy is within 5 feet
     * - Various conditions and environmental effects
     */
    DISADVANTAGE
}
