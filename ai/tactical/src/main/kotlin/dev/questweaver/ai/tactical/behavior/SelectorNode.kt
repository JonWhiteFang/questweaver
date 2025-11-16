package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Selector node tries children in order until one succeeds.
 * Returns the first successful result, or Failure if all children fail.
 * This implements the "OR" logic in behavior trees.
 *
 * @property children Child nodes to evaluate in order
 */
data class SelectorNode(
    val children: List<BehaviorNode>
) : BehaviorNode {
    init {
        require(children.isNotEmpty()) { "SelectorNode must have at least one child" }
    }
    
    @Suppress("ReturnCount")
    override suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult {
        for (child in children) {
            val result = child.evaluate(creature, context)
            when (result) {
                is BehaviorResult.Success -> return result
                is BehaviorResult.ActionCandidates -> return result
                is BehaviorResult.Failure -> continue // Try next child
            }
        }
        return BehaviorResult.Failure
    }
}
