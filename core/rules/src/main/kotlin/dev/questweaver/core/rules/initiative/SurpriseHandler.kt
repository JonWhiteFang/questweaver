package dev.questweaver.core.rules.initiative

/**
 * Manages surprise round mechanics and surprised condition.
 *
 * Implements D&D 5e SRD surprise rules:
 * - If any creatures are surprised, a surprise round occurs before round 1
 * - Only non-surprised creatures can act during the surprise round
 * - Surprised creatures skip their turn in the surprise round
 * - After the surprise round, all creatures lose the surprised condition
 *
 * ## Usage Examples
 *
 * ### Check for Surprise Round
 * ```kotlin
 * val handler = SurpriseHandler()
 * val surprisedCreatures = setOf(2L, 3L)
 *
 * if (handler.hasSurpriseRound(surprisedCreatures)) {
 *     println("Surprise round occurs!")
 * }
 * ```
 *
 * ### Check if Creature Can Act
 * ```kotlin
 * val canAct = handler.canActInSurpriseRound(
 *     creatureId = 1L,
 *     surprisedCreatures = setOf(2L, 3L)
 * )
 * if (canAct) {
 *     println("Creature 1 can act in surprise round")
 * } else {
 *     println("Creature 1 is surprised and cannot act")
 * }
 * ```
 *
 * ### End Surprise Round
 * ```kotlin
 * val stillSurprised = handler.endSurpriseRound()
 * // Returns empty set - all creatures no longer surprised
 * ```
 */
class SurpriseHandler {
    
    /**
     * Determines if a surprise round should occur.
     *
     * A surprise round occurs when at least one creature is surprised.
     * If no creatures are surprised, combat begins normally at round 1.
     *
     * @param surprisedCreatures Set of creature IDs that are surprised
     * @return True if any creatures are surprised (surprise round occurs)
     */
    fun hasSurpriseRound(surprisedCreatures: Set<Long>): Boolean {
        return surprisedCreatures.isNotEmpty()
    }
    
    /**
     * Checks if a creature can act in the surprise round.
     *
     * Only non-surprised creatures can take actions during the surprise round.
     * Surprised creatures must skip their turn.
     *
     * @param creatureId The creature to check
     * @param surprisedCreatures Set of surprised creature IDs
     * @return True if the creature can act (not surprised)
     */
    fun canActInSurpriseRound(
        creatureId: Long,
        surprisedCreatures: Set<Long>
    ): Boolean {
        return creatureId !in surprisedCreatures
    }
    
    /**
     * Removes surprised condition from all creatures.
     *
     * Called at the end of the surprise round. All creatures lose the
     * surprised condition and can act normally starting in round 1.
     *
     * @return Empty set (all creatures no longer surprised)
     */
    fun endSurpriseRound(): Set<Long> {
        return emptySet()
    }
}
