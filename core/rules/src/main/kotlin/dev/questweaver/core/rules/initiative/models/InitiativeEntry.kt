package dev.questweaver.core.rules.initiative.models

/**
 * Initiative entry for a single creature.
 *
 * Represents a creature's initiative roll result with all components needed for sorting.
 * Implements Comparable to enable automatic sorting by initiative rules:
 * 1. Higher total initiative acts first
 * 2. Ties broken by higher Dexterity modifier
 * 3. Remaining ties broken by creature ID (deterministic)
 *
 * @property creatureId The creature's unique identifier
 * @property roll The d20 roll result (1-20)
 * @property modifier The Dexterity modifier applied to the roll
 * @property total The final initiative score (roll + modifier)
 */
data class InitiativeEntry(
    val creatureId: Long,
    val roll: Int,
    val modifier: Int,
    val total: Int
) : Comparable<InitiativeEntry> {
    
    /**
     * Compares initiative entries for sorting.
     *
     * Sort order (descending):
     * 1. Total initiative score (higher acts first)
     * 2. Dexterity modifier (higher wins ties)
     * 3. Creature ID (higher wins remaining ties - deterministic)
     *
     * @param other The entry to compare against
     * @return Negative if this entry should come before other, positive if after, zero if equal
     */
    override fun compareTo(other: InitiativeEntry): Int {
        return compareValuesBy(
            this, other,
            { -it.total },        // Descending: higher total first
            { -it.modifier },     // Descending: higher modifier first
            { -it.creatureId }    // Descending: higher ID first (deterministic)
        )
    }
}
