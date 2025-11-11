package dev.questweaver.rules.modifiers

/**
 * Represents a creature's proficiency level for skills, tools, or saving throws.
 *
 * Proficiency levels determine how much of the proficiency bonus is added to rolls:
 * - None: No proficiency bonus (0x multiplier)
 * - Proficient: Standard proficiency bonus (1x multiplier)
 * - Expertise: Double proficiency bonus (2x multiplier)
 */
enum class ProficiencyLevel {
    /**
     * No proficiency - proficiency bonus is not added to the roll.
     */
    None,
    
    /**
     * Proficient - proficiency bonus is added once to the roll.
     */
    Proficient,
    
    /**
     * Expertise - proficiency bonus is added twice to the roll.
     */
    Expertise
}
