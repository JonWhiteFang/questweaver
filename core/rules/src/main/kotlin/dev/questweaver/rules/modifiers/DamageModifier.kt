package dev.questweaver.rules.modifiers

/**
 * Represents modifiers that affect damage taken by a creature.
 *
 * Damage modifiers alter the final damage amount based on the damage type:
 * - Resistance: Damage is halved (rounded down)
 * - Vulnerability: Damage is doubled
 * - Immunity: Damage is reduced to zero
 */
sealed interface DamageModifier {
    /**
     * The damage type this modifier applies to.
     */
    val damageType: DamageType
    
    /**
     * Resistance to a damage type - damage is halved (rounded down).
     *
     * @property damageType The type of damage this resistance applies to
     */
    data class Resistance(override val damageType: DamageType) : DamageModifier
    
    /**
     * Vulnerability to a damage type - damage is doubled.
     *
     * @property damageType The type of damage this vulnerability applies to
     */
    data class Vulnerability(override val damageType: DamageType) : DamageModifier
    
    /**
     * Immunity to a damage type - damage is reduced to zero.
     *
     * @property damageType The type of damage this immunity applies to
     */
    data class Immunity(override val damageType: DamageType) : DamageModifier
}
