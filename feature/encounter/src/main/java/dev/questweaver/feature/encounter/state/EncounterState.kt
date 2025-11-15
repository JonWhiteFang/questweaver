package dev.questweaver.feature.encounter.state

import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.CompletionStatus
import dev.questweaver.feature.map.ui.MapState

/**
 * Domain state representing the complete encounter state.
 * This is the source of truth derived from event replay.
 */
data class EncounterState(
    val sessionId: Long,
    val roundState: RoundState,
    val creatures: Map<Long, Creature>,
    val mapGrid: MapGridState,
    val readiedActions: Map<Long, ReadiedAction>,
    val isCompleted: Boolean,
    val completionStatus: CompletionStatus?
)

/**
 * State of the current round and turn.
 */
data class RoundState(
    val roundNumber: Int,
    val isSurpriseRound: Boolean,
    val initiativeOrder: List<Long>,
    val activeCreatureId: Long?,
    val turnPhase: TurnPhaseState
)

/**
 * Turn phase state.
 */
enum class TurnPhaseState {
    Start,
    Action,
    BonusAction,
    Movement,
    Reaction,
    End
}

/**
 * Map grid state for the encounter.
 */
data class MapGridState(
    val width: Int,
    val height: Int,
    val blockedPositions: Set<dev.questweaver.domain.values.GridPos>,
    val difficultTerrainPositions: Set<dev.questweaver.domain.values.GridPos>
)

/**
 * Represents a readied action.
 */
data class ReadiedAction(
    val creatureId: Long,
    val actionType: String,
    val trigger: String,
    val targetId: Long? = null
)

/**
 * Rewards for completing an encounter.
 */
data class EncounterRewards(
    val xpAwarded: Int,
    val loot: List<LootItem>
)

/**
 * Represents a loot item.
 */
data class LootItem(
    val id: Long,
    val name: String,
    val quantity: Int
)
