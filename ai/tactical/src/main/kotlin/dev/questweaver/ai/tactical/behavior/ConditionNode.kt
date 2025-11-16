package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Condition node evaluates a predicate function.
 * Returns Success if the predicate returns true, Failure otherwise.
 *
 * @property predicate Function that checks a condition
 * @property description Optional description of what this condition checks
 */
data class ConditionNode(
    val predicate: suspend (Creature, TacticalContext) -> Boolean,
    val description: String = "Unnamed condition"
) : BehaviorNode {
    override suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult {
        return if (predicate(creature, context)) {
            BehaviorResult.Success
        } else {
            BehaviorResult.Failure
        }
    }
}
