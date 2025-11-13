package dev.questweaver.core.rules.initiative.models

/**
 * Types of actions a creature can take during combat.
 *
 * Represents the different action economy types in D&D 5e SRD.
 * Used to track and validate action availability during a turn.
 */
enum class ActionType {
    /**
     * Standard action (attack, cast a spell, dash, etc.)
     * One per turn.
     */
    Action,
    
    /**
     * Bonus action (special abilities, spells with bonus action casting time)
     * One per turn, requires a feature that grants a bonus action.
     */
    BonusAction,
    
    /**
     * Reaction (opportunity attack, counterspell, etc.)
     * One per round, can be used on any creature's turn.
     */
    Reaction,
    
    /**
     * Movement (walking, climbing, swimming)
     * Limited by movement speed per turn.
     */
    Movement,
    
    /**
     * Free action (object interaction, communication)
     * Generally unlimited, but subject to DM discretion.
     */
    FreeAction
}
