package dev.questweaver.rules.combat

import dev.questweaver.domain.dice.DiceRoller
import dev.questweaver.domain.dice.DieType
import dev.questweaver.rules.modifiers.DamageModifier
import dev.questweaver.rules.modifiers.DamageType
import dev.questweaver.rules.outcomes.DamageOutcome

/**
 * Calculates damage for attacks, handling critical hits and damage modifiers.
 *
 * This class is responsible for:
 * - Parsing and rolling damage dice expressions (e.g., "2d6", "1d8+3")
 * - Doubling dice for critical hits (but not modifiers)
 * - Applying resistance (half damage, rounded down)
 * - Applying vulnerability (double damage)
 * - Applying immunity (zero damage)
 *
 * All calculations are deterministic based on the seeded DiceRoller.
 *
 * ## Usage Examples
 *
 * ### Basic Damage
 * ```kotlin
 * val roller = DiceRoller(seed = 42L)
 * val calculator = DamageCalculator(roller)
 *
 * val outcome = calculator.calculateDamage(
 *     damageDice = "2d6",
 *     damageModifier = 3,
 *     damageType = DamageType.Slashing,
 *     isCritical = false,
 *     targetModifiers = emptySet()
 * )
 * // outcome.diceRolls = [4, 5]
 * // outcome.diceTotal = 9
 * // outcome.baseDamage = 12 (9 + 3)
 * // outcome.finalDamage = 12
 * ```
 *
 * ### Critical Hit
 * ```kotlin
 * val outcome = calculator.calculateDamage(
 *     damageDice = "2d6",
 *     damageModifier = 3,
 *     damageType = DamageType.Slashing,
 *     isCritical = true,
 *     targetModifiers = emptySet()
 * )
 * // Rolls 4d6 (doubled) + 3 modifier
 * // outcome.isCritical = true
 * ```
 *
 * ### With Resistance
 * ```kotlin
 * val outcome = calculator.calculateDamage(
 *     damageDice = "2d6",
 *     damageModifier = 3,
 *     damageType = DamageType.Fire,
 *     isCritical = false,
 *     targetModifiers = setOf(DamageModifier.Resistance(DamageType.Fire))
 * )
 * // baseDamage = 12
 * // finalDamage = 6 (halved, rounded down)
 * ```
 *
 * @param diceRoller The seeded dice roller for deterministic damage rolls
 */
class DamageCalculator(private val diceRoller: DiceRoller) {

    /**
     * Calculates damage for an attack.
     *
     * @param damageDice The damage dice expression (e.g., "2d6", "1d8", "3d4")
     * @param damageModifier Flat damage modifier to add
     * @param damageType Type of damage (slashing, fire, etc.)
     * @param isCritical Whether this is a critical hit (doubles dice, not modifier)
     * @param targetModifiers Target's resistances/vulnerabilities/immunities
     * @return DamageOutcome with roll details and final damage
     * @throws IllegalArgumentException if damageDice format is invalid
     */
    fun calculateDamage(
        damageDice: String,
        damageModifier: Int,
        damageType: DamageType,
        isCritical: Boolean,
        targetModifiers: Set<DamageModifier>
    ): DamageOutcome {
        // Parse damage dice expression
        val (count, dieType) = parseDamageDice(damageDice)
        
        // Determine actual dice count (double for critical)
        val actualCount = if (isCritical) count * 2 else count
        
        // Roll damage dice
        val diceRoll = diceRoller.roll(count = actualCount, die = dieType)
        val diceRolls = diceRoll.rolls
        val diceTotal = diceRoll.naturalTotal
        
        // Calculate base damage (dice + modifier)
        val baseDamage = diceTotal + damageModifier
        
        // Apply damage modifiers
        val relevantModifiers = targetModifiers.filter { it.damageType == damageType }.toSet()
        val finalDamage = applyDamageModifiers(baseDamage, relevantModifiers)
        
        return DamageOutcome(
            diceRolls = diceRolls,
            diceTotal = diceTotal,
            damageModifier = damageModifier,
            baseDamage = baseDamage,
            damageType = damageType,
            isCritical = isCritical,
            appliedModifiers = relevantModifiers,
            finalDamage = finalDamage
        )
    }

    /**
     * Parses a damage dice expression into count and die type.
     *
     * Supported formats:
     * - "2d6" -> 2 dice of type D6
     * - "1d8" -> 1 die of type D8
     * - "3d4" -> 3 dice of type D4
     *
     * @param damageDice The dice expression to parse
     * @return Pair of (count, DieType)
     * @throws IllegalArgumentException if format is invalid
     */
    @Suppress("MagicNumber") // Die sides are domain constants
    private fun parseDamageDice(damageDice: String): Pair<Int, DieType> {
        val trimmed = damageDice.trim().lowercase()
        
        // Match pattern like "2d6"
        val regex = Regex("""(\d+)d(\d+)""")
        val matchResult = regex.matchEntire(trimmed)
            ?: throw IllegalArgumentException(
                "Invalid damage dice format: $damageDice. Expected format like '2d6'"
            )
        
        val count = matchResult.groupValues[1].toInt()
        val sides = matchResult.groupValues[2].toInt()
        
        require(count > 0) { "Dice count must be positive, got $count" }
        
        // Map sides to DieType
        val dieType = when (sides) {
            4 -> DieType.D4
            6 -> DieType.D6
            8 -> DieType.D8
            10 -> DieType.D10
            12 -> DieType.D12
            20 -> DieType.D20
            100 -> DieType.D100
            else -> throw IllegalArgumentException(
                "Unsupported die type: d$sides. Supported: d4, d6, d8, d10, d12, d20, d100"
            )
        }
        
        return count to dieType
    }

    /**
     * Applies damage modifiers (resistance/vulnerability/immunity) to base damage.
     *
     * Order of operations:
     * 1. Check for immunity (reduces damage to 0)
     * 2. Apply resistance (halve damage, rounded down)
     * 3. Apply vulnerability (double damage)
     *
     * If multiple modifiers of the same type exist, only one is applied.
     * Immunity takes precedence over all other modifiers.
     *
     * @param baseDamage The base damage before modifiers
     * @param modifiers The set of damage modifiers to apply
     * @return The final damage after all modifiers
     */
    private fun applyDamageModifiers(baseDamage: Int, modifiers: Set<DamageModifier>): Int {
        // Check for immunity first (overrides everything)
        if (modifiers.any { it is DamageModifier.Immunity }) {
            return 0
        }
        
        var damage = baseDamage
        
        // Apply resistance (half damage, rounded down)
        if (modifiers.any { it is DamageModifier.Resistance }) {
            damage = damage / 2
        }
        
        // Apply vulnerability (double damage)
        if (modifiers.any { it is DamageModifier.Vulnerability }) {
            damage = damage * 2
        }
        
        return damage
    }
}
