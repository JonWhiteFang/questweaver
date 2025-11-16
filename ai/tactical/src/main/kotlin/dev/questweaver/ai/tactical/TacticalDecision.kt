package dev.questweaver.ai.tactical

import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.values.GridPos

/**
 * Represents an AI tactical decision for a creature's turn.
 *
 * @property action The action to perform
 * @property target The target creature (if applicable)
 * @property position The position to move to (if applicable)
 * @property path The path to follow for movement
 * @property reasoning Explanation of why this decision was made
 */
data class TacticalDecision(
    val action: TacticalAction,
    val target: Creature?,
    val position: GridPos?,
    val path: List<GridPos>,
    val reasoning: DecisionReasoning
)

/**
 * Represents a tactical action that can be performed.
 * This is a simplified action model for AI decision-making.
 */
sealed interface TacticalAction {
    val actionType: ActionType
    
    data class Attack(
        val weaponName: String,
        override val actionType: ActionType = ActionType.ATTACK
    ) : TacticalAction
    
    data class CastSpell(
        val spellName: String,
        val spellLevel: Int,
        override val actionType: ActionType = ActionType.CAST_SPELL
    ) : TacticalAction
    
    data class Move(
        val distance: Int,
        override val actionType: ActionType = ActionType.MOVE
    ) : TacticalAction
    
    data object Dash : TacticalAction {
        override val actionType: ActionType = ActionType.DASH
    }
    
    data object Disengage : TacticalAction {
        override val actionType: ActionType = ActionType.DISENGAGE
    }
    
    data object Dodge : TacticalAction {
        override val actionType: ActionType = ActionType.DODGE
    }
    
    data object Help : TacticalAction {
        override val actionType: ActionType = ActionType.HELP
    }
    
    data class UseAbility(
        val abilityName: String,
        override val actionType: ActionType = ActionType.USE_ABILITY
    ) : TacticalAction
}

/**
 * Types of actions available in combat.
 */
enum class ActionType {
    ATTACK,
    CAST_SPELL,
    MOVE,
    DASH,
    DISENGAGE,
    DODGE,
    HELP,
    USE_ABILITY
}

/**
 * Explains why a particular decision was made.
 *
 * @property behaviorPath The path through the behavior tree (e.g., "Aggressive > CanAttack > Attack")
 * @property topScores Top 3 scored actions for debugging
 * @property selectedScore Score of the selected action
 * @property opportunities Tactical opportunities identified
 * @property resourcesUsed Resources consumed by this action
 */
data class DecisionReasoning(
    val behaviorPath: String,
    val topScores: List<ScoredAction>,
    val selectedScore: Float,
    val opportunities: List<TacticalOpportunity>,
    val resourcesUsed: List<Resource>
)

/**
 * An action candidate with its score.
 */
data class ScoredAction(
    val candidate: ActionCandidate,
    val score: Float,
    val breakdown: ScoreBreakdown
)

/**
 * Breakdown of how an action was scored.
 */
data class ScoreBreakdown(
    val damageScore: Float,
    val hitProbabilityScore: Float,
    val targetPriorityScore: Float,
    val resourceCostScore: Float,
    val tacticalValueScore: Float,
    val positioningScore: Float
)
