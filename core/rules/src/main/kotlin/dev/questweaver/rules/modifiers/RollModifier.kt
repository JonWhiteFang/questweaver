package dev.questweaver.rules.modifiers

/**
 * Represents modifiers that affect d20 rolls in D&D 5e.
 *
 * Roll modifiers determine how many d20s are rolled and which result is used:
 * - Normal: Roll one d20
 * - Advantage: Roll two d20s, use the higher result
 * - Disadvantage: Roll two d20s, use the lower result
 */
sealed interface RollModifier {
    /**
     * Normal roll - roll one d20 and use the result.
     */
    data object Normal : RollModifier
    
    /**
     * Advantage - roll two d20s and use the higher result.
     */
    data object Advantage : RollModifier
    
    /**
     * Disadvantage - roll two d20s and use the lower result.
     */
    data object Disadvantage : RollModifier
}
