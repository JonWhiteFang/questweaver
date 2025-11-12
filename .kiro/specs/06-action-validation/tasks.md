# Implementation Plan

- [x] 1. Set up validation module structure and core types




  - Create package structure in `core/rules/src/main/kotlin/dev/questweaver/core/rules/validation/`
  - Create subdirectories: `validators/`, `state/`, `results/`
  - Define sealed interface `ValidationResult` with Success, Failure, RequiresChoice variants
  - Define sealed interface `ValidationFailure` with all failure types (ActionEconomyExhausted, InsufficientResources, OutOfRange, LineOfEffectBlocked, ConcentrationConflict, ConditionPreventsAction, InvalidTarget)
  - Define data class `ResourceCost` with actionEconomy, resources, movementCost, breaksConcentration fields
  - Define sealed interface `ActionChoice` with SpellSlotLevel, TargetSelection, FeatureOption variants
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 2. Implement action economy types and validator



  - [x] 2.1 Create action economy data structures


    - Define enum `ActionEconomyResource` with Action, BonusAction, Reaction, Movement, FreeAction
    - Define enum `ActionType` with Action, BonusAction, Reaction, Movement, FreeAction
    - Define sealed interface `GameAction` with actorId and actionType properties
    - Implement GameAction variants: Attack, CastSpell, Move, Dash, Disengage, Dodge, UseClassFeature, OpportunityAttack
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  
  - [x] 2.2 Implement ActionEconomyValidator


    - Create `ActionEconomyValidator` class with validateActionEconomy method
    - Implement getActionCost method to determine resource consumption
    - Implement validation logic: check if action/bonus/reaction already used
    - Implement movement validation: check if movement exceeds remaining movement
    - Return ValidationResult.Failure with ActionEconomyExhausted if resources unavailable
    - Return ValidationResult.Success with ResourceCost if validation passes
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 3. Implement resource tracking and validation



  - [x] 3.1 Create resource data structures

    - Define sealed interface `Resource` with SpellSlot, ClassFeature, ItemCharge, HitDice variants
    - Define data class `ResourcePool` with spellSlots, classFeatures, itemCharges, hitDice maps
    - Implement ResourcePool.hasResource method to check availability
    - Implement ResourcePool.consume method to create updated pool with resource consumed
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.1, 8.2, 8.3, 8.4, 8.5_
  

  - [x] 3.2 Implement ResourceValidator

    - Create `ResourceValidator` class with validateResources method
    - Implement getResourceCost method to determine required resources
    - Implement spell slot validation: check if slot of required level or higher available
    - Implement upcasting logic: allow higher-level slots for lower-level spells
    - Implement class feature validation: check remaining uses
    - Implement item charge validation: check remaining charges
    - Return ValidationResult.Failure with InsufficientResources if resources unavailable
    - Return ValidationResult.Success with ResourceCost if validation passes
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 4. Implement range and line-of-effect validation



  - [x] 4.1 Create range data structures

    - Define sealed interface `Range` with Touch, Feet, Sight, Self, Radius variants
    - Define data class `GridPos` for grid positions (if not already in core:domain)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  

  - [x] 4.2 Implement RangeValidator

    - Create `RangeValidator` class with geometryCalculator dependency
    - Implement validateRange method to check distance and line-of-effect
    - Implement calculateDistance method using D&D 5e grid rules (5ft per square, diagonal movement)
    - Implement hasLineOfEffect method to check for obstacles
    - Integrate with Combat Rules Engine's GeometryCalculator for spatial calculations
    - Return ValidationResult.Failure with OutOfRange if target too far
    - Return ValidationResult.Failure with LineOfEffectBlocked if path obstructed
    - Return ValidationResult.Success if range and line-of-effect valid
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 5. Implement concentration validation




  - [x] 5.1 Create concentration data structures

    - Define data class `ConcentrationInfo` with spell, startedRound, dc fields
    - Define data class `ConcentrationState` with activeConcentrations map
    - Implement ConcentrationState.isConcentrating method
    - Implement ConcentrationState.getConcentration method
    - Implement ConcentrationState.startConcentration method
    - Implement ConcentrationState.breakConcentration method
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  

  - [x] 5.2 Implement ConcentrationValidator

    - Create `ConcentrationValidator` class with validateConcentration method
    - Implement getActiveConcentration method to check current concentration
    - Implement validation logic: check if already concentrating on another spell
    - Return ValidationResult.Failure with ConcentrationConflict if already concentrating
    - Return ValidationResult.Success with breaksConcentration flag if casting new concentration spell
    - Allow non-concentration spells while concentrating
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 6. Implement condition-based validation
  - [ ] 6.1 Implement ConditionValidator
    - Create `ConditionValidator` class with conditionRegistry dependency
    - Implement validateConditions method to check if conditions prevent action
    - Implement getBlockingCondition method to find blocking conditions
    - Integrate with Combat Rules Engine's ConditionRegistry
    - Check Stunned: prevents actions, reactions, movement
    - Check Incapacitated: prevents actions, reactions
    - Check Paralyzed: prevents actions, reactions, movement
    - Check Unconscious: prevents actions, reactions, movement
    - Return ValidationResult.Failure with ConditionPreventsAction if blocked
    - Return ValidationResult.Success if no blocking conditions
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 7. Implement turn state management
  - [ ] 7.1 Create TurnState data class
    - Define data class `TurnState` with creatureId, round, actionUsed, bonusActionUsed, reactionUsed, movementUsed, movementTotal, resourcePool, concentrationState fields
    - Implement remainingMovement method
    - Implement hasActionAvailable, hasBonusActionAvailable, hasReactionAvailable methods
    - Implement useAction, useBonusAction, useReaction, useMovement methods
    - Implement consumeResources method to apply ResourceCost
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 8. Implement main ActionValidator orchestrator
  - [ ] 8.1 Create ActionValidator class
    - Create `ActionValidator` class with dependencies on all validators
    - Inject ActionEconomyValidator, ResourceValidator, RangeValidator, ConcentrationValidator, ConditionValidator
    - _Requirements: 1.1, 5.1, 5.2, 5.3, 5.4, 5.5, 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [ ] 8.2 Implement validation orchestration
    - Implement validate method with action, actor, turnState, encounterState parameters
    - Step 1: Validate conditions (fail-fast if blocked)
    - Step 2: Validate action economy (fail-fast if exhausted)
    - Step 3: Validate resources (fail-fast if insufficient)
    - Step 4: Validate range and line-of-effect (fail-fast if invalid)
    - Step 5: Validate concentration (fail-fast if conflict)
    - Aggregate ResourceCost from all validators
    - Return ValidationResult.Success with complete ResourceCost if all checks pass
    - Return ValidationResult.Failure with first failure reason if any check fails
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 5.4, 5.5, 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 9. Add Koin dependency injection module
  - Create `validationModule` in `core/rules/src/main/kotlin/dev/questweaver/core/rules/di/ValidationModule.kt`
  - Register ActionValidator as factory
  - Register all sub-validators as factory
  - Inject dependencies from Combat Rules Engine (ConditionRegistry, GeometryCalculator)
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 10. Write comprehensive unit tests
  - [ ] 10.1 Test ActionEconomyValidator
    - Test action validation: can't take action if already used
    - Test bonus action validation: can't take bonus action if already used
    - Test reaction validation: can't take reaction if already used
    - Test movement validation: can't move more than remaining movement
    - Test Dash action doubles movement
    - Test action economy resets on new turn
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  
  - [ ] 10.2 Test ResourceValidator
    - Test spell slot validation: requires available slot
    - Test upcasting: higher-level slot works for lower-level spell
    - Test cantrips: don't consume spell slots
    - Test class features: require remaining uses
    - Test item charges: require remaining charges
    - Test resource consumption updates pool correctly
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.1, 8.2, 8.3, 8.4, 8.5_
  
  - [ ] 10.3 Test RangeValidator
    - Test touch range: requires 5 feet or less
    - Test ranged actions: fail beyond max range
    - Test line-of-effect: blocked by obstacles
    - Test self-targeted actions: always valid
    - Test edge case: exactly at max range
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ] 10.4 Test ConcentrationValidator
    - Test concentration conflict: can't cast while concentrating
    - Test non-concentration spells: allowed while concentrating
    - Test breaking concentration: new concentration spell breaks old
    - Test Incapacitated breaks concentration
    - Test Unconscious breaks concentration
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [ ] 10.5 Test ConditionValidator
    - Test Stunned prevents all actions
    - Test Incapacitated prevents actions and reactions
    - Test Paralyzed prevents actions, reactions, movement
    - Test Unconscious prevents all actions
    - Test Prone doesn't prevent actions
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ] 10.6 Test ActionValidator integration
    - Test complete validation flow with all checks
    - Test fail-fast behavior: first failure returned
    - Test resource cost aggregation from multiple validators
    - Test turn state updates after validation
    - Test deterministic validation with same inputs
    - Test multiple simultaneous failures prioritized correctly
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 11. Write property-based tests
  - Test validation is deterministic for same inputs
  - Test successful validation always returns resource cost
  - Test failed validation always returns specific failure reason
  - Test resource consumption never produces negative values
  - _Requirements: 5.4_

- [ ] 12. Add performance benchmarks
  - Create JMH benchmark for ActionValidator.validate
  - Verify validation completes within 50ms target
  - Benchmark individual validators for performance profiling
  - _Requirements: 1.5_

- [ ] 13. Update module documentation
  - Add KDoc comments to all public APIs
  - Document validation flow in ActionValidator
  - Document resource cost calculation
  - Document failure reason meanings
  - Create module README with usage examples
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
