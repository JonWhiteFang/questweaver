# Implementation Plan

## Overview

This implementation plan breaks down the Tactical AI Agent into discrete, manageable coding tasks. Each task builds incrementally on previous work, with integration points clearly defined. The plan follows implementation-first development: build the feature before writing comprehensive tests.

---

## Tasks

- [x] 1. Set up ai:tactical module structure
  - Create `ai/tactical/` module directory with Gradle build configuration
  - Add dependencies: core:domain, core:rules, feature:map, kotlinx-coroutines, kotlin-logging
  - Create package structure: `agent/`, `behavior/`, `scoring/`, `targeting/`, `positioning/`, `resources/`, `di/`
  - Add module to `settings.gradle.kts`
  - Create Koin DI module for tactical AI components
  - _Requirements: All requirements (module foundation)_

- [x] 2. Implement core data models
  - [x] 2.1 Create TacticalContext data class
    - Include encounterId, round, creatures, allies, enemies, mapState, activeConditions, concentrationSpells, recentDamage, seed
    - Add helper methods: `getAllies(creature)`, `getEnemies(creature)`, `isAlly(creature1, creature2)`
    - _Requirements: 1.1, 6.1, 6.2_
  
  - [x] 2.2 Create TacticalDecision data class
    - Include action, target, position, path, reasoning
    - Create DecisionReasoning with behaviorPath, topScores, selectedScore, opportunities, resourcesUsed
    - _Requirements: 1.1, 6.4_
  
  - [x] 2.3 Create ActionCandidate and ScoredAction data classes
    - ActionCandidate: action, targets, positions, resourceCost
    - ScoredAction: candidate, score, breakdown
    - ScoreBreakdown: damageScore, hitProbabilityScore, targetPriorityScore, resourceCostScore, tacticalValueScore, positioningScore
    - ResourceCost: spellSlot, abilityUse, consumableItem
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [x] 2.4 Create Difficulty enum and Resource sealed interface
    - Difficulty: EASY, NORMAL, HARD with variance percentages
    - Resource: SpellSlot, LimitedAbility, ConsumableItem
    - _Requirements: 5.1, 9.1, 9.2_
  
  - [x] 2.5 Create TacticalOpportunity sealed interface
    - Flanking, ProneTarget, IncapacitatedTarget, ConcentrationBreak, MultiTargetAoE, ForcedMovement
    - Each with bonusScore property
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 3. Implement BehaviorTree framework
  - [x] 3.1 Create BehaviorNode sealed interface
    - Define `evaluate(creature, context): BehaviorResult` method
    - Create BehaviorResult sealed interface: Success, Failure, ActionCandidates
    - _Requirements: 1.1, 1.2_
  
  - [x] 3.2 Implement SelectorNode
    - Tries children in order until one succeeds
    - Returns first successful result or Failure if all fail
    - _Requirements: 1.2, 1.3_
  
  - [x] 3.3 Implement SequenceNode
    - Executes children in order until one fails
    - Returns Success only if all children succeed
    - _Requirements: 1.2_
  
  - [x] 3.4 Implement ConditionNode
    - Evaluates predicate function
    - Returns Success or Failure based on result
    - _Requirements: 1.2_
  
  - [x] 3.5 Implement ActionNode
    - Returns ActionCandidates with specified actionType and priority
    - _Requirements: 1.1, 1.4_
  
  - [x] 3.6 Create BehaviorTreeFactory
    - Factory methods for common trees: aggressiveMelee(), rangedAttacker(), spellcaster(), defensive()
    - Support for custom creature-specific trees
    - _Requirements: 1.5_

- [x] 4. Implement ActionCandidateGenerator
  - [x] 4.1 Create ActionCandidateGenerator class
    - Constructor: RulesEngine dependency
    - Method: `generate(creature, context, actionTypes): List<ActionCandidate>`
    - _Requirements: 2.1, 8.1_
  
  - [x] 4.2 Implement action generation logic
    - Generate attack actions for all weapons
    - Generate spell actions for available spell slots
    - Generate movement actions (Move, Dash, Disengage)
    - Generate defensive actions (Dodge, Help)
    - Generate ability actions (Second Wind, Action Surge, etc.)
    - _Requirements: 2.1, 5.1, 8.3_
  
  - [x] 4.3 Implement resource filtering
    - Check spell slot availability
    - Check limited ability uses remaining
    - Check consumable item availability
    - _Requirements: 5.1, 5.2, 5.4_
  
  - [x] 4.4 Implement search space limiting
    - Limit to top 20 action candidates
    - Limit to top 10 positions per action
    - Early pruning of obviously invalid actions
    - _Requirements: 7.2, 7.3_

- [x] 5. Implement ThreatAssessor
  - [x] 5.1 Create ThreatAssessor class
    - Method: `assessThreat(creature, context): Float`
    - Calculate threat score based on damage, healing, control, HP, concentration, role
    - _Requirements: 10.1, 10.2_
  
  - [x] 5.2 Implement damage output calculation
    - Estimate average damage per round
    - Consider multi-attack, spell damage, abilities
    - _Requirements: 10.2_
  
  - [x] 5.3 Implement healing capability calculation
    - Identify healing spells and abilities
    - Estimate average healing per round
    - _Requirements: 10.5_
  
  - [x] 5.4 Implement control potential calculation
    - Identify control spells (Hold Person, Slow, etc.)
    - Weight by save DC and target save bonuses
    - _Requirements: 10.2_
  
  - [x] 5.5 Implement concentration and role bonuses
    - Add bonus for active concentration spells
    - Add role-based bonuses (healer, spellcaster, striker, tank)
    - _Requirements: 10.4, 10.5_

- [x] 6. Implement OpportunityEvaluator
  - [x] 6.1 Create OpportunityEvaluator class
    - Constructor: GeometryCalculator dependency
    - Method: `evaluateOpportunities(action, context): List<TacticalOpportunity>`
    - _Requirements: 11.1_
  
  - [x] 6.2 Implement flanking detection
    - Check if action enables flanking position
    - Calculate flanking bonus
    - _Requirements: 11.1, 11.2_
  
  - [x] 6.3 Implement condition-based opportunities
    - Detect prone targets (advantage on melee)
    - Detect incapacitated targets (auto-crit)
    - Detect concentration targets (break concentration)
    - _Requirements: 11.3, 11.4_
  
  - [x] 6.4 Implement AoE opportunity detection
    - Count targets in AoE radius
    - Calculate multi-target bonus
    - Check for friendly fire
    - _Requirements: 11.5_
  
  - [x] 6.5 Implement forced movement opportunities
    - Detect hazards near targets (fire, cliffs)
    - Calculate forced movement bonus
    - _Requirements: 11.6_

- [ ] 7. Implement ActionScorer
  - [x] 7.1 Create ActionScorer class
    - Constructor: ThreatAssessor, OpportunityEvaluator, DiceRoller dependencies
    - Method: `scoreAll(candidates, context, difficulty): List<ScoredAction>`
    - _Requirements: 2.1, 2.2_
  
  - [x] 7.2 Implement damage score calculation
    - Calculate expected damage (base × hit probability)
    - Apply critical hit multiplier if advantage
    - Apply resistance/vulnerability/immunity multipliers
    - _Requirements: 2.2_
  
  - [x] 7.3 Implement hit probability calculation
    - Calculate from attack bonus vs target AC
    - Adjust for advantage/disadvantage
    - Handle saving throw spells
    - _Requirements: 2.2, 8.4_
  
  - [x] 7.4 Implement target priority scoring
    - Use ThreatAssessor for base priority
    - Apply role multipliers (healer, spellcaster, etc.)
    - Apply HP percentage multipliers
    - _Requirements: 2.2, 10.1_
  
  - [x] 7.5 Implement resource cost scoring
    - Penalize spell slot usage by level
    - Penalize limited ability usage
    - Penalize consumable item usage
    - _Requirements: 2.3, 5.2_
  
  - [x] 7.6 Implement tactical value scoring
    - Use OpportunityEvaluator for bonuses
    - Add flanking, AoE, concentration break bonuses
    - _Requirements: 2.3, 11.1_
  
  - [x] 7.7 Implement positioning score calculation
    - Score cover availability
    - Score optimal range
    - Penalize opportunity attacks
    - _Requirements: 2.4, 4.1, 4.2_
  
  - [x] 7.8 Implement difficulty-based variance
    - Apply random variance based on difficulty level
    - Use seeded DiceRoller for determinism
    - _Requirements: 2.6, 6.1, 9.1, 9.2_

- [x] 8. Implement TargetSelector
  - [x] 8.1 Create TargetSelector class
    - Constructor: ThreatAssessor dependency
    - Method: `selectTarget(action, candidates, context): Creature`
    - _Requirements: 3.1_
  
  - [x] 8.2 Implement threat-based selection
    - Weight threat level at 40%
    - Consider damage output, HP, conditions, healing
    - _Requirements: 3.2_
  
  - [x] 8.3 Implement vulnerability-based selection
    - Weight vulnerability at 30%
    - Consider HP percentage, AC, resistances, conditions
    - _Requirements: 3.3_
  
  - [x] 8.4 Implement tactical value selection
    - Weight tactical value at 30%
    - Consider role, concentration, position, range
    - _Requirements: 3.4_
  
  - [x] 8.5 Implement tie-breaking logic
    - Prefer targets within optimal range
    - Use seeded randomness for equal targets
    - _Requirements: 3.5, 3.7_

- [x] 9. Implement PositioningStrategy
  - [x] 9.1 Create PositioningStrategy class
    - Constructor: Pathfinder, GeometryCalculator dependencies
    - Method: `selectPosition(creature, action, target, context): PositionDecision`
    - Create PositionDecision data class: position, path, reasoning
    - _Requirements: 4.1_
  
  - [x] 9.2 Implement melee positioning logic
    - Prefer flanking positions
    - Avoid opportunity attacks
    - Stay adjacent to high-value targets
    - _Requirements: 4.3_
  
  - [x] 9.3 Implement ranged positioning logic
    - Prefer positions with cover
    - Maintain optimal range
    - Maximize distance from melee threats
    - _Requirements: 4.2_
  
  - [x] 9.4 Implement spellcaster positioning logic
    - Maximize AoE coverage
    - Avoid friendly fire
    - Maintain distance from threats
    - _Requirements: 4.5_
  
  - [x] 9.5 Implement defensive positioning logic
    - Maximize distance from threats when HP < 30%
    - Seek cover or concealment
    - Move toward allies for protection
    - _Requirements: 4.4, 12.1, 12.2_
  
  - [x] 9.6 Implement opportunity attack evaluation
    - Calculate risk vs benefit for movement
    - Prefer Disengage if multiple attacks provoked
    - _Requirements: 4.6_

- [ ] 10. Implement ResourceManager
  - [ ] 10.1 Create ResourceManager class
    - Method: `shouldUseResource(resource, action, context): Boolean`
    - _Requirements: 5.1, 5.2_
  
  - [ ] 10.2 Implement spell slot usage logic
    - Reserve high-level slots (7-9) for critical situations
    - Use mid-level slots (4-6) when 3+ enemies remain
    - Use low-level slots (1-3) freely in early combat
    - Prefer cantrips when resources low (<30%)
    - _Requirements: 5.2, 5.4, 5.6_
  
  - [ ] 10.3 Implement limited ability usage logic
    - Use powerful abilities when 4+ enemies remain
    - Reserve defensive abilities for HP < 50%
    - Use utility abilities freely
    - _Requirements: 5.3_
  
  - [ ] 10.4 Implement consumable item usage logic
    - Use healing potions when HP < 30% and no healer
    - Use buff potions before major encounters
    - Avoid consumables in trivial encounters
    - _Requirements: 5.5_
  
  - [ ] 10.5 Implement encounter difficulty estimation
    - Estimate remaining encounter duration
    - Count remaining enemies
    - Assess ally HP and resources
    - _Requirements: 5.2, 5.7_

- [ ] 11. Implement TacticalAgent (main orchestrator)
  - [ ] 11.1 Create TacticalAgent class
    - Constructor: all component dependencies
    - Method: `decideTurn(creature, context): TacticalDecision`
    - _Requirements: 1.1, 6.1_
  
  - [ ] 11.2 Implement decision pipeline
    - Evaluate behavior tree to get action types
    - Generate action candidates
    - Score all candidates
    - Select best action
    - Select target if needed
    - Select position if needed
    - Validate with RulesEngine
    - _Requirements: 1.1, 8.1, 8.2_
  
  - [ ] 11.3 Implement performance monitoring
    - Track time for each pipeline stage
    - Log warning if exceeds 300ms budget
    - Return best action found if timeout
    - _Requirements: 7.1, 7.2_
  
  - [ ] 11.4 Implement fallback logic
    - Handle invalid actions by selecting next-best
    - Fall back to Dodge if all actions invalid
    - Fall back to Attack nearest enemy if no valid actions
    - _Requirements: 1.4, 8.2_
  
  - [ ] 11.5 Implement decision logging
    - Log behavior tree path taken
    - Log top 3 scored actions
    - Log selected action and reasoning
    - Log opportunities identified
    - Log resources used
    - _Requirements: 6.4_
  
  - [ ] 11.6 Implement deterministic behavior
    - Use seeded DiceRoller for all randomness
    - Ensure same context + seed → same decision
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] 12. Implement defensive tactics
  - [ ] 12.1 Add defensive behavior to BehaviorTreeFactory
    - Create defensive tree for low HP situations
    - Prioritize healing, Disengage, Dodge
    - _Requirements: 12.1, 12.2_
  
  - [ ] 12.2 Implement healing action scoring
    - Score healing based on ally HP percentage
    - Prioritize critically wounded allies
    - _Requirements: 12.3_
  
  - [ ] 12.3 Implement defensive ability usage
    - Identify defensive spells (Shield, Absorb Elements)
    - Reserve resources for reactive defense
    - _Requirements: 12.4, 12.5_

- [ ] 13. Create integration use case
  - [ ] 13.1 Create DecideAITurn use case
    - Constructor: TacticalAgent, EventRepository dependencies
    - Method: `invoke(creatureId, encounterId): TacticalDecision`
    - Build TacticalContext from encounter state
    - Call TacticalAgent.decideTurn
    - Generate AIDecisionMade event
    - _Requirements: All requirements (integration)_
  
  - [ ] 13.2 Create AIDecisionMade event
    - Extends Event interface
    - Include creatureId, decision, timestamp, seed
    - _Requirements: 6.1, 6.5_

- [ ] 14. Write comprehensive tests
  - [ ] 14.1 Write BehaviorTree unit tests
    - Test each node type (Selector, Sequence, Condition, Action)
    - Test tree traversal logic
    - Test custom behavior trees
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 14.2 Write ActionScorer unit tests
    - Test scoring formula with known inputs
    - Test component score calculations
    - Test difficulty variance
    - Property-based tests for determinism
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [ ] 14.3 Write TargetSelector unit tests
    - Test target prioritization logic
    - Test threat assessment
    - Test tie-breaking
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  
  - [ ] 14.4 Write PositioningStrategy unit tests
    - Test position evaluation for different roles
    - Test pathfinding integration
    - Test opportunity attack avoidance
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_
  
  - [ ] 14.5 Write ResourceManager unit tests
    - Test resource usage decisions
    - Test spell slot conservation
    - Test ability usage thresholds
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
  
  - [ ] 14.6 Write ThreatAssessor unit tests
    - Test threat calculation
    - Test component scoring
    - Test role and concentration bonuses
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 14.7 Write OpportunityEvaluator unit tests
    - Test opportunity detection
    - Test flanking, AoE, concentration break
    - Test forced movement opportunities
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  
  - [ ] 14.8 Write TacticalAgent integration tests
    - Test full decision pipeline
    - Test deterministic behavior (same seed → same decision)
    - Test performance within 300ms budget
    - Test with various creature types and situations
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ] 14.9 Write rules engine integration tests
    - Test action validation
    - Test handling of invalid actions
    - Test action legality checks
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_
  
  - [ ] 14.10 Write combat scenario tests
    - Test outnumbered scenario (1 vs 3)
    - Test even fight (2 vs 2)
    - Test overwhelming advantage (4 vs 1)
    - Test mixed roles (tank, healer, striker, spellcaster)
    - Test low resources scenario
    - Test critical HP scenario
    - _Requirements: All requirements (scenario validation)_
  
  - [ ] 14.11 Write tactical scenario tests
    - Test flanking opportunities
    - Test cover usage
    - Test AoE spell opportunities
    - Test concentration spell protection
    - Test forced movement near hazards
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  
  - [ ] 14.12 Write property-based tests
    - Test determinism across 1000+ scenarios
    - Test performance across various complexities
    - Test fairness (AI uses same rules as player)
    - _Requirements: 6.1, 6.2, 7.1, 8.1_

- [ ] 15. Create module documentation
  - Create `ai/tactical/README.md` with architecture overview
  - Document behavior tree patterns and customization
  - Document scoring formula and weights
  - Document performance characteristics and optimization
  - Add KDoc comments to all public APIs
  - Include usage examples for common scenarios
  - Document testing approach and coverage targets
  - _Requirements: All requirements (documentation)_

---

**Implementation Notes:**

1. **Module Dependencies**: ai:tactical depends on core:domain, core:rules, and feature:map. Ensure no circular dependencies.

2. **Deterministic Behavior**: Always use seeded DiceRoller for randomness. Never use unseeded Random().

3. **Performance Budget**: Target 300ms total decision time. Profile and optimize if exceeding budget.

4. **Testing Strategy**: Implement features first, then write comprehensive tests. Focus on determinism, performance, and tactical correctness.

5. **Integration Points**: 
   - RulesEngine for action validation
   - Pathfinder for movement planning
   - GeometryCalculator for tactical calculations
   - EventRepository for decision logging

6. **Error Handling**: Always provide fallback actions. Never crash or block turns.

7. **Logging**: Use kotlin-logging for structured logging. Log decision rationale for debugging.

---

**Last Updated**: 2025-11-16
