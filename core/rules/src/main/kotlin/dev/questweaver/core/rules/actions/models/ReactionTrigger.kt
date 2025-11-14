package dev.questweaver.core.rules.actions.models

import dev.questweaver.domain.values.GridPos
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing events that can trigger reactions.
 */
@Serializable
sealed interface ReactionTrigger {
    /**
     * Triggered when a creature moves from one position to another.
     */
    @Serializable
    data class CreatureMoved(
        val creatureId: Long,
        val fromPos: GridPos,
        val toPos: GridPos
    ) : ReactionTrigger
    
    /**
     * Triggered when a spell is cast.
     */
    @Serializable
    data class SpellCast(
        val casterId: Long,
        val spellId: Long,
        val targets: List<Long>
    ) : ReactionTrigger
    
    /**
     * Triggered when an attack is made.
     */
    @Serializable
    data class AttackMade(
        val attackerId: Long,
        val targetId: Long
    ) : ReactionTrigger
    
    /**
     * Triggered when a custom trigger condition is met.
     */
    @Serializable
    data class TriggerConditionMet(
        val condition: String
    ) : ReactionTrigger
}
