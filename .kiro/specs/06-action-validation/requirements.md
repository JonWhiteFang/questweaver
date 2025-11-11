# Requirements Document

## Introduction

The Action Validation System ensures that all player and AI actions in QuestWeaver comply with D&D 5e SRD rules. This system validates action legality, tracks resource consumption (spell slots, movement, action economy), enforces turn phase restrictions, and validates spatial requirements (range, line-of-effect). The system must be deterministic, work offline, and integrate with the Combat Rules Engine to provide immediate feedback on action validity before execution.

## Glossary

- **Action Validation System**: The component responsible for determining whether a proposed action is legal according to game rules and current state
- **Action Economy**: The D&D 5e system limiting creatures to one action, one bonus action, one reaction, and movement per turn
- **Resource**: A consumable or limited capability (spell slots, movement points, class features, item charges)
- **Turn Phase**: The current phase of a creature's turn (movement, action, bonus action, reaction)
- **Range**: The maximum distance at which an action can be performed
- **Line-of-Effect**: An unobstructed path between source and target required for certain actions
- **Legality Check**: The process of determining if an action can be performed given current game state
- **Validation Result**: A sealed type indicating success, failure with reason, or requiring additional choices

## Requirements

### Requirement 1

**User Story:** As a player, I want the system to prevent me from taking illegal actions, so that I don't accidentally break game rules or waste resources.

#### Acceptance Criteria

1. WHEN a player attempts an action, THE Action Validation System SHALL validate the action against current game state before execution
2. IF an action violates game rules, THEN THE Action Validation System SHALL return a validation failure with a specific reason
3. WHEN an action is valid, THE Action Validation System SHALL return a success result with any resource costs identified
4. THE Action Validation System SHALL validate actions without modifying game state (pure validation)
5. THE Action Validation System SHALL complete validation within 50 milliseconds for typical actions

### Requirement 2

**User Story:** As a player, I want the system to track my available actions and movement, so that I know what I can do on my turn.

#### Acceptance Criteria

1. WHEN a creature's turn begins, THE Action Validation System SHALL initialize action economy tracking with one action, one bonus action, one reaction, and full movement speed
2. WHEN an action is validated successfully, THE Action Validation System SHALL identify which action economy resources would be consumed
3. WHEN a creature has already used their action, THE Action Validation System SHALL reject attempts to take another action
4. WHEN a creature has already used their bonus action, THE Action Validation System SHALL reject attempts to take another bonus action
5. WHEN a creature has already used their reaction, THE Action Validation System SHALL reject attempts to take another reaction
6. WHEN a creature has used all their movement, THE Action Validation System SHALL reject movement actions that exceed remaining movement

### Requirement 3

**User Story:** As a spellcaster, I want the system to track my spell slots and prevent me from casting spells I can't afford, so that I don't waste turns on invalid actions.

#### Acceptance Criteria

1. WHEN a spell casting action is validated, THE Action Validation System SHALL verify the caster has an available spell slot of the required level or higher
2. WHEN a spell is cast at a higher level, THE Action Validation System SHALL identify the spell slot level that would be consumed
3. WHEN a creature has no spell slots of sufficient level, THE Action Validation System SHALL reject the spell casting action with a specific reason
4. THE Action Validation System SHALL track spell slot consumption per spell level (1st through 9th)
5. WHERE a spell has special resource requirements (material components, concentration), THE Action Validation System SHALL validate those requirements

### Requirement 4

**User Story:** As a player, I want the system to validate range and line-of-effect for my actions, so that I don't attempt actions against targets I can't reach.

#### Acceptance Criteria

1. WHEN an action targets a creature or location, THE Action Validation System SHALL verify the target is within the action's maximum range
2. WHERE an action requires line-of-effect, THE Action Validation System SHALL verify an unobstructed path exists between source and target
3. WHEN a target is out of range, THE Action Validation System SHALL reject the action with the actual distance and required range
4. WHEN line-of-effect is blocked, THE Action Validation System SHALL reject the action with the blocking obstacle information
5. THE Action Validation System SHALL use grid-based distance calculations consistent with D&D 5e rules (5ft per square, diagonal movement)

### Requirement 5

**User Story:** As a game master AI, I want the system to validate NPC actions using the same rules as player actions, so that combat is fair and consistent.

#### Acceptance Criteria

1. THE Action Validation System SHALL apply identical validation rules to both player and NPC actions
2. WHEN validating an NPC action, THE Action Validation System SHALL use the NPC's specific resources and capabilities
3. THE Action Validation System SHALL validate actions without knowledge of whether the actor is a player or NPC
4. THE Action Validation System SHALL be deterministic, producing identical results for identical inputs
5. THE Action Validation System SHALL not depend on Android framework classes (pure Kotlin in core:rules module)

### Requirement 6

**User Story:** As a developer, I want validation results to provide clear feedback, so that the UI can display helpful error messages to players.

#### Acceptance Criteria

1. WHEN validation fails, THE Action Validation System SHALL return a result containing a machine-readable failure reason
2. WHEN validation fails, THE Action Validation System SHALL include relevant context (missing resources, distance values, blocking obstacles)
3. WHEN validation succeeds, THE Action Validation System SHALL return a result containing the resources that would be consumed
4. THE Action Validation System SHALL use sealed interfaces for validation results to ensure exhaustive handling
5. WHERE an action requires additional choices (spell slot level, target selection), THE Action Validation System SHALL return a result indicating required choices

### Requirement 7

**User Story:** As a player, I want the system to validate opportunity attacks and reactions, so that I can respond to enemy actions appropriately.

#### Acceptance Criteria

1. WHEN a creature moves out of an enemy's reach, THE Action Validation System SHALL validate whether an opportunity attack is legal
2. WHEN a reaction is triggered, THE Action Validation System SHALL verify the creature has not already used their reaction this round
3. WHEN validating a reaction, THE Action Validation System SHALL verify the triggering condition is met
4. THE Action Validation System SHALL track reaction availability separately from action and bonus action
5. WHEN a creature's turn ends, THE Action Validation System SHALL not reset reaction availability (reactions persist until start of creature's next turn)

### Requirement 8

**User Story:** As a player using class features, I want the system to track limited-use abilities, so that I know when I can use special powers.

#### Acceptance Criteria

1. WHERE a creature has limited-use class features, THE Action Validation System SHALL track remaining uses per feature
2. WHEN a class feature action is validated, THE Action Validation System SHALL verify the feature has remaining uses
3. WHEN a feature recharges on a short rest, THE Action Validation System SHALL track that separately from long rest features
4. WHEN a feature recharges on a dice roll, THE Action Validation System SHALL validate based on current availability
5. THE Action Validation System SHALL support per-day, per-short-rest, per-long-rest, and recharge-on-roll resource types

### Requirement 9

**User Story:** As a player, I want the system to validate concentration requirements, so that I don't accidentally cast multiple concentration spells.

#### Acceptance Criteria

1. WHERE a spell requires concentration, THE Action Validation System SHALL verify the caster is not already concentrating on another spell
2. WHEN a concentration spell is validated successfully, THE Action Validation System SHALL indicate that existing concentration would be broken
3. THE Action Validation System SHALL track which creature is concentrating on which spell
4. WHEN a creature takes damage while concentrating, THE Action Validation System SHALL validate concentration saving throws
5. THE Action Validation System SHALL allow non-concentration spells to be cast while concentrating

### Requirement 10

**User Story:** As a developer, I want the validation system to integrate with the Combat Rules Engine, so that validation uses consistent rule interpretations.

#### Acceptance Criteria

1. THE Action Validation System SHALL depend on the Combat Rules Engine for range calculations
2. THE Action Validation System SHALL depend on the Combat Rules Engine for line-of-effect calculations
3. THE Action Validation System SHALL use the Combat Rules Engine's ability check and saving throw mechanics
4. THE Action Validation System SHALL not duplicate logic from the Combat Rules Engine
5. THE Action Validation System SHALL be implemented in the core:rules module with no Android dependencies
