package dev.questweaver.feature.encounter.viewmodel

import dev.questweaver.feature.map.ui.GridPos
import dev.questweaver.feature.map.ui.MapState

/**
 * Immutable UI state for encounter screen.
 * Represents complete state for rendering the encounter UI.
 */
data class EncounterUiState(
    // Encounter metadata
    val sessionId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Combat state
    val roundNumber: Int = 0,
    val isSurpriseRound: Boolean = false,
    val isCompleted: Boolean = false,
    val completionStatus: CompletionStatus? = null,
    
    // Initiative and turn
    val initiativeOrder: List<InitiativeEntry> = emptyList(),
    val activeCreatureId: Long? = null,
    val turnPhase: TurnPhase? = null,
    
    // Creatures
    val creatures: Map<Long, CreatureState> = emptyMap(),
    
    // Map integration
    val mapState: MapState? = null,
    
    // Available actions for active creature
    val availableActions: List<ActionOption> = emptyList(),
    
    // Undo/redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    
    // UI feedback
    val lastActionResult: ActionResult? = null,
    val pendingChoice: ActionChoice? = null
)

/**
 * Represents a creature's state in the UI.
 */
data class CreatureState(
    val id: Long,
    val name: String,
    val hpCurrent: Int,
    val hpMax: Int,
    val ac: Int,
    val position: GridPos,
    val conditions: Set<Condition>,
    val isPlayerControlled: Boolean,
    val isDefeated: Boolean
)

/**
 * Completion status for an encounter.
 */
enum class CompletionStatus {
    Victory,
    Defeat,
    Fled
}

/**
 * Represents a choice the player must make.
 */
data class ActionChoice(
    val prompt: String,
    val options: List<ActionOption>
)

/**
 * Initiative entry for turn order display.
 */
data class InitiativeEntry(
    val creatureId: Long,
    val initiativeScore: Int,
    val dexterityModifier: Int,
    val rollResult: Int
)

/**
 * Turn phase within a creature's turn.
 */
enum class TurnPhase {
    Start,
    Action,
    BonusAction,
    Movement,
    Reaction,
    End
}

/**
 * Available action option for the active creature.
 */
data class ActionOption(
    val id: String,
    val name: String,
    val description: String,
    val actionType: ActionType
)

/**
 * Type of action.
 */
enum class ActionType {
    Action,
    BonusAction,
    Reaction,
    Movement,
    FreeAction
}

/**
 * Result of an action attempt.
 */
sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val choice: ActionChoice) : ActionResult
}

/**
 * Condition affecting a creature.
 */
enum class Condition {
    Blinded,
    Charmed,
    Deafened,
    Frightened,
    Grappled,
    Incapacitated,
    Invisible,
    Paralyzed,
    Petrified,
    Poisoned,
    Prone,
    Restrained,
    Stunned,
    Unconscious,
    Exhaustion1,
    Exhaustion2,
    Exhaustion3,
    Exhaustion4,
    Exhaustion5,
    Exhaustion6
}
