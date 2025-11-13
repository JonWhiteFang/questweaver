# Requirements Document

## Introduction

The Encounter State Management system is responsible for managing the complete lifecycle of tactical combat encounters. It implements the MVI (Model-View-Intent) pattern with event sourcing to maintain encounter state, coordinate initiative and action processing, and provide a reactive interface for the UI layer. The system must handle encounter creation, state persistence, completion conditions, and full state replay from events while maintaining deterministic behavior.

## Glossary

- **Encounter State**: The complete representation of an ongoing combat encounter including creatures, initiative order, current turn, and round number
- **EncounterViewModel**: The MVI ViewModel that manages encounter state and processes user intents
- **Encounter Intent**: A sealed interface representing user actions (start encounter, take action, end turn, etc.)
- **Encounter UI State**: An immutable data class representing the current state for UI rendering
- **Encounter Lifecycle**: The phases of an encounter (initialization, in progress, completed)
- **State Persistence**: The process of saving encounter state through event sourcing
- **State Replay**: The process of reconstructing encounter state from a sequence of events
- **Victory Condition**: The criteria for the player side winning the encounter (all enemies defeated)
- **Defeat Condition**: The criteria for the player side losing the encounter (all player characters defeated)
- **Encounter Context**: The environmental and situational information for an encounter (location, time, surprise status)
- **Creature Roster**: The list of all creatures participating in the encounter
- **Active Encounter**: An encounter currently in progress that has not reached completion

## Requirements

### Requirement 1

**User Story:** As a game developer, I want the EncounterViewModel to initialize encounters, so that combat can begin with proper setup according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an encounter is created, THE EncounterViewModel SHALL accept a list of creatures, encounter context, and session ID
2. WHEN an encounter is initialized, THE EncounterViewModel SHALL use the Initiative System to roll initiative for all creatures and establish turn order
3. WHEN surprise is indicated in the encounter context, THE EncounterViewModel SHALL create a surprise round before the first normal round
4. WHEN initialization is complete, THE EncounterViewModel SHALL generate an EncounterStarted event containing all creatures, initiative rolls, and encounter context
5. WHEN initialization is complete, THE EncounterViewModel SHALL update the UI state to reflect the first creature's turn

### Requirement 2

**User Story:** As a game developer, I want the EncounterViewModel to implement the MVI pattern, so that state management follows a unidirectional data flow architecture.

#### Acceptance Criteria

1. THE EncounterViewModel SHALL expose a StateFlow of EncounterUiState for reactive UI updates
2. THE EncounterViewModel SHALL accept EncounterIntent instances through a handle() method for all user actions
3. WHEN an intent is received, THE EncounterViewModel SHALL process the intent, update state, and emit the new UI state
4. THE EncounterViewModel SHALL maintain a single immutable EncounterUiState data class containing all UI-relevant state
5. THE EncounterViewModel SHALL use sealed interfaces for EncounterIntent to ensure exhaustive intent handling

### Requirement 3

**User Story:** As a game developer, I want the EncounterViewModel to process combat actions, so that player and AI actions are executed and state is updated accordingly.

#### Acceptance Criteria

1. WHEN an AttackIntent is received, THE EncounterViewModel SHALL use the Action Processor to validate and execute the attack
2. WHEN a MoveIntent is received, THE EncounterViewModel SHALL use the Action Processor to validate and execute the movement
3. WHEN a SpellIntent is received, THE EncounterViewModel SHALL use the Action Processor to validate and execute the spell casting
4. WHEN an action succeeds, THE EncounterViewModel SHALL persist the generated events and update the UI state to reflect the outcome
5. WHEN an action fails validation, THE EncounterViewModel SHALL update the UI state with an error message and not modify encounter state

### Requirement 4

**User Story:** As a game developer, I want the EncounterViewModel to manage turn progression, so that combat advances through turns and rounds according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an EndTurnIntent is received, THE EncounterViewModel SHALL use the Initiative System to advance to the next creature in turn order
2. WHEN a turn ends, THE EncounterViewModel SHALL generate a TurnEnded event and update the UI state to reflect the new active creature
3. WHEN the last creature in a round completes their turn, THE EncounterViewModel SHALL increment the round counter and generate a RoundStarted event
4. WHEN a new round begins, THE EncounterViewModel SHALL reset per-round resources (reactions, movement) for all creatures
5. WHEN a creature is defeated during combat, THE EncounterViewModel SHALL remove the creature from the initiative order and check for encounter completion

### Requirement 5

**User Story:** As a game developer, I want the EncounterViewModel to detect encounter completion, so that victory and defeat conditions are recognized and the encounter ends appropriately.

#### Acceptance Criteria

1. WHEN all enemy creatures are defeated or have fled, THE EncounterViewModel SHALL detect victory and generate an EncounterCompleted event with victory status
2. WHEN all player characters are defeated, THE EncounterViewModel SHALL detect defeat and generate an EncounterCompleted event with defeat status
3. WHEN encounter completion is detected, THE EncounterViewModel SHALL update the UI state to reflect the completion status and outcome
4. WHEN an encounter is completed, THE EncounterViewModel SHALL prevent further action intents from being processed
5. WHEN an encounter is completed, THE EncounterViewModel SHALL calculate and include rewards (XP, loot) in the completion event

### Requirement 6

**User Story:** As a game developer, I want the EncounterViewModel to persist state through event sourcing, so that encounters can be saved, loaded, and replayed.

#### Acceptance Criteria

1. WHEN any state change occurs, THE EncounterViewModel SHALL generate immutable events and persist them through the EventRepository
2. WHEN an encounter is loaded, THE EncounterViewModel SHALL replay all events for the session to reconstruct the current state
3. WHEN events are replayed, THE EncounterViewModel SHALL produce identical state to the original encounter given the same event sequence
4. THE EncounterViewModel SHALL derive all state from events without storing mutable state outside the event log
5. THE EncounterViewModel SHALL use seeded random number generation to ensure deterministic replay of random elements

### Requirement 7

**User Story:** As a game developer, I want the EncounterViewModel to provide comprehensive UI state, so that the UI can render all necessary encounter information.

#### Acceptance Criteria

1. THE EncounterUiState SHALL include the current round number, active creature ID, and turn phase information
2. THE EncounterUiState SHALL include the complete initiative order with creature IDs and initiative scores
3. THE EncounterUiState SHALL include current HP, conditions, and position for all creatures
4. THE EncounterUiState SHALL include available actions, movement remaining, and resource status for the active creature
5. THE EncounterUiState SHALL include error messages, loading states, and completion status for UI feedback

### Requirement 8

**User Story:** As a game developer, I want the EncounterViewModel to handle encounter cleanup, so that resources are properly released when an encounter ends.

#### Acceptance Criteria

1. WHEN an encounter is completed, THE EncounterViewModel SHALL mark the encounter as inactive in the database
2. WHEN an encounter is completed, THE EncounterViewModel SHALL clear any temporary state or cached data
3. WHEN the ViewModel is cleared, THE EncounterViewModel SHALL cancel any ongoing coroutines and release resources
4. WHEN an encounter is abandoned (user exits), THE EncounterViewModel SHALL persist the current state for later resumption
5. WHEN an encounter is resumed, THE EncounterViewModel SHALL load and replay events to restore the exact state at abandonment

### Requirement 9

**User Story:** As a game developer, I want the EncounterViewModel to support undo/redo functionality, so that players can correct mistakes during combat.

#### Acceptance Criteria

1. WHEN an UndoIntent is received, THE EncounterViewModel SHALL remove the most recent event from the event log
2. WHEN an undo occurs, THE EncounterViewModel SHALL replay all remaining events to reconstruct state before the undone action
3. WHEN a RedoIntent is received, THE EncounterViewModel SHALL restore the most recently undone event to the event log
4. WHEN a redo occurs, THE EncounterViewModel SHALL replay events including the restored event to reconstruct the state
5. THE EncounterViewModel SHALL maintain an undo stack with a maximum depth of 10 actions to limit memory usage

### Requirement 10

**User Story:** As a game developer, I want the EncounterViewModel to integrate with the tactical map, so that spatial information is synchronized between encounter state and map rendering.

#### Acceptance Criteria

1. WHEN creature positions change, THE EncounterViewModel SHALL update the UI state with new positions for map rendering
2. WHEN movement is validated, THE EncounterViewModel SHALL provide pathfinding information to the map for path visualization
3. WHEN area-of-effect spells are cast, THE EncounterViewModel SHALL provide affected grid positions to the map for highlighting
4. WHEN range is checked for actions, THE EncounterViewModel SHALL provide range information to the map for overlay rendering
5. WHEN the active creature changes, THE EncounterViewModel SHALL update the UI state to highlight the active creature on the map

### Requirement 11

**User Story:** As a game developer, I want the EncounterViewModel to be testable, so that encounter logic can be verified through unit tests.

#### Acceptance Criteria

1. THE EncounterViewModel SHALL accept dependencies through constructor injection for testability
2. THE EncounterViewModel SHALL use interfaces for all external dependencies (repositories, use cases, rules engine)
3. THE EncounterViewModel SHALL expose state through StateFlow for easy verification in tests
4. THE EncounterViewModel SHALL process intents synchronously in tests when using TestCoroutineDispatcher
5. THE EncounterViewModel SHALL be implemented with pure Kotlin logic that can be tested without Android dependencies
