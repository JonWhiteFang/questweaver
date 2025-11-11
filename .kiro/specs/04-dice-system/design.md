# Design Document

## Overview

The Deterministic Dice System is a pure Kotlin module within `core:domain` that provides seeded random number generation for all dice rolling operations. The system ensures reproducible outcomes critical for event sourcing and replay functionality. It implements standard D&D dice mechanics including advantage, disadvantage, and modifiers while maintaining 100% deterministic behavior.

## Architecture

### Module Location
- **Package**: `dev.questweaver.core.domain.dice`
- **Module**: `core:domain` (pure Kotlin, NO Android dependencies)
- **Dependencies**: Kotlin stdlib only

### Key Design Principles
1. **Immutability**: All dice roll results are immutable value objects
2. **Determinism**: Seeded RNG ensures identical outcomes for identical seeds
3. **Type Safety**: Sealed classes for roll types, data classes for results
4. **Testability**: Property-based testing for exhaustive verification
5. **Performance**: Lightweight operations suitable for frequent use

## Components and Interfaces

### 1. DiceRoller

The primary interface for all dice rolling operations.

```kotlin
package dev.questweaver.core.domain.dice

import kotlin.random.Random

/**
 * Deterministic dice roller using seeded random number generation.
 * 
 * All rolls with the same seed will produce identical results, enabling
 * event replay and deterministic game state reconstruction.
 *
 * @param seed The seed value for the random number generator
 */
class DiceRoller(seed: Long) {
    private val random = Random(seed)
    
    /**
     * Roll a single die of the specified type.
     */
    fun roll(die: DieType, modifier: Int = 0): DiceRoll
    
    /**
     * Roll multiple dice of the same type.
     */
    fun roll(count: Int, die: DieType, modifier: Int = 0): DiceRoll
    
    /**
     * Roll a d20 with advantage (take higher of two rolls).
     */
    fun rollWithAdvantage(modifier: Int = 0): DiceRoll
    
    /**
     * Roll a d20 with disadvantage (take lower of two rolls).
     */
    fun rollWithDisadvantage(modifier: Int = 0): DiceRoll
    
    // Convenience methods for common dice
    fun d4(modifier: Int = 0): DiceRoll = roll(DieType.D4, modifier)
    fun d6(modifier: Int = 0): DiceRoll = roll(DieType.D6, modifier)
    fun d8(modifier: Int = 0): DiceRoll = roll(DieType.D8, modifier)
    fun d10(modifier: Int = 0): DiceRoll = roll(DieType.D10, modifier)
    fun d12(modifier: Int = 0): DiceRoll = roll(DieType.D12, modifier)
    fun d20(modifier: Int = 0): DiceRoll = roll(DieType.D20, modifier)
    fun d100(modifier: Int = 0): DiceRoll = roll(DieType.D100, modifier)
}
```

### 2. DieType

Enum representing standard D&D dice types.

```kotlin
package dev.questweaver.core.domain.dice

/**
 * Standard D&D dice types.
 */
enum class DieType(val sides: Int) {
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20),
    D100(100);
    
    /**
     * Roll this die type using the provided random generator.
     */
    internal fun roll(random: Random): Int = random.nextInt(1, sides + 1)
}
```

### 3. DiceRoll

Immutable value object representing a dice roll result.

```kotlin
package dev.questweaver.core.domain.dice

import kotlinx.serialization.Serializable

/**
 * Immutable result of a dice roll operation.
 *
 * Contains all information about the roll including individual die results,
 * modifiers, and roll type (normal/advantage/disadvantage).
 */
@Serializable
data class DiceRoll(
    val dieType: DieType,
    val rolls: List<Int>,
    val modifier: Int = 0,
    val rollType: RollType = RollType.NORMAL
) {
    /**
     * The sum of all individual die rolls (before modifier).
     */
    val naturalTotal: Int = rolls.sum()
    
    /**
     * The final result including modifier.
     */
    val total: Int = naturalTotal + modifier
    
    /**
     * For advantage/disadvantage, the value that was selected.
     * For normal rolls, same as naturalTotal.
     */
    val selectedValue: Int = when (rollType) {
        RollType.ADVANTAGE -> rolls.maxOrNull() ?: 0
        RollType.DISADVANTAGE -> rolls.minOrNull() ?: 0
        RollType.NORMAL -> naturalTotal
    }
    
    /**
     * The final result for advantage/disadvantage rolls.
     */
    val result: Int = selectedValue + modifier
    
    init {
        require(rolls.isNotEmpty()) { "Dice roll must contain at least one result" }
        require(rolls.all { it in 1..dieType.sides }) {
            "All roll values must be between 1 and ${dieType.sides}"
        }
    }
}
```

### 4. RollType

Sealed interface for roll type classification.

```kotlin
package dev.questweaver.core.domain.dice

import kotlinx.serialization.Serializable

/**
 * Type of dice roll (normal, advantage, or disadvantage).
 */
@Serializable
enum class RollType {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE
}
```

## Data Models

### DiceRoll Structure

```
DiceRoll
├── dieType: DieType          // Type of die rolled (d4, d6, etc.)
├── rolls: List<Int>          // Individual die results [3, 5, 2]
├── modifier: Int             // Modifier applied (+3, -1, etc.)
├── rollType: RollType        // NORMAL, ADVANTAGE, or DISADVANTAGE
├── naturalTotal: Int         // Sum of rolls (computed)
├── total: Int                // naturalTotal + modifier (computed)
├── selectedValue: Int        // For adv/dis, the chosen value (computed)
└── result: Int               // Final result for adv/dis (computed)
```

### Example Usage

```kotlin
// Initialize with session seed
val roller = DiceRoller(seed = sessionId)

// Simple d20 roll
val attackRoll = roller.d20(modifier = 5)
// attackRoll.rolls = [14]
// attackRoll.naturalTotal = 14
// attackRoll.total = 19

// Multiple dice (damage)
val damageRoll = roller.roll(count = 2, die = DieType.D6, modifier = 3)
// damageRoll.rolls = [4, 5]
// damageRoll.naturalTotal = 9
// damageRoll.total = 12

// Advantage
val advantageRoll = roller.rollWithAdvantage(modifier = 2)
// advantageRoll.rolls = [8, 15]
// advantageRoll.selectedValue = 15
// advantageRoll.result = 17

// Disadvantage
val disadvantageRoll = roller.rollWithDisadvantage(modifier = 2)
// disadvantageRoll.rolls = [12, 7]
// disadvantageRoll.selectedValue = 7
// disadvantageRoll.result = 9
```

## Error Handling

### Input Validation

```kotlin
class DiceRoller(seed: Long) {
    fun roll(count: Int, die: DieType, modifier: Int = 0): DiceRoll {
        require(count > 0) { "Dice count must be positive, got $count" }
        // Roll logic
    }
}

data class DiceRoll(...) {
    init {
        require(rolls.isNotEmpty()) { "Dice roll must contain at least one result" }
        require(rolls.all { it in 1..dieType.sides }) {
            "All roll values must be between 1 and ${dieType.sides}"
        }
    }
}
```

### Error Scenarios

| Scenario | Validation | Error Type |
|----------|------------|------------|
| count < 1 | `require(count > 0)` | IllegalArgumentException |
| Empty rolls list | `require(rolls.isNotEmpty())` | IllegalArgumentException |
| Roll out of range | `require(rolls.all { it in 1..sides })` | IllegalArgumentException |

## Testing Strategy

### Unit Tests (kotest)

```kotlin
class DiceRollerTest : FunSpec({
    context("DiceRoller with seed") {
        test("produces identical sequences for same seed") {
            val roller1 = DiceRoller(seed = 42)
            val roller2 = DiceRoller(seed = 42)
            
            val roll1 = roller1.d20()
            val roll2 = roller2.d20()
            
            roll1.rolls shouldBe roll2.rolls
        }
        
        test("produces different sequences for different seeds") {
            val roller1 = DiceRoller(seed = 42)
            val roller2 = DiceRoller(seed = 43)
            
            val rolls1 = List(100) { roller1.d20().rolls[0] }
            val rolls2 = List(100) { roller2.d20().rolls[0] }
            
            rolls1 shouldNotBe rolls2
        }
    }
    
    context("advantage rolls") {
        test("returns higher of two d20 rolls") {
            val roller = DiceRoller(seed = 42)
            val roll = roller.rollWithAdvantage()
            
            roll.rolls.size shouldBe 2
            roll.selectedValue shouldBe roll.rolls.maxOrNull()
        }
    }
    
    context("disadvantage rolls") {
        test("returns lower of two d20 rolls") {
            val roller = DiceRoller(seed = 42)
            val roll = roller.rollWithDisadvantage()
            
            roll.rolls.size shouldBe 2
            roll.selectedValue shouldBe roll.rolls.minOrNull()
        }
    }
})
```

### Property-Based Tests (kotest)

```kotlin
class DiceRollerPropertyTest : FunSpec({
    test("d20 always returns 1-20 for any seed") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val roll = roller.d20()
            roll.rolls[0] shouldBeInRange 1..20
        }
    }
    
    test("advantage always >= both individual rolls") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val roll = roller.rollWithAdvantage()
            
            roll.selectedValue shouldBeGreaterThanOrEqual roll.rolls[0]
            roll.selectedValue shouldBeGreaterThanOrEqual roll.rolls[1]
            roll.selectedValue shouldBe roll.rolls.maxOrNull()
        }
    }
    
    test("disadvantage always <= both individual rolls") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val roll = roller.rollWithDisadvantage()
            
            roll.selectedValue shouldBeLessThanOrEqual roll.rolls[0]
            roll.selectedValue shouldBeLessThanOrEqual roll.rolls[1]
            roll.selectedValue shouldBe roll.rolls.minOrNull()
        }
    }
    
    test("multiple dice sum equals individual roll sum") {
        checkAll(Arb.long(), Arb.int(1..10)) { seed, count ->
            val roller = DiceRoller(seed)
            val roll = roller.roll(count, DieType.D6)
            
            roll.naturalTotal shouldBe roll.rolls.sum()
        }
    }
    
    test("modifier is correctly applied to total") {
        checkAll(Arb.long(), Arb.int(-10..10)) { seed, modifier ->
            val roller = DiceRoller(seed)
            val roll = roller.d20(modifier)
            
            roll.total shouldBe (roll.naturalTotal + modifier)
        }
    }
})
```

### Test Coverage Goals

- **DiceRoller**: 95%+ (critical for determinism)
- **DiceRoll**: 90%+ (value object validation)
- **DieType**: 100% (simple enum)

### Performance Benchmarks

```kotlin
@Test
fun `rolling 1000 dice completes in under 10ms`() {
    val roller = DiceRoller(seed = 42)
    
    val duration = measureTimeMillis {
        repeat(1000) {
            roller.d20()
        }
    }
    
    duration shouldBeLessThan 10
}
```

## Integration with Event Sourcing

### Storing Dice Rolls in Events

```kotlin
@Serializable
@SerialName("attack_resolved")
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val attackRoll: DiceRoll,  // Embedded dice roll
    val damageRoll: DiceRoll?, // Null if miss
    val hit: Boolean
) : GameEvent
```

### Replay Consistency

Since `DiceRoll` is serialized with the event, replay doesn't need to re-roll dice. The original roll results are preserved in the event log, ensuring perfect replay consistency even if the RNG implementation changes.

## Performance Considerations

### Memory Footprint

```kotlin
// DiceRoll memory estimate
data class DiceRoll(
    val dieType: DieType,        // 4 bytes (enum ordinal)
    val rolls: List<Int>,        // 16 bytes + (4 * count)
    val modifier: Int,           // 4 bytes
    val rollType: RollType       // 4 bytes
)
// Total: ~28 bytes + (4 * roll count)
```

Typical rolls (1-2 dice) consume ~36-40 bytes, which is negligible for event storage.

### CPU Performance

- Single die roll: ~100ns (random number generation)
- Advantage/disadvantage: ~200ns (two rolls + comparison)
- Multiple dice: ~100ns * count

Target: <1μs for typical combat rolls (well within budget).

## Design Decisions

### 1. Why Kotlin Random instead of Java Random?

**Decision**: Use `kotlin.random.Random`

**Rationale**:
- Kotlin-first API design
- Better type safety
- Consistent with project's Kotlin-only approach
- Same deterministic guarantees as Java Random

### 2. Why store all individual rolls in DiceRoll?

**Decision**: Store complete roll history in `rolls: List<Int>`

**Rationale**:
- Enables detailed combat logs ("You rolled 8 and 15, taking 15")
- Supports future features (exploding dice, rerolls)
- Minimal memory overhead (4 bytes per die)
- Critical for debugging and testing

### 3. Why separate naturalTotal and total?

**Decision**: Provide both unmodified and modified totals

**Rationale**:
- Rules often need unmodified roll (critical hits check natural 20)
- UI displays both ("15 + 3 = 18")
- Event logging benefits from explicit separation
- Prevents confusion in rules engine

### 4. Why enum for DieType instead of Int parameter?

**Decision**: Use `enum class DieType`

**Rationale**:
- Type safety prevents invalid die types (d7, d13)
- Self-documenting code
- Enables exhaustive when expressions
- Supports future extensions (custom dice)

### 5. Why not support dice expressions (e.g., "2d6+3")?

**Decision**: Defer string parsing to future iteration

**Rationale**:
- YAGNI - not required for v1.0
- Programmatic API sufficient for rules engine
- String parsing adds complexity and error handling
- Can be added later without breaking changes

## Future Enhancements (Out of Scope for v1.0)

1. **Dice Expression Parser**: Parse strings like "2d6+3", "1d20+5 advantage"
2. **Exploding Dice**: Reroll on max value (common in homebrew)
3. **Reroll Mechanics**: Reroll 1s (Great Weapon Fighting)
4. **Custom Dice**: Support non-standard dice (d3, d7, etc.)
5. **Roll History**: Track all rolls in a session for statistics
6. **Dice Pools**: Support systems with dice pools (count successes)

---

**Last Updated**: 2025-11-11
