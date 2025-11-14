# Implementation Plan

- [x] 1. Set up module structure and core data models





  - Create package structure in `core/rules/initiative/`: root level and `models/` subdirectory
  - Add dependencies on `core:domain` and `04-dice-system` in `core/rules/build.gradle.kts`
  - Verify module compiles with no Android dependencies
  - _Requirements: 8.5_

- [x] 2. Implement immutable state data classes





- [x] 2.1 Create InitiativeEntry data class


  - Define fields: creatureId, roll, modifier, total
  - Implement Comparable interface for sorting (total desc, modifier desc, creatureId asc)
  - _Requirements: 1.1, 1.2, 1.3, 1.4_


- [x] 2.2 Create TurnPhase data class

  - Define fields: creatureId, movementRemaining, actionAvailable, bonusActionAvailable, reactionAvailable
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 2.3 Create TurnState data class


  - Define fields: activeCreatureId, turnPhase, turnIndex
  - _Requirements: 5.1, 5.2, 5.3_


- [x] 2.4 Create RoundState data class

  - Define fields: roundNumber, isSurpriseRound, initiativeOrder, surprisedCreatures, delayedCreatures, currentTurn
  - _Requirements: 4.1, 4.2, 4.3_


- [x] 2.5 Create ActionType enum

  - Define values: Action, BonusAction, Reaction, Movement, FreeAction
  - _Requirements: 3.1_

- [x] 3. Implement event data classes


- [x] 3.1 Create EncounterStarted event


  - Define fields: sessionId, timestamp, initiativeRolls, surprisedCreatures
  - Extend GameEvent sealed interface
  - _Requirements: 1.5, 8.2_


- [x] 3.2 Create RoundStarted event

  - Define fields: sessionId, timestamp, roundNumber
  - _Requirements: 4.2, 8.2_



- [x] 3.3 Create TurnStarted event
  - Define fields: sessionId, timestamp, creatureId, roundNumber, turnIndex

  - _Requirements: 5.1, 8.2_

- [x] 3.4 Create TurnEnded event

  - Define fields: sessionId, timestamp, creatureId, roundNumber
  - _Requirements: 5.2, 8.2_



- [x] 3.5 Create ReactionUsed event
  - Define fields: sessionId, timestamp, creatureId, reactionType, trigger
  - _Requirements: 6.5, 8.2_



- [x] 3.6 Create TurnDelayed event
  - Define fields: sessionId, timestamp, creatureId, originalInitiative

  - _Requirements: 7.5, 8.2_

- [x] 3.7 Create DelayedTurnResumed event

  - Define fields: sessionId, timestamp, creatureId, newInitiative
  - _Requirements: 7.3, 8.2_

- [x] 3.8 Create CreatureAddedToCombat event

  - Define fields: sessionId, timestamp, creatureId, initiativeEntry
  - _Requirements: 4.4, 8.2_

- [x] 3.9 Create CreatureRemovedFromCombat event

  - Define fields: sessionId, timestamp, creatureId, reason
  - _Requirements: 4.5, 8.2_

- [x] 4. Implement InitiativeRoller



- [x] 4.1 Create InitiativeRoller class with DiceRoller dependency

  - Define constructor accepting DiceRoller
  - _Requirements: 1.1, 8.1_


- [x] 4.2 Implement rollInitiative() method
  - Roll d20 using DiceRoller
  - Add Dexterity modifier to roll
  - Create and return InitiativeEntry with creature ID, roll, modifier, and total
  - _Requirements: 1.1_


- [x] 4.3 Implement rollInitiativeForAll() method
  - Call rollInitiative() for each creature in the map
  - Sort resulting InitiativeEntry list using Comparable implementation


  - Return sorted list (highest initiative first)
  - _Requirements: 1.2, 1.3, 1.4_

- [x] 5. Implement SurpriseHandler


- [x] 5.1 Create SurpriseHandler class

  - Implement hasSurpriseRound() method returning true if surprisedCreatures is not empty
  - _Requirements: 2.1_


- [x] 5.2 Implement canActInSurpriseRound() method
  - Return true if creatureId is not in surprisedCreatures set
  - _Requirements: 2.2_


- [x] 5.3 Implement endSurpriseRound() method
  - Return empty set (all creatures no longer surprised)
  - _Requirements: 2.4_

- [x] 6. Implement TurnPhaseManager





- [x] 6.1 Create TurnPhaseManager class


  - Implement startTurn() method creating TurnPhase with all actions available
  - _Requirements: 3.1_

- [x] 6.2 Implement resource consumption methods

  - Implement consumeMovement() reducing movementRemaining
  - Implement consumeAction() setting actionAvailable to false
  - Implement consumeBonusAction() setting bonusActionAvailable to false
  - Implement consumeReaction() setting reactionAvailable to false
  - _Requirements: 3.2, 3.3, 3.4_

- [x] 6.3 Implement restoreReaction() method

  - Return TurnPhase copy with reactionAvailable set to true
  - _Requirements: 6.3_


- [x] 6.4 Implement isActionAvailable() method

  - Check appropriate field based on ActionType parameter
  - Return true if action is available, false otherwise
  - _Requirements: 3.1_

- [x] 7. Implement InitiativeTracker




- [x] 7.1 Create InitiativeTracker class


  - Implement initialize() method creating initial RoundState
  - Set roundNumber to 0 (or 1 if no surprise round)
  - Set isSurpriseRound based on surprisedCreatures
  - Set initiativeOrder from sorted entries
  - Create initial TurnState for first creature
  - _Requirements: 1.5, 2.1_



- [x] 7.2 Implement advanceTurn() method

  - Increment turn index
  - If index exceeds order length, reset to 0 and increment round
  - If surprise round and creature is surprised, skip and recurse
  - Create new TurnState with updated active creature
  - Return updated RoundState
  - _Requirements: 4.1, 5.2, 5.5_




- [x] 7.3 Implement addCreature() method

  - Insert new InitiativeEntry into sorted position in initiative order
  - Adjust turn index if insertion is before current turn
  - Return updated RoundState


  - _Requirements: 4.4_



- [x] 7.4 Implement removeCreature() method
  - Find and remove creature from initiative order
  - If creature is before current turn, decrement turn index
  - If removed creature was active, advance turn


  - Return updated RoundState

  - _Requirements: 4.5_


- [x] 7.5 Implement delayTurn() method
  - Remove creature from current position in initiative order
  - Add creature to delayedCreatures map with original InitiativeEntry


  - Advance turn to next creature
  - Return updated RoundState
  - _Requirements: 7.1, 7.5_

- [x] 7.6 Implement resumeDelayedTurn() method

  - Remove creature from delayedCreatures map
  - Create new InitiativeEntry with new initiative score
  - Insert creature at current position in initiative order
  - Return updated RoundState
  - _Requirements: 7.2, 7.3_

- [x] 8. Implement InitiativeStateBuilder for event sourcing




- [x] 8.1 Create InitiativeStateBuilder class


  - Implement buildState() method accepting list of GameEvents
  - Initialize empty RoundState
  - _Requirements: 8.2, 8.3_


- [x] 8.2 Implement event handlers for state derivation

  - Implement handleEncounterStarted() setting initial state from event
  - Implement handleRoundStarted() incrementing round number
  - Implement handleTurnStarted() updating current turn
  - Implement handleTurnEnded() clearing current turn
  - Implement handleReactionUsed() updating turn phase
  - Implement handleTurnDelayed() moving creature to delayed map
  - Implement handleDelayedTurnResumed() inserting creature back into order
  - Implement handleCreatureAdded() inserting creature into order
  - Implement handleCreatureRemoved() removing creature from order
  - _Requirements: 8.2, 8.3_


- [x] 8.3 Implement exhaustive when expression for event types


  - Handle all initiative-related events
  - Return unchanged state for non-initiative events
  - _Requirements: 8.2, 8.3_

- [x] 9. Implement sealed result types for error handling




- [x] 9.1 Create InitiativeResult sealed interface


  - Define Success data class with generic value
  - Define InvalidState data class with reason string
  - _Requirements: 8.2_


- [x] 9.2 Add validation to InitiativeTracker methods

  - Validate initiative order is not empty
  - Validate active creature exists in order
  - Validate turn index is within bounds
  - Return InvalidState for validation failures
  - _Requirements: 8.2_

- [x] 10. Add Koin dependency injection module





  - Create InitiativeModule.kt in core/rules/di/ package
  - Define factory binding for InitiativeRoller with DiceRoller dependency
  - Define factory binding for InitiativeTracker
  - Define factory binding for TurnPhaseManager
  - Define factory binding for SurpriseHandler
  - Define factory binding for InitiativeStateBuilder
  - _Requirements: 8.1_

- [-] 11. Write comprehensive unit tests


- [x] 11.1 Write InitiativeRoller tests


  - Test initiative = d20 + Dexterity modifier
  - Test multiple creatures sorted correctly (highest first)
  - Test ties broken by Dexterity modifier
  - Test identical scores and modifiers use creature ID as tiebreaker
  - Test deterministic with same seed
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 8.4_


- [x] 11.2 Write InitiativeTracker turn advancement tests

  - Test turn advances to next creature
  - Test last creature wraps to first and increments round
  - Test round counter increments correctly
  - Test turn index resets to 0 at round start
  - _Requirements: 4.1, 5.5_



- [x] 11.3 Write SurpriseHandler tests
  - Test surprise round occurs when creatures are surprised
  - Test surprised creatures cannot act in surprise round
  - Test non-surprised creatures can act in surprise round
  - Test surprise condition removed after surprise round
  - _Requirements: 2.1, 2.2, 2.3, 2.4_


- [x] 11.4 Write TurnPhaseManager tests

  - Test all actions available at turn start
  - Test actions consumed correctly
  - Test cannot consume action twice
  - Test reaction restored at turn start
  - Test movement tracks remaining distance
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.3_

- [x] 11.5 Write InitiativeTracker creature lifecycle tests


  - Test add creature inserts at correct position
  - Test remove creature maintains turn order
  - Test remove active creature advances turn
  - Test remove creature before current turn adjusts index
  - _Requirements: 4.4, 4.5_


- [x] 11.6 Write InitiativeTracker delayed turn tests

  - Test delay removes creature from order
  - Test resume inserts at current position
  - Test delayed creature maintains new initiative
  - Test round end places delayed creature at end
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 11.7 Write InitiativeStateBuilder event sourcing tests


  - Test state derived from events matches original
  - Test replay produces identical state
  - Test all events handled exhaustively
  - Test non-initiative events don't affect state
  - _Requirements: 8.2, 8.3_

- [x] 11.8 Write property-based determinism tests


  - Test initiative with same seed produces same order
  - Test turn advancement eventually returns to first creature
  - Test event replay produces identical state
  - Test initiative order is always sorted correctly
  - _Requirements: 8.1, 8.4_

- [x] 11.9 Write property-based invariant tests

  - Test turn index always within bounds
  - Test active creature always exists in order
  - Test round number never decreases
  - Test movement remaining never negative
  - Test actions can only be consumed once per turn
  - _Requirements: 3.2, 4.1, 5.3_
