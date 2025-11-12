package dev.questweaver.core.rules.validation.actions

/**
 * Enum representing the type of action economy resource consumed by an action.
 *
 * In D&D 5e, creatures have limited actions per turn:
 * - One Action per turn
 * - One Bonus Action per turn (if available)
 * - One Reaction per round (resets at start of creature's turn)
 * - Movement up to their speed
 * - Free Actions (no limit, but DM discretion)
 */
enum class ActionType {
    /**
     * Standard action (one per turn).
     * Examples: Attack, Cast a Spell, Dash, Disengage, Dodge, Help, Hide, Ready, Search, Use an Object
     */
    Action,

    /**
     * Bonus action (one per turn, only if available).
     * Examples: Off-hand attack, certain spells, class features
     */
    BonusAction,

    /**
     * Reaction (one per round, triggered by specific events).
     * Examples: Opportunity attack, Counterspell, Shield spell
     */
    Reaction,

    /**
     * Movement (up to creature's speed per turn).
     * Can be split before and after actions.
     */
    Movement,

    /**
     * Free action (no action economy cost).
     * Examples: Drop an item, speak a few words, interact with environment
     */
    FreeAction
}
