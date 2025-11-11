package dev.questweaver.rules.conditions

/**
 * Represents status conditions that can affect creatures in D&D 5e.
 *
 * Conditions modify a creature's capabilities, affecting attack rolls, saving throws,
 * ability checks, and available actions. This sealed interface includes a subset of
 * D&D 5e SRD conditions for v1.
 */
sealed interface Condition {
    /**
     * Prone condition - creature is lying on the ground.
     *
     * Effects:
     * - Disadvantage on attack rolls
     * - Melee attacks against the creature have advantage
     * - Ranged attacks against the creature have disadvantage
     */
    data object Prone : Condition
    
    /**
     * Stunned condition - creature is incapacitated and cannot move.
     *
     * Effects:
     * - Cannot take actions or reactions
     * - Automatically fails Strength and Dexterity saving throws
     * - Attack rolls against the creature have advantage
     */
    data object Stunned : Condition
    
    /**
     * Poisoned condition - creature is suffering from poison.
     *
     * Effects:
     * - Disadvantage on attack rolls
     * - Disadvantage on ability checks
     */
    data object Poisoned : Condition
    
    /**
     * Blinded condition - creature cannot see.
     *
     * Effects:
     * - Automatically fails ability checks that require sight
     * - Disadvantage on attack rolls
     * - Attack rolls against the creature have advantage
     */
    data object Blinded : Condition
    
    /**
     * Restrained condition - creature's movement is restricted.
     *
     * Effects:
     * - Speed becomes 0
     * - Disadvantage on attack rolls
     * - Disadvantage on Dexterity saving throws
     * - Attack rolls against the creature have advantage
     */
    data object Restrained : Condition
    
    /**
     * Incapacitated condition - creature cannot take actions or reactions.
     *
     * Effects:
     * - Cannot take actions or reactions
     */
    data object Incapacitated : Condition
    
    /**
     * Paralyzed condition - creature is frozen in place.
     *
     * Effects:
     * - Incapacitated (cannot take actions or reactions)
     * - Automatically fails Strength and Dexterity saving throws
     * - Attack rolls against the creature have advantage
     * - Melee attacks within 5 feet are automatic critical hits if they hit
     */
    data object Paralyzed : Condition
    
    /**
     * Unconscious condition - creature is knocked out.
     *
     * Effects:
     * - Incapacitated (cannot take actions or reactions)
     * - Cannot move or speak
     * - Unaware of surroundings
     * - Drops whatever it's holding and falls prone
     * - Automatically fails Strength and Dexterity saving throws
     * - Attack rolls against the creature have advantage
     * - Melee attacks within 5 feet are automatic critical hits if they hit
     */
    data object Unconscious : Condition
}
