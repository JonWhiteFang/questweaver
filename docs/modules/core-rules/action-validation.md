# Action Validation System

**Package:** `dev.questweaver.core.rules.validation`

The Action Validation System ensures that all player and AI actions comply with D&D 5e SRD rules before execution. It validates action legality, tracks resource consumption, enforces turn phase restrictions, and validates spatial requirements.

## Overview

This system provides deterministic, stateless validation of game actions. It checks:

1. **Conditions** - Whether conditions (Stunned, Incapacitated, etc.) prevent the action
2. **Action Economy** - Whether action/bonus action/reaction/movement is available
3. **Resources** - Whether required resources (spell slots, class features) are available
4. **Range & Line-of-Effect** - Whether targets are within range and unobstructed
5. **Concentration** - Whether casting a concentration spell while already concentrating

## Key Design Principles

- **Stateless Validation**: Validators are pure functions that don't modify game state
- **Fail-Fast**: Returns the first validation failure encountered
- **Deterministic**: Same inputs always produce same outputs
- **Exhaustive Types**: Uses sealed interfaces for compile-time safety
- **Clear Feedback**: Provides specific failure reasons with context for UI display

## Core Components

### ActionValidator

Main orchestrator that coordinates all validation checks.

```kotlin
class ActionValidator(
    private val actionEconomyValidator: ActionEconomyValidator,
    private val resourceValidator: ResourceValidator,
    private val rangeValidator: RangeValidator,
    private val concentrationValidator: ConcentrationValidator,
    private val conditionValidator: ConditionValidator
)
```

**Validation Flow:**
1. Conditions → 2. Action Economy → 3. Resources → 4. Range → 5. Concentration

Each validator returns a `ValidationResult`. The first failure encountered is returned immediately (fail-fast).

### Validation Result Types

```kotlin
sealed interface ValidationResult {
    data class Success(val resourceCost: ResourceCost) : ValidationResult
    data class Failure(val reason: ValidationFailure) : ValidationResult
    data class RequiresChoice(val choices: List<ActionChoice>) : ValidationResult
}
```

- **Success**: Action is valid, includes resource cost that would be consumed
- **Failure**: Action is invalid, includes specific reason with context
- **RequiresChoice**: User must make a choice (e.g., spell slot level) before validation can complete

### Validation Failures

All failure types include context for meaningful error messages:

```kotlin
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
        val activeSpell: String,
        val newSpell: String
    ) : ValidationFailure
    
    data class ConditionPreventsAction(
        val condition: Condition,
        val reason: String
    ) : ValidationFailure
    
    data class InvalidTarget(
        val reason: String
    ) : ValidationFailure
}
```

**Failure Meanings:**

- **ActionEconomyExhausted**: The creature has already used their action, bonus action, reaction, or has insufficient movement remaining
- **InsufficientResources**: The creature lacks required spell slots, class feature uses, or item charges
- **OutOfRange**: The target is beyond the maximum range of the action
- **LineOfEffectBlocked**: An obstacle blocks the path between actor and target
- **ConcentrationConflict**: The creature is already concentrating on another spell
- **ConditionPreventsAction**: A condition (Stunned, Incapacitated, etc.) prevents this action
- **InvalidTarget**: The target is invalid for this action (wrong creature, wrong turn, etc.)

### Resource Cost

Represents resources that would be consumed by executing an action:

```kotlin
data class ResourceCost(
    val actionEconomy: Set<ActionEconomyResource>,  // Action, BonusAction, Reaction, Movement
    val resources: Set<Resource>,                    // Spell slots, class features, etc.
    val movementCost: Int,                           // Feet of movement
    val breaksConcentration: Boolean                 // Whether this breaks concentration
)
```

**Resource Cost Calculation:**

The `ActionValidator` aggregates resource costs from all validators:
- Action economy resources (action, bonus action, reaction) from `ActionEconomyValidator`
- Consumable resources (spell slots, class features) from `ResourceValidator`
- Movement cost in feet from `ActionEconomyValidator`
- Concentration breaking flag from `ConcentrationValidator`

## Usage Examples

### Basic Validation

```kotlin
// Create validators (typically via Koin DI)
val actionValidator = ActionValidator(
    actionEconomyValidator = ActionEconomyValidator(),
    resourceValidator = ResourceValidator(),
    rangeValidator = RangeValidator(),
    concentrationValidator = ConcentrationValidator(),
    conditionValidator = ConditionValidator()
)

// Create action
val action = GameAction.Attack(
    actorId = 1L,
    targetId = 2L,
    weaponId = null  // Unarmed strike
)

// Validate action
val result = actionValidator.validate(
    action = action,
    actorConditions = emptySet(),
    turnState = turnState,
    encounterState = encounterState
)

// Handle result
when (result) {
    is ValidationResult.Success -> {
        println("Action is valid!")
        println("Cost: ${result.resourceCost}")
        // Execute action and consume resources
    }
    is ValidationResult.Failure -> {
        println("Action failed: ${result.reason}")
        // Display error to user
    }
    is ValidationResult.RequiresChoice -> {
        println("User must choose: ${result.choices}")
        // Prompt user for choices
    }
}
```

### Handling Specific Failures

```kotlin
when (val result = actionValidator.validate(action, conditions, turnState, encounterState)) {
    is ValidationResult.Failure -> {
        when (val reason = result.reason) {
            is ValidationFailure.ActionEconomyExhausted -> {
                showError("You've already used your ${reason.required}")
            }
            is ValidationFailure.InsufficientResources -> {
                showError("Need ${reason.needed} ${reason.required}, but only have ${reason.available}")
            }
            is ValidationFailure.OutOfRange -> {
                showError("Target is ${reason.actualDistance}ft away, max range is ${reason.maxRange}ft")
            }
            is ValidationFailure.LineOfEffectBlocked -> {
                showError("Path blocked by obstacle at ${reason.blockingObstacle}")
            }
            is ValidationFailure.ConcentrationConflict -> {
                showError("Already concentrating on ${reason.activeSpell}")
            }
            is ValidationFailure.ConditionPreventsAction -> {
                showError("${reason.condition} prevents this action: ${reason.reason}")
            }
            is ValidationFailure.InvalidTarget -> {
                showError("Invalid target: ${reason.reason}")
            }
        }
    }
    else -> { /* handle success or choice */ }
}
```

### Spell Casting with Slot Selection

```kotlin
// Cast a spell without specifying slot level
val castSpell = GameAction.CastSpell(
    actorId = 1L,
    spellId = "fireball",
    targetPos = GridPos(10, 10),
    slotLevel = null  // Let validator determine available slots
)

when (val result = actionValidator.validate(castSpell, conditions, turnState, encounterState)) {
    is ValidationResult.RequiresChoice -> {
        val choice = result.choices.first() as ActionChoice.SpellSlotLevel
        println("Choose spell slot level:")
        choice.availableLevels.forEach { level ->
            println("  - Level $level")
        }
        // User selects level, then re-validate with chosen level
    }
    is ValidationResult.Success -> {
        println("Spell cast successfully!")
    }
    is ValidationResult.Failure -> {
        println("Cannot cast spell: ${result.reason}")
    }
}
```

### Consuming Resources After Validation

```kotlin
val result = actionValidator.validate(action, conditions, turnState, encounterState)

if (result is ValidationResult.Success) {
    // Apply resource cost to turn state
    val updatedTurnState = turnState.consumeResources(result.resourceCost)
    
    // Now execute the action with updated state
    executeAction(action, updatedTurnState)
}
```

## Turn State Management

The `TurnState` tracks ephemeral turn-specific state:

```kotlin
val turnState = TurnState(
    creatureId = 1L,
    round = 1,
    actionUsed = false,
    bonusActionUsed = false,
    reactionUsed = false,
    movementUsed = 0,
    movementTotal = 30,  // 30 feet movement speed
    resourcePool = ResourcePool(
        spellSlots = mapOf(1 to 4, 2 to 3, 3 to 2),  // Level -> count
        classFeatures = mapOf("action_surge" to 1),
        itemCharges = emptyMap(),
        hitDice = mapOf("d8" to 5)
    ),
    concentrationState = ConcentrationState.Empty
)

// Check available resources
println("Has action: ${turnState.hasActionAvailable()}")
println("Remaining movement: ${turnState.remainingMovement()}ft")

// Consume resources (returns new TurnState)
val updatedState = turnState.consumeResources(resourceCost)
```

## Resource Pool Management

The `ResourcePool` tracks consumable resources:

```kotlin
val pool = ResourcePool(
    spellSlots = mapOf(
        1 to 4,  // 4 first-level slots
        2 to 3,  // 3 second-level slots
        3 to 2   // 2 third-level slots
    ),
    classFeatures = mapOf(
        "action_surge" to 1,
        "second_wind" to 1
    ),
    itemCharges = mapOf(
        123L to 3  // Item ID 123 has 3 charges
    ),
    hitDice = mapOf(
        "d8" to 5  // 5 d8 hit dice
    )
)

// Check resource availability
val hasSlot = pool.hasResource(Resource.SpellSlot(level = 2))
val hasFeature = pool.hasResource(Resource.ClassFeature("action_surge", uses = 1))

// Consume resources (returns new pool)
val updatedPool = pool.consume(Resource.SpellSlot(level = 2))

// Get available spell slot levels
val availableLevels = pool.getAvailableSpellSlotLevels()  // [1, 2, 3]
```

## Concentration Management

The `ConcentrationState` tracks active concentration spells:

```kotlin
var concentrationState = ConcentrationState.Empty

// Start concentrating on a spell
concentrationState = concentrationState.startConcentration(
    creatureId = 1L,
    spellId = "bless",
    round = 1
)

// Check if concentrating
val isConcentrating = concentrationState.isConcentrating(1L)  // true

// Get concentration info
val info = concentrationState.getConcentration(1L)
println("Concentrating on: ${info?.spellId}")  // "bless"

// Break concentration
concentrationState = concentrationState.breakConcentration(1L)
```

## Action Types

All actions implement the `GameAction` sealed interface:

```kotlin
// Attack with a weapon
GameAction.Attack(actorId = 1L, targetId = 2L, weaponId = 456L)

// Cast a spell
GameAction.CastSpell(
    actorId = 1L,
    spellId = "fireball",
    targetIds = listOf(2L, 3L),
    targetPos = GridPos(10, 10),
    slotLevel = 3
)

// Move along a path
GameAction.Move(
    actorId = 1L,
    path = listOf(GridPos(0, 0), GridPos(1, 0), GridPos(2, 0))
)

// Take the Dash action
GameAction.Dash(actorId = 1L)

// Take the Disengage action
GameAction.Disengage(actorId = 1L)

// Take the Dodge action
GameAction.Dodge(actorId = 1L)

// Use a class feature
GameAction.UseClassFeature(
    actorId = 1L,
    featureId = "action_surge",
    targetId = null
)

// Make an opportunity attack
GameAction.OpportunityAttack(actorId = 1L, targetId = 2L)
```

## Individual Validators

You can also use individual validators directly for specific checks:

### ActionEconomyValidator

Validates action economy constraints (action, bonus action, reaction, movement).

```kotlin
val validator = ActionEconomyValidator()

// Validate action economy
val result = validator.validateActionEconomy(action, turnState)

// Get action cost
val cost = validator.getActionCost(action)  // Set<ActionEconomyResource>
```

### ResourceValidator

Validates resource availability (spell slots, class features, item charges).

```kotlin
val validator = ResourceValidator()

// Validate resources
val result = validator.validateResources(action, turnState.resourcePool)

// Get resource cost
val cost = validator.getResourceCost(action)  // ResourceCost
```

### RangeValidator

Validates range and line-of-effect requirements.

```kotlin
val validator = RangeValidator()

// Validate range
val result = validator.validateRange(
    action,
    actorPos = GridPos(0, 0),
    targetPos = GridPos(5, 5),
    encounterState
)

// Calculate distance (Chebyshev distance in D&D 5e)
val distance = validator.calculateDistance(
    from = GridPos(0, 0),
    to = GridPos(3, 4)
)  // 20ft (4 squares * 5ft)

// Check line-of-effect
val hasLOS = validator.hasLineOfEffect(
    from = GridPos(0, 0),
    to = GridPos(5, 5),
    obstacles = setOf(GridPos(2, 2))
)  // false (blocked)
```

### ConcentrationValidator

Validates concentration requirements for spells.

```kotlin
val validator = ConcentrationValidator()

// Validate concentration
val result = validator.validateConcentration(
    action,
    actorId = 1L,
    turnState.concentrationState
)

// Get active concentration
val activeSpell = validator.getActiveConcentration(1L, concentrationState)
```

### ConditionValidator

Validates whether conditions prevent actions.

```kotlin
val validator = ConditionValidator()

// Validate conditions
val result = validator.validateConditions(action, actorConditions)

// Get blocking condition
val blockingCondition = validator.getBlockingCondition(actorConditions)
```

## Integration with Event Sourcing

Validation results are immutable and can be embedded in events:

```kotlin
// Validate action
val validationResult = actionValidator.validate(action, conditions, turnState, encounterState)

// Generate event with validation result
val event = when (validationResult) {
    is ValidationResult.Success -> {
        GameEvent.ActionValidated(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            action = action,
            resourceCost = validationResult.resourceCost
        )
    }
    is ValidationResult.Failure -> {
        GameEvent.ActionRejected(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            action = action,
            reason = validationResult.reason
        )
    }
    is ValidationResult.RequiresChoice -> {
        GameEvent.ActionRequiresChoice(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            action = action,
            choices = validationResult.choices
        )
    }
}

// Persist event
eventRepository.append(event)
```

## Testing

The validation system is designed for easy testing with deterministic behavior:

```kotlin
class ActionValidatorTest : FunSpec({
    test("attack action requires available action") {
        val turnState = TurnState(
            creatureId = 1L,
            round = 1,
            actionUsed = true,  // Action already used
            movementTotal = 30,
            resourcePool = ResourcePool.Empty,
            concentrationState = ConcentrationState.Empty
        )
        
        val action = GameAction.Attack(actorId = 1L, targetId = 2L, weaponId = null)
        val result = actionValidator.validate(action, emptySet(), turnState, encounterState)
        
        result shouldBe ValidationResult.Failure(
            ValidationFailure.ActionEconomyExhausted(
                required = ActionEconomyResource.Action,
                alreadyUsed = true
            )
        )
    }
    
    test("spell casting requires available spell slot") {
        val turnState = TurnState(
            creatureId = 1L,
            round = 1,
            movementTotal = 30,
            resourcePool = ResourcePool(spellSlots = mapOf(1 to 0)),  // No slots
            concentrationState = ConcentrationState.Empty
        )
        
        val action = GameAction.CastSpell(
            actorId = 1L,
            spellId = "magic_missile",
            slotLevel = 1
        )
        val result = actionValidator.validate(action, emptySet(), turnState, encounterState)
        
        result shouldBeInstanceOf ValidationResult.Failure::class
    }
})
```

## Performance Considerations

- **Target**: <50ms per validation operation
- **Optimization**: Fail-fast validation short-circuits on first failure
- **No Allocations**: Minimal allocations in hot paths
- **Integer Math**: Range calculations use integer math (no floating point)

## Package Structure

```
dev.questweaver.core.rules.validation/
├── ActionValidator.kt              # Main validation orchestrator
├── actions/
│   ├── GameAction.kt              # Sealed interface for all actions
│   └── ActionType.kt              # Action economy resource types
├── validators/
│   ├── ActionEconomyValidator.kt  # Action/bonus/reaction/movement validation
│   ├── ResourceValidator.kt       # Spell slots, class features, item charges
│   ├── RangeValidator.kt          # Distance and line-of-effect validation
│   ├── ConcentrationValidator.kt  # Concentration spell validation
│   └── ConditionValidator.kt      # Condition-based action restrictions
├── state/
│   ├── TurnState.kt               # Current turn phase and resource tracking
│   ├── ResourcePool.kt            # Resource availability tracking
│   ├── ConcentrationState.kt     # Active concentration tracking
│   ├── EncounterState.kt         # Encounter-wide state (positions, obstacles)
│   ├── GridPos.kt                # Grid position
│   └── Resource.kt               # Resource types
└── results/
    ├── ValidationResult.kt        # Sealed validation result types
    ├── ValidationFailure.kt       # Failure reasons with context
    ├── ResourceCost.kt            # Resources consumed by action
    └── ActionChoice.kt            # Choices required from user
```

## Dependencies

- `core:domain` - Entity definitions (Creature, GridPos)
- `core:rules` - ConditionRegistry for condition effects
- No Android dependencies (pure Kotlin)

## Future Enhancements

- Multi-target validation for area-of-effect spells
- Reaction trigger validation (opportunity attacks, counterspell)
- Grapple and shove action validation
- Mounted combat validation
- Cover bonuses to AC
- Difficult terrain movement costs
- Climbing and swimming movement validation
- Readied action validation

---

**Last Updated**: 2025-11-12
