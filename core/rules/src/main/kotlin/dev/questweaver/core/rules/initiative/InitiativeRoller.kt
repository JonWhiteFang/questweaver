package dev.questweaver.core.rules.initiative

import dev.questweaver.core.rules.initiative.models.InitiativeEntry
import dev.questweaver.domain.dice.DiceRoller

/**
 * Rolls initiative for creatures and establishes initial turn order.
 *
 * Uses seeded DiceRoller to ensure deterministic initiative rolls for event replay.
 * Implements D&D 5e SRD initiative mechanics: d20 + Dexterity modifier, with ties
 * broken by Dexterity modifier, then creature ID.
 *
 * ## Usage Examples
 *
 * ### Roll Initiative for Single Creature
 * ```kotlin
 * val roller = InitiativeRoller(DiceRoller(seed = 42L))
 * val entry = roller.rollInitiative(
 *     creatureId = 1L,
 *     dexterityModifier = 3
 * )
 * println("Initiative: ${entry.total}") // e.g., 18 (15 + 3)
 * ```
 *
 * ### Roll Initiative for All Creatures
 * ```kotlin
 * val creatures = mapOf(
 *     1L to 3,  // Creature 1: +3 Dex
 *     2L to 2,  // Creature 2: +2 Dex
 *     3L to 3   // Creature 3: +3 Dex
 * )
 * val order = roller.rollInitiativeForAll(creatures)
 * // Returns sorted list: highest initiative first
 * // Ties broken by Dex modifier, then creature ID
 * ```
 *
 * @param diceRoller Seeded dice roller for deterministic initiative rolls
 */
class InitiativeRoller(private val diceRoller: DiceRoller) {
    
    /**
     * Rolls initiative for a single creature.
     *
     * Initiative = d20 + Dexterity modifier
     *
     * @param creatureId The creature's unique identifier
     * @param dexterityModifier The creature's Dexterity modifier
     * @return InitiativeEntry with roll result and creature ID
     */
    fun rollInitiative(
        creatureId: Long,
        dexterityModifier: Int
    ): InitiativeEntry {
        val roll = diceRoller.d20()
        val rollValue = roll.rolls.first() // Get the natural d20 roll
        val total = rollValue + dexterityModifier
        
        return InitiativeEntry(
            creatureId = creatureId,
            roll = rollValue,
            modifier = dexterityModifier,
            total = total
        )
    }
    
    /**
     * Rolls initiative for multiple creatures and sorts by score.
     *
     * Sorting rules (D&D 5e SRD):
     * 1. Higher total initiative acts first
     * 2. Ties broken by higher Dexterity modifier
     * 3. Remaining ties broken by creature ID (deterministic)
     *
     * @param creatures Map of creature ID to Dexterity modifier
     * @return Sorted list of InitiativeEntry (highest first)
     */
    fun rollInitiativeForAll(
        creatures: Map<Long, Int>
    ): List<InitiativeEntry> {
        return creatures.map { (creatureId, dexModifier) ->
            rollInitiative(creatureId, dexModifier)
        }.sorted() // InitiativeEntry.compareTo handles sorting rules
    }
}
