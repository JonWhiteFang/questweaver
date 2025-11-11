# Combat Rules Engine Design

## Overview

The Combat Rules Engine is a pure Kotlin module in `core/rules` that implements D&D 5e SRD-compatible combat mechanics. It provides deterministic resolution of attacks, saving throws, ability checks, and condition effects. The engine is stateless and depends only on the seeded DiceRoller from spec 04-dice-system, ensuring reproducible outcomes for event sourcing.

**Key Design Principles:**
- Pure Kotlin with no Android dependencies
- 100% deterministic using seeded random number generation
- Stateless functions that return immutable outcome objects
- Exhaustive sealed types for all outcomes and conditions
- SRD-compatible D&D 5e mechanics

## Architecture

### Module Location
`core/rules/src/main/kotlin/dev/questweaver/core/rules/`

### Package Structure
```
core/rules/
├── combat/
│   ├── AttackResolver.kt          # Attack roll and damage resolution
│   ├── SavingThrowResolver.kt     # Saving throw resolution
│   ├── AbilityCheckResolver.kt    # Ability check resolution
│   └── DamageCalculator.kt        # Damage calculation with resistances
├── conditions/
│   ├── Condition.kt               # Sealed interface for conditions
│   ├── ConditionEffect.kt         # Condition effect modifiers
│   └── ConditionRegistry.kt       # Condition lookup and application
├── modifiers/
│   ├── RollModifier.kt            # Advantage/disadvantage handling
│   └── DamageModifier.kt          # Resistance/vulnerability/immunity
└── outcomes/
    ├── AttackOutcome.kt           # Attack resolution result
    ├── SavingThrowOutcome.kt      # Saving throw result
    ├── AbilityCheckOutcome.kt     # Ability check result
    └── DamageOutcome.kt           # Damage calculation result
```

### Dependencies
- `core:domain` - Entity definitions (Creature, Abilities)
- `04-dice-system` - DiceRoller for seeded randomness

## Components and Interfaces

### 1. AttackResolver

Resolves attack rolls against targets, handling advantage/disadvantage, critical hits, and automatic misses.

```kotlin
class AttackResolver(private val diceRoller: DiceRoller) {
    /**
     * Resolves an attack roll against a target.
     *
     * @param attackBonus The attacker's attack bonus (ability modifier + proficiency)
     * @param targetAC The target's Armor Class
     * @param rollModifier Advantage, disadvantage, or normal roll
     * @param attackerConditions Active conditions on the attacker
     * @param targetConditions Active conditions on the target
     * @return AttackOutcome with roll details and hit status
     */
    fun resolveAttack(
        attackBonus: Int,
        targetAC: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        attackerConditions: Set<Condition> = emptySet(),
        targetConditions: Set<Condition> = emptySet()
    ): AttackOutcome
}
```

**Algorithm:**
1. Determine effective roll modifier by combining base modifier with condition effects
2. Roll d20 (or 2d20 for advantage/disadvantage) using DiceRoller
3. Check for natural 20 (critical hit) or natural 1 (automatic miss)
4. Calculate total: d20 result + attack bonus
5. Compare total to target AC
6. Return AttackOutcome with all details

### 2. DamageCalculator

Calculates damage for successful attacks, handling critical hits and damage modifiers.

```kotlin
class DamageCalculator(private val diceRoller: DiceRoller) {
    /**
     * Calculates damage for an attack.
     *
     * @param damageDice The damage dice expression (e.g., "2d6")
     * @param damageModifier Flat damage modifier to add
     * @param damageType Type of damage (slashing, fire, etc.)
     * @param isCritical Whether this is a critical hit
     * @param targetModifiers Target's resistances/vulnerabilities/immunities
     * @return DamageOutcome with roll details and final damage
     */
    fun calculateDamage(
        damageDice: String,
        damageModifier: Int,
        damageType: DamageType,
        isCritical: Boolean,
        targetModifiers: Set<DamageModifier>
    ): DamageOutcome
}
```

**Algorithm:**
1. Parse damage dice expression
2. Roll damage dice using DiceRoller
3. If critical hit, double the number of dice (not the modifier)
4. Add damage modifier to total
5. Apply resistance (half damage, rounded down)
6. Apply vulnerability (double damage)
7. Apply immunity (zero damage)
8. Return DamageOutcome with breakdown

### 3. SavingThrowResolver

Resolves saving throws against a DC, handling advantage/disadvantage and proficiency.

```kotlin
class SavingThrowResolver(private val diceRoller: DiceRoller) {
    /**
     * Resolves a saving throw.
     *
     * @param abilityModifier The creature's ability modifier for this save
     * @param proficiencyBonus The creature's proficiency bonus (if proficient)
     * @param dc The Difficulty Class to meet or exceed
     * @param rollModifier Advantage, disadvantage, or normal roll
     * @param isProficient Whether the creature is proficient in this save
     * @param conditions Active conditions on the creature
     * @return SavingThrowOutcome with roll details and success status
     */
    fun resolveSavingThrow(
        abilityModifier: Int,
        proficiencyBonus: Int,
        dc: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        isProficient: Boolean = false,
        conditions: Set<Condition> = emptySet()
    ): SavingThrowOutcome
}
```

**Algorithm:**
1. Determine effective roll modifier from conditions
2. Roll d20 (or 2d20 for advantage/disadvantage)
3. Check for natural 20 (automatic success)
4. Calculate total: d20 + ability modifier + (proficiency if proficient)
5. Check for auto-fail conditions (Stunned for STR/DEX saves)
6. Compare total to DC
7. Return SavingThrowOutcome with details

### 4. AbilityCheckResolver

Resolves ability checks against a DC, handling advantage/disadvantage, proficiency, and expertise.

```kotlin
class AbilityCheckResolver(private val diceRoller: DiceRoller) {
    /**
     * Resolves an ability check.
     *
     * @param abilityModifier The creature's ability modifier
     * @param proficiencyBonus The creature's proficiency bonus
     * @param dc The Difficulty Class to meet or exceed
     * @param rollModifier Advantage, disadvantage, or normal roll
     * @param proficiencyLevel None, proficient, or expertise
     * @param conditions Active conditions on the creature
     * @return AbilityCheckOutcome with roll details and success status
     */
    fun resolveAbilityCheck(
        abilityModifier: Int,
        proficiencyBonus: Int,
        dc: Int,
        rollModifier: RollModifier = RollModifier.Normal,
        proficiencyLevel: ProficiencyLevel = ProficiencyLevel.None,
        conditions: Set<Condition> = emptySet()
    ): AbilityCheckOutcome
}
```

**Algorithm:**
1. Determine effective roll modifier from conditions
2. Roll d20 (or 2d20 for advantage/disadvantage)
3. Calculate proficiency multiplier (0x, 1x, or 2x for expertise)
4. Calculate total: d20 + ability modifier + (proficiency × multiplier)
5. Compare total to DC
6. Return AbilityCheckOutcome with details

### 5. ConditionRegistry

Manages condition effects and their impact on rolls and actions.

```kotlin
object ConditionRegistry {
    /**
     * Gets the effect of a condition on attack rolls.
     *
     * @param condition The condition to check
     * @param isAttacker Whether this is for the attacker (vs defender)
     * @return RollModifier to apply (advantage, disadvantage, or none)
     */
    fun getAttackRollEffect(condition: Condition, isAttacker: Boolean): RollModifier?
    
    /**
     * Gets the effect of a condition on saving throws.
     *
     * @param condition The condition to check
     * @param abilityType The ability being saved (STR, DEX, etc.)
     * @return SaveEffect (auto-fail, disadvantage, or none)
     */
    fun getSavingThrowEffect(condition: Condition, abilityType: AbilityType): SaveEffect?
    
    /**
     * Gets the effect of a condition on ability checks.
     *
     * @param condition The condition to check
     * @return RollModifier to apply
     */
    fun getAbilityCheckEffect(condition: Condition): RollModifier?
    
    /**
     * Checks if a condition prevents actions.
     *
     * @param condition The condition to check
     * @return True if the creature cannot take actions
     */
    fun preventsActions(condition: Condition): Boolean
}
```

## Data Models

### Sealed Types

```kotlin
// Roll modifiers
sealed interface RollModifier {
    object Normal : RollModifier
    object Advantage : RollModifier
    object Disadvantage : RollModifier
}

// Proficiency levels
enum class ProficiencyLevel {
    None,      // No proficiency (0x bonus)
    Proficient, // Proficient (1x bonus)
    Expertise   // Expertise (2x bonus)
}

// Damage types (SRD)
enum class DamageType {
    Slashing, Piercing, Bludgeoning,  // Physical
    Fire, Cold, Lightning, Thunder,    // Elemental
    Acid, Poison, Necrotic, Radiant,  // Other
    Force, Psychic
}

// Damage modifiers
sealed interface DamageModifier {
    data class Resistance(val damageType: DamageType) : DamageModifier
    data class Vulnerability(val damageType: DamageType) : DamageModifier
    data class Immunity(val damageType: DamageType) : DamageModifier
}

// Conditions (SRD subset for v1)
sealed interface Condition {
    object Prone : Condition
    object Stunned : Condition
    object Poisoned : Condition
    object Blinded : Condition
    object Restrained : Condition
    object Incapacitated : Condition
    object Paralyzed : Condition
    object Unconscious : Condition
}

// Ability types
enum class AbilityType {
    Strength, Dexterity, Constitution,
    Intelligence, Wisdom, Charisma
}

// Save effects
sealed interface SaveEffect {
    object AutoFail : SaveEffect
    object Disadvantage : SaveEffect
    object Normal : SaveEffect
}
```

### Outcome Data Classes

```kotlin
data class AttackOutcome(
    val d20Roll: Int,              // The natural d20 roll
    val attackBonus: Int,          // Attack bonus applied
    val totalRoll: Int,            // d20 + bonus
    val targetAC: Int,             // Target's AC
    val hit: Boolean,              // Whether the attack hit
    val isCritical: Boolean,       // Whether it was a critical hit
    val isAutoMiss: Boolean,       // Whether it was a natural 1
    val rollModifier: RollModifier, // Advantage/disadvantage applied
    val appliedConditions: Set<Condition> // Conditions that affected the roll
)

data class DamageOutcome(
    val diceRolls: List<Int>,      // Individual die results
    val diceTotal: Int,            // Sum of dice
    val damageModifier: Int,       // Flat modifier added
    val baseDamage: Int,           // Dice + modifier before resistances
    val damageType: DamageType,    // Type of damage
    val isCritical: Boolean,       // Whether dice were doubled
    val appliedModifiers: Set<DamageModifier>, // Resistances/vulnerabilities applied
    val finalDamage: Int           // Damage after all modifiers
)

data class SavingThrowOutcome(
    val d20Roll: Int,              // The natural d20 roll
    val abilityModifier: Int,      // Ability modifier applied
    val proficiencyBonus: Int,     // Proficiency bonus (if proficient)
    val totalRoll: Int,            // d20 + modifiers
    val dc: Int,                   // Target DC
    val success: Boolean,          // Whether the save succeeded
    val isAutoSuccess: Boolean,    // Whether it was a natural 20
    val rollModifier: RollModifier, // Advantage/disadvantage applied
    val appliedConditions: Set<Condition> // Conditions that affected the roll
)

data class AbilityCheckOutcome(
    val d20Roll: Int,              // The natural d20 roll
    val abilityModifier: Int,      // Ability modifier applied
    val proficiencyBonus: Int,     // Proficiency bonus applied
    val totalRoll: Int,            // d20 + modifiers
    val dc: Int,                   // Target DC
    val success: Boolean,          // Whether the check succeeded
    val rollModifier: RollModifier, // Advantage/disadvantage applied
    val proficiencyLevel: ProficiencyLevel, // Proficiency level applied
    val appliedConditions: Set<Condition> // Conditions that affected the roll
)
```

## Error Handling

The Combat Rules Engine uses sealed result types for operations that can fail:

```kotlin
sealed interface RulesResult<out T> {
    data class Success<T>(val value: T) : RulesResult<T>
    data class InvalidInput(val reason: String) : RulesResult<Nothing>
}
```

**Validation Rules:**
- Attack bonus must be in range -10 to +20
- AC must be in range 1 to 30
- DC must be in range 1 to 30
- Ability modifiers must be in range -5 to +10
- Proficiency bonus must be in range 0 to +6
- Damage dice expressions must be valid (e.g., "2d6", "1d8+3")

Invalid inputs return `RulesResult.InvalidInput` with a descriptive reason.

## Testing Strategy

### Unit Tests (kotest)

**Coverage Target:** 90%+

**Test Categories:**

1. **Attack Resolution Tests**
   - Normal attacks hit/miss based on AC
   - Natural 20 always hits (critical)
   - Natural 1 always misses
   - Advantage takes higher of two rolls
   - Disadvantage takes lower of two rolls
   - Conditions apply correct modifiers

2. **Damage Calculation Tests**
   - Basic damage: dice + modifier
   - Critical hits double dice (not modifier)
   - Resistance halves damage (rounded down)
   - Vulnerability doubles damage
   - Immunity reduces damage to zero
   - Multiple modifiers apply in correct order

3. **Saving Throw Tests**
   - Success/failure based on DC
   - Natural 20 always succeeds
   - Proficiency adds bonus
   - Advantage/disadvantage work correctly
   - Stunned auto-fails STR/DEX saves
   - Conditions apply correct effects

4. **Ability Check Tests**
   - Success/failure based on DC
   - Proficiency adds 1x bonus
   - Expertise adds 2x bonus
   - Advantage/disadvantage work correctly
   - Poisoned applies disadvantage

5. **Condition Effect Tests**
   - Each condition applies correct modifiers
   - Multiple conditions stack appropriately
   - Condition effects are exhaustive

### Property-Based Tests

Use kotest property testing to verify invariants:

```kotlin
test("attack roll with same seed produces same result") {
    checkAll(Arb.long(), Arb.int(-5..10), Arb.int(10..20)) { seed, bonus, ac ->
        val roller1 = DiceRoller(seed)
        val resolver1 = AttackResolver(roller1)
        val outcome1 = resolver1.resolveAttack(bonus, ac)
        
        val roller2 = DiceRoller(seed)
        val resolver2 = AttackResolver(roller2)
        val outcome2 = resolver2.resolveAttack(bonus, ac)
        
        outcome1 shouldBe outcome2
    }
}

test("damage with resistance is always half or less") {
    checkAll(Arb.string(), Arb.int(0..10)) { dice, modifier ->
        val outcome = calculator.calculateDamage(
            dice, modifier, DamageType.Fire, false,
            setOf(DamageModifier.Resistance(DamageType.Fire))
        )
        outcome.finalDamage shouldBeLessThanOrEqual (outcome.baseDamage / 2)
    }
}
```

### Determinism Tests

Critical tests to verify reproducibility:

```kotlin
test("identical inputs with same seed produce identical outcomes") {
    val seed = 42L
    val resolver1 = AttackResolver(DiceRoller(seed))
    val resolver2 = AttackResolver(DiceRoller(seed))
    
    repeat(100) {
        val outcome1 = resolver1.resolveAttack(5, 15)
        val outcome2 = resolver2.resolveAttack(5, 15)
        outcome1 shouldBe outcome2
    }
}
```

## Performance Considerations

**Target:** <1ms per resolution operation

**Optimizations:**
- Condition effects are pre-computed in ConditionRegistry (object singleton)
- Damage dice parsing is cached for repeated expressions
- No allocations in hot paths (use primitive types where possible)
- Sealed types enable exhaustive when expressions (no runtime checks)

**Benchmarking:**
- Use JMH for micro-benchmarks of core resolution functions
- Target 1000+ operations per millisecond on mid-tier devices

## Integration Points

### With DiceRoller (04-dice-system)
- All resolvers depend on DiceRoller for d20 and damage rolls
- DiceRoller must be seeded for deterministic behavior
- Resolvers do not create their own DiceRoller instances

### With Domain Entities (02-core-domain)
- Creature entity provides ability modifiers, AC, proficiency bonus
- Conditions are stored as Set<Condition> on Creature
- DamageModifiers (resistances) are stored on Creature

### With Event Sourcing
- Outcome objects are immutable and can be directly embedded in GameEvents
- All outcomes are serializable for event persistence
- Deterministic behavior ensures event replay produces same outcomes

## Design Decisions

### Why Stateless Resolvers?
Stateless functions are easier to test, reason about, and parallelize. All state is passed as parameters, making dependencies explicit.

### Why Sealed Types for Outcomes?
Sealed types enable exhaustive when expressions, catching missing cases at compile time. They also make the API self-documenting.

### Why Separate Damage Calculation?
Damage calculation is complex (critical hits, resistances) and used in multiple contexts (attacks, spells, environmental). Separating it allows reuse and focused testing.

### Why ConditionRegistry as Object?
Condition effects are static and never change. Using an object singleton avoids repeated allocations and provides a single source of truth.

### Why Not Use Creature Directly?
Resolvers accept primitive parameters (Int, Set<Condition>) rather than Creature objects to:
- Reduce coupling to domain entities
- Make testing simpler (no need to construct full Creature objects)
- Allow use in contexts where Creature doesn't exist (e.g., environmental hazards)

## Future Enhancements (Out of Scope for v1)

- Spell attack resolution (ranged spell attacks, spell saves)
- Area-of-effect damage (multiple targets)
- Concentration checks
- Death saving throws
- Additional conditions (charmed, frightened, grappled, invisible, petrified)
- Cover bonuses to AC
- Flanking advantage (optional rule)
- Mounted combat rules
