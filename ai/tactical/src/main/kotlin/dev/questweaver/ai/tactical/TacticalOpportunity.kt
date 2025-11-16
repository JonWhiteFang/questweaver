package dev.questweaver.ai.tactical

/**
 * Represents a tactical opportunity that can be exploited.
 * Each opportunity provides a bonus score to actions that exploit it.
 */
sealed interface TacticalOpportunity {
    /**
     * The bonus score added to actions that exploit this opportunity.
     */
    val bonusScore: Float
    
    /**
     * Flanking position provides advantage on melee attacks.
     */
    data class Flanking(override val bonusScore: Float = 15f) : TacticalOpportunity
    
    /**
     * Target is prone, providing advantage on melee attacks.
     */
    data class ProneTarget(override val bonusScore: Float = 20f) : TacticalOpportunity
    
    /**
     * Target is incapacitated, providing automatic critical hits.
     */
    data class IncapacitatedTarget(override val bonusScore: Float = 30f) : TacticalOpportunity
    
    /**
     * Target is concentrating on a spell that can be broken.
     */
    data class ConcentrationBreak(override val bonusScore: Float = 25f) : TacticalOpportunity
    
    /**
     * Area of effect spell can hit multiple targets.
     */
    data class MultiTargetAoE(
        val targetCount: Int,
        override val bonusScore: Float
    ) : TacticalOpportunity {
        init {
            require(targetCount >= MIN_TARGETS) { "Multi-target AoE must hit at least $MIN_TARGETS targets" }
        }
        
        companion object {
            private const val MIN_TARGETS = 2
            private const val BONUS_PER_TARGET = 20f
            
            /**
             * Creates a MultiTargetAoE opportunity with bonus based on target count.
             * Bonus is 20 points per additional target beyond the first.
             */
            fun create(targetCount: Int): MultiTargetAoE {
                require(targetCount >= MIN_TARGETS) { "Multi-target AoE must hit at least $MIN_TARGETS targets" }
                val bonus = BONUS_PER_TARGET * (targetCount - 1)
                return MultiTargetAoE(targetCount, bonus)
            }
        }
    }
    
    /**
     * Forced movement can push target into a hazard.
     */
    data class ForcedMovement(
        val hazardNearby: Boolean,
        override val bonusScore: Float
    ) : TacticalOpportunity {
        companion object {
            private const val HAZARD_BONUS = 30f
            private const val NO_HAZARD_BONUS = 10f
            
            /**
             * Creates a ForcedMovement opportunity.
             * Bonus is 30 if hazard nearby, 10 otherwise.
             */
            fun create(hazardNearby: Boolean): ForcedMovement {
                val bonus = if (hazardNearby) HAZARD_BONUS else NO_HAZARD_BONUS
                return ForcedMovement(hazardNearby, bonus)
            }
        }
    }
}
