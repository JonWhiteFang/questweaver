# core:rules

**Deterministic D&D 5e SRD rules engine**

## Purpose

The `core:rules` module implements a deterministic rules engine for D&D 5e System Reference Document (SRD) mechanics. This module is pure Kotlin with zero Android dependencies and zero AI integration, ensuring 100% reproducible outcomes for event sourcing and replay.

## Responsibilities

- Implement D&D 5e SRD rules (combat, spells, abilities, conditions)
- Provide seeded random number generation for deterministic outcomes
- Validate player actions against rules
- Resolve combat (attacks, damage, saving throws)
- Calculate modifiers and bonuses
- Apply conditions and status effects
- Ensure all outcomes are reproducible from event log

## Key Classes and Interfaces

### Action Validation System

**See [Action Validation Documentation](./action-validation.md) for detailed usage.**

- `ActionValidator`: Main orchestrator for validating action legality
- `ActionEconomyValidator`: Validates action/bonus action/reaction/movement availability
- `ResourceValidator`: Validates spell slots, class features, item charges
- `RangeValidator`: Validates distance and line-of-effect
- `ConcentrationValidator`: Validates concentration spell requirements
- `ConditionValidator`: Validates whether conditions prevent actions
- `TurnState`: Tracks turn-specific state (action economy, resources, concentration)
- `ResourcePool`: Tracks consumable resources (spell slots, class features, etc.)
- `ValidationResult`: Sealed interface for validation outcomes (Success, Failure, RequiresChoice)

### Rules Engine (Placeholder)

- `RulesEngine`: Main entry point for rules validation and resolution
- `CombatResolver`: Resolves attacks, damage, and combat actions
- `SpellResolver`: Handles spell casting and effects
- `ConditionManager`: Manages status effects and conditions

### Dice Rolling (Placeholder)

- `DiceRoller`: Seeded random number generator for dice rolls
- `DiceExpression`: Parses and evaluates dice notation (e.g., "2d6+3")

### Calculators (Placeholder)

- `AttackCalculator`: Calculates attack rolls and modifiers
- `DamageCalculator`: Calculates damage and resistance
- `InitiativeCalculator`: Calculates initiative order

## Dependencies

### Production

- `core:domain`: Domain entities and events
- `kotlin-stdlib`: Kotlin standard library
- `kotlinx-coroutines-core`: Coroutines for async operations

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `kotest-property`: Property-based testing
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Pure Kotlin code only
- Seeded random number generation
- Deterministic calculations
- D&D 5e SRD mechanics

### ❌ Forbidden

- **NO Android dependencies** (android.*, androidx.*)
- **NO AI integration** (rules are deterministic, not AI-driven)
- NO unseeded random number generation
- NO network calls
- NO database access

## Architecture Patterns

### Deterministic Dice Rolling

Always use seeded RNG:

```kotlin
class DiceRoller(private val seed: Long) {
    private val random = Random(seed)
    
    fun d20(): Int = random.nextInt(1, 21)
    fun d6(): Int = random.nextInt(1, 7)
    fun roll(sides: Int): Int = random.nextInt(1, sides + 1)
}
```

### Rules Validation

Validate actions before execution:

```kotlin
class ActionValidator(private val rulesEngine: RulesEngine) {
    fun validate(action: NLAction, state: EncounterState): ValidationResult {
        return when (action) {
            is NLAction.Attack -> validateAttack(action, state)
            is NLAction.Move -> validateMovement(action, state)
            is NLAction.CastSpell -> validateSpell(action, state)
        }
    }
}
```

### Combat Resolution

Deterministic combat outcomes:

```kotlin
class CombatResolver(private val roller: DiceRoller) {
    fun resolveAttack(
        attacker: Creature,
        target: Creature,
        advantage: Boolean = false
    ): AttackOutcome {
        val roll = if (advantage) {
            maxOf(roller.d20(), roller.d20())
        } else {
            roller.d20()
        }
        
        val modifier = attacker.abilities.str.modifier
        val total = roll + modifier
        val hit = total >= target.ac
        
        val damage = if (hit) {
            roller.roll(8) + modifier // 1d8 + STR
        } else null
        
        return AttackOutcome(roll, total, hit, damage)
    }
}
```

## Testing Approach

### Unit Tests

- Test rules validation logic
- Test combat resolution with known seeds
- Test dice rolling distributions
- Property-based tests for determinism

### Property-Based Tests

Verify deterministic behavior:

```kotlin
test("same seed produces same dice rolls") {
    checkAll(Arb.long()) { seed ->
        val roller1 = DiceRoller(seed)
        val roller2 = DiceRoller(seed)
        
        val rolls1 = List(100) { roller1.d20() }
        val rolls2 = List(100) { roller2.d20() }
        
        rolls1 shouldBe rolls2
    }
}

test("d20 always returns 1-20") {
    checkAll(Arb.long()) { seed ->
        val roller = DiceRoller(seed)
        roller.d20() shouldBeInRange 1..20
    }
}
```

### Coverage Target

**90%+** code coverage (highest in project due to critical nature)

### Example Test

```kotlin
class CombatResolverTest : FunSpec({
    test("attack with known seed produces expected result") {
        val roller = DiceRoller(seed = 42)
        val resolver = CombatResolver(roller)
        
        val attacker = Fixtures.mockCreature(abilities = Abilities(str = 16))
        val target = Fixtures.mockCreature(ac = 15)
        
        val outcome = resolver.resolveAttack(attacker, target)
        
        outcome.hit shouldBe true
        outcome.damage shouldNotBe null
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :core:rules:build

# Run tests
./gradlew :core:rules:test

# Run tests with coverage
./gradlew :core:rules:test koverHtmlReport
```

## D&D 5e SRD Coverage

### Implemented (Placeholder)

- Basic attack resolution
- Damage calculation
- Ability score modifiers
- Armor class calculation
- Initiative rolls

### Planned

- Spell casting mechanics
- Saving throws
- Conditions and status effects
- Advantage/disadvantage
- Critical hits
- Resistance and vulnerability
- Concentration checks

## Package Structure

```
dev.questweaver.rules/
├── validation/       # Action validation system (IMPLEMENTED)
│   ├── ActionValidator.kt
│   ├── actions/     # GameAction types
│   ├── validators/  # Individual validators
│   ├── state/       # TurnState, ResourcePool, etc.
│   └── results/     # ValidationResult, ValidationFailure, etc.
├── engine/          # RulesEngine and main logic (Placeholder)
├── dice/            # DiceRoller and expressions (Placeholder)
├── combat/          # Combat resolution (Placeholder)
├── spells/          # Spell mechanics (Placeholder)
└── conditions/      # Status effects (Placeholder)
```

## Integration Points

### Consumed By

- `core:domain` (use cases call rules engine)
- `feature:encounter` (validates combat actions)

### Depends On

- `core:domain` (uses entities and events)

## Determinism Guarantees

### Requirements

1. **Seeded RNG**: All randomness uses seeded `DiceRoller`
2. **Pure Functions**: No side effects in calculation functions
3. **Reproducible**: Same inputs + same seed = same outputs
4. **Event Sourcing**: All outcomes captured in events for replay

### Verification

```kotlin
test("combat resolution is deterministic") {
    val seed = 12345L
    val attacker = Fixtures.mockCreature()
    val target = Fixtures.mockCreature()
    
    val outcome1 = CombatResolver(DiceRoller(seed)).resolveAttack(attacker, target)
    val outcome2 = CombatResolver(DiceRoller(seed)).resolveAttack(attacker, target)
    
    outcome1 shouldBe outcome2
}
```

## Performance Considerations

- **Fast Calculations**: Rules resolution should complete in <10ms
- **No Allocations**: Minimize object creation in hot paths
- **Inline Functions**: Use inline for small, frequently called functions

## Notes

- This module MUST remain pure Kotlin with zero Android dependencies
- This module MUST NOT integrate with AI (rules are deterministic)
- Always use seeded `DiceRoller` - never `Random()` directly
- All outcomes must be reproducible for event sourcing
- Use property-based tests to verify determinism
- Follow D&D 5e SRD rules (avoid copyrighted content)

## Additional Documentation

- [Action Validation System](./action-validation.md) - Comprehensive guide to action validation
- [Benchmarks](./benchmarks.md) - Performance benchmarks and targets

---

**Last Updated**: 2025-11-12
