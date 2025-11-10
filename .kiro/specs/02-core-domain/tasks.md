# Implementation Plan: Core Domain Models

- [x] 1. Set up core domain module structure





  - Create `core/domain` module directory with Kotlin JVM configuration
  - Configure `build.gradle.kts` with Kotlin JVM plugin, kotlinx-serialization, and coroutines dependencies
  - Set up package structure: `entities/`, `events/`, `repositories/`, `usecases/`, `values/`
  - Configure test dependencies (kotest, kotest-property)
  - _Requirements: 1.1, 1.3_

- [x] 2. Implement value objects





  - [x] 2.1 Create GridPos value object


    - Write `GridPos` data class with x and y properties
    - Add validation in init block for non-negative coordinates
    - Implement `distanceTo()` method using Chebyshev distance
    - Implement `neighbors()` method returning 8 adjacent positions
    - Add `@Serializable` annotation
    - _Requirements: 9.1_
  
  - [x] 2.2 Create Abilities value object


    - Write `Abilities` data class with six ability score properties
    - Add validation in init block for scores between 1 and 30
    - Implement computed modifier properties for all six abilities
    - Add `@Serializable` annotation
    - _Requirements: 2.2, 2.3, 9.2_
  
  - [x] 2.3 Create DiceRoll value object


    - Write `DiceRoll` data class with diceType, count, modifier, and result properties
    - Add validation for valid dice types (d4, d6, d8, d10, d12, d20, d100)
    - Add validation for result within possible range
    - Implement `toString()` method for readable output
    - Add `@Serializable` annotation
    - _Requirements: 9.3_
  
  - [x] 2.4 Create enum types


    - Write `TerrainType` enum with EMPTY, DIFFICULT, IMPASSABLE, OCCUPIED
    - Write `Condition` enum with all D&D 5e conditions
    - Write `Difficulty` enum with EASY, NORMAL, HARD, DEADLY
    - Write `ContentRating` enum with EVERYONE, TEEN, MATURE
    - Write `EncounterStatus` enum with IN_PROGRESS, VICTORY, DEFEAT, FLED
    - Add `@Serializable` annotation to all enums
    - _Requirements: 5.3_
  
  - [x] 2.5 Write unit tests for value objects


    - Test GridPos validation, distance calculation, and neighbors
    - Test Abilities validation and modifier calculations with property-based tests
    - Test DiceRoll validation for all dice types
    - Test enum serialization
    - _Requirements: 9.5_

- [x] 3. Implement entity classes




  - [x] 3.1 Create Creature entity


    - Write `Creature` data class with id, name, AC, HP, speed, abilities, proficiencyBonus, conditions
    - Add validation in init block for positive id, non-blank name, valid HP range
    - Implement `isAlive` and `isBloodied` computed properties
    - Add `@Serializable` annotation
    - _Requirements: 2.1, 2.2, 2.4, 2.5_
  
  - [x] 3.2 Create Campaign entity


    - Write `Campaign` data class with id, name, timestamps, playerCharacterId, settings
    - Write `CampaignSettings` data class with difficulty and contentRating
    - Add validation in init block for positive ids, non-blank name, valid timestamps
    - Add `@Serializable` annotation
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [x] 3.3 Create Encounter entity


    - Write `Encounter` data class with id, campaignId, timestamp, round, activeCreatureId, participants, initiativeOrder, status
    - Write `InitiativeEntry` data class with creatureId and initiative value
    - Add validation for positive round, non-empty participants, valid activeCreatureId
    - Add validation that initiativeOrder matches participants
    - Add `@Serializable` annotation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 3.4 Create MapGrid entity


    - Write `MapGrid` data class with width, height, terrain map, creaturePositions map
    - Add validation for positive dimensions and in-bounds positions
    - Implement `isInBounds()` and `getTerrainAt()` methods
    - Add `@Serializable` annotation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 3.5 Write unit tests for entities


    - Test Creature validation, computed properties, and copy operations
    - Test Campaign validation and settings
    - Test Encounter validation and initiative order consistency
    - Test MapGrid validation and helper methods
    - Test entity serialization and deserialization
    - _Requirements: 2.5, 3.5, 4.5, 5.5_

- [x] 4. Implement event sourcing architecture




  - [x] 4.1 Create GameEvent sealed interface

    - Write `GameEvent` sealed interface with sessionId and timestamp properties
    - Add `@Serializable` annotation
    - _Requirements: 6.1, 6.4_
  


  - [x] 4.2 Create encounter event types
    - Write `EncounterStarted` event with encounterId, participants, initiativeOrder
    - Write `RoundStarted` event with encounterId and roundNumber
    - Write `TurnStarted` event with encounterId and creatureId
    - Write `TurnEnded` event with encounterId and creatureId
    - Write `EncounterEnded` event with encounterId and status
    - Add `@Serializable` annotation to all events
    - _Requirements: 6.2, 6.3, 6.4_

  

  - [x] 4.3 Create combat event types
    - Write `AttackResolved` event with attacker, target, roll, AC, hit, critical
    - Write `DamageApplied` event with target, damageRoll, amount, hpBefore, hpAfter
    - Write `ConditionApplied` event with target, condition, duration
    - Write `ConditionRemoved` event with target and condition
    - Add `@Serializable` annotation to all events
    - _Requirements: 6.2, 6.3, 6.4_

  

  - [x] 4.4 Create movement event types
    - Write `MoveCommitted` event with creatureId, fromPos, toPos, path, movementCost
    - Add `@Serializable` annotation
    - _Requirements: 6.2, 6.3, 6.4_

  
  - [x] 4.5 Write unit tests for events


    - Test event immutability
    - Test event serialization and deserialization
    - Test exhaustive when expressions for GameEvent sealed interface
    - _Requirements: 6.4, 6.5_

- [x] 5. Define repository interfaces






  - [x] 5.1 Create EventRepository interface

    - Write interface with `append()`, `appendAll()`, `forSession()`, `observeSession()` methods
    - Use suspend functions for one-shot operations
    - Use Flow return type for `observeSession()`
    - _Requirements: 7.1, 7.2, 7.3, 7.5_
  

  - [x] 5.2 Create CreatureRepository interface

    - Write interface with CRUD methods: `getById()`, `getAll()`, `insert()`, `update()`, `delete()`
    - Add `observe()` method returning Flow
    - Use suspend functions and domain entities only
    - _Requirements: 7.1, 7.3, 7.4, 7.5_
  

  - [x] 5.3 Create CampaignRepository interface


    - Write interface with CRUD methods: `getById()`, `getAll()`, `insert()`, `update()`, `delete()`
    - Add `observeAll()` method returning Flow
    - Use suspend functions and domain entities only
    - _Requirements: 7.1, 7.3, 7.4, 7.5_

  
  - [x] 5.4 Create EncounterRepository interface

    - Write interface with CRUD methods: `getById()`, `getByCampaign()`, `insert()`, `update()`, `delete()`
    - Add `observe()` method returning Flow
    - Use suspend functions and domain entities only
    - _Requirements: 7.1, 7.3, 7.4, 7.5_

- [x] 6. Define use case result types






  - [x] 6.1 Create ActionResult sealed interface

    - Write sealed interface with Success, Failure, RequiresChoice subtypes
    - Success contains list of GameEvent instances
    - Failure contains reason string
    - RequiresChoice contains list of ActionOption instances
    - _Requirements: 8.3, 10.1, 10.2, 10.3, 10.4, 10.5_
  

  - [x] 6.2 Create ActionOption data class


    - Write data class with id, description, and metadata properties
    - Add `@Serializable` annotation
    - _Requirements: 10.4_


  
  - [x] 6.3 Write unit tests for result types


    - Test exhaustive when expressions for ActionResult
    - Test ActionOption creation and serialization
    - _Requirements: 10.5_

- [x] 7. Create domain error types





  - Write `DomainError` sealed class with InvalidEntity, InvalidOperation, NotFound subtypes
  - Add descriptive properties for error context
  - _Requirements: 1.1_

- [x] 8. Verify module dependencies





  - Run Gradle build to verify no Android dependencies
  - Verify module compiles with Kotlin JVM only
  - Check that all imports are from Kotlin stdlib or kotlinx libraries
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 9. Integration verification


  - [x] 9.1 Create sample domain objects


    - Write test code creating instances of all entities
    - Verify copy operations work correctly
    - Verify serialization round-trips successfully
    - _Requirements: 2.5, 3.4, 4.4, 5.4_
  
  - [x] 9.2 Verify event sourcing pattern
    - Write test code generating events for a sample encounter
    - Verify events are immutable
    - Verify exhaustive when expressions compile
    - _Requirements: 6.3, 6.4, 6.5_
  
  - [x] 9.3 Verify repository contracts
    - Write mock implementations of repository interfaces
    - Verify Flow types work correctly
    - Verify suspend functions compile
    - _Requirements: 7.2, 7.3, 7.5_
