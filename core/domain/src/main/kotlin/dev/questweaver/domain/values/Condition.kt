package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents D&D 5e conditions that can affect creatures.
 */
@Serializable
enum class Condition {
    /** A blinded creature can't see and automatically fails ability checks that require sight */
    BLINDED,
    
    /** A charmed creature can't attack the charmer or target them with harmful abilities */
    CHARMED,
    
    /** A deafened creature can't hear and automatically fails ability checks that require hearing */
    DEAFENED,
    
    /** A frightened creature has disadvantage on ability checks and attack rolls while the source is in sight */
    FRIGHTENED,
    
    /** A grappled creature's speed becomes 0 and can't benefit from bonuses to speed */
    GRAPPLED,
    
    /** An incapacitated creature can't take actions or reactions */
    INCAPACITATED,
    
    /** An invisible creature is impossible to see without special senses */
    INVISIBLE,
    
    /** A paralyzed creature is incapacitated and can't move or speak */
    PARALYZED,
    
    /** A petrified creature is transformed into a solid inanimate substance */
    PETRIFIED,
    
    /** A poisoned creature has disadvantage on attack rolls and ability checks */
    POISONED,
    
    /** A prone creature's only movement option is to crawl */
    PRONE,
    
    /** A restrained creature's speed becomes 0 and can't benefit from bonuses to speed */
    RESTRAINED,
    
    /** A stunned creature is incapacitated, can't move, and can speak only falteringly */
    STUNNED,
    
    /** An unconscious creature is incapacitated, can't move or speak, and is unaware of surroundings */
    UNCONSCIOUS
}
