package dev.questweaver.rules.modifiers

/**
 * Represents the type of damage dealt in D&D 5e SRD.
 *
 * Damage types are used to determine resistances, vulnerabilities, and immunities.
 * This enum includes all damage types from the D&D 5e System Reference Document.
 */
enum class DamageType {
    // Physical damage types
    /**
     * Slashing damage - from swords, axes, claws, etc.
     */
    Slashing,
    
    /**
     * Piercing damage - from arrows, spears, bites, etc.
     */
    Piercing,
    
    /**
     * Bludgeoning damage - from clubs, hammers, falls, etc.
     */
    Bludgeoning,
    
    // Elemental damage types
    /**
     * Fire damage - from flames, lava, red dragon breath, etc.
     */
    Fire,
    
    /**
     * Cold damage - from ice, freezing water, white dragon breath, etc.
     */
    Cold,
    
    /**
     * Lightning damage - from electricity, blue dragon breath, etc.
     */
    Lightning,
    
    /**
     * Thunder damage - from sonic booms, thunderwave, etc.
     */
    Thunder,
    
    // Other damage types
    /**
     * Acid damage - from corrosive substances, black dragon breath, etc.
     */
    Acid,
    
    /**
     * Poison damage - from venoms, toxic gases, green dragon breath, etc.
     */
    Poison,
    
    /**
     * Necrotic damage - from undead, life-draining effects, etc.
     */
    Necrotic,
    
    /**
     * Radiant damage - from holy light, celestial beings, etc.
     */
    Radiant,
    
    /**
     * Force damage - from pure magical energy, magic missile, etc.
     */
    Force,
    
    /**
     * Psychic damage - from mental attacks, mind flayers, etc.
     */
    Psychic
}
