# Implementation Plan

- [x] 1. Set up module structure and dependencies
  - Create `feature/encounter` module with `build.gradle.kts` configured as Android library
  - Add module to `settings.gradle.kts`
  - Create package structure: `viewmodel/`, `state/`, `usecases/`, `di/`
  - Add dependencies on `core:domain`, `core:rules`, `feature:map`, `androidx.lifecycle`, `kotlinx.coroutines`
  - _Requirements: 11.5_

- [x] 2. Implement UI state data classes
  - [x] 2.1 Create CreatureState data class
  - Define fields: id, name, hpCurrent, hpMax, ac, position, conditions, isPlayerControlled, isDefeated
  - _Requirements: 7.3_

  - [x] 2.2 Create CompletionStatus enum
  - Define values: Victory, Defeat, Fled
  - _Requirements: 5.1, 5.2_

  - [x] 2.3 Create ActionChoice data class
  - Define fields: prompt, options
  - _Requirements: 3.5_

  - [x] 2.4 Create EncounterUiState data class
  - Define fields: sessionId, isLoading, error, roundNumber, isSurpriseRound, isCompleted, completionStatus
  - Add fields: initiativeOrder, activeCreatureId, turnPhase, creatures, mapState
  - Add fields: availableActions, canUndo, canRedo, lastActionResult, pendingChoice
  - _Requirements: 2.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 3. Implement intent sealed interface
  - [x] 3.1 Create EncounterIntent sealed interface
  - Define StartEncounter intent with creatures, surprisedCreatures, mapGrid parameters
  - Define EndTurn intent
  - _Requirements: 1.1, 2.2, 4.1_

  - [x] 3.2 Add combat action intents
  - Define Attack intent with targetId, weaponId parameters
  - Define MoveTo intent with path parameter
  - Define CastSpell intent with spellId, targets, spellLevel parameters
  - Define Dodge, Disengage intents
  - Define Help intent with targetId, helpType parameters
  - Define Ready intent with action, trigger parameters
  - _Requirements: 3.1, 3.2, 3.3_

  - [x] 3.3 Add undo/redo and choice intents
  - Define Undo intent
  - Define Redo intent
  - Define ResolveChoice intent with selectedOption parameter
  - _Requirements: 9.1, 9.2, 3.5_

- [x] 4. Implement domain state data classes
  - [x] 4.1 Create EncounterState data class
  - Define fields: sessionId, roundState, creatures, mapGrid, readiedActions, isCompleted, completionStatus
  - _Requirements: 6.1, 6.2_

  - [x] 4.2 Create EncounterRewards data class
  - Define fields: xpAwarded, loot
  - _Requirements: 5.5_

  - [x] 4.3 Create LootItem data class
  - Define fields: id, name, quantity
  - _Requirements: 5.5_

- [x] 5. Implement CompletionDetector
  - [x] 5.1 Create CompletionDetector class
  - Implement checkCompletion() method accepting creatures map
  - Separate creatures into player-controlled and enemies
  - Return Victory if all enemies defeated or fled
  - Return Defeat if all player characters defeated
  - Return null if encounter continues
  - _Requirements: 5.1, 5.2, 5.3_

  - [x] 5.2 Implement calculateRewards() method
  - Calculate XP based on defeated creature challenge ratings
  - Generate loot based on creature loot tables
  - Return EncounterRewards with XP and loot
  - _Requirements: 5.5_

- [x] 6. Implement UndoRedoManager
  - [x] 6.1 Create UndoRedoManager class with EventRepository dependency
  - Initialize undoStack and redoStack as mutable lists
  - _Requirements: 9.1, 9.2_

  - [x] 6.2 Implement undo() method
  - Load current events from repository
  - Remove most recent event from event log
  - Push removed event to undo stack
  - Clear redo stack
  - Return updated event list
  - _Requirements: 9.1, 9.2_

  - [x] 6.3 Implement redo() method
  - Pop event from undo stack
  - Append event back to event log in repository
  - Return updated event list
  - _Requirements: 9.3, 9.4_

  - [x] 6.4 Implement helper methods
  - Implement canUndo() returning true if undo stack not empty
  - Implement canRedo() returning true if redo stack not empty
  - Implement clearRedo() clearing the redo stack
  - Limit undo stack to maximum 10 actions
  - _Requirements: 9.5_

- [x] 7. Implement EncounterStateBuilder
  - [x] 7.1 Create EncounterStateBuilder class with InitiativeStateBuilder dependency
  - _Requirements: 6.3_

  - [x] 7.2 Implement buildState() method
  - Initialize empty EncounterState
  - Replay events in sequence using when expression
  - Handle EncounterStarted: set initial creatures and initiative
  - Handle RoundStarted: increment round number
  - Handle TurnStarted: set active creature
  - Handle AttackResolved: update creature HP
  - Handle MoveCommitted: update creature position
  - Handle SpellCast: update spell slots and HP
  - Handle CreatureDefeated: mark creature as defeated
  - Handle EncounterCompleted: set completion status
  - Derive initiative state using InitiativeStateBuilder
  - Return complete EncounterState
  - _Requirements: 6.1, 6.2, 6.3_

  - [x] 7.3 Implement buildUiState() method
  - Convert EncounterState to EncounterUiState
  - Build CreatureState objects from creatures map
  - Integrate MapState from parameter
  - Determine available actions for active creature
  - Set canUndo and canRedo from UndoRedoManager
  - Return EncounterUiState
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. Implement use cases
  - [x] 8.1 Create InitializeEncounter use case
  - Accept sessionId, creatures, surprisedCreatures, mapGrid parameters
  - Use InitiativeRoller to roll initiative for all creatures
  - Use SurpriseHandler to check for surprise round
  - Generate EncounterStarted event with all data
  - Return event
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 8.2 Create ProcessPlayerAction use case
  - Accept action and context parameters
  - Build ActionContext from current encounter state
  - Use ActionProcessor to validate and execute action
  - If successful, use TurnPhaseManager to update turn phase
  - Return ActionResult with generated events
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 8.3 Create AdvanceTurn use case
  - Accept currentState parameter
  - Generate TurnEnded event for current creature
  - Use InitiativeTracker to advance to next creature
  - Generate TurnStarted event for new creature
  - If round wrapped, generate RoundStarted event
  - Use TurnPhaseManager to initialize turn phase for new creature
  - Return list of events
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 9. Implement EncounterViewModel
  - [x] 9.1 Create EncounterViewModel class with dependencies
  - Accept InitializeEncounter, ProcessPlayerAction, AdvanceTurn use cases
  - Accept EventRepository, EncounterStateBuilder, CompletionDetector, UndoRedoManager
  - Extend ViewModel class
  - _Requirements: 2.1, 11.1_

  - [x] 9.2 Initialize StateFlow for UI state
  - Create private MutableStateFlow with initial EncounterUiState
  - Expose public StateFlow as read-only
  - _Requirements: 2.1, 2.2, 2.3_

  - [x] 9.3 Implement handle() method for intent processing
  - Accept EncounterIntent parameter
  - Use exhaustive when expression for all intent types
  - Launch coroutine in viewModelScope for each intent
  - _Requirements: 2.2, 2.3_

  - [x] 9.4 Implement StartEncounter intent handler
  - Call InitializeEncounter use case
  - Persist EncounterStarted event to repository
  - Rebuild state from events
  - Update UI state via StateFlow
  - Handle errors and update error state
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 9.5 Implement combat action intent handlers
  - Implement Attack intent handler calling ProcessPlayerAction
  - Implement MoveTo intent handler calling ProcessPlayerAction
  - Implement CastSpell intent handler calling ProcessPlayerAction
  - Implement Dodge, Disengage, Help, Ready intent handlers
  - Persist generated events to repository
  - Rebuild state and update UI
  - Handle ActionResult.RequiresChoice by setting pendingChoice in UI state
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 9.6 Implement EndTurn intent handler
  - Call AdvanceTurn use case
  - Persist generated events to repository
  - Check for creature defeat and remove from initiative if needed
  - Check for encounter completion using CompletionDetector
  - If completed, generate EncounterCompleted event with rewards
  - Rebuild state and update UI
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 9.7 Implement Undo/Redo intent handlers
  - Implement Undo intent handler calling UndoRedoManager.undo()
  - Implement Redo intent handler calling UndoRedoManager.redo()
  - Rebuild state from updated event list
  - Update UI state with new state and undo/redo availability
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 9.8 Implement ResolveChoice intent handler
  - Get pending choice from UI state
  - Create action with selected option
  - Call ProcessPlayerAction with resolved action
  - Clear pendingChoice from UI state
  - _Requirements: 3.5_

  - [x] 9.9 Implement loadEncounter() method
  - Load events for sessionId from repository
  - Use EncounterStateBuilder to rebuild state from events
  - Update UI state with loaded state
  - Handle load errors
  - _Requirements: 6.1, 6.2, 6.3, 8.4_

  - [x] 9.10 Implement encounter cleanup
  - Override onCleared() to cancel coroutines
  - Mark encounter as inactive when ViewModel cleared
  - Clear any temporary state or cached data
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 10. Implement error handling
  - [ ] 10.1 Create EncounterError sealed interface
  - Define InitializationFailed with reason
  - Define ActionFailed with reason
  - Define StateCorrupted with reason
  - Define LoadFailed with sessionId and reason
  - _Requirements: 3.5_

  - [ ] 10.2 Add error handling to ViewModel
  - Wrap use case calls in try-catch blocks
  - Convert exceptions to EncounterError instances
  - Update UI state error field with user-friendly message
  - Log detailed error for debugging
  - Prevent state corruption by not persisting events on error
  - _Requirements: 3.5_

- [ ] 11. Implement map integration
  - [ ] 11.1 Add map state synchronization
  - Update creature positions in UI state when MoveCommitted events occur
  - Provide pathfinding information to map for path visualization
  - Provide affected grid positions for AoE spell highlighting
  - Provide range information for action range overlays
  - Highlight active creature on map
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 12. Add Koin dependency injection module
  - Create EncounterModule.kt in di/ package
  - Define viewModel binding for EncounterViewModel with all dependencies
  - Define factory bindings for InitializeEncounter, ProcessPlayerAction, AdvanceTurn use cases
  - Define single binding for EncounterStateBuilder
  - Define single binding for CompletionDetector
  - Define factory binding for UndoRedoManager
  - _Requirements: 11.1, 11.2_

- [ ] 13. Write comprehensive unit tests
  - [ ] 13.1 Write ViewModel intent handling tests
  - Test StartEncounter intent initializes encounter correctly
  - Test Attack intent processes attack and updates state
  - Test MoveTo intent processes movement and updates state
  - Test CastSpell intent processes spell and updates state
  - Test EndTurn intent advances turn correctly
  - Test Undo intent removes last event and rebuilds state
  - Test Redo intent restores undone event
  - Test invalid intents rejected with error
  - Test state flow emits updated state after each intent
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 4.1, 9.1, 9.2_

  - [ ] 13.2 Write state building tests
  - Test state derived from events matches original
  - Test event replay produces identical state
  - Test all event types handled exhaustively
  - Test UI state built correctly from domain state
  - Test creature states include all required fields
  - Test available actions determined correctly for active creature
  - _Requirements: 6.1, 6.2, 6.3, 7.1, 7.2, 7.3_

  - [ ] 13.3 Write completion detection tests
  - Test victory detected when all enemies defeated
  - Test defeat detected when all PCs defeated
  - Test encounter continues when neither condition met
  - Test rewards calculated correctly based on defeated creatures
  - Test XP calculation uses creature challenge ratings
  - _Requirements: 5.1, 5.2, 5.3, 5.5_

  - [ ] 13.4 Write undo/redo tests
  - Test undo removes last event and rebuilds state
  - Test redo restores undone event
  - Test undo stack limited to 10 actions
  - Test redo cleared when new action taken
  - Test canUndo returns true when undo available
  - Test canRedo returns true when redo available
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [ ] 13.5 Write use case tests
  - Test InitializeEncounter rolls initiative correctly
  - Test InitializeEncounter creates surprise round when needed
  - Test ProcessPlayerAction validates actions before execution
  - Test ProcessPlayerAction returns failure for invalid actions
  - Test ProcessPlayerAction returns RequiresChoice when needed
  - Test AdvanceTurn progresses to next creature
  - Test AdvanceTurn wraps to first creature and increments round
  - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.4, 3.5, 4.1, 4.2_

  - [ ] 13.6 Write integration tests
  - Test complete encounter flow from start to victory
  - Test complete encounter flow from start to defeat
  - Test player actions generate events and update state
  - Test turn progression through multiple rounds
  - Test encounter completion triggers rewards calculation
  - Test creature defeat removes from initiative order
  - Test load encounter restores exact state
  - _Requirements: 1.1, 3.1, 4.1, 5.1, 5.2, 5.5, 6.1, 8.4_

  - [ ] 13.7 Write error handling tests
  - Test initialization failure updates error state
  - Test action failure updates error state
  - Test load failure updates error state
  - Test errors don't corrupt state
  - Test errors logged for debugging
  - _Requirements: 3.5_

  - [ ] 13.8 Write map integration tests
  - Test creature positions updated on movement
  - Test pathfinding information provided to map
  - Test AoE spell areas provided to map
  - Test range overlays provided to map
  - Test active creature highlighted on map
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 13.9 Write ViewModel lifecycle tests
  - Test coroutines cancelled on ViewModel clear
  - Test encounter marked inactive on cleanup
  - Test temporary state cleared on cleanup
  - Test encounter can be resumed after abandonment
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [ ] 13.10 Write testability verification tests
  - Test ViewModel accepts dependencies through constructor
  - Test all dependencies are interfaces
  - Test state exposed through StateFlow
  - Test intents processed synchronously in tests with TestCoroutineDispatcher
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
