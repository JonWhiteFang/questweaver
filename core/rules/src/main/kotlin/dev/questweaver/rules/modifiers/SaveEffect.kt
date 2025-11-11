package dev.questweaver.rules.modifiers

/**
 * Represents special effects that can be applied to saving throws.
 *
 * Save effects modify how saving throws are resolved:
 * - AutoFail: The saving throw automatically fails regardless of the roll
 * - Disadvantage: Roll two d20s and use the lower result
 * - Normal: No special effect applied
 */
sealed interface SaveEffect {
    /**
     * Automatic failure - the saving throw fails regardless of the roll.
     * Used for conditions like Stunned (auto-fails STR/DEX saves).
     */
    data object AutoFail : SaveEffect
    
    /**
     * Disadvantage - roll two d20s and use the lower result.
     */
    data object Disadvantage : SaveEffect
    
    /**
     * Normal - no special effect, roll normally.
     */
    data object Normal : SaveEffect
}
