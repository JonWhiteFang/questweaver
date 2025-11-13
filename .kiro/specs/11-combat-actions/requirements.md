# Requirements Document

## Introduction

The Combat Action Processing system is responsible for executing and validating combat actions during encounters. It integrates the Combat Rules Engine, Action Validation System, and Initiative & Turn Management to process attacks, movement, spell casting, bonus actions, and reactions. The system must enforce D&D 5e SRD action economy rules, generate appropriate events for state changes, and maintain deterministic behavior for event sourcing.

## Glossary

- **Combat Action**: Any action a creature can take during combat (attack, move, cast spell, dash, dodge, etc.)
- **Action Economy**: The system of actions, bonus actions, reactions, and movement available per turn
- **Attack Action**: Using the action phase to make one or more weapon or spell attacks
- **Movement Action**: Using movement speed to change position on the tactical map
- **Spell Casting**: Using an action, bonus action, or reaction to cast a spell
- **Bonus Action**: A special action that can be taken in addition to the main action on a turn
- **Reaction**: An instantaneous response to a trigger that occurs outside a creature's turn
- **Opportunity Attack**: A reaction triggered when a hostile creature moves out of reach
- **Action Processor**: The component that validates and executes combat actions
- **Action Result**: The outcome of processing an action (success, failure, requires choice)
- **Action Context**: The current state information needed to validate and process an action
- **Resource Consumption**: The depletion of limited resources (spell slots, movement, action phases)

## Requirements

### Requirement 1

**User Story:** As a game developer, I want the Action Processor to handle attack actions, so that creatures can make weapon and spell attacks according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature takes an attack action, THE Action Processor SHALL validate that the creature has an action available and the target is within range
2. WHEN an attack action is validated, THE Action Processor SHALL use the Combat Rules Engine to resolve the attack roll and damage
3. WHEN an attack hits, THE Action Processor SHALL generate an AttackResolved event containing the attack roll, hit status, damage, and damage type
4. WHEN an attack action is completed, THE Action Processor SHALL mark the creature's action phase as consumed for the current turn
5. WHEN a creature has multiple attacks (Extra Attack feature), THE Action Processor SHALL allow multiple attack rolls within a single attack action

### Requirement 2

**User Story:** As a game developer, I want the Action Processor to handle movement actions, so that creatures can change position on the tactical map according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature moves, THE Action Processor SHALL validate that the creature has sufficient movement remaining for the requested path
2. WHEN movement is validated, THE Action Processor SHALL check for difficult terrain and calculate movement cost accordingly
3. WHEN a creature moves out of a threatened square, THE Action Processor SHALL trigger opportunity attacks from hostile creatures with reactions available
4. WHEN movement is completed, THE Action Processor SHALL generate a MoveCommitted event containing the creature ID, path taken, and remaining movement
5. WHEN a creature uses the Dash action, THE Action Processor SHALL double the creature's movement speed for the current turn and consume the action phase

### Requirement 3

**User Story:** As a game developer, I want the Action Processor to handle spell casting, so that creatures can cast spells according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature casts a spell, THE Action Processor SHALL validate that the creature has the spell available and sufficient spell slots
2. WHEN a spell requires a spell attack roll, THE Action Processor SHALL use the Combat Rules Engine to resolve the attack
3. WHEN a spell requires a saving throw, THE Action Processor SHALL use the Combat Rules Engine to resolve the save for each target
4. WHEN a spell is cast, THE Action Processor SHALL consume the appropriate spell slot and mark the action or bonus action phase as consumed
5. WHEN a spell is cast as a bonus action, THE Action Processor SHALL enforce the restriction that only cantrips can be cast as actions on the same turn

### Requirement 4

**User Story:** As a game developer, I want the Action Processor to handle bonus actions, so that creatures can use class features and special abilities according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature takes a bonus action, THE Action Processor SHALL validate that the creature has a bonus action available for the current turn
2. WHEN a bonus action is validated, THE Action Processor SHALL verify that the creature has the ability or feature that grants the bonus action
3. WHEN a bonus action is completed, THE Action Processor SHALL mark the bonus action phase as consumed for the current turn
4. WHEN a creature attempts to take a bonus action without an available bonus action phase, THE Action Processor SHALL reject the action as illegal
5. WHEN a bonus action is taken, THE Action Processor SHALL generate a BonusActionTaken event containing the creature ID and action type

### Requirement 5

**User Story:** As a game developer, I want the Action Processor to handle reactions, so that creatures can respond to triggers outside their turn according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a reaction trigger occurs, THE Action Processor SHALL identify all creatures with reactions available that can respond to the trigger
2. WHEN multiple creatures can react to the same trigger, THE Action Processor SHALL resolve reactions in initiative order (highest first)
3. WHEN a creature uses a reaction, THE Action Processor SHALL mark the reaction as consumed until the start of the creature's next turn
4. WHEN an opportunity attack is triggered, THE Action Processor SHALL validate that the triggering creature moved out of reach and the reacting creature has a melee weapon equipped
5. WHEN a reaction is used, THE Action Processor SHALL generate a ReactionUsed event containing the creature ID, reaction type, and trigger

### Requirement 6

**User Story:** As a game developer, I want the Action Processor to enforce action economy, so that creatures cannot exceed their available actions per turn according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature attempts to take an action, THE Action Processor SHALL verify that the appropriate action phase (action, bonus action, reaction) is available
2. WHEN a creature has already used their action, THE Action Processor SHALL reject additional action attempts as illegal
3. WHEN a creature has already used their bonus action, THE Action Processor SHALL reject additional bonus action attempts as illegal
4. WHEN a creature has already used their reaction, THE Action Processor SHALL reject additional reaction attempts until the reaction is restored
5. WHEN a creature's turn ends, THE Action Processor SHALL reset action and bonus action availability but maintain reaction consumption until the next turn

### Requirement 7

**User Story:** As a game developer, I want the Action Processor to handle special actions, so that creatures can use standard D&D 5e SRD actions like Dodge, Disengage, and Help.

#### Acceptance Criteria

1. WHEN a creature takes the Dodge action, THE Action Processor SHALL apply the Dodging condition until the start of the creature's next turn and consume the action phase
2. WHEN a creature takes the Disengage action, THE Action Processor SHALL prevent opportunity attacks against the creature for the remainder of the turn and consume the action phase
3. WHEN a creature takes the Help action, THE Action Processor SHALL grant advantage on the next ability check or attack roll made by the target ally and consume the action phase
4. WHEN a creature takes the Ready action, THE Action Processor SHALL store the prepared action and trigger condition for later execution and consume the action phase
5. WHEN a readied action's trigger occurs, THE Action Processor SHALL execute the prepared action using the creature's reaction

### Requirement 8

**User Story:** As a game developer, I want the Action Processor to integrate with the Action Validation System, so that all actions are validated before execution according to game rules.

#### Acceptance Criteria

1. WHEN an action is submitted, THE Action Processor SHALL use the Action Validation System to verify action legality before processing
2. WHEN an action fails validation, THE Action Processor SHALL return an ActionResult.Failure with a descriptive reason
3. WHEN an action requires a choice (multiple targets, spell slot level), THE Action Processor SHALL return an ActionResult.RequiresChoice with available options
4. WHEN an action is validated successfully, THE Action Processor SHALL execute the action and return an ActionResult.Success with generated events
5. WHEN an action consumes resources, THE Action Processor SHALL verify resource availability through the Action Validation System before execution

### Requirement 9

**User Story:** As a game developer, I want the Action Processor to be deterministic and event-sourced, so that combat actions can be replayed from events and outcomes are reproducible.

#### Acceptance Criteria

1. THE Action Processor SHALL use only seeded random number generation from the DiceRoller for all random elements
2. THE Action Processor SHALL generate immutable events for all action outcomes (AttackResolved, MoveCommitted, SpellCast, BonusActionTaken, ReactionUsed)
3. THE Action Processor SHALL derive all state from event replay without storing mutable state
4. THE Action Processor SHALL produce identical outcomes when given the same seed, action inputs, and sequence of events
5. THE Action Processor SHALL be implemented with pure Kotlin core logic and no Android dependencies in the action processing layer
