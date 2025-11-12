package dev.questweaver.core.rules.validation.state

/**
 * Tracks active concentration spells for all creatures in an encounter.
 *
 * In D&D 5e, a creature can only concentrate on one spell at a time.
 * Casting a new concentration spell breaks the existing concentration.
 */
data class ConcentrationState(
    /**
     * Map of creature IDs to their active concentration information.
     */
    val activeConcentrations: Map<Long, ConcentrationInfo> = emptyMap()
) {
    /**
     * Checks if a creature is currently concentrating on a spell.
     */
    fun isConcentrating(creatureId: Long): Boolean {
        return creatureId in activeConcentrations
    }
    
    /**
     * Gets the concentration information for a creature.
     *
     * @return ConcentrationInfo if concentrating, null otherwise
     */
    fun getConcentration(creatureId: Long): ConcentrationInfo? {
        return activeConcentrations[creatureId]
    }
    
    /**
     * Returns a new ConcentrationState with the creature starting concentration on a spell.
     *
     * If the creature was already concentrating, the old concentration is replaced.
     */
    fun startConcentration(creatureId: Long, spellId: String, round: Int): ConcentrationState {
        return copy(
            activeConcentrations = activeConcentrations + (creatureId to ConcentrationInfo(
                spellId = spellId,
                startedRound = round,
                dc = 10 // Base DC, can be increased by damage
            ))
        )
    }
    
    /**
     * Returns a new ConcentrationState with the creature's concentration broken.
     */
    fun breakConcentration(creatureId: Long): ConcentrationState {
        return copy(
            activeConcentrations = activeConcentrations - creatureId
        )
    }
    
    companion object {
        /**
         * An empty concentration state with no active concentrations.
         */
        val Empty = ConcentrationState()
    }
}

/**
 * Information about a creature's active concentration.
 */
data class ConcentrationInfo(
    /**
     * The ID of the spell being concentrated on.
     */
    val spellId: String,
    
    /**
     * The round when concentration started.
     */
    val startedRound: Int,
    
    /**
     * The DC for concentration saving throws.
     * Base DC is 10, or half the damage taken (whichever is higher).
     */
    val dc: Int
)
