package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Base interface for behavior tree nodes.
 * Behavior trees provide hierarchical decision-making structure.
 */
sealed interface BehaviorNode {
    /**
     * Evaluates this node against the current tactical context.
     *
     * @param creature The creature making the decision
     * @param context The current tactical situation
     * @return The result of evaluating this node
     */
    suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult
}

/**
 * Result of evaluating a behavior tree node.
 */
sealed interface BehaviorResult {
    /**
     * Node evaluation succeeded.
     */
    data object Success : BehaviorResult
    
    /**
     * Node evaluation failed.
     */
    data object Failure : BehaviorResult
    
    /**
     * Node produced action candidates for scoring.
     *
     * @property candidates List of action candidates to evaluate
     */
    data class ActionCandidates(val candidates: List<ActionCandidate>) : BehaviorResult
}
