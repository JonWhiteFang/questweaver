# Design Document

## Overview

The Tactical AI Agent provides intelligent combat decision-making for NPCs and AI-controlled party members. The system combines behavior trees for high-level decision structure with utility-based action scoring for tactical evaluation. This hybrid approach enables flexible AI personalities while maintaining deterministic, performant, and tactically sound behavior.

**Key Design Principles:**
- **Deterministic**: All decisions use seeded randomness for reproducible behavior
- **Performant**: Complete decisions within 300ms budget
- **Modular**: Separate concerns (behavior trees, scoring, targeting, positioning)
- **Extensible**: Easy to add new behaviors, actions, and tactics
- **Fair**: Respects game rules and validates all actions with RulesEngine

## Architecture

### High-Level Flow

```
TacticalAgent.decideTurn(creature, context)
  ↓
BehaviorTree.evaluate(creature, context)
  ↓
ActionCandidateGenerator.generate(creature, context)
  ↓
ActionScorer.scoreAll(candidates, context)
  ↓
TargetSelector.selectTarget(action, context)
  ↓
PositioningStrategy.selectPosition(action, target, context)
  ↓
RulesEngine.validate(action)
  ↓
Return TacticalDecision
```

### Module Location

**Location**: `ai/tactical/` (new module)

**Dependencies**:
- `core:domain` - entities, events, use cases
- `core:rules` - RulesEngine, DiceRoller, combat resolution
- `feature:map` - GridPos, pathfinding, geometry

**Module Rules**:
- ✅ Can depend on core:domain, core:rules, feature:map
- ❌ Cannot depend on feature:encounter (to avoid circular dependency)
- ✅ Must use seeded randomness (DiceRoller)
- ✅ Must validate all actions with RulesEngine

## Components and Interfaces

### 1. TacticalAgent (Main Entry Point)

```kotlin
/**
 * Main AI agent that decides actions for NPCs and AI-controlled party members.
 */
class TacticalAgent(
    private val behaviorTreeFactory: BehaviorTreeFactory,
    private val actionCandidateGenerator: ActionCandidateGenerator,
    private val actionScorer: ActionScorer,
    private val targetSelector: TargetSelector,
    private val positioningStrategy: PositioningStrategy,
    private val rulesEngine: RulesEngine,
    private val roller: DiceRoller
) {
    /**
     * Decides the best action for a creature's turn.
     * 
     * @param creature The creature making the decision
     * @param context Current tactical situation
     * @return TacticalDecision with action, target, and position
     */
    suspend fun decideTurn(
        creature: Creature,
        context: TacticalContext
    ): TacticalDecision
}
```

**Responsibilities**:
- Orchestrate the decision-making pipeline
- Enforce performance budget (300ms)
- Log decision rationale for debugging
- Handle fallback to default actions if pipeline fails

### 2. BehaviorTree (Decision Structure)

```kotlin
/**
 * Hierarchical decision tree that selects high-level behaviors.
 */
sealed interface BehaviorNode {
    suspend fun evaluate(creature: Creature, context: TacticalContext): BehaviorResult
}

/**
 * Selector node: tries children in order until one succeeds.
 */
data class SelectorNode(
    val children: List<BehaviorNode>
) : BehaviorNode

/**
 * Sequence node: executes children in order until one fails.
 */
data class SequenceNode(
    val children: List<BehaviorNode>
) : BehaviorNode

/**
 * Condition node: checks a predicate.
 */
data class ConditionNode(
    val predicate: suspend (Creature, TacticalContext) -> Boolean
) : BehaviorNode

/**
 * Action node: generates action candidates for scoring.
 */
data class ActionNode(
    val actionType: ActionType,
    val priority: Int
) : BehaviorNode

sealed interface BehaviorResult {
    object Success : BehaviorResult
    object Failure : BehaviorResult
    data class ActionCandidates(val candidates: List<ActionCandidate>) : BehaviorResult
}
```

**Common Behavior Trees**:

**Aggressive Melee**:
```
Selector
├─ Sequence (Low HP)
│  ├─ Condition: HP < 30%
│  └─ Action: Disengage or Dodge
├─ Sequence (Can Attack)
│  ├─ Condition: Enemy in melee range
│  └─ Action: Attack
├─ Sequence (Move to Attack)
│  ├─ Condition: Enemy within movement + melee range
│  └─ Action: Move + Attack
└─ Action: Dash toward nearest enemy
```

**Ranged Attacker**:
```
Selector
├─ Sequence (Threatened)
│  ├─ Condition: Enemy in melee range
│  └─ Action: Disengage + Move to cover
├─ Sequence (Has Clear Shot)
│  ├─ Condition: Enemy in range, no cover
│  └─ Action: Attack
├─ Sequence (Reposition)
│  ├─ Condition: Can reach better position
│  └─ Action: Move to cover with line of sight
└─ Action: Hold position, Dodge
```

**Spellcaster**:
```
Selector
├─ Sequence (Concentration Broken)
│  ├─ Condition: Lost concentration this round
│  └─ Action: Re-cast concentration spell
├─ Sequence (High-Value AoE)
│  ├─ Condition: 3+ enemies clustered
│  └─ Action: Fireball or similar
├─ Sequence (Buff Allies)
│  ├─ Condition: Allies unbuffed, spell slots available
│  └─ Action: Haste, Bless, etc.
├─ Sequence (Debuff Enemies)
│  ├─ Condition: High-value target, spell slots available
│  └─ Action: Hold Person, Slow, etc.
└─ Action: Cantrip attack
```

### 3. ActionCandidateGenerator

```kotlin
/**
 * Generates all valid action candidates for a creature.
 */
class ActionCandidateGenerator(
    private val rulesEngine: RulesEngine
) {
    /**
     * Generates action candidates based on behavior tree results.
     * 
     * @param creature The creature generating actions
     * @param context Current tactical situation
     * @param actionTypes Preferred action types from behavior tree
     * @return List of valid action candidates
     */
    suspend fun generate(
        creature: Creature,
        context: TacticalContext,
        actionTypes: List<ActionType>
    ): List<ActionCandidate>
}

data class ActionCandidate(
    val action: Action,
    val targets: List<Creature>,
    val positions: List<GridPos>,
    val resourceCost: ResourceCost
)

data class ResourceCost(
    val spellSlot: Int? = null,
    val abilityUse: String? = null,
    val consumableItem: String? = null
)
```

**Responsibilities**:
- Generate all legal actions for the creature
- Filter by available resources (spell slots, abilities)
- Limit search space for performance (e.g., top 10 positions)
- Validate basic legality (range, line of sight)

### 4. ActionScorer (Utility-Based Evaluation)

```kotlin
/**
 * Scores action candidates based on tactical value.
 */
class ActionScorer(
    private val threatAssessor: ThreatAssessor,
    private val opportunityEvaluator: OpportunityEvaluator,
    private val roller: DiceRoller
) {
    /**
     * Scores all action candidates and returns them sorted by score.
     * 
     * @param candidates Action candidates to score
     * @param context Current tactical situation
     * @param difficulty AI difficulty level
     * @return Candidates sorted by score (highest first)
     */
    suspend fun scoreAll(
        candidates: List<ActionCandidate>,
        context: TacticalContext,
        difficulty: Difficulty
    ): List<ScoredAction>
}

data class ScoredAction(
    val candidate: ActionCandidate,
    val score: Float,
    val breakdown: ScoreBreakdown
)

data class ScoreBreakdown(
    val damageScore: Float,
    val hitProbabilityScore: Float,
    val targetPriorityScore: Float,
    val resourceCostScore: Float,
    val tacticalValueScore: Float,
    val positioningScore: Float
)
```

**Scoring Formula**:

```
Total Score = (Damage × HitProb × TargetPriority) 
            + TacticalValue 
            + PositioningValue 
            - ResourceCost
            + RandomVariance
```

**Component Scores**:

1. **Damage Score**: Expected damage output
   - Base damage × hit probability
   - Multiply by 1.5 for critical hits (if advantage)
   - Multiply by 2.0 for vulnerable targets
   - Multiply by 0.5 for resistant targets
   - Zero for immune targets

2. **Hit Probability Score**: Likelihood of success
   - Calculate from attack bonus vs target AC
   - Advantage: +25% effective probability
   - Disadvantage: -25% effective probability
   - Saving throw: use target's save modifier

3. **Target Priority Score**: Value of the target
   - Base: 1.0 for standard enemies
   - Healer: 2.0 multiplier
   - Spellcaster: 1.8 multiplier
   - Concentrating: 1.5 multiplier
   - Low HP (<30%): 1.3 multiplier
   - High threat: 1.2 multiplier

4. **Resource Cost Score**: Penalty for expensive actions
   - Spell slot level × -10
   - Limited ability use × -15
   - Consumable item × -20

5. **Tactical Value Score**: Situational bonuses
   - Flanking: +15
   - High ground: +10
   - Cover for target: -10
   - AoE hitting multiple targets: +20 per additional target
   - Breaking concentration: +25

6. **Positioning Score**: Quality of resulting position
   - Cover available: +15
   - Optimal range: +10
   - Near allies: +5
   - Exposed position: -20
   - Provokes opportunity attacks: -15 per attack

7. **Random Variance**: Controlled randomness
   - Easy: ±30% variance (more unpredictable)
   - Normal: ±15% variance
   - Hard: ±5% variance (more optimal)

### 5. TargetSelector

```kotlin
/**
 * Selects the best target for an action.
 */
class TargetSelector(
    private val threatAssessor: ThreatAssessor
) {
    /**
     * Selects the optimal target for an action.
     * 
     * @param action The action being performed
     * @param candidates Potential targets
     * @param context Current tactical situation
     * @return Selected target
     */
    suspend fun selectTarget(
        action: Action,
        candidates: List<Creature>,
        context: TacticalContext
    ): Creature
}
```

**Target Selection Criteria**:

1. **Threat Assessment** (40% weight)
   - Damage output per round
   - HP remaining
   - Active conditions (Haste, Bless, etc.)
   - Healing capability

2. **Vulnerability** (30% weight)
   - Current HP percentage
   - AC relative to attack bonus
   - Resistances/immunities
   - Active defensive conditions

3. **Tactical Value** (30% weight)
   - Role (healer > spellcaster > striker > tank)
   - Concentration spell active
   - Position (isolated, exposed)
   - Range (prefer closer targets if tied)

### 6. PositioningStrategy

```kotlin
/**
 * Determines optimal positioning for actions.
 */
class PositioningStrategy(
    private val pathfinder: Pathfinder,
    private val geometryCalculator: GeometryCalculator
) {
    /**
     * Selects the best position for an action.
     * 
     * @param creature The creature moving
     * @param action The action being performed
     * @param target The target of the action
     * @param context Current tactical situation
     * @return Optimal position and path
     */
    suspend fun selectPosition(
        creature: Creature,
        action: Action,
        target: Creature?,
        context: TacticalContext
    ): PositionDecision
}

data class PositionDecision(
    val position: GridPos,
    val path: List<GridPos>,
    val reasoning: String
)
```

**Positioning Heuristics**:

**Melee Attackers**:
- Prefer positions that enable flanking (+2 to hit)
- Avoid positions that provoke opportunity attacks
- Stay adjacent to high-value targets
- Move to block enemy movement if advantageous

**Ranged Attackers**:
- Prefer positions with cover (half or three-quarters)
- Maintain optimal range (no disadvantage)
- Maximize distance from melee threats
- Ensure line of sight to priority targets

**Spellcasters**:
- Prefer positions that maximize AoE coverage
- Avoid friendly fire
- Maintain distance from threats
- Position for concentration spell protection

**Low HP Creatures**:
- Maximize distance from all threats
- Seek cover or concealment
- Move toward allies for protection
- Disengage if surrounded

### 7. ThreatAssessor

```kotlin
/**
 * Evaluates threat levels of enemy creatures.
 */
class ThreatAssessor {
    /**
     * Calculates threat score for a creature.
     * 
     * @param creature The creature to assess
     * @param context Current tactical situation
     * @return Threat score (higher = more dangerous)
     */
    fun assessThreat(
        creature: Creature,
        context: TacticalContext
    ): Float
}
```

**Threat Calculation**:

```
Threat = (DamagePerRound × 2.0)
       + (HealingPerRound × 3.0)
       + (ControlPotential × 2.5)
       + (HPRemaining × 0.1)
       + ConcentrationBonus
       + RoleBonus
```

- **DamagePerRound**: Average damage output
- **HealingPerRound**: Average healing output (healers are high priority)
- **ControlPotential**: Ability to disable allies (Hold Person, etc.)
- **HPRemaining**: Durability factor
- **ConcentrationBonus**: +50 if concentrating on powerful spell
- **RoleBonus**: Healer +30, Spellcaster +20, Striker +10, Tank +0

### 8. OpportunityEvaluator

```kotlin
/**
 * Identifies tactical opportunities on the battlefield.
 */
class OpportunityEvaluator(
    private val geometryCalculator: GeometryCalculator
) {
    /**
     * Evaluates tactical opportunities for an action.
     * 
     * @param action The action being considered
     * @param context Current tactical situation
     * @return List of identified opportunities
     */
    fun evaluateOpportunities(
        action: Action,
        context: TacticalContext
    ): List<TacticalOpportunity>
}

sealed interface TacticalOpportunity {
    val bonusScore: Float
    
    data class Flanking(override val bonusScore: Float = 15f) : TacticalOpportunity
    data class ProneTarget(override val bonusScore: Float = 20f) : TacticalOpportunity
    data class IncapacitatedTarget(override val bonusScore: Float = 30f) : TacticalOpportunity
    data class ConcentrationBreak(override val bonusScore: Float = 25f) : TacticalOpportunity
    data class MultiTargetAoE(val targetCount: Int, override val bonusScore: Float) : TacticalOpportunity
    data class ForcedMovement(val hazardNearby: Boolean, override val bonusScore: Float) : TacticalOpportunity
}
```

### 9. ResourceManager

```kotlin
/**
 * Manages limited resources (spell slots, abilities, consumables).
 */
class ResourceManager {
    /**
     * Decides whether to use a resource for an action.
     * 
     * @param resource The resource being considered
     * @param action The action requiring the resource
     * @param context Current tactical situation
     * @return Whether to use the resource
     */
    fun shouldUseResource(
        resource: Resource,
        action: Action,
        context: TacticalContext
    ): Boolean
}

sealed interface Resource {
    data class SpellSlot(val level: Int) : Resource
    data class LimitedAbility(val name: String, val usesRemaining: Int) : Resource
    data class ConsumableItem(val name: String) : Resource
}
```

**Resource Usage Heuristics**:

**Spell Slots**:
- Use high-level slots (7-9) only for critical situations
- Use mid-level slots (4-6) when 3+ enemies remain
- Use low-level slots (1-3) freely in early combat
- Prefer cantrips when resources are low (<30% remaining)

**Limited Abilities**:
- Use powerful abilities (Action Surge, Rage) when 4+ enemies remain
- Reserve defensive abilities (Second Wind) for HP < 50%
- Use utility abilities (Cunning Action) freely

**Consumable Items**:
- Use healing potions when HP < 30% and no healer available
- Use buff potions before major encounters
- Avoid using consumables in trivial encounters

## Data Models

### TacticalContext

```kotlin
data class TacticalContext(
    val encounterId: Long,
    val round: Int,
    val creatures: List<Creature>,
    val allies: List<Creature>,
    val enemies: List<Creature>,
    val mapState: MapState,
    val activeConditions: Map<Long, List<Condition>>,
    val concentrationSpells: Map<Long, Spell>,
    val recentDamage: Map<Long, Int>, // Last 2 rounds
    val seed: Long // For deterministic randomness
)
```

### TacticalDecision

```kotlin
data class TacticalDecision(
    val action: Action,
    val target: Creature?,
    val position: GridPos?,
    val path: List<GridPos>,
    val reasoning: DecisionReasoning
)

data class DecisionReasoning(
    val behaviorPath: String, // e.g., "Aggressive > CanAttack > Attack"
    val topScores: List<ScoredAction>, // Top 3 for debugging
    val selectedScore: Float,
    val opportunities: List<TacticalOpportunity>,
    val resourcesUsed: List<Resource>
)
```

### Difficulty

```kotlin
enum class Difficulty {
    EASY,    // Makes suboptimal decisions 30% of time
    NORMAL,  // Makes optimal decisions with occasional errors
    HARD     // Makes optimal decisions consistently
}
```

## Error Handling

### Validation Failures

**Scenario**: RulesEngine rejects the selected action

**Handling**:
1. Log the validation failure with action details
2. Remove invalid action from candidates
3. Select next-best action from scored list
4. If all actions invalid, fall back to Dodge action
5. Never crash or block the turn

### Performance Timeout

**Scenario**: Decision takes longer than 300ms

**Handling**:
1. Log performance warning with timing breakdown
2. Return best action found so far
3. If no action selected yet, return default action (Attack nearest enemy or Dodge)
4. Track timeout occurrences for optimization

### No Valid Actions

**Scenario**: No legal actions available (rare edge case)

**Handling**:
1. Log the situation for investigation
2. Return Dodge action as safe default
3. If Dodge unavailable, return Pass turn
4. Never crash or block the turn

### Resource Tracking Errors

**Scenario**: Resource state inconsistent with creature state

**Handling**:
1. Log the inconsistency with details
2. Refresh resource state from creature
3. Re-evaluate action candidates with corrected state
4. Continue with decision process

## Testing Strategy

### Unit Tests

**BehaviorTree**:
- Test each node type (Selector, Sequence, Condition, Action)
- Verify tree traversal logic
- Test custom behavior trees for different creature types

**ActionScorer**:
- Test scoring formula with known inputs
- Verify component score calculations
- Test difficulty variance application
- Property-based tests for score consistency

**TargetSelector**:
- Test target prioritization logic
- Verify threat assessment calculations
- Test edge cases (all targets equal, single target)

**PositioningStrategy**:
- Test position evaluation for different roles
- Verify pathfinding integration
- Test opportunity attack avoidance
- Test cover and range optimization

**ResourceManager**:
- Test resource usage decisions
- Verify spell slot conservation
- Test ability usage thresholds

### Integration Tests

**Full Decision Pipeline**:
- Test complete decision flow from context to action
- Verify deterministic behavior with same seed
- Test performance within 300ms budget
- Test with various creature types and situations

**Rules Engine Integration**:
- Verify all generated actions are validated
- Test handling of invalid actions
- Verify action legality checks

**Map Integration**:
- Test pathfinding integration
- Verify geometry calculations (flanking, cover, range)
- Test line of sight checks

### Property-Based Tests

**Determinism**:
- Same context + same seed → same decision
- Verify across 1000+ random scenarios

**Performance**:
- All decisions complete within 300ms
- Verify across various creature counts and complexity

**Fairness**:
- AI never cheats (uses same rules as player)
- All actions validated by RulesEngine

### Scenario Tests

**Combat Scenarios**:
- Outnumbered (1 vs 3)
- Even fight (2 vs 2)
- Overwhelming advantage (4 vs 1)
- Mixed roles (tank, healer, striker, spellcaster)
- Low resources (few spell slots remaining)
- Critical HP (multiple creatures below 30%)

**Tactical Scenarios**:
- Flanking opportunities
- Cover usage
- AoE spell opportunities
- Concentration spell protection
- Forced movement near hazards

## Performance Considerations

### Decision Budget: 300ms

**Breakdown**:
- Behavior tree evaluation: 20ms
- Action candidate generation: 50ms
- Action scoring: 150ms
- Target selection: 30ms
- Positioning evaluation: 40ms
- Validation: 10ms

### Optimization Strategies

1. **Limit Search Space**:
   - Evaluate top 10 positions per action (not all reachable positions)
   - Limit action candidates to 20 per creature
   - Cache threat assessments per round

2. **Early Pruning**:
   - Skip scoring for obviously bad actions (out of range, no resources)
   - Prune positions that provoke 3+ opportunity attacks
   - Skip targets with immunity to action's damage type

3. **Efficient Calculations**:
   - Pre-calculate distances and ranges
   - Cache pathfinding results for common positions
   - Use fast heuristics instead of simulation

4. **Parallel Evaluation** (Future):
   - Score action candidates in parallel
   - Evaluate positions concurrently
   - Requires careful seed management for determinism

## Dependencies

### Production Dependencies

- `core:domain` - Creature, Action, Event entities
- `core:rules` - RulesEngine, DiceRoller, combat resolution
- `feature:map` - GridPos, Pathfinder, GeometryCalculator
- `kotlinx-coroutines` - Async operations
- `kotlin-logging` - Structured logging

### Test Dependencies

- `kotest` - Testing framework
- `mockk` - Mocking
- `kotest-property` - Property-based testing

## Future Enhancements

### Machine Learning Integration

**Behavior Learning**:
- Train behavior trees from player combat logs
- Learn optimal action scoring weights
- Adapt difficulty based on player performance

**Tactical Pattern Recognition**:
- Identify player strategies
- Counter common tactics
- Learn from player mistakes

### Advanced Tactics

**Coordinated Actions**:
- Multi-creature combos
- Synchronized attacks
- Tactical formations

**Environmental Awareness**:
- Use terrain features (high ground, chokepoints)
- Exploit hazards (push enemies into fire)
- Dynamic cover usage

**Psychological Tactics**:
- Feints and misdirection
- Morale effects
- Intimidation and fear

---

**Last Updated**: 2025-11-16
