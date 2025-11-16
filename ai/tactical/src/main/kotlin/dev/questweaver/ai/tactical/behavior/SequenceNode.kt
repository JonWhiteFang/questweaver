package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Sequence node executes children in order until one fails.
 * Returns Success only if all children succeed.
 * Returns the first Failure or ActionCandidates result encountered.
 * This implements the "AND" logic in behavior trees.
 *
 * @property children Child nodes to evaluate in order
 */
data class SequenceNode(
    val children: List<BehaviorNode>
) : BehaviorNode {
    init {
        require(children.isNotEmpty()) { "SequenceNode must have at least one child" }
    }
    
    @Suppress("ReturnCount")
    override suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult {
        for (child in children) {
            val result = child.evaluate(creature, context)
            when (result) {
                is BehaviorResult.Failure -> return result
                is BehaviorResult.ActionCandidates -> return result
                is BehaviorResult.Success -> continue // Continue to next child
            }
        }
        return BehaviorResult.Success
    }
}
