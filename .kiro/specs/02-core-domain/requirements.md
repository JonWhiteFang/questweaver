# Requirements Document: Core Domain Models

## Introduction

This specification defines the foundational domain models for QuestWeaver, a solo D&D-style RPG Android application. The core domain layer provides pure Kotlin business entities, event sourcing architecture, repository interfaces, use case interfaces, and value objects that form the basis for all game logic. This layer must remain free of Android dependencies to ensure testability, portability, and adherence to Clean Architecture principles.

## Glossary

- **Domain Layer**: The pure Kotlin module (`core:domain`) containing business entities, use cases, events, and repository interfaces with no Android dependencies
- **Entity**: An immutable data class representing a core business object with identity (e.g., Creature, Campaign, Encounter)
- **Value Object**: An immutable data class representing a value without identity (e.g., GridPos, Abilities, DiceRoll)
- **Event Sourcing**: An architectural pattern where all state mutations produce immutable GameEvent instances that are logged for replay capability
- **GameEvent**: An immutable sealed interface representing a state mutation in the game system
- **Repository Interface**: A contract defining data access operations, with implementations provided in the data layer
- **Use Case Interface**: A contract defining a single business operation, typically with a single invoke method
- **Sealed Interface**: A Kotlin construct that restricts inheritance to a known set of subtypes, enabling exhaustive when expressions
- **SRD**: System Reference Document - the open-licensed subset of D&D 5e rules

## Requirements

### Requirement 1

**User Story:** As a developer, I want pure Kotlin domain entities with no Android dependencies, so that I can test business logic in isolation and maintain Clean Architecture boundaries.

#### Acceptance Criteria

1. THE Domain Layer SHALL contain only Kotlin standard library dependencies and no Android framework imports
2. THE Domain Layer SHALL define all entity classes as immutable data classes using val properties only
3. THE Domain Layer SHALL reside in the module path `core/domain` with package structure `dev.questweaver.core.domain`
4. WHEN any Android dependency is added to the Domain Layer, THEN THE build system SHALL fail with a dependency violation error
5. THE Domain Layer SHALL be compilable and testable on the JVM without Android runtime

### Requirement 2

**User Story:** As a developer, I want a Creature entity representing characters and monsters, so that I can model combatants with D&D 5e attributes and state.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a Creature entity with properties for id, name, armorClass, hitPointsCurrent, hitPointsMax, speed, and abilities
2. THE Creature entity SHALL include an Abilities value object containing strength, dexterity, constitution, intelligence, wisdom, and charisma scores
3. THE Creature entity SHALL provide computed properties for ability modifiers following D&D 5e rules where modifier equals floor of ability score minus ten divided by two
4. THE Creature entity SHALL be immutable with all properties declared as val
5. THE Creature entity SHALL support copy operations for creating modified instances

### Requirement 3

**User Story:** As a developer, I want a Campaign entity representing a game campaign, so that I can track campaign metadata, settings, and state across multiple sessions.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a Campaign entity with properties for id, name, createdTimestamp, lastPlayedTimestamp, and playerCharacterId
2. THE Campaign entity SHALL include a settings property containing difficulty level and content rating preferences
3. THE Campaign entity SHALL be immutable with all properties declared as val
4. THE Campaign entity SHALL support serialization for persistence
5. THE Campaign entity SHALL validate that playerCharacterId references a valid Creature entity

### Requirement 4

**User Story:** As a developer, I want an Encounter entity representing a combat encounter, so that I can manage tactical combat state including participants, turn order, and round tracking.

#### Acceptance Criteria

1. THE Domain Layer SHALL define an Encounter entity with properties for id, campaignId, createdTimestamp, currentRound, and activeCreatureId
2. THE Encounter entity SHALL include a collection of participant creature IDs
3. THE Encounter entity SHALL include an initiative order list mapping creature IDs to initiative values
4. THE Encounter entity SHALL be immutable with all properties declared as val
5. THE Encounter entity SHALL validate that activeCreatureId exists in the participant collection

### Requirement 5

**User Story:** As a developer, I want a MapGrid entity representing a tactical combat map, so that I can model grid-based positioning and terrain for encounters.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a MapGrid entity with properties for width, height, and a collection of terrain tiles
2. THE MapGrid entity SHALL support grid positions using a GridPos value object with x and y integer coordinates
3. THE MapGrid entity SHALL define terrain types including empty, difficult, impassable, and occupied
4. THE MapGrid entity SHALL be immutable with all properties declared as val
5. THE MapGrid entity SHALL validate that width and height are positive integers greater than zero

### Requirement 6

**User Story:** As a developer, I want an event sourcing architecture with a GameEvent sealed hierarchy, so that I can capture all state mutations as immutable events for replay and audit capabilities.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a GameEvent sealed interface with properties for sessionId and timestamp
2. THE GameEvent sealed interface SHALL have concrete implementations for all state-changing operations including EncounterStarted, MoveCommitted, AttackResolved, DamageApplied, and TurnEnded
3. WHEN a state mutation occurs, THEN THE system SHALL produce at least one GameEvent instance capturing both intent and outcome
4. THE GameEvent implementations SHALL be immutable data classes with all properties declared as val
5. THE GameEvent sealed interface SHALL enable exhaustive when expressions without else branches

### Requirement 7

**User Story:** As a developer, I want repository interfaces in the domain layer, so that I can define data access contracts without coupling to specific persistence implementations.

#### Acceptance Criteria

1. THE Domain Layer SHALL define repository interfaces for EventRepository, CreatureRepository, CampaignRepository, and EncounterRepository
2. THE EventRepository interface SHALL declare methods for appending events and observing events by session using Flow return types
3. THE repository interfaces SHALL use suspend functions for one-shot operations and Flow for reactive queries
4. THE repository interfaces SHALL accept and return domain entities only, not data layer entities
5. THE repository interfaces SHALL have no implementation code, only method signatures

### Requirement 8

**User Story:** As a developer, I want use case interfaces in the domain layer, so that I can define business operations as single-responsibility contracts.

#### Acceptance Criteria

1. THE Domain Layer SHALL define use case interfaces with a single suspend operator fun invoke method
2. THE use case interfaces SHALL accept domain entities or value objects as parameters
3. THE use case interfaces SHALL return sealed Result types capturing success, failure, or choice scenarios
4. THE use case interfaces SHALL have descriptive names using verb phrases such as ProcessPlayerAction or ResolveAttack
5. THE use case interfaces SHALL have no implementation code, only method signatures

### Requirement 9

**User Story:** As a developer, I want value objects for GridPos, Abilities, and DiceRoll, so that I can represent domain concepts with type safety and immutability.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a GridPos value object with x and y integer properties
2. THE Domain Layer SHALL define an Abilities value object with integer properties for all six D&D ability scores
3. THE Domain Layer SHALL define a DiceRoll value object with properties for diceType, count, modifier, and result
4. THE value objects SHALL be immutable data classes with all properties declared as val
5. THE value objects SHALL provide validation in their constructors to ensure valid state

### Requirement 10

**User Story:** As a developer, I want sealed Result types for operation outcomes, so that I can handle success, failure, and choice scenarios with exhaustive when expressions.

#### Acceptance Criteria

1. THE Domain Layer SHALL define a sealed interface ActionResult with subtypes Success, Failure, and RequiresChoice
2. THE ActionResult Success subtype SHALL contain a list of GameEvent instances produced by the operation
3. THE ActionResult Failure subtype SHALL contain a reason string describing why the operation failed
4. THE ActionResult RequiresChoice subtype SHALL contain a list of ActionOption instances for user selection
5. THE ActionResult sealed interface SHALL enable exhaustive when expressions without else branches
