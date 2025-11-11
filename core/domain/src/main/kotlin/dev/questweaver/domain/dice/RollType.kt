package dev.questweaver.domain.dice

import kotlinx.serialization.Serializable

/**
 * Type of dice roll (normal, advantage, or disadvantage).
 *
 * Used to distinguish between standard rolls and D&D 5e advantage/disadvantage
 * mechanics where two d20s are rolled and the higher or lower value is selected.
 */
@Serializable
enum class RollType {
    /**
     * Standard roll with no special mechanics.
     */
    NORMAL,

    /**
     * Roll two d20s and take the higher result.
     */
    ADVANTAGE,

    /**
     * Roll two d20s and take the lower result.
     */
    DISADVANTAGE
}
