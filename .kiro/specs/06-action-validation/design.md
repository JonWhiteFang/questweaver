# Action Validation System Design

## Overview

The Action Validation System is a pure Kotlin module in `core/rules` that validates the legality of player and AI actions before execution. It ensures actions comply with D&D 5e SRD rules by checking action economy, resource availability, range requirements, and line-of-effect constraints. The system is deterministic, stateless, and integrates with the Combat Rules Engine to provide immediate feedback on action validity without modifying game state.

**Key Design Principles:**
- Pure Kotlin with no Android dependencies (core:rules module)
- Stateless validation functions that return immutable result objects
- Deterministic behavior for consistent validation across replays
- Exhaustive sealed types for validation results
- Integration with Combat Rules Engine for spatial calculations
- Clear, actionable error messages for UI display

## Architecture

### Module Location
`core/rules/src/main/kotlin/dev/questweaver/core/rules/validation/`

### Package Structure
```
core/rules/validation/
├── ActionValidator.kt              # Main validation orchestrator
├── validators/
│   ├── ActionEconomyValidator.kt   # Action/bonus/reaction/movement validation
│   ├── ResourceValidator.kt        # Spell slots, class features, item charges
│   ├── RangeValidator.kt           # Distance and line-of-effect validation
│   ├── ConcentrationValidator.kt   # Concentration spell validation
│   └── ConditionValidator.kt       # Condition-based action restrictions
├── state/
│   ├── TurnState.kt                # Current turn phase and resource tracking
│   ├── ResourcePool.kt             # Resource availability tracking
│   └── ConcentrationState.kt      # Active concentration tracking
└── results/
    ├── ValidationResult.kt         # Sealed validation result types
    ├── ValidationFailure.kt        # Failure reasons with context
    └── ResourceCost.kt             # Resources consumed by action
```

### Dependencies
- `core:domain` - Entity definitions (Creature, Action types, GridPos)
- `05-combat-rules` - Range calculations, line-of-effect, condition effects
- `04-dice-system` - DiceRoller (for concentration checks, recharge rolls)

## Components and Interfaces

### 1. ActionValidator

Main orchestrator that coordinates all validation checks.

```kotlin
class ActionValidator(
    private val actionEconomyValidator: ActionEconomyValidator,
    private val resourceValidator: ResourceValidator,
    private val rangeValidator: RangeValidator,
    private val concentrationValidator: ConcentrationValidator,
    private val conditionValidator: ConditionValidator
) {
    /**
     * Validates whether an action can be performed given current game state.
     *
     * @param action The action to validate
     * @param actor The creature attempting the action
     * @param turnState Current turn phase and resource availability
     * @param encounterState Current encounter state (positions, conditions, etc.)
     * @return ValidationResult indicating success, failure, or required choices
     */
    fun validate(
        action: GameAction,
        actor: Creature,
        turnState: TurnState,
        encounterState: EncounterState
    ): ValidationResult
}
```

**Validation Flow:**
1. Check if actor's conditions prevent actions (Stunned, Incapacitated, etc.)
2. Validate action economy (has action/bonus/reaction/movement available)
3. Validate resource requirements (spell slots, class features, etc.)
4. Validate spatial requirements (range, line-of-effect)
5. Validate concentration requirements (if casting concentration spell)
6. Return ValidationResult with success or specific failure reason

### 2. ActionEconomyValidator

Validates action economy constraints (action, bonus action, reaction, movement).

```kotlin
class ActionEconomyValidator {
    /**
     * Validates whether the action can be taken given current action economy.
     *
     * @param action The action to validate
     * @param turnState Current turn state with action economy tracking
     * @return ValidationResult indicating success or failure with reason
     */
    fun validateActionEconomy(
        action: GameAction,
        turnState: TurnState
    ): ValidationResult
    
    /**
     * Determines which action economy resources would be consumed.
     *
     * @param action The action being validated
     * @return Set of action economy resources consumed
     */
    fun getActionCost(action: GameAction): Set<ActionEconomyResource>
}
```

**Action Economy Resources:**
```kotlin
enum class ActionEconomyResource {
    Action,        // Standard action
    BonusAction,   // Bonus action
    Reaction,      // Reaction (triggered)
    Movement,      // Movement (in feet)
    FreeAction     // Free action (no cost)
}
```

**Validation Rules:**
- Attack actions consume Action
- Dash/Disengage/Dodge consume Action
- Bonus action spells consume BonusAction
- Opportunity attacks consume Reaction
- Movement actions consume Movement (tracked in feet)
- Can't take Action if already used this turn
- Can't take BonusAction if already used this turn
- Can't take Reaction if already used this round
- Can't move more than remaining movement

### 3. ResourceValidator

Validates resource availability (spell slots, class features, item charges).

```kotlin
class ResourceValidator {
    /**
     * Validates whether the actor has required resources.
     *
     * @param action The action requiring resources
     * @param actor The creature with resource pools
     * @param resourcePool Current resource availability
     * @return ValidationResult indicating success or failure with missing resources
     */
    fun validateResources(
        action: GameAction,
        actor: Creature,
        resourcePool: ResourcePool
    ): ValidationResult
    
    /**
     * Determines which resources would be consumed.
     *
     * @param action The action being validated
     * @param actor The creature performing the action
     * @return ResourceCost with specific resources consumed
     */
    fun getResourceCost(
        action: GameAction,
        actor: Creature
    ): ResourceCost
}
```

**Resource Types:**
```kotlin
sealed interface Resource {
    data class SpellSlot(val level: Int) : Resource
    data class ClassFeature(val featureId: String, val uses: Int) : Resource
    data class ItemCharge(val itemId: Long, val charges: Int) : Resource
    data class HitDice(val diceType: String, val count: Int) : Resource
}

data class ResourcePool(
    val spellSlots: Map<Int, Int>,           // Level -> remaining slots
    val classFeatures: Map<String, Int>,     // Feature ID -> remaining uses
    val itemCharges: Map<Long, Int>,         // Item ID -> remaining charges
    val hitDice: Map<String, Int>            // Dice type -> remaining dice
) {
    fun hasResource(resource: Resource): Boolean
    fun consume(resource: Resource): ResourcePool
}
```

**Validation Rules:**
- Spell casting requires available spell slot of required level or higher
- Class features require remaining uses
- Item usage requires remaining charges
- Upcasting spells consumes higher-level slot
- Cantrips don't consume spell slots

### 4. RangeValidator

Validates range and line-of-effect requirements using Combat Rules Engine.

```kotlin
class RangeValidator(
    private val geometryCalculator: GeometryCalculator
) {
    /**
     * Validates whether the target is within range and line-of-effect.
     *
     * @param action The action with range requirements
     * @param actorPos The actor's position
     * @param targetPos The target's position (if applicable)
     * @param encounterState Current encounter state with obstacles
     * @return ValidationResult indicating success or failure with distance/obstacle info
     */
    fun validateRange(
        action: GameAction,
        actorPos: GridPos,
        targetPos: GridPos?,
        encounterState: EncounterState
    ): ValidationResult
    
    /**
     * Calculates distance between two positions using D&D 5e rules.
     *
     * @param from Source position
     * @param to Target position
     * @return Distance in feet (5ft per square, diagonal movement)
     */
    fun calculateDistance(from: GridPos, to: GridPos): Int
    
    /**
     * Checks if line-of-effect exists between two positions.
     *
     * @param from Source position
     * @param to Target position
     * @param obstacles Set of obstacle positions
     * @return True if unobstructed path exists
     */
    fun hasLineOfEffect(
        from: GridPos,
        to: GridPos,
        obstacles: Set<GridPos>
    ): Boolean
}
```

**Range Types:**
```kotlin
sealed interface Range {
    object Touch : Range                    // 5 feet
    data class Feet(val distance: Int) : Range  // Specific distance
    object Sight : Range                    // Line of sight
    object Self : Range                     // Self only
    data class Radius(val feet: Int) : Range    // Area around self
}
```

**Validation Rules:**
- Touch range requires target within 5 feet
- Ranged actions require target within specified distance
- Line-of-effect required for most ranged actions
- Self-targeted actions don't require range validation
- Area effects validate all targets within radius

### 5. ConcentrationValidator

Validates concentration spell requirements.

```kotlin
class ConcentrationValidator {
    /**
     * Validates whether a concentration spell can be cast.
     *
     * @param spell The spell requiring concentration
     * @param actor The creature casting the spell
     * @param concentrationState Current concentration tracking
     * @return ValidationResult indicating success or warning about breaking concentration
     */
    fun validateConcentration(
        spell: Spell,
        actor: Creature,
        concentrationState: ConcentrationState
    ): ValidationResult
    
    /**
     * Checks if the actor is currently concentrating.
     *
     * @param actorId The creature ID
     * @param concentrationState Current concentration tracking
     * @return Active concentration spell, if any
     */
    fun getActiveConcentration(
        actorId: Long,
        concentrationState: ConcentrationState
    ): Spell?
}
```

**Concentration State:**
```kotlin
data class ConcentrationState(
    val activeConcentrations: Map<Long, ConcentrationInfo>  // Creature ID -> concentration info
) {
    fun isConcentrating(creatureId: Long): Boolean
    fun getConcentration(creatureId: Long): ConcentrationInfo?
    fun startConcentration(creatureId: Long, spell: Spell): ConcentrationState
    fun breakConcentration(creatureId: Long): ConcentrationState
}

data class ConcentrationInfo(
    val spell: Spell,
    val startedRound: Int,
    val dc: Int  // Concentration save DC (10 or half damage, whichever is higher)
)
```

**Validation Rules:**
- Only one concentration spell active per creature
- Casting new concentration spell breaks existing concentration
- Non-concentration spells can be cast while concentrating
- Concentration broken by Incapacitated or Unconscious conditions
- Concentration requires saving throw when taking damage

### 6. ConditionValidator

Validates whether conditions prevent actions.

```kotlin
class ConditionValidator(
    private val conditionRegistry: ConditionRegistry
) {
    /**
     * Validates whether the actor's conditions allow the action.
     *
     * @param action The action to validate
     * @param actor The creature with conditions
     * @return ValidationResult indicating success or failure with blocking condition
     */
    fun validateConditions(
        action: GameAction,
        actor: Creature
    ): ValidationResult
    
    /**
     * Checks if any conditions prevent all actions.
     *
     * @param conditions Active conditions on the creature
     * @return Blocking condition, if any
     */
    fun getBlockingCondition(conditions: Set<Condition>): Condition?
}
```

**Condition Restrictions:**
- Stunned: Cannot take actions or reactions, cannot move
- Incapacitated: Cannot take actions or reactions
- Paralyzed: Cannot take actions or reactions, cannot move
- Unconscious: Cannot take actions or reactions, cannot move
- Prone: Movement costs double, disadvantage on attacks
- Restrained: Movement speed is 0, disadvantage on attacks

## Data Models

### Sealed Types

```kotlin
// Validation results
sealed interface ValidationResult {
    data class Success(val resourceCost: ResourceCost) : ValidationResult
    data class Failure(val reason: ValidationFailure) : ValidationResult
    data class RequiresChoice(val choices: List<ActionChoice>) : ValidationResult
}

// Validation failures with context
sealed interface ValidationFailure {
    data class ActionEconomyExhausted(
        val required: ActionEconomyResource,
        val alreadyUsed: Boolean
    ) : ValidationFailure
    
    data class InsufficientResources(
        val required: Resource,
        val available: Int,
        val needed: Int
    ) : ValidationFailure
    
    data class OutOfRange(
        val actualDistance: Int,
        val maxRange: Int,
        val rangeType: Range
    ) : ValidationFailure
    
    data class LineOfEffectBlocked(
        val blockingObstacle: GridPos,
        val obstacleType: String
    ) : ValidationFailure
    
    data class ConcentrationConflict(
        val activeSpell: Spell,
        val newSpell: Spell
    ) : ValidationFailure
    
    data class ConditionPreventsAction(
        val condition: Condition,
        val reason: String
    ) : ValidationFailure
    
    data class InvalidTarget(
        val reason: String
    ) : ValidationFailure
}

// Resource costs
data class ResourceCost(
    val actionEconomy: Set<ActionEconomyResource>,
    val resources: Set<Resource>,
    val movementCost: Int,  // In feet
    val breaksConcentration: Boolean
) {
    companion object {
        val None = ResourceCost(
            actionEconomy = emptySet(),
            resources = emptySet(),
            movementCost = 0,
            breaksConcentration = false
        )
    }
}

// Action choices (for disambiguation)
sealed interface ActionChoice {
    data class SpellSlotLevel(
        val minLevel: Int,
        val availableLevels: List<Int>
    ) : ActionChoice
    
    data class TargetSelection(
        val validTargets: List<Long>,
        val minTargets: Int,
        val maxTargets: Int
    ) : ActionChoice
    
    data class FeatureOption(
        val featureId: String,
        val options: List<String>
    ) : ActionChoice
}
```

### Turn State

```kotlin
data class TurnState(
    val creatureId: Long,
    val round: Int,
    val actionUsed: Boolean,
    val bonusActionUsed: Boolean,
    val reactionUsed: Boolean,
    val movementUsed: Int,      // Feet of movement used
    val movementTotal: Int,     // Total movement speed
    val resourcePool: ResourcePool,
    val concentrationState: ConcentrationState
) {
    fun remainingMovement(): Int = movementTotal - movementUsed
    
    fun hasActionAvailable(): Boolean = !actionUsed
    fun hasBonusActionAvailable(): Boolean = !bonusActionUsed
    fun hasReactionAvailable(): Boolean = !reactionUsed
    
    fun useAction(): TurnState = copy(actionUsed = true)
    fun useBonusAction(): TurnState = copy(bonusActionUsed = true)
    fun useReaction(): TurnState = copy(reactionUsed = true)
    fun useMovement(feet: Int): TurnState = copy(movementUsed = movementUsed + feet)
    
    fun consumeResources(cost: ResourceCost): TurnState = copy(
        actionUsed = actionUsed || ActionEconomyResource.Action in cost.actionEconomy,
        bonusActionUsed = bonusActionUsed || ActionEconomyResource.BonusAction in cost.actionEconomy,
        reactionUsed = reactionUsed || ActionEconomyResource.Reaction in cost.actionEconomy,
        movementUsed = movementUsed + cost.movementCost,
        resourcePool = cost.resources.fold(resourcePool) { pool, resource ->
            pool.consume(resource)
        },
        concentrationState = if (cost.breaksConcentration) {
            concentrationState.breakConcentration(creatureId)
        } else {
            concentrationState
        }
    )
}
```

### Game Actions

```kotlin
sealed interface GameAction {
    val actorId: Long
    val actionType: ActionType
    
    data class Attack(
        override val actorId: Long,
        val targetId: Long,
        val weaponId: Long?
    ) : GameAction {
        override val actionType = ActionType.Action
    }
    
    data class CastSpell(
        override val actorId: Long,
        val spellId: String,
        val targetIds: List<Long>,
        val targetPos: GridPos?,
        val slotLevel: Int?  // Null for cantrips
    ) : GameAction {
        override val actionType: ActionType  // Determined by spell
    }
    
    data class Move(
        override val actorId: Long,
        val path: List<GridPos>
    ) : GameAction {
        override val actionType = ActionType.Movement
    }
    
    data class Dash(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }
    
    data class Disengage(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }
    
    data class Dodge(
        override val actorId: Long
    ) : GameAction {
        override val actionType = ActionType.Action
    }
    
    data class UseClassFeature(
        override val actorId: Long,
        val featureId: String,
        val targetId: Long?
    ) : GameAction {
        override val actionType: ActionType  // Determined by feature
    }
    
    data class OpportunityAttack(
        override val actorId: Long,
        val targetId: Long
    ) : GameAction {
        override val actionType = ActionType.Reaction
    }
}

enum class ActionType {
    Action,
    BonusAction,
    Reaction,
    Movement,
    FreeAction
}
```

## Error Handling

The Action Validation System uses sealed result types for all validation operations:

```kotlin
sealed interface ValidationResult {
    data class Success(val resourceCost: ResourceCost) : ValidationResult
    data class Failure(val reason: ValidationFailure) : ValidationResult
    data class RequiresChoice(val choices: List<ActionChoice>) : ValidationResult
}
```

**No exceptions are thrown** - all validation failures are returned as `ValidationResult.Failure` with specific context.

## Testing Strategy

### Unit Tests (kotest)

**Coverage Target:** 90%+

**Test Categories:**

1. **Action Economy Tests**
   - Can't take action if already used
   - Can't take bonus action if already used
   - Can't take reaction if already used
   - Can't move more than remaining movement
   - Dash doubles movement speed
   - Action economy resets on new turn

2. **Resource Validation Tests**
   - Spell casting requires available spell slot
   - Upcasting consumes higher-level slot
   - Cantrips don't consume spell slots
   - Class features require remaining uses
   - Item usage requires remaining charges
   - Resource consumption updates pool correctly

3. **Range Validation Tests**
   - Touch range requires 5 feet or less
   - Ranged actions fail beyond max range
   - Line-of-effect blocked by obstacles
   - Self-targeted actions always valid
   - Area effects validate all targets

4. **Concentration Tests**
   - Can't cast concentration spell while concentrating
   - Non-concentration spells allowed while concentrating
   - Concentration broken by new concentration spell
   - Incapacitated breaks concentration
   - Unconscious breaks concentration

5. **Condition Validation Tests**
   - Stunned prevents all actions
   - Incapacitated prevents actions and reactions
   - Paralyzed prevents actions, reactions, and movement
   - Unconscious prevents all actions
   - Prone doesn't prevent actions (but affects rolls)

6. **Integration Tests**
   - Complete action validation flow
   - Multiple validation failures prioritized correctly
   - Resource costs calculated accurately
   - Turn state updates correctly after validation

### Property-Based Tests

```kotlin
test("validation is deterministic for same inputs") {
    checkAll(Arb.long(), Arb.gameAction()) { seed, action ->
        val validator1 = createValidator(seed)
        val validator2 = createValidator(seed)
        
        val result1 = validator1.validate(action, actor, turnState, encounterState)
        val result2 = validator2.validate(action, actor, turnState, encounterState)
        
        result1 shouldBe result2
    }
}

test("successful validation always returns resource cost") {
    checkAll(Arb.validAction()) { action ->
        val result = validator.validate(action, actor, turnState, encounterState)
        
        if (result is ValidationResult.Success) {
            result.resourceCost shouldNotBe null
        }
    }
}
```

### Edge Case Tests

```kotlin
test("validation handles multiple simultaneous failures") {
    // Action already used AND out of range
    val action = GameAction.Attack(actorId = 1, targetId = 2, weaponId = null)
    val turnState = TurnState(..., actionUsed = true)
    val encounterState = EncounterState(positions = mapOf(
        1L to GridPos(0, 0),
        2L to GridPos(100, 100)  // Far away
    ))
    
    val result = validator.validate(action, actor, turnState, encounterState)
    
    result shouldBe ValidationResult.Failure(
        ValidationFailure.ActionEconomyExhausted(ActionEconomyResource.Action, true)
    )
    // First failure takes priority
}

test("validation handles edge case: exactly at max range") {
    val action = GameAction.Attack(actorId = 1, targetId = 2, weaponId = longbowId)
    val encounterState = EncounterState(positions = mapOf(
        1L to GridPos(0, 0),
        2L to GridPos(30, 0)  // Exactly 150 feet (longbow range)
    ))
    
    val result = validator.validate(action, actor, turnState, encounterState)
    
    result shouldBe ValidationResult.Success(...)
}
```

## Performance Considerations

**Target:** <50ms per validation operation

**Optimizations:**
- Validation short-circuits on first failure (fail-fast)
- Condition effects cached in ConditionRegistry
- Range calculations use integer math (no floating point)
- Resource lookups use HashMap (O(1) average)
- No allocations in hot paths where possible

**Benchmarking:**
- Use JMH for micro-benchmarks of validation operations
- Target 20+ validations per millisecond on mid-tier devices

## Integration Points

### With Combat Rules Engine (05-combat-rules)
- Uses GeometryCalculator for range and line-of-effect
- Uses ConditionRegistry for condition effects
- Shares Condition sealed interface definitions

### With Domain Entities (02-core-domain)
- Creature entity provides abilities, conditions, resources
- GameAction sealed interface defines all action types
- EncounterState provides spatial information

### With Event Sourcing
- ValidationResult is immutable and can be embedded in events
- TurnState is derived from event replay
- ResourcePool is derived from event replay
- Deterministic validation ensures consistent replay

### With UI Layer
- ValidationFailure provides actionable error messages
- ResourceCost shows what will be consumed
- RequiresChoice prompts user for disambiguation

## Design Decisions

### Why Separate Validators?
Each validator has a single responsibility (action economy, resources, range, etc.), making them easier to test and maintain. The ActionValidator orchestrates them.

### Why Sealed ValidationResult?
Sealed types enable exhaustive when expressions, catching missing cases at compile time. They also make the API self-documenting and prevent invalid states.

### Why TurnState Instead of Mutating Creature?
TurnState is immutable and represents ephemeral turn-specific state. This separates persistent creature state from temporary turn state, making event sourcing cleaner.

### Why ResourcePool Separate from Creature?
ResourcePool tracks consumable resources that change frequently. Separating it from Creature allows efficient updates without copying the entire Creature object.

### Why Fail-Fast Validation?
Returning the first failure is simpler and faster than collecting all failures. Users can only fix one issue at a time anyway.

### Why No Automatic Fixes?
The validator never modifies state or "fixes" invalid actions. It only reports validity. This keeps validation pure and predictable.

## Future Enhancements (Out of Scope for v1)

- Multi-target validation (area-of-effect spells)
- Reaction trigger validation (opportunity attacks, counterspell)
- Grapple and shove action validation
- Mounted combat validation
- Cover bonuses to AC (affects range validation)
- Difficult terrain movement costs
- Climbing and swimming movement validation
- Readied action validation
- Bonus action spell + cantrip rule enforcement
