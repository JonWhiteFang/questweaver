# Encounter State Management Design

## Overview

The Encounter State Management system is the Android-layer ViewModel that orchestrates tactical combat encounters using the MVI (Model-View-Intent) pattern. It integrates Initiative & Turn Management, Combat Action Processing, and event sourcing to provide a reactive interface for the UI. The system manages the complete encounter lifecycle from initialization through completion, with full state persistence and replay capabilities.

**Key Design Principles:**
- MVI pattern with unidirectional data flow
- Event-sourced state management (all state derived from events)
- Reactive UI updates via StateFlow
- Deterministic state replay from event log
- Clean separation between domain logic and UI concerns
- Comprehensive error handling and user feedback

## Architecture

### Module Location
`feature/encounter/src/main/kotlin/dev/questweaver/feature/encounter/`

### Package Structure
```
feature/encounter/
├── viewmodel/
│   ├── EncounterViewModel.kt      # Main MVI ViewModel
│   ├── EncounterUiState.kt        # Immutable UI state
│   └── EncounterIntent.kt         # Sealed intent interface
├── state/
│   ├── EncounterStateBuilder.kt   # Derives state from events
│   ├── CompletionDetector.kt      # Victory/defeat detection
│   └── UndoRedoManager.kt         # Undo/redo functionality
├── usecases/
│   ├── InitializeEncounter.kt     # Encounter setup use case
│   ├── ProcessPlayerAction.kt     # Player action processing
│   └── AdvanceTurn.kt             # Turn progression use case
└── di/
    └── EncounterModule.kt         # Koin DI module
```

### Dependencies
- `core:domain` - Entity definitions, GameEvent, repositories
- `core:rules` - Initiative system, action processor
- `10-initiative-turns` - Turn management
- `11-combat-actions` - Action processing
- `feature:map` - Map state integration
- `androidx.lifecycle:lifecycle-viewmodel-ktx` - ViewModel
- `kotlinx.coroutines` - Coroutines and Flow

## Components and Interfaces

### 1. EncounterViewModel

Main MVI ViewModel that manages encounter state and processes intents.

```kotlin
class EncounterViewModel(
    private val initializeEncounter: InitializeEncounter,
    private val processPlayerAction: ProcessPlayerAction,
    private val advanceTurn: AdvanceTurn,
    private val eventRepository: EventRepository,
    private val stateBuilder: EncounterStateBuilder,
    private val completionDetector: CompletionDetector,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    /**
     * Handles user intents.
     *
     * @param intent The intent to process
     */
    fun handle(intent: EncounterIntent)
    
    /**
     * Loads an encounter from the event log.
     *
     * @param sessionId The session ID to load
     */
    suspend fun loadEncounter(sessionId: Long)
}
```

**Intent Processing Algorithm:**
1. Receive intent through handle() method
2. Validate intent is legal in current state
3. Execute appropriate use case
4. Collect generated events
5. Persist events to repository
6. Rebuild state from events
7. Check for encounter completion
8. Update UI state via StateFlow
9. Handle errors and update error state

### 2. EncounterUiState

Immutable data class representing complete UI state.

```kotlin
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

enum class CompletionStatus {
    Victory,
    Defeat,
    Fled
}

data class ActionChoice(
    val prompt: String,
    val options: List<ActionOption>
)
```

### 3. EncounterIntent

Sealed interface for all user actions.

```kotlin
sealed interface EncounterIntent {
    // Encounter lifecycle
    data class StartEncounter(
        val creatures: List<Creature>,
        val surprisedCreatures: Set<Long>,
        val mapGrid: MapGrid
    ) : EncounterIntent
    
    object EndTurn : EncounterIntent
    
    // Combat actions
    data class Attack(
        val targetId: Long,
        val weaponId: Long?
    ) : EncounterIntent
    
    data class MoveTo(
        val path: List<GridPos>
    ) : EncounterIntent
    
    data class CastSpell(
        val spellId: Long,
        val targets: List<Long>,
        val spellLevel: Int
    ) : EncounterIntent
    
    object Dodge : EncounterIntent
    object Disengage : EncounterIntent
    
    data class Help(
        val targetId: Long,
        val helpType: HelpType
    ) : EncounterIntent
    
    data class Ready(
        val action: CombatAction,
        val trigger: String
    ) : EncounterIntent
    
    // Undo/redo
    object Undo : EncounterIntent
    object Redo : EncounterIntent
    
    // Action choice resolution
    data class ResolveChoice(
        val selectedOption: ActionOption
    ) : EncounterIntent
}
```

### 4. EncounterStateBuilder

Derives encounter state from event sequence.

```kotlin
class EncounterStateBuilder(
    private val initiativeStateBuilder: InitiativeStateBuilder
) {
    /**
     * Builds encounter state from events.
     *
     * @param events Sequence of game events
     * @return Current encounter state
     */
    fun buildState(events: List<GameEvent>): EncounterState
    
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
    ): EncounterUiState
}
```

**State Building Algorithm:**
1. Initialize empty encounter state
2. Replay events in sequence:
   - EncounterStarted: Set initial creatures and initiative
   - RoundStarted: Increment round number
   - TurnStarted: Set active creature
   - AttackResolved: Update creature HP
   - MoveCommitted: Update creature position
   - SpellCast: Update spell slots and HP
   - CreatureDefeated: Mark creature as defeated
   - EncounterCompleted: Set completion status
3. Derive initiative state using InitiativeStateBuilder
4. Build creature states from current HP and conditions
5. Determine available actions for active creature
6. Return complete encounter state

### 5. CompletionDetector

Detects victory and defeat conditions.

```kotlin
class CompletionDetector {
    /**
     * Checks if encounter is complete.
     *
     * @param creatures Current creature states
     * @return CompletionStatus if complete, null otherwise
     */
    fun checkCompletion(
        creatures: Map<Long, Creature>
    ): CompletionStatus?
    
    /**
     * Calculates rewards for encounter completion.
     *
     * @param creatures Defeated creatures
     * @param completionStatus How encounter ended
     * @return Rewards (XP, loot)
     */
    fun calculateRewards(
        creatures: Map<Long, Creature>,
        completionStatus: CompletionStatus
    ): EncounterRewards
}
```

**Completion Detection Algorithm:**
1. Separate creatures into player-controlled and enemies
2. Check if all enemies are defeated or fled → Victory
3. Check if all player characters are defeated → Defeat
4. If neither condition met → null (encounter continues)

### 6. UndoRedoManager

Manages undo/redo functionality.

```kotlin
class UndoRedoManager(
    private val eventRepository: EventRepository
) {
    private val undoStack = mutableListOf<GameEvent>()
    private val redoStack = mutableListOf<GameEvent>()
    
    /**
     * Undoes the last action.
     *
     * @param sessionId Current session ID
     * @return Updated event list after undo
     */
    suspend fun undo(sessionId: Long): List<GameEvent>
    
    /**
     * Redoes the last undone action.
     *
     * @param sessionId Current session ID
     * @return Updated event list after redo
     */
    suspend fun redo(sessionId: Long): List<GameEvent>
    
    /**
     * Checks if undo is available.
     */
    fun canUndo(): Boolean
    
    /**
     * Checks if redo is available.
     */
    fun canRedo(): Boolean
    
    /**
     * Clears redo stack when new action is taken.
     */
    fun clearRedo()
}
```

**Undo Algorithm:**
1. Pop most recent event from event log
2. Push event to undo stack
3. Clear redo stack
4. Replay remaining events to rebuild state
5. Return updated event list

**Redo Algorithm:**
1. Pop event from undo stack
2. Append event back to event log
3. Replay all events to rebuild state
4. Return updated event list

### 7. Use Cases

#### InitializeEncounter

```kotlin
class InitializeEncounter(
    private val initiativeRoller: InitiativeRoller,
    private val surpriseHandler: SurpriseHandler
) {
    suspend operator fun invoke(
        sessionId: Long,
        creatures: List<Creature>,
        surprisedCreatures: Set<Long>,
        mapGrid: MapGrid
    ): EncounterStarted
}
```

**Algorithm:**
1. Roll initiative for all creatures
2. Sort by initiative score
3. Check for surprise round
4. Generate EncounterStarted event
5. Return event

#### ProcessPlayerAction

```kotlin
class ProcessPlayerAction(
    private val actionProcessor: ActionProcessor,
    private val turnPhaseManager: TurnPhaseManager
) {
    suspend operator fun invoke(
        action: CombatAction,
        context: ActionContext
    ): ActionResult
}
```

**Algorithm:**
1. Build action context from current state
2. Process action using ActionProcessor
3. If successful, update turn phase
4. Return action result with events

#### AdvanceTurn

```kotlin
class AdvanceTurn(
    private val initiativeTracker: InitiativeTracker,
    private val turnPhaseManager: TurnPhaseManager
) {
    suspend operator fun invoke(
        currentState: RoundState
    ): List<GameEvent>
}
```

**Algorithm:**
1. End current creature's turn
2. Advance to next creature using InitiativeTracker
3. Start new creature's turn
4. Generate TurnEnded and TurnStarted events
5. If round wrapped, generate RoundStarted event
6. Return list of events

## Data Models

### Domain State

```kotlin
data class EncounterState(
    val sessionId: Long,
    val roundState: RoundState,
    val creatures: Map<Long, Creature>,
    val mapGrid: MapGrid,
    val readiedActions: Map<Long, ReadiedAction>,
    val isCompleted: Boolean,
    val completionStatus: CompletionStatus?
)

data class EncounterRewards(
    val xpAwarded: Int,
    val loot: List<LootItem>
)

data class LootItem(
    val id: Long,
    val name: String,
    val quantity: Int
)
```

## Error Handling

```kotlin
sealed interface EncounterError {
    data class InitializationFailed(val reason: String) : EncounterError
    data class ActionFailed(val reason: String) : EncounterError
    data class StateCorrupted(val reason: String) : EncounterError
    data class LoadFailed(val sessionId: Long, val reason: String) : EncounterError
}
```

**Error Handling Strategy:**
- Catch exceptions in ViewModel
- Convert to user-friendly error messages
- Update UI state with error
- Log detailed error for debugging
- Prevent state corruption by rolling back on error

## Testing Strategy

### Unit Tests (kotest)

**Coverage Target:** 80%+

**Test Categories:**

1. **ViewModel Intent Handling Tests**
   - Test each intent updates state correctly
   - Test invalid intents rejected with error
   - Test state flow emits updated state
   - Test coroutines cancelled on ViewModel clear

2. **State Building Tests**
   - Test state derived from events matches original
   - Test event replay produces identical state
   - Test all event types handled exhaustively
   - Test UI state built correctly from domain state

3. **Completion Detection Tests**
   - Test victory detected when all enemies defeated
   - Test defeat detected when all PCs defeated
   - Test encounter continues when neither condition met
   - Test rewards calculated correctly

4. **Undo/Redo Tests**
   - Test undo removes last event and rebuilds state
   - Test redo restores undone event
   - Test undo stack limited to 10 actions
   - Test redo cleared when new action taken

5. **Use Case Tests**
   - Test InitializeEncounter rolls initiative correctly
   - Test ProcessPlayerAction validates and executes actions
   - Test AdvanceTurn progresses to next creature

6. **Integration Tests**
   - Test complete encounter flow from start to completion
   - Test player actions generate events and update state
   - Test turn progression through multiple rounds
   - Test encounter completion triggers rewards

### UI Tests (Compose)

```kotlin
@Test
fun encounterScreen_displaysInitiativeOrder() {
    val state = EncounterUiState(
        initiativeOrder = listOf(
            InitiativeEntry(1L, 18, 2, 20),
            InitiativeEntry(2L, 15, 1, 16)
        )
    )
    
    composeTestRule.setContent {
        EncounterScreen(state = state, onIntent = {})
    }
    
    composeTestRule.onNodeWithText("Initiative: 20").assertExists()
    composeTestRule.onNodeWithText("Initiative: 16").assertExists()
}
```

## Performance Considerations

**Target:** <100ms for state updates, <50ms for event replay

**Optimizations:**
- State building cached between events
- Creature lookup by ID uses Map for O(1) access
- Event replay uses efficient list operations
- StateFlow only emits when state actually changes
- Coroutines use Dispatchers.Default for CPU-intensive work

## Integration Points

### With Initiative & Turn Management (10-initiative-turns)
- Uses InitiativeTracker for turn progression
- Uses TurnPhaseManager for action economy
- Integrates RoundState into EncounterUiState

### With Combat Action Processing (11-combat-actions)
- Uses ActionProcessor for all combat actions
- Builds ActionContext from EncounterUiState
- Persists action events to repository

### With Tactical Map (feature:map)
- Integrates MapState into EncounterUiState
- Provides creature positions for rendering
- Receives movement paths from map UI

### With Event Repository (core:data)
- Persists all events to database
- Loads events for encounter replay
- Observes events for reactive updates

## Design Decisions

### Why MVI Pattern?
MVI provides unidirectional data flow, making state changes predictable and testable. It separates UI concerns from business logic and integrates naturally with Compose.

### Why StateFlow Instead of LiveData?
StateFlow is Kotlin-native, works with coroutines, and provides better type safety. It's the recommended approach for Compose UIs.

### Why Separate State Builder?
Separating state building from the ViewModel improves testability and allows state derivation logic to be reused in other contexts.

### Why Use Cases Instead of Direct Repository Access?
Use cases encapsulate business logic, making the ViewModel thinner and more focused on UI concerns. They're also easier to test in isolation.

### Why Limit Undo Stack to 10?
Limiting the undo stack prevents unbounded memory growth while still providing sufficient undo capability for tactical corrections.

## Future Enhancements (Out of Scope for v1)

- AI turn processing (integrated in spec 14-tactical-ai)
- Narrative generation integration (spec 15-narrative-generation)
- Encounter templates and presets
- Combat log with detailed action history
- Encounter statistics and analytics
- Save/load encounter mid-combat
- Spectator mode for completed encounters
