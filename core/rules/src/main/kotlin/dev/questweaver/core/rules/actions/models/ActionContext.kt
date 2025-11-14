package dev.questweaver.core.rules.actions.models

import dev.questweaver.domain.entities.Creature
import dev.questweaver.domain.entities.MapGrid
import dev.questweaver.core.rules.initiative.models.TurnPhase

/**
 * Context information needed to process combat actions.
 * Contains all state required for validation and execution.
 */
data class ActionContext(
    val sessionId: Long,
    val roundNumber: Int,
    val turnPhase: TurnPhase,
    val creatures: Map<Long, Creature>,
    val mapGrid: MapGrid,
    val activeConditions: Map<Long, Set<String>>,
    val readiedActions: Map<Long, ReadiedAction>
)

/**
 * Represents an action that has been readied for later execution.
 */
data class ReadiedAction(
    val creatureId: Long,
    val action: CombatAction,
    val trigger: String
)
