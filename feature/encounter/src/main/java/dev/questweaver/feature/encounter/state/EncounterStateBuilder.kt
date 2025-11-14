package dev.questweaver.feature.encounter.state

import dev.questweaver.core.rules.initiative.InitiativeStateBuilder
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.RoundStarted
import dev.questweaver.domain.events.TurnStarted
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.events.SpellCast
import dev.questweaver.domain.events.CreatureDefeated
import dev.questweaver.domain.events.EncounterEnded
import dev.questweaver.feature.encounter.viewmodel.Creature
import dev.questweaver.feature.encounter.viewmodel.CreatureState
import dev.questweaver.feature.encounter.viewmodel.EncounterUiState
import dev.questweaver.feature.encounter.viewmodel.InitiativeEntry
import dev.questweaver.feature.encounter.viewmodel.TurnPhase
import dev.questweaver.feature.encounter.viewmodel.ActionOption
import dev.questweaver.feature.encounter.viewmodel.Condition
import dev.questweaver.feature.map.ui.GridPos
import dev.questweaver.feature.map.ui.MapState

/**
 * Derives encounter state from event sequence for event sourcing.
 * Integrates with InitiativeStateBuilder to rebuild complete encounter state.
 */
@Suppress("TooManyFunctions", "UnusedPrivateProperty")
class EncounterStateBuilder(
    private val initiativeStateBuilder: InitiativeStateBuilder
) {
    
    /**
     * Builds encounter state from events.
     *
     * @param events Sequence of game events
     * @return Current encounter state
     */
    fun buildState(events: List<GameEvent>): EncounterState {
        // Initialize empty encounter state
        var state = EncounterState(
            sessionId = 0L,
            roundState = RoundState(
                roundNumber = 0,
                isSurpriseRound = false,
                initiativeOrder = emptyList(),
                activeCreatureId = null,
                turnPhase = TurnPhaseState.Start
            ),
            creatures = emptyMap(),
            mapGrid = MapGridState(0, 0, emptySet(), emptySet()),
            readiedActions = emptyMap(),
            isCompleted = false,
            completionStatus = null
        )
        
        // Replay events in sequence
        for (event in events) {
            state = when (event) {
                is EncounterStarted -> handleEncounterStarted(state, event)
                is RoundStarted -> handleRoundStarted(state, event)
                is TurnStarted -> handleTurnStarted(state, event)
                is AttackResolved -> handleAttackResolved(state, event)
                is MoveCommitted -> handleMoveCommitted(state, event)
                is SpellCast -> handleSpellCast(state, event)
                is CreatureDefeated -> handleCreatureDefeated(state, event)
                is EncounterEnded -> handleEncounterEnded(state, event)
                else -> state // Ignore other event types
            }
        }
        
        return state
    }
    
    /**
     * Builds UI state from domain state.
     *
     * @param encounterState Domain encounter state
     * @param creatures Current creature states
     * @param mapState Current map state
     * @return UI state for rendering
     */
    fun buildUiState(
        encounterState: EncounterState,
        creatures: Map<Long, Creature>,
        mapState: MapState?
    ): EncounterUiState {
        // Build creature states from current HP and conditions
        val creatureStates = creatures.mapValues { (id, creature) ->
            CreatureState(
                id = id,
                name = creature.name,
                hpCurrent = creature.hpCurrent,
                hpMax = creature.hpMax,
                ac = creature.ac,
                position = creature.position,
                conditions = emptySet(), // TODO: Track conditions from events
                isPlayerControlled = creature.isPlayerControlled,
                isDefeated = creature.hpCurrent <= 0
            )
        }
        
        // Convert initiative order to UI format
        val initiativeEntries = encounterState.roundState.initiativeOrder.map { creatureId ->
            InitiativeEntry(
                creatureId = creatureId,
                initiativeScore = 0, // TODO: Get from initiative state
                dexterityModifier = 0, // TODO: Get from creature
                rollResult = 0 // TODO: Get from initiative state
            )
        }
        
        // Determine available actions for active creature
        val availableActions = if (encounterState.roundState.activeCreatureId != null) {
            determineAvailableActions(encounterState.roundState.turnPhase)
        } else {
            emptyList()
        }
        
        return EncounterUiState(
            sessionId = encounterState.sessionId,
            isLoading = false,
            error = null,
            roundNumber = encounterState.roundState.roundNumber,
            isSurpriseRound = encounterState.roundState.isSurpriseRound,
            isCompleted = encounterState.isCompleted,
            completionStatus = encounterState.completionStatus,
            initiativeOrder = initiativeEntries,
            activeCreatureId = encounterState.roundState.activeCreatureId,
            turnPhase = mapTurnPhase(encounterState.roundState.turnPhase),
            creatures = creatureStates,
            mapState = mapState,
            availableActions = availableActions,
            canUndo = false, // Set by ViewModel
            canRedo = false, // Set by ViewModel
            lastActionResult = null,
            pendingChoice = null
        )
    }
    
    // ========== Event Handlers ==========
    
    @Suppress("UnusedParameter")
    private fun handleEncounterStarted(state: EncounterState, event: EncounterStarted): EncounterState {
        // TODO: Extract creatures and initiative from event
        // TODO: Set up initial map grid
        return state.copy(
            sessionId = event.sessionId,
            roundState = state.roundState.copy(
                roundNumber = 1,
                isSurpriseRound = false // TODO: Get from event
            )
        )
    }
    
    @Suppress("UnusedParameter")
    private fun handleRoundStarted(state: EncounterState, event: RoundStarted): EncounterState {
        return state.copy(
            roundState = state.roundState.copy(
                roundNumber = state.roundState.roundNumber + 1
            )
        )
    }
    
    private fun handleTurnStarted(state: EncounterState, event: TurnStarted): EncounterState {
        return state.copy(
            roundState = state.roundState.copy(
                activeCreatureId = event.creatureId,
                turnPhase = TurnPhaseState.Start
            )
        )
    }
    
    @Suppress("UnusedParameter")
    private fun handleAttackResolved(state: EncounterState, event: AttackResolved): EncounterState {
        // TODO: Update creature HP based on damage
        return state
    }
    
    @Suppress("UnusedParameter")
    private fun handleMoveCommitted(state: EncounterState, event: MoveCommitted): EncounterState {
        // TODO: Update creature position
        return state
    }
    
    @Suppress("UnusedParameter")
    private fun handleSpellCast(state: EncounterState, event: SpellCast): EncounterState {
        // TODO: Update spell slots and apply effects
        return state
    }
    
    @Suppress("UnusedParameter")
    private fun handleCreatureDefeated(state: EncounterState, event: CreatureDefeated): EncounterState {
        // TODO: Mark creature as defeated
        return state
    }
    
    private fun handleEncounterEnded(
        state: EncounterState,
        event: EncounterEnded
    ): EncounterState {
        val completionStatus = when (event.status) {
            dev.questweaver.domain.values.EncounterStatus.VICTORY ->
                dev.questweaver.feature.encounter.viewmodel.CompletionStatus.Victory
            dev.questweaver.domain.values.EncounterStatus.DEFEAT ->
                dev.questweaver.feature.encounter.viewmodel.CompletionStatus.Defeat
            dev.questweaver.domain.values.EncounterStatus.FLED ->
                dev.questweaver.feature.encounter.viewmodel.CompletionStatus.Fled
            dev.questweaver.domain.values.EncounterStatus.IN_PROGRESS -> null
        }
        return state.copy(
            isCompleted = true,
            completionStatus = completionStatus
        )
    }
    
    // ========== Helper Methods ==========
    
    @Suppress("UnusedParameter")
    private fun determineAvailableActions(turnPhase: TurnPhaseState): List<ActionOption> {
        // TODO: Implement based on turn phase and creature capabilities
        return emptyList()
    }
    
    private fun mapTurnPhase(phase: TurnPhaseState): TurnPhase {
        return when (phase) {
            TurnPhaseState.Start -> TurnPhase.Start
            TurnPhaseState.Action -> TurnPhase.Action
            TurnPhaseState.BonusAction -> TurnPhase.BonusAction
            TurnPhaseState.Movement -> TurnPhase.Movement
            TurnPhaseState.Reaction -> TurnPhase.Reaction
            TurnPhaseState.End -> TurnPhase.End
        }
    }
}
