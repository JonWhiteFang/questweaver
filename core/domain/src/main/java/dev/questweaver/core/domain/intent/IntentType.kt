package dev.questweaver.core.domain.intent

/**
 * Represents the type of action a player intends to perform.
 * 
 * These intent types correspond to common D&D 5e actions that can be
 * classified from natural language input.
 */
enum class IntentType {
    /** Attack a creature with a weapon or unarmed strike */
    ATTACK,
    
    /** Move to a different location on the map */
    MOVE,
    
    /** Cast a spell */
    CAST_SPELL,
    
    /** Use an item from inventory */
    USE_ITEM,
    
    /** Take the Dash action (double movement) */
    DASH,
    
    /** Take the Dodge action (impose disadvantage on attacks) */
    DODGE,
    
    /** Take the Help action (assist an ally) */
    HELP,
    
    /** Take the Hide action (attempt to hide) */
    HIDE,
    
    /** Take the Disengage action (move without provoking opportunity attacks) */
    DISENGAGE,
    
    /** Ready an action to trigger on a condition */
    READY,
    
    /** Take the Search action (look for something) */
    SEARCH,
    
    /** Intent could not be determined */
    UNKNOWN
}
