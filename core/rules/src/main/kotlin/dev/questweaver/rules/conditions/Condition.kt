package dev.questweaver.rules.conditions

/**
 * Sealed interface representing D&D 5e conditions that can affect creatures.
 *
 * Conditions modify a creature's capabilities, imposing penalties or restrictions
 * on their actions, movement, and rolls. This is a subset of SRD conditions
 * implemented for v1.
 */
sealed interface Condition {
    /**
     * Prone: A prone creature's only movement option is to crawl, unless it stands up.
     * The creature has disadvantage on attack rolls. An attack roll against the creature
     * has advantage if the attacker is within 5 feet, otherwise disadvantage.
     */
    object Prone : Condition

    /**
     * Stunned: A stunned creature is incapacitated, can't move, and can speak only falteringly.
     * The creature automatically fails Strength and Dexterity saving throws.
     * Attack rolls against the creature have advantage.
     */
    object Stunned : Condition

    /**
     * Poisoned: A poisoned creature has disadvantage on attack rolls and ability checks.
     */
    object Poisoned : Condition

    /**
     * Blinded: A blinded creature can't see and automatically fails any ability check
     * that requires sight. Attack rolls against the creature have advantage, and the
     * creature's attack rolls have disadvantage.
     */
    object Blinded : Condition

    /**
     * Restrained: A restrained creature's speed becomes 0, and it can't benefit from
     * any bonus to its speed. Attack rolls against the creature have advantage, and
     * the creature's attack rolls have disadvantage. The creature has disadvantage
     * on Dexterity saving throws.
     */
    object Restrained : Condition

    /**
     * Incapacitated: An incapacitated creature can't take actions or reactions.
     */
    object Incapacitated : Condition

    /**
     * Paralyzed: A paralyzed creature is incapacitated and can't move or speak.
     * The creature automatically fails Strength and Dexterity saving throws.
     * Attack rolls against the creature have advantage. Any attack that hits the
     * creature is a critical hit if the attacker is within 5 feet.
     */
    object Paralyzed : Condition

    /**
     * Unconscious: An unconscious creature is incapacitated, can't move or speak,
     * and is unaware of its surroundings. The creature drops whatever it's holding
     * and falls prone. The creature automatically fails Strength and Dexterity
     * saving throws. Attack rolls against the creature have advantage. Any attack
     * that hits the creature is a critical hit if the attacker is within 5 feet.
     */
    object Unconscious : Condition
}
