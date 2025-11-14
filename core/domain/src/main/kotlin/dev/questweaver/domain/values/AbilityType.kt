package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents the six core ability scores in D&D 5e.
 *
 * Each ability type is used for different types of checks, saves, and modifiers:
 * - Strength: Physical power, melee attacks, Athletics
 * - Dexterity: Agility, ranged attacks, AC, Acrobatics, Stealth
 * - Constitution: Endurance, hit points, concentration
 * - Intelligence: Reasoning, memory, Arcana, Investigation
 * - Wisdom: Awareness, intuition, Perception, Insight
 * - Charisma: Force of personality, Persuasion, Deception
 */
@Serializable
enum class AbilityType {
    /**
     * Strength - physical power and athletic prowess.
     */
    Strength,
    
    /**
     * Dexterity - agility, reflexes, and balance.
     */
    Dexterity,
    
    /**
     * Constitution - health, stamina, and vital force.
     */
    Constitution,
    
    /**
     * Intelligence - reasoning, memory, and analytical skill.
     */
    Intelligence,
    
    /**
     * Wisdom - awareness, intuition, and insight.
     */
    Wisdom,
    
    /**
     * Charisma - confidence, eloquence, and leadership.
     */
    Charisma
}
