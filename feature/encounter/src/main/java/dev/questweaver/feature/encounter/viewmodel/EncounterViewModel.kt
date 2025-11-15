package dev.questweaver.feature.encounter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.feature.encounter.state.CompletionDetector
import dev.questweaver.feature.encounter.state.EncounterStateBuilder
import dev.questweaver.feature.encounter.state.UndoRedoManager
import dev.questweaver.feature.encounter.usecases.AdvanceTurn
import dev.questweaver.feature.encounter.usecases.InitializeEncounter
import dev.questweaver.feature.encounter.usecases.ProcessPlayerAction
import dev.questweaver.feature.encounter.usecases.ActionContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing encounter state using MVI pattern.
 * Processes user intents, manages state through event sourcing, and exposes UI state via StateFlow.
 *
 * Follows Clean Architecture principles with dependency injection for testability.
 */
@Suppress("TooManyFunctions", "LongParameterList")
class EncounterViewModel(
    private val initializeEncounter: InitializeEncounter,
    private val processPlayerAction: ProcessPlayerAction,
    private val advanceTurn: AdvanceTurn,
    private val eventRepository: EventRepository,
    private val stateBuilder: EncounterStateBuilder,
    private val completionDetector: CompletionDetector,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {
    
    companion object {
        private const val ERROR_NO_ACTIVE_CREATURE = "No active creature"
        private const val ERROR_NO_ACTIVE_SESSION = "No active session"
        private const val ERROR_NO_PENDING_CHOICE = "No pending choice"
    }
    
    // Private mutable state
    private val _state = MutableStateFlow(EncounterUiState())
    
    // Public read-only state exposed to UI
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    /**
     * Handles user intents using exhaustive when expression.
     * Launches coroutines in viewModelScope for async operations.
     *
     * @param intent The user intent to process
     */
    fun handle(intent: EncounterIntent) {
        viewModelScope.launch {
            when (intent) {
                is EncounterIntent.StartEncounter -> handleStartEncounter(intent)
                is EncounterIntent.EndTurn -> handleEndTurn()
                is EncounterIntent.Attack -> handleAttack(intent)
                is EncounterIntent.MoveTo -> handleMoveTo(intent)
                is EncounterIntent.CastSpell -> handleCastSpell(intent)
                is EncounterIntent.Dodge -> handleDodge()
                is EncounterIntent.Disengage -> handleDisengage()
                is EncounterIntent.Help -> handleHelp(intent)
                is EncounterIntent.Ready -> handleReady(intent)
                is EncounterIntent.Undo -> handleUndo()
                is EncounterIntent.Redo -> handleRedo()
                is EncounterIntent.ResolveChoice -> handleResolveChoice(intent)
            }
        }
    }
    
    /**
     * Loads an encounter from the event log.
     * Rebuilds state from events and updates UI.
     *
     * @param sessionId The session ID to load
     */
    suspend fun loadEncounter(sessionId: Long) {
        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Load events from repository
            val events = eventRepository.forSession(sessionId)
            
            // Rebuild state from events
            val encounterState = stateBuilder.buildState(events)
            
            // Build creatures map from state
            // TODO: Extract creatures from encounter state properly
            val creatures = emptyMap<Long, Creature>()
            
            // Build UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures,
                mapState = null
            ).copy(
                isLoading = false,
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
        } catch (e: IllegalStateException) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to load encounter: ${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Invalid encounter data: ${e.message}"
            )
        }
    }
    
    // ========== Intent Handlers ==========
    
    /**
     * Handles StartEncounter intent.
     * Initializes encounter, persists event, and updates UI state.
     */
    private suspend fun handleStartEncounter(intent: EncounterIntent.StartEncounter) {
        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Generate session ID (in real implementation, this would come from a session manager)
            val sessionId = System.currentTimeMillis()
            
            // Call InitializeEncounter use case
            val encounterStartedEvent = initializeEncounter(
                sessionId = sessionId,
                creatures = intent.creatures,
                surprisedCreatures = intent.surprisedCreatures,
                mapGrid = intent.mapGrid
            )
            
            // Persist event to repository
            eventRepository.append(encounterStartedEvent)
            
            // Rebuild state from events
            val events = eventRepository.forSession(sessionId)
            val encounterState = stateBuilder.buildState(events)
            
            // Build creatures map
            val creatures = intent.creatures.associateBy { it.id }
            
            // Build and update UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures,
                mapState = null
            ).copy(
                isLoading = false,
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
        } catch (e: IllegalStateException) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to start encounter: ${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Invalid encounter configuration: ${e.message}"
            )
        }
    }
    
    /**
     * Handles Attack intent.
     * Processes attack action and updates state.
     */
    private suspend fun handleAttack(intent: EncounterIntent.Attack) {
        handleCombatAction(
            CombatAction(
                type = "attack",
                targetId = intent.targetId
            )
        )
    }
    
    /**
     * Handles MoveTo intent.
     * Processes movement action and updates state.
     */
    @Suppress("UnusedParameter")
    private suspend fun handleMoveTo(intent: EncounterIntent.MoveTo) {
        // TODO: Use intent.path for actual movement validation
        handleCombatAction(
            CombatAction(
                type = "move",
                targetId = null
            )
        )
    }
    
    /**
     * Handles CastSpell intent.
     * Processes spell casting action and updates state.
     */
    private suspend fun handleCastSpell(intent: EncounterIntent.CastSpell) {
        handleCombatAction(
            CombatAction(
                type = "cast_spell",
                targetId = intent.targets.firstOrNull()
            )
        )
    }
    
    /**
     * Handles Dodge intent.
     * Processes dodge action and updates state.
     */
    private suspend fun handleDodge() {
        handleCombatAction(
            CombatAction(
                type = "dodge",
                targetId = null
            )
        )
    }
    
    /**
     * Handles Disengage intent.
     * Processes disengage action and updates state.
     */
    private suspend fun handleDisengage() {
        handleCombatAction(
            CombatAction(
                type = "disengage",
                targetId = null
            )
        )
    }
    
    /**
     * Handles Help intent.
     * Processes help action and updates state.
     */
    private suspend fun handleHelp(intent: EncounterIntent.Help) {
        handleCombatAction(
            CombatAction(
                type = "help",
                targetId = intent.targetId
            )
        )
    }
    
    /**
     * Handles Ready intent.
     * Processes ready action and updates state.
     */
    private suspend fun handleReady(intent: EncounterIntent.Ready) {
        // Ready action stores the action for later trigger
        // For now, just process it immediately
        handleCombatAction(intent.action)
    }
    
    /**
     * Common handler for all combat actions.
     * Calls ProcessPlayerAction use case and handles results.
     */
    private suspend fun handleCombatAction(action: CombatAction) {
        try {
            val currentState = _state.value
            
            // Ensure we have an active creature
            val activeCreatureId = currentState.activeCreatureId
                ?: return updateError(ERROR_NO_ACTIVE_CREATURE)
            
            val sessionId = currentState.sessionId
                ?: return updateError(ERROR_NO_ACTIVE_SESSION)
            
            // Build action context
            val context = ActionContext(
                sessionId = sessionId,
                activeCreatureId = activeCreatureId,
                roundNumber = currentState.roundNumber
            )
            
            // Process action
            val result = processPlayerAction(action, context)
            
            // Handle result
            when (result) {
                is ActionResult.Success -> {
                    // Action succeeded - state already updated via events
                    rebuildStateFromEvents(sessionId)
                    _state.value = _state.value.copy(
                        lastActionResult = result,
                        pendingChoice = null
                    )
                }
                is ActionResult.Failure -> {
                    // Action failed - update error state
                    _state.value = _state.value.copy(
                        error = result.reason,
                        lastActionResult = result
                    )
                }
                is ActionResult.RequiresChoice -> {
                    // Action requires user choice
                    _state.value = _state.value.copy(
                        pendingChoice = result.choice,
                        lastActionResult = result
                    )
                }
            }
        } catch (e: IllegalStateException) {
            updateError("Action failed: ${e.message}")
        } catch (e: IllegalArgumentException) {
            updateError("Invalid action: ${e.message}")
        }
    }
    
    /**
     * Handles EndTurn intent.
     * Advances turn, checks for completion, and updates state.
     */
    private suspend fun handleEndTurn() {
        try {
            val currentState = _state.value
            val sessionId = currentState.sessionId
                ?: return updateError(ERROR_NO_ACTIVE_SESSION)
            
            // Load current encounter state
            val events = eventRepository.forSession(sessionId)
            val encounterState = stateBuilder.buildState(events)
            
            // Call AdvanceTurn use case
            val turnEvents = advanceTurn(encounterState.roundState)
            
            // Persist generated events
            eventRepository.appendAll(turnEvents)
            
            // Check for creature defeat
            // TODO: Implement creature defeat detection and removal from initiative
            
            // Rebuild state
            rebuildStateFromEvents(sessionId)
            
            // Check for encounter completion
            val creatures = currentState.creatures.mapValues { (id, creatureState) ->
                Creature(
                    id = id,
                    name = creatureState.name,
                    hpCurrent = creatureState.hpCurrent,
                    hpMax = creatureState.hpMax,
                    ac = creatureState.ac,
                    position = creatureState.position,
                    isPlayerControlled = creatureState.isPlayerControlled
                )
            }
            
            val completionStatus = completionDetector.checkCompletion(creatures)
            
            if (completionStatus != null) {
                // Encounter is complete - calculate rewards and generate completion event
                completionDetector.calculateRewards(creatures, completionStatus)
                
                // TODO: Generate EncounterCompleted event with rewards
                // For now, just update UI state
                _state.value = _state.value.copy(
                    isCompleted = true,
                    completionStatus = completionStatus
                )
            }
        } catch (e: IllegalStateException) {
            updateError("Failed to end turn: ${e.message}")
        } catch (e: IllegalArgumentException) {
            updateError("Invalid turn state: ${e.message}")
        }
    }
    
    /**
     * Handles Undo intent.
     * Removes last event and rebuilds state.
     */
    private suspend fun handleUndo() {
        try {
            val sessionId = _state.value.sessionId
                ?: return updateError(ERROR_NO_ACTIVE_SESSION)
            
            // Call UndoRedoManager
            val updatedEvents = undoRedoManager.undo(sessionId)
            
            // Rebuild state from updated events
            val encounterState = stateBuilder.buildState(updatedEvents)
            
            // TODO: Extract creatures properly
            val creatures = emptyMap<Long, Creature>()
            
            // Update UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures,
                mapState = _state.value.mapState
            ).copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
        } catch (e: IllegalStateException) {
            updateError("Failed to undo: ${e.message}")
        } catch (e: IllegalArgumentException) {
            updateError("Cannot undo: ${e.message}")
        }
    }
    
    /**
     * Handles Redo intent.
     * Restores last undone event and rebuilds state.
     */
    private suspend fun handleRedo() {
        try {
            val sessionId = _state.value.sessionId
                ?: return updateError("No active session")
            
            // Call UndoRedoManager
            val updatedEvents = undoRedoManager.redo(sessionId)
            
            // Rebuild state from updated events
            val encounterState = stateBuilder.buildState(updatedEvents)
            
            // TODO: Extract creatures properly
            val creatures = emptyMap<Long, Creature>()
            
            // Update UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures,
                mapState = _state.value.mapState
            ).copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
        } catch (e: IllegalStateException) {
            updateError("Failed to redo: ${e.message}")
        } catch (e: IllegalArgumentException) {
            updateError("Cannot redo: ${e.message}")
        }
    }
    
    /**
     * Handles ResolveChoice intent.
     * Resolves pending choice and processes the selected action.
     */
    private suspend fun handleResolveChoice(intent: EncounterIntent.ResolveChoice) {
        try {
            // Get pending choice from UI state
            _state.value.pendingChoice
                ?: return updateError(ERROR_NO_PENDING_CHOICE)
            
            // Create action with selected option
            val action = CombatAction(
                type = intent.selectedOption.id,
                targetId = null
            )
            
            // Clear pending choice
            _state.value = _state.value.copy(pendingChoice = null)
            
            // Process the resolved action
            handleCombatAction(action)
        } catch (e: IllegalStateException) {
            updateError("Failed to resolve choice: ${e.message}")
        } catch (e: IllegalArgumentException) {
            updateError("Invalid choice: ${e.message}")
        }
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Rebuilds state from events for the given session.
     */
    private suspend fun rebuildStateFromEvents(sessionId: Long) {
        val events = eventRepository.forSession(sessionId)
        val encounterState = stateBuilder.buildState(events)
        
        // TODO: Extract creatures properly from encounter state
        val creatures = emptyMap<Long, Creature>()
        
        val uiState = stateBuilder.buildUiState(
            encounterState = encounterState,
            creatures = creatures,
            mapState = _state.value.mapState
        ).copy(
            canUndo = undoRedoManager.canUndo(),
            canRedo = undoRedoManager.canRedo()
        )
        
        _state.value = uiState
    }
    
    /**
     * Updates error state.
     */
    private fun updateError(message: String) {
        _state.value = _state.value.copy(error = message)
    }
    
    /**
     * Cleans up resources when ViewModel is cleared.
     * Cancels coroutines and marks encounter as inactive.
     */
    override fun onCleared() {
        super.onCleared()
        
        // Coroutines are automatically cancelled by viewModelScope
        
        // TODO: Mark encounter as inactive in database
        // TODO: Clear any temporary state or cached data
    }
}
