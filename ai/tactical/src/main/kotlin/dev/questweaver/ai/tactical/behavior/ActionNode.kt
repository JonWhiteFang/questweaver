package dev.questweaver.ai.tactical.behavior

import dev.questweaver.ai.tactical.ActionCandidate
import dev.questweaver.ai.tactical.ActionType
import dev.questweaver.ai.tactical.TacticalContext
import dev.questweaver.domain.entities.Creature

/**
 * Action node generates action candidates for a specific action type.
 * Returns ActionCandidates result with the specified action type and priority.
 *
 * @property actionType The type of action to generate
 * @property priority Priority level for this action (higher = more preferred)
 * @property generator Function that generates action candidates
 */
data class ActionNode(
    val actionType: ActionType,
    val priority: Int,
    val generator: suspend (Creature, TacticalContext) -> List<ActionCandidate>
) : BehaviorNode {
    override suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult {
        val candidates = generator(creature, context)
        return if (candidates.isNotEmpty()) {
            BehaviorResult.ActionCandidates(candidates)
        } else {
            BehaviorResult.Failure
        }
    }
}
