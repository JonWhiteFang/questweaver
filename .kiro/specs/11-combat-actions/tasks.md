# Implementation Plan

- [x] 1. Set up module structure and core data models
  - Create package structure in `core/rules/actions/`: root level, `models/`, and `validation/` subdirectories
  - Add dependencies on `05-combat-rules`, `06-action-validation`, and `10-initiative-turns` in `core/rules/build.gradle.kts`
  - Verify module compiles with no Android dependencies
  - _Requirements: 9.5_

- [x] 2. Implement sealed action types
- [x] 2.1 Create CombatAction sealed interface
  - Define base interface with actorId property
  - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 7.1_

- [x] 2.2 Create Attack action data class
  - Define fields: actorId, targetId, weaponId, attackBonus, damageDice, damageModifier, damageType
  - _Requirements: 1.1, 1.5_

- [x] 2.3 Create Move action data class
  - Define fields: actorId, path, isDash
  - _Requirements: 2.1, 2.5_

- [x] 2.4 Create CastSpell action data class
  - Define fields: actorId, spellId, spellLevel, targets, spellEffect, isBonusAction
  - _Requirements: 3.1, 3.5_

- [x] 2.5 Create special action data classes
  - Create Dodge, Disengage, Help, Ready action data classes
  - Define Help with targetId and helpType fields
  - Define Ready with preparedAction and trigger fields
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 2.6 Create Reaction action data class
  - Define fields: actorId, reactionType, targetId
  - _Requirements: 5.1, 5.3_

- [x] 3. Implement supporting enums and sealed types
- [x] 3.1 Create HelpType enum
  - Define values: Attack, AbilityCheck
  - _Requirements: 7.3_

- [x] 3.2 Create ReactionType enum
  - Define values: OpportunityAttack, ReadiedAction, Shield, Counterspell, Other
  - _Requirements: 5.1, 5.4_

- [x] 3.3 Create SpellEffect sealed interface
  - Define Attack, Save, and Utility data classes
  - _Requirements: 3.2, 3.3_

- [x] 3.4 Create ReactionTrigger sealed interface
  - Define CreatureMoved, SpellCast, AttackMade, TriggerConditionMet data classes
  - _Requirements: 5.2, 5.5_

- [x] 4. Implement action context and result types
- [x] 4.1 Create ActionContext data class
  - Define fields: sessionId, roundNumber, turnPhase, creatures, mapGrid, activeConditions, readiedActions
  - _Requirements: 8.1_

- [x] 4.2 Create ReadiedAction data class
  - Define fields: creatureId, action, trigger
  - _Requirements: 7.4, 7.5_

- [x] 4.3 Create ActionResult sealed interface
  - Define Success, Failure, and RequiresChoice data classes
  - _Requirements: 8.2, 8.3, 8.4_

- [x] 4.4 Create ActionOption data class
  - Define fields: id, description, action
  - _Requirements: 8.3_

- [x] 4.5 Create ActionError sealed interface
  - Define InvalidAction, InsufficientResources, OutOfRange, NoLineOfEffect, ActionNotAvailable data classes
  - _Requirements: 8.2_

- [x] 5. Implement event data classes
- [x] 5.1 Create AttackResolved event
  - Define fields: sessionId, timestamp, attackerId, targetId, attackRoll, attackBonus, targetAC, hit, isCritical, damage, damageType
  - Extend GameEvent sealed interface
  - _Requirements: 1.3, 9.2_

- [x] 5.2 Create MoveCommitted event
  - Define fields: sessionId, timestamp, creatureId, path, movementUsed, movementRemaining
  - _Requirements: 2.4, 9.2_

- [x] 5.3 Create SpellCast event
  - Define fields: sessionId, timestamp, casterId, spellId, spellLevel, slotConsumed, targets, outcomes
  - Create SpellOutcome data class with targetId, attackRoll, saveRoll, success, damage, damageType
  - _Requirements: 3.4, 9.2_

- [x] 5.4 Create BonusActionTaken event
  - Define fields: sessionId, timestamp, creatureId, actionType
  - _Requirements: 4.5, 9.2_

- [x] 5.5 Create special action events
  - Create DodgeAction, DisengageAction, HelpAction, ReadyAction events
  - Each with sessionId, timestamp, and action-specific fields
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 9.2_

- [x] 5.6 Create CreatureDefeated event
  - Define fields: sessionId, timestamp, creatureId, defeatedBy
  - _Requirements: 1.3, 9.2_

- [x] 6. Implement ActionValidator
- [x] 6.1 Create ActionValidator class with ActionValidationSystem dependency
  - Define constructor accepting ActionValidationSystem
  - _Requirements: 8.1_

- [x] 6.2 Implement validate() method
  - Check action phase availability (action, bonus action, reaction)
  - Check resource availability (spell slots, movement)
  - Check range and line-of-effect
  - Check target validity
  - Check condition restrictions
  - Return ValidationResult (Valid, Invalid, RequiresChoice)
  - _Requirements: 8.1, 8.2, 8.3_

- [x] 7. Implement AttackActionHandler
- [x] 7.1 Create AttackActionHandler class with dependencies
  - Accept AttackResolver and DamageCalculator in constructor
  - _Requirements: 1.1, 9.1_

- [x] 7.2 Implement handleAttack() method
  - Get attacker and target creatures from context
  - Calculate attack bonus and target AC
  - Determine roll modifiers from conditions
  - Use AttackResolver to resolve attack roll
  - If hit, use DamageCalculator to calculate damage
  - Generate AttackResolved event with outcome
  - If target HP reaches 0, generate CreatureDefeated event
  - Return list of events
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 7.3 Implement handleMultiAttack() method
  - Call handleAttack() for each attack in the list
  - Collect all events from all attacks
  - Return combined list of events
  - _Requirements: 1.5_

- [x] 8. Implement MovementActionHandler
- [x] 8.1 Create MovementActionHandler class with dependencies
  - Accept Pathfinder and ReactionHandler in constructor
  - _Requirements: 2.1, 9.1_

- [x] 8.2 Implement handleMovement() method
  - Validate path using pathfinder
  - Calculate movement cost (difficult terrain, etc.)
  - Check if movement remaining is sufficient
  - Identify threatened squares along path
  - For each threatened square, check for opportunity attacks
  - Trigger opportunity attacks via ReactionHandler
  - Generate MoveCommitted event with path and remaining movement
  - Return list of events (movement + any opportunity attacks)
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 8.3 Implement handleDash() method
  - Double movement speed for current turn
  - Mark action phase as consumed
  - Generate DashAction event
  - Return list of events
  - _Requirements: 2.5_

- [x] 9. Implement SpellActionHandler



- [x] 9.1 Create SpellActionHandler class with dependencies

  - Accept AttackResolver, SavingThrowResolver, and DamageCalculator in constructor
  - _Requirements: 3.1, 9.1_


- [x] 9.2 Implement handleSpellCast() method
  - Validate spell slot availability
  - Check bonus action spell restriction (if applicable)
  - Determine spell effect type (attack, save, utility)
  - For spell attacks: use AttackResolver and DamageCalculator for each target
  - For saving throw spells: use SavingThrowResolver for each target and apply effects
  - Consume spell slot
  - Generate SpellCast event with outcomes
  - Return list of events


  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 10. Implement SpecialActionHandler

- [x] 10.1 Create SpecialActionHandler class

  - No external dependencies needed
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 10.2 Implement handleDodge() method

  - Apply Dodging condition until start of next turn
  - Mark action phase as consumed
  - Generate DodgeAction event
  - Return list of events
  - _Requirements: 7.1_

- [x] 10.3 Implement handleDisengage() method

  - Apply Disengaged condition for remainder of turn
  - Mark action phase as consumed
  - Generate DisengageAction event
  - Return list of events
  - _Requirements: 7.2_

- [x] 10.4 Implement handleHelp() method

  - Grant advantage on next ability check or attack roll for target
  - Mark action phase as consumed
  - Generate HelpAction event
  - Return list of events
  - _Requirements: 7.3_

- [x] 10.5 Implement handleReady() method

  - Store prepared action and trigger condition in context
  - Mark action phase as consumed
  - Generate ReadyAction event
  - Return list of events
  - _Requirements: 7.4, 7.5_

- [x] 11. Implement ReactionHandler


- [x] 11.1 Create ReactionHandler class with dependencies


  - Accept AttackActionHandler in constructor
  - _Requirements: 5.1, 9.1_

- [x] 11.2 Implement identifyReactors() method

  - Based on trigger type, identify creatures that can react
  - Check if creatures have reactions available
  - Return list of creature IDs in initiative order
  - _Requirements: 5.2_


- [x] 11.3 Implement handleReaction() method
  - Validate reaction is available
  - Process reaction based on type (opportunity attack, readied action, etc.)
  - Mark reaction as consumed
  - Generate ReactionUsed event
  - Return list of events
  - _Requirements: 5.1, 5.3, 5.4, 5.5_


- [x] 11.4 Implement opportunity attack logic
  - Check if reacting creature has reaction available
  - Check if reacting creature has melee weapon equipped
  - Check if triggering creature moved out of reach


  - Use AttackActionHandler to process attack
  - Return attack events
  - _Requirements: 5.4_

- [x] 12. Implement ActionProcessor


- [x] 12.1 Create ActionProcessor class with all handler dependencies

  - Accept AttackActionHandler, MovementActionHandler, SpellActionHandler, SpecialActionHandler, ReactionHandler, ActionValidator in constructor
  - _Requirements: 9.1_

- [x] 12.2 Implement processAction() method

  - Validate action using ActionValidator
  - If validation fails, return ActionResult.Failure
  - If validation requires choice, return ActionResult.RequiresChoice
  - Route to appropriate handler based on action type using exhaustive when
  - Handler executes action and generates events
  - Return ActionResult.Success with events
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.1_


- [x] 12.3 Implement action routing logic
  - Route Attack actions to AttackActionHandler
  - Route Move actions to MovementActionHandler
  - Route CastSpell actions to SpellActionHandler
  - Route Dodge, Disengage, Help, Ready to SpecialActionHandler
  - Route Reaction actions to ReactionHandler
  - Use exhaustive when expression for sealed types
  - _Requirements: 9.1_

- [x] 13. Add Koin dependency injection module






  - Create ActionsModule.kt in core/rules/di/ package
  - Define factory binding for ActionValidator with ActionValidationSystem dependency
  - Define factory binding for AttackActionHandler with AttackResolver and DamageCalculator dependencies
  - Define factory binding for MovementActionHandler with Pathfinder and ReactionHandler dependencies
  - Define factory binding for SpellActionHandler with AttackResolver, SavingThrowResolver, and DamageCalculator dependencies
  - Define factory binding for SpecialActionHandler
  - Define factory binding for ReactionHandler with AttackActionHandler dependency
  - Define factory binding for ActionProcessor with all handler dependencies
  - _Requirements: 9.1_

- [ ] 14. Write comprehensive unit tests
- [ ] 14.1 Write AttackActionHandler tests
  - Test attack hits and misses based on AC
  - Test critical hits double damage dice
  - Test damage applied to target HP
  - Test multiple attacks processed correctly
  - Test conditions affect attack rolls
  - Test CreatureDefeated event generated when HP reaches 0
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 14.2 Write MovementActionHandler tests
  - Test movement consumes correct amount
  - Test difficult terrain doubles cost
  - Test opportunity attacks triggered correctly
  - Test Dash action doubles movement
  - Test Disengage prevents opportunity attacks
  - Test MoveCommitted event contains correct path and remaining movement
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 14.3 Write SpellActionHandler tests
  - Test spell slots consumed correctly
  - Test spell attacks resolved correctly
  - Test saving throws resolved correctly
  - Test bonus action spell restriction enforced
  - Test area-of-effect targets multiple creatures
  - Test SpellCast event contains all outcomes
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 14.4 Write SpecialActionHandler tests
  - Test Dodge applies condition until next turn
  - Test Disengage prevents opportunity attacks
  - Test Help grants advantage on next roll
  - Test Ready action stores and triggers correctly
  - Test all special actions consume action phase
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 14.5 Write ReactionHandler tests
  - Test opportunity attacks trigger on movement
  - Test reactions consume reaction resource
  - Test multiple reactions resolved in initiative order
  - Test readied actions execute on trigger
  - Test identifyReactors returns correct creatures
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 14.6 Write ActionValidator tests
  - Test invalid actions rejected with reason
  - Test resource checks prevent illegal actions
  - Test range checks enforce distance limits
  - Test turn phase checks enforce action economy
  - Test condition restrictions prevent actions
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 14.7 Write ActionProcessor integration tests
  - Test complete attack sequence generates correct events
  - Test movement with opportunity attacks generates all events
  - Test spell casting with multiple targets generates outcomes
  - Test action validation failures return appropriate results
  - Test exhaustive when expression handles all action types
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] 14.8 Write event generation tests
  - Test all actions generate appropriate events
  - Test events contain complete outcome information
  - Test events are immutable and serializable
  - Test event timestamps are set correctly
  - _Requirements: 9.2_

- [ ] 14.9 Write property-based tests
  - Test attack damage never exceeds maximum possible
  - Test movement cost never exceeds available movement
  - Test spell slot consumption never goes negative
  - Test action phase consumption is idempotent
  - _Requirements: 6.1, 6.2, 6.3, 6.4_
