# Requirements Document

## Introduction

The Initiative & Turn Management system is responsible for tracking turn order, managing combat rounds, and enforcing turn phase rules in tactical encounters. It implements D&D 5e SRD-compatible initiative mechanics with deterministic ordering, surprise round handling, and turn phase tracking (move/action/bonus action/reaction). The system must integrate with the Combat Rules Engine for initiative rolls and support event sourcing for full replay capability.

## Glossary

- **Initiative System**: The component that determines the order in which creatures act during combat
- **Initiative Roll**: A d20 roll plus Dexterity modifier to determine turn order
- **Initiative Tracker**: The data structure that maintains the ordered list of creatures in combat
- **Turn Order**: The sequence in which creatures take their turns, sorted by initiative score (highest to lowest)
- **Round**: A complete cycle through all creatures in the initiative order
- **Turn**: A single creature's opportunity to act within a round
- **Turn Phase**: A subdivision of a turn (movement, action, bonus action, reaction)
- **Active Creature**: The creature whose turn is currently in progress
- **Surprise Round**: A special first round where only non-surprised creatures can act
- **Surprised Condition**: A status preventing a creature from acting during the surprise round
- **Initiative Tie**: When two or more creatures roll the same initiative score
- **Delayed Turn**: A creature choosing to act later in the initiative order
- **Ready Action**: A creature preparing an action to trigger on a specific condition

## Requirements

### Requirement 1

**User Story:** As a game developer, I want the Initiative System to roll initiative for all creatures at encounter start, so that turn order is established according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an encounter begins, THE Initiative System SHALL roll initiative (d20 + Dexterity modifier) for each creature using the seeded DiceRoller
2. WHEN initiative is rolled, THE Initiative System SHALL sort creatures in descending order by initiative score (highest acts first)
3. WHEN two creatures have the same initiative score, THE Initiative System SHALL use the higher Dexterity modifier as the tiebreaker
4. WHEN two creatures have the same initiative score and Dexterity modifier, THE Initiative System SHALL use creature ID as a deterministic tiebreaker
5. WHEN initiative is rolled, THE Initiative System SHALL generate an EncounterStarted event containing all initiative rolls and the resulting turn order

### Requirement 2

**User Story:** As a game developer, I want the Initiative System to handle surprise rounds, so that ambushes and unexpected encounters follow D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN an encounter begins with surprised creatures, THE Initiative System SHALL create a surprise round before the first normal round
2. WHEN a surprise round occurs, THE Initiative System SHALL allow only non-surprised creatures to take turns
3. WHEN a surprised creature's turn arrives in the surprise round, THE Initiative System SHALL skip that creature and advance to the next creature
4. WHEN the surprise round ends, THE Initiative System SHALL remove the surprised condition from all creatures
5. WHEN the surprise round ends, THE Initiative System SHALL begin round 1 with all creatures able to act

### Requirement 3

**User Story:** As a game developer, I want the Initiative System to track turn phases, so that action economy is enforced according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature's turn begins, THE Initiative System SHALL initialize turn phases (movement available, action available, bonus action available, reaction available)
2. WHEN a creature uses their movement, THE Initiative System SHALL track the remaining movement distance for that turn
3. WHEN a creature takes an action, THE Initiative System SHALL mark the action phase as consumed for that turn
4. WHEN a creature takes a bonus action, THE Initiative System SHALL mark the bonus action phase as consumed for that turn
5. WHEN a creature's turn ends, THE Initiative System SHALL reset all turn phases for the next creature's turn

### Requirement 4

**User Story:** As a game developer, I want the Initiative System to manage round progression, so that combat advances through multiple rounds until completion.

#### Acceptance Criteria

1. WHEN all creatures have completed their turns, THE Initiative System SHALL increment the round counter and return to the first creature in initiative order
2. WHEN a new round begins, THE Initiative System SHALL generate a RoundStarted event containing the round number
3. WHEN a round begins, THE Initiative System SHALL reset all per-round resources (reactions, movement) for all creatures
4. WHEN a creature is added to combat mid-encounter, THE Initiative System SHALL roll initiative for the new creature and insert it into the turn order
5. WHEN a creature is removed from combat (defeated or fled), THE Initiative System SHALL remove the creature from the turn order without disrupting the current round

### Requirement 5

**User Story:** As a game developer, I want the Initiative System to track the active creature, so that the UI can display whose turn it is and enforce turn-based actions.

#### Acceptance Criteria

1. WHEN a creature's turn begins, THE Initiative System SHALL set that creature as the active creature and generate a TurnStarted event
2. WHEN a creature's turn ends, THE Initiative System SHALL generate a TurnEnded event and advance to the next creature in initiative order
3. WHEN the active creature is queried, THE Initiative System SHALL return the creature whose turn is currently in progress
4. WHEN an action is attempted by a non-active creature outside their reaction window, THE Initiative System SHALL reject the action as illegal
5. WHEN the last creature in the round completes their turn, THE Initiative System SHALL advance to the first creature and increment the round counter

### Requirement 6

**User Story:** As a game developer, I want the Initiative System to handle reactions, so that creatures can respond to triggers outside their turn according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature has a reaction available, THE Initiative System SHALL allow the creature to use the reaction in response to a valid trigger
2. WHEN a creature uses their reaction, THE Initiative System SHALL mark the reaction as consumed until the start of the creature's next turn
3. WHEN a creature's turn begins, THE Initiative System SHALL restore the creature's reaction if it was consumed in the previous round
4. WHEN multiple creatures have reactions triggered by the same event, THE Initiative System SHALL resolve reactions in initiative order (highest first)
5. WHEN a reaction is used, THE Initiative System SHALL generate a ReactionUsed event containing the creature ID and reaction type

### Requirement 7

**User Story:** As a game developer, I want the Initiative System to support delayed turns, so that creatures can choose to act later in the initiative order according to D&D 5e SRD rules.

#### Acceptance Criteria

1. WHEN a creature delays their turn, THE Initiative System SHALL remove the creature from the current position in initiative order
2. WHEN a delayed creature chooses to act, THE Initiative System SHALL insert the creature into the initiative order at the current position
3. WHEN a delayed creature acts, THE Initiative System SHALL maintain the new initiative position for the remainder of the encounter
4. WHEN a round ends with a creature still delaying, THE Initiative System SHALL place the creature at the end of the initiative order for the next round
5. WHEN a creature delays, THE Initiative System SHALL generate a TurnDelayed event containing the creature ID and original initiative score

### Requirement 8

**User Story:** As a game developer, I want the Initiative System to be deterministic and event-sourced, so that combat can be replayed from events and outcomes are reproducible.

#### Acceptance Criteria

1. THE Initiative System SHALL use only seeded random number generation from the DiceRoller for initiative rolls
2. THE Initiative System SHALL generate immutable events for all state changes (EncounterStarted, TurnStarted, TurnEnded, RoundStarted, ReactionUsed, TurnDelayed)
3. THE Initiative System SHALL derive all state from event replay without storing mutable state
4. THE Initiative System SHALL produce identical turn order and progression when given the same seed and sequence of events
5. THE Initiative System SHALL be implemented as pure Kotlin with no Android dependencies in the core logic
