package dev.questweaver.feature.encounter.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.feature.encounter.state.CompletionDetector
import dev.questweaver.feature.encounter.state.EncounterStateBuilder
import dev.questweaver.feature.encounter.state.UndoRedoManager
import dev.questweaver.feature.encounter.state.MapIntegration
import dev.questweaver.feature.encounter.usecases.AdvanceTurn
import dev.questweaver.feature.encounter.usecases.InitializeEncounter
import dev.questweaver.feature.encounter.usecases.ProcessPlayerAction
import dev.questweaver.feature.encounter.usecases.ActionContext
import dev.questweaver.feature.map.ui.RangeOverlayData
import dev.questweaver.feature.map.ui.AoEOverlayData
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
        private const val TAG = "EncounterViewModel"
        private const val ERROR_NO_ACTIVE_CREATURE = "No active creature"
        private const val ERROR_NO_ACTIVE_SESSION = "No active session"
        private const val ERROR_NO_PENDING_CHOICE = "No pending choice"
        private const val ERROR_INVALID_STATE = "Invalid state"
        private const val ERROR_INVALID_ACTION = "Invalid action"
        private const val ERROR_INVALID_DATA = "Invalid data"
        private const val ERROR_UNKNOWN_STATE = "Unknown state error"
        private const val ERROR_INVALID_CONFIG = "Invalid configuration"
        private const val ERROR_INVALID_TURN_STATE = "Invalid turn state"
        private const val ERROR_FAILED_TO_UNDO = "Failed to undo"
        private const val ERROR_CANNOT_UNDO = "Cannot undo"
        private const val ERROR_FAILED_TO_REDO = "Failed to redo"
        private const val ERROR_CANNOT_REDO = "Cannot redo"
        private const val ERROR_FAILED_TO_RESOLVE = "Failed to resolve choice"
        private const val ERROR_INVALID_CHOICE = "Invalid choice"
        private const val ERROR_FAILED_TO_REBUILD = "Failed to rebuild state"
        private const val DEFAULT_MOVEMENT_FEET = 30
        private const val CONTEXT_LOAD_ENCOUNTER = "loadEncounter"
        private const val CONTEXT_START_ENCOUNTER = "handleStartEncounter"
        private const val CONTEXT_COMBAT_ACTION = "handleCombatAction"
        private const val CONTEXT_END_TURN = "handleEndTurn"
        private const val CONTEXT_UNDO = "handleUndo"
        private const val CONTEXT_REDO = "handleRedo"
        private const val CONTEXT_RESOLVE_CHOICE = "handleResolveChoice"
        private const val CONTEXT_REBUILD_STATE = "rebuildStateFromEvents"
    }
    
    // Private mutable state
    private val _state = MutableStateFlow(EncounterUiState())
    
    // Public read-only state exposed to UI
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    /**
     * Sets the range overlay for the current action.
     * Used by the UI to show valid targets for an action.
     *
     * @param rangeOverlay The range overlay data, or null to clear
     */
    fun setRangeOverlay(rangeOverlay: RangeOverlayData?) {
        _state.value = _state.value.copy(rangeOverlay = rangeOverlay)
    }
    
    /**
     * Sets the AoE overlay for the current spell.
     * Used by the UI to show affected positions for an AoE spell.
     *
     * @param aoeOverlay The AoE overlay data, or null to clear
     */
    fun setAoEOverlay(aoeOverlay: AoEOverlayData?) {
        _state.value = _state.value.copy(aoeOverlay = aoeOverlay)
    }
    
    /**
     * Clears all map overlays (range, AoE, movement path).
     * Used when canceling an action or after completing an action.
     */
    fun clearMapOverlays() {
        _state.value = _state.value.copy(
            rangeOverlay = null,
            aoeOverlay = null,
            movementPath = null
        )
    }
    
    /**
     * Provides movement range overlay for the active creature.
     * Used by the map to visualize reachable positions.
     *
     * @return RangeOverlayData for movement range, or null if no active creature
     */
    @Suppress("ReturnCount")
    fun getMovementRangeOverlay(): RangeOverlayData? {
        val currentState = _state.value
        val activeCreatureId = currentState.activeCreatureId ?: return null
        val activeCreature = currentState.creatures[activeCreatureId] ?: return null
        
        // Calculate remaining movement (simplified - would come from turn state)
        val movementRemaining = DEFAULT_MOVEMENT_FEET
        
        // Get blocked positions from map state
        val blockedPositions = currentState.mapState?.blocked ?: emptySet()
        
        return MapIntegration.buildMovementRangeOverlay(
            origin = activeCreature.position,
            movementRemaining = movementRemaining,
            blockedPositions = blockedPositions
        )
    }
    
    /**
     * Provides weapon range overlay for an attack action.
     * Used by the map to visualize valid attack targets.
     *
     * @param weaponRangeInFeet The weapon's range in feet
     * @return RangeOverlayData for weapon range, or null if no active creature
     */
    @Suppress("ReturnCount")
    fun getWeaponRangeOverlay(weaponRangeInFeet: Int): RangeOverlayData? {
        val currentState = _state.value
        val activeCreatureId = currentState.activeCreatureId ?: return null
        val activeCreature = currentState.creatures[activeCreatureId] ?: return null
        
        return MapIntegration.buildWeaponRangeOverlay(
            origin = activeCreature.position,
            rangeInFeet = weaponRangeInFeet
        )
    }
    
    /**
     * Provides spell range overlay for a spell action.
     * Used by the map to visualize valid spell targets.
     *
     * @param spellRangeInFeet The spell's range in feet
     * @return RangeOverlayData for spell range, or null if no active creature
     */
    @Suppress("ReturnCount")
    fun getSpellRangeOverlay(spellRangeInFeet: Int): RangeOverlayData? {
        val currentState = _state.value
        val activeCreatureId = currentState.activeCreatureId ?: return null
        val activeCreature = currentState.creatures[activeCreatureId] ?: return null
        
        return MapIntegration.buildSpellRangeOverlay(
            origin = activeCreature.position,
            rangeInFeet = spellRangeInFeet
        )
    }
    
    /**
     * Provides AoE overlay for an area-of-effect spell.
     * Used by the map to visualize affected positions.
     *
     * @param template The AoE template (sphere, cube, cone, etc.)
     * @param origin The origin position of the AoE
     * @param radiusInFeet The radius or size of the AoE in feet
     * @return AoEOverlayData for AoE visualization
     */
    fun getAoEOverlay(
        template: dev.questweaver.domain.map.geometry.AoETemplate,
        origin: dev.questweaver.feature.map.ui.GridPos,
        radiusInFeet: Int
    ): AoEOverlayData {
        return MapIntegration.buildAoEOverlay(
            template = template,
            origin = origin,
            radiusInFeet = radiusInFeet
        )
    }
    
    /**
     * Validates a movement path before committing.
     * Used by the map to check if a proposed path is valid.
     *
     * @param path The proposed movement path
     * @return true if the path is valid, false otherwise
     */
    fun validateMovementPath(path: List<dev.questweaver.feature.map.ui.GridPos>): Boolean {
        val currentState = _state.value
        val blockedPositions = currentState.mapState?.blocked ?: emptySet()
        
        return MapIntegration.validateMovementPath(path, blockedPositions)
    }
    
    /**
     * Calculates the movement cost for a path.
     * Used by the map to show remaining movement after a proposed path.
     *
     * @param path The movement path
     * @return Movement cost in feet
     */
    fun calculateMovementCost(path: List<dev.questweaver.feature.map.ui.GridPos>): Int {
        val currentState = _state.value
        val difficultTerrain = currentState.mapState?.difficult ?: emptySet()
        
        return MapIntegration.calculateMovementCost(path, difficultTerrain)
    }
    
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
            
            if (events.isEmpty()) {
                handleError(
                    EncounterError.LoadFailed(sessionId, "No events found for session"),
                    CONTEXT_LOAD_ENCOUNTER
                )
                return
            }
            
            // Rebuild state from events
            val encounterState = stateBuilder.buildState(events)
            
            // Build creatures map from state
            // TODO: Extract creatures from encounter state properly
            val creatures = emptyMap<Long, Creature>()
            
            // Build UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures
            ).copy(
                isLoading = false,
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
            Log.i(TAG, "Successfully loaded encounter for session $sessionId")
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.LoadFailed(sessionId, e.message ?: ERROR_UNKNOWN_STATE),
                CONTEXT_LOAD_ENCOUNTER,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.LoadFailed(sessionId, e.message ?: ERROR_INVALID_DATA),
                CONTEXT_LOAD_ENCOUNTER,
                e
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
            
            // Validate input
            if (intent.creatures.isEmpty()) {
                handleError(
                    EncounterError.InitializationFailed("Cannot start encounter with no creatures"),
                    CONTEXT_START_ENCOUNTER
                )
                return
            }
            
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
                creatures = creatures
            ).copy(
                isLoading = false,
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
            Log.i(TAG, "Successfully started encounter with session $sessionId")
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.InitializationFailed(e.message ?: ERROR_UNKNOWN_STATE),
                CONTEXT_START_ENCOUNTER,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.InitializationFailed(e.message ?: ERROR_INVALID_CONFIG),
                CONTEXT_START_ENCOUNTER,
                e
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
     * Processes movement action and updates state with path visualization.
     */
    private suspend fun handleMoveTo(intent: EncounterIntent.MoveTo) {
        // Validate the path first
        if (!validateMovementPath(intent.path)) {
            handleError(
                EncounterError.ActionFailed("Invalid movement path: blocked positions"),
                "handleMoveTo"
            )
            return
        }
        
        // Set the movement path in UI state for visualization
        _state.value = _state.value.copy(
            movementPath = intent.path
        )
        
        // Process the movement action
        handleCombatAction(
            CombatAction(
                type = "move",
                targetId = null
            )
        )
        
        // Clear the movement path after processing
        _state.value = _state.value.copy(
            movementPath = null
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
                ?: return handleError(
                    EncounterError.ActionFailed(ERROR_NO_ACTIVE_CREATURE),
                    CONTEXT_COMBAT_ACTION
                )
            
            val sessionId = currentState.sessionId
                ?: return handleError(
                    EncounterError.ActionFailed(ERROR_NO_ACTIVE_SESSION),
                    CONTEXT_COMBAT_ACTION
                )
            
            // Build action context and process
            val context = ActionContext(
                sessionId = sessionId,
                activeCreatureId = activeCreatureId,
                roundNumber = currentState.roundNumber
            )
            val result = processPlayerAction(action, context)
            
            // Handle result
            handleActionResult(result, sessionId, action)
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_INVALID_STATE),
                CONTEXT_COMBAT_ACTION,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_INVALID_ACTION),
                CONTEXT_COMBAT_ACTION,
                e
            )
        }
    }
    
    /**
     * Handles the result of a combat action.
     */
    private suspend fun handleActionResult(
        result: ActionResult,
        sessionId: Long,
        action: CombatAction
    ) {
        when (result) {
            is ActionResult.Success -> handleActionSuccess(result, sessionId, action)
            is ActionResult.Failure -> handleActionFailure(result)
            is ActionResult.RequiresChoice -> handleActionChoice(result)
        }
    }
    
    /**
     * Handles successful action result.
     */
    private suspend fun handleActionSuccess(
        result: ActionResult.Success,
        sessionId: Long,
        action: CombatAction
    ) {
        rebuildStateFromEvents(sessionId)
        _state.value = _state.value.copy(
            lastActionResult = result,
            pendingChoice = null,
            error = null
        )
        Log.i(TAG, "Combat action succeeded: ${action.type}")
    }
    
    /**
     * Handles failed action result.
     */
    private fun handleActionFailure(result: ActionResult.Failure) {
        handleError(
            EncounterError.ActionFailed(result.reason),
            CONTEXT_COMBAT_ACTION
        )
        _state.value = _state.value.copy(
            lastActionResult = result
        )
    }
    
    /**
     * Handles action requiring user choice.
     */
    private fun handleActionChoice(result: ActionResult.RequiresChoice) {
        _state.value = _state.value.copy(
            pendingChoice = result.choice,
            lastActionResult = result,
            error = null
        )
        Log.i(TAG, "Combat action requires choice: ${result.choice.prompt}")
    }
    
    /**
     * Handles EndTurn intent.
     * Advances turn, checks for completion, and updates state.
     */
    private suspend fun handleEndTurn() {
        try {
            // Clear any active overlays when ending turn
            clearMapOverlays()
            
            val currentState = _state.value
            val sessionId = currentState.sessionId
                ?: return handleError(
                    EncounterError.ActionFailed(ERROR_NO_ACTIVE_SESSION),
                    CONTEXT_END_TURN
                )
            
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
                    completionStatus = completionStatus,
                    error = null
                )
                Log.i(TAG, "Encounter completed with status: $completionStatus")
            } else {
                Log.i(TAG, "Turn ended successfully")
            }
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.StateCorrupted(e.message ?: ERROR_INVALID_TURN_STATE),
                CONTEXT_END_TURN,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_INVALID_TURN_STATE),
                CONTEXT_END_TURN,
                e
            )
        }
    }
    
    /**
     * Handles Undo intent.
     * Removes last event and rebuilds state.
     */
    private suspend fun handleUndo() {
        try {
            val sessionId = _state.value.sessionId
                ?: return handleError(
                    EncounterError.ActionFailed(ERROR_NO_ACTIVE_SESSION),
                    CONTEXT_UNDO
                )
            
            if (!undoRedoManager.canUndo()) {
                handleError(
                    EncounterError.ActionFailed("No actions to undo"),
                    CONTEXT_UNDO
                )
                return
            }
            
            // Call UndoRedoManager
            val updatedEvents = undoRedoManager.undo(sessionId)
            
            // Rebuild state from updated events
            val encounterState = stateBuilder.buildState(updatedEvents)
            
            // TODO: Extract creatures properly
            val creatures = emptyMap<Long, Creature>()
            
            // Update UI state
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures
            ).copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo(),
                error = null
            )
            
            _state.value = uiState
            Log.i(TAG, "Undo successful")
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.StateCorrupted(e.message ?: ERROR_FAILED_TO_UNDO),
                CONTEXT_UNDO,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_CANNOT_UNDO),
                CONTEXT_UNDO,
                e
            )
        }
    }
    
    /**
     * Handles Redo intent.
     * Restores last undone event and rebuilds state.
     */
    private suspend fun handleRedo() {
        try {
            val sessionId = _state.value.sessionId
                ?: return handleError(
                    EncounterError.ActionFailed(ERROR_NO_ACTIVE_SESSION),
                    CONTEXT_REDO
                )
            
            if (!undoRedoManager.canRedo()) {
                handleError(
                    EncounterError.ActionFailed("No actions to redo"),
                    CONTEXT_REDO
                )
                return
            }
            
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
                canRedo = undoRedoManager.canRedo(),
                error = null
            )
            
            _state.value = uiState
            Log.i(TAG, "Redo successful")
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.StateCorrupted(e.message ?: ERROR_FAILED_TO_REDO),
                CONTEXT_REDO,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_CANNOT_REDO),
                CONTEXT_REDO,
                e
            )
        }
    }
    
    /**
     * Handles ResolveChoice intent.
     * Resolves pending choice and processes the selected action.
     */
    private suspend fun handleResolveChoice(intent: EncounterIntent.ResolveChoice) {
        try {
            // Get pending choice from UI state
            val pendingChoice = _state.value.pendingChoice
            if (pendingChoice == null) {
                handleError(
                    EncounterError.ActionFailed(ERROR_NO_PENDING_CHOICE),
                    CONTEXT_RESOLVE_CHOICE
                )
                return
            }
            
            // Validate selected option is in the available options
            if (!pendingChoice.options.contains(intent.selectedOption)) {
                handleError(
                    EncounterError.ActionFailed("Invalid choice: option not available"),
                    CONTEXT_RESOLVE_CHOICE
                )
                return
            }
            
            // Create action with selected option
            val action = CombatAction(
                type = intent.selectedOption.id,
                targetId = null
            )
            
            // Clear pending choice
            _state.value = _state.value.copy(pendingChoice = null)
            
            // Process the resolved action
            handleCombatAction(action)
            Log.i(TAG, "Choice resolved: ${intent.selectedOption.name}")
        } catch (e: IllegalStateException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_FAILED_TO_RESOLVE),
                CONTEXT_RESOLVE_CHOICE,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.ActionFailed(e.message ?: ERROR_INVALID_CHOICE),
                CONTEXT_RESOLVE_CHOICE,
                e
            )
        }
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Rebuilds state from events for the given session.
     * Does not persist any events - only reads and rebuilds.
     */
    private suspend fun rebuildStateFromEvents(sessionId: Long) {
        try {
            val events = eventRepository.forSession(sessionId)
            val encounterState = stateBuilder.buildState(events)
            
            // TODO: Extract creatures properly from encounter state
            val creatures = emptyMap<Long, Creature>()
            
            val uiState = stateBuilder.buildUiState(
                encounterState = encounterState,
                creatures = creatures
            ).copy(
                canUndo = undoRedoManager.canUndo(),
                canRedo = undoRedoManager.canRedo()
            )
            
            _state.value = uiState
        } catch (e: IllegalStateException) {
            // If rebuild fails, state is corrupted
            handleError(
                EncounterError.StateCorrupted(e.message ?: ERROR_FAILED_TO_REBUILD),
                CONTEXT_REBUILD_STATE,
                e
            )
        } catch (e: IllegalArgumentException) {
            handleError(
                EncounterError.StateCorrupted(e.message ?: ERROR_FAILED_TO_REBUILD),
                CONTEXT_REBUILD_STATE,
                e
            )
        }
    }
    
    /**
     * Handles errors by converting to user-friendly messages and logging.
     * Prevents state corruption by not persisting events on error.
     *
     * @param error The EncounterError to handle
     * @param context The context where the error occurred (method name)
     * @param exception Optional exception for detailed logging
     */
    private fun handleError(
        error: EncounterError,
        context: String,
        exception: Throwable? = null
    ) {
        // Convert to user-friendly message
        val userMessage = error.toUserMessage()
        
        // Log detailed error for debugging
        if (exception != null) {
            Log.e(TAG, "Error in $context: $userMessage", exception)
        } else {
            Log.w(TAG, "Error in $context: $userMessage")
        }
        
        // Update UI state with error message
        // Note: We do NOT persist any events when an error occurs
        // This prevents state corruption
        _state.value = _state.value.copy(
            error = userMessage,
            isLoading = false
        )
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
