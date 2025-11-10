# core:domain

**Pure Kotlin business logic module with zero Android dependencies**

## Purpose

The `core:domain` module contains the core business logic, domain entities, use cases, and event definitions for QuestWeaver. This module is intentionally kept pure Kotlin (no Android dependencies) to ensure:

- **Testability**: Fast unit tests without Android framework
- **Reusability**: Logic can be shared across platforms if needed
- **Separation of Concerns**: Business rules isolated from UI and infrastructure

## Responsibilities

- Define immutable domain entities (Creature, Campaign, Encounter, MapGrid)
- Implement use cases with single responsibility
- Define event sourcing event hierarchy
- Declare repository interfaces (implementations in `core:data`)
- Define result types for operations

## Key Classes and Interfaces

### Entities (Placeholder)

- `Creature`: Represents a character or NPC with stats, HP, abilities
- `Campaign`: Top-level container for a game campaign
- `Encounter`: A combat or social encounter instance
- `MapGrid`: Grid-based tactical map representation
- `Abilities`: D&D ability scores (STR, DEX, CON, INT, WIS, CHA)

### Use Cases (Placeholder)

- `ProcessPlayerAction`: Validates and processes player actions
- `RunCombatRound`: Executes a full round of combat
- `CalculateInitiative`: Determines turn order for encounters

### Events (Placeholder)

```kotlin
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

// Examples:
// - EncounterStarted
// - MoveCommitted
// - AttackResolved
// - SpellCast
// - ConditionApplied
```

### Repositories (Interfaces Only)

- `EventRepository`: Append and query game events
- `CampaignRepository`: CRUD operations for campaigns
- `CreatureRepository`: Manage creatures and party members

## Dependencies

### Production

- `kotlin-stdlib`: Kotlin standard library
- `kotlinx-coroutines-core`: Coroutines for async operations
- `kotlinx-serialization-json`: JSON serialization

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `kotest-property`: Property-based testing
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Pure Kotlin code only
- Coroutines and Flow for async operations
- Sealed classes/interfaces for ADTs
- Data classes for immutable entities

### ❌ Forbidden

- **NO Android dependencies** (android.*, androidx.*)
- NO UI code
- NO database implementations (only interfaces)
- NO network code

## Architecture Patterns

### Immutability

All entities are immutable data classes:

```kotlin
data class Creature(
    val id: Long,
    val name: String,
    val ac: Int,
    val hpCurrent: Int,
    val hpMax: Int,
    val speed: Int,
    val abilities: Abilities
)
```

### Use Case Pattern

Single responsibility with operator invoke:

```kotlin
class ProcessPlayerAction(
    private val rulesEngine: RulesEngine,
    private val eventRepo: EventRepository
) {
    suspend operator fun invoke(action: NLAction): ActionResult {
        // Validate, execute, generate events
    }
}
```

### Event Sourcing

All state mutations produce events:

```kotlin
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent
```

### Result Types

Sealed interfaces for operation results:

```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}
```

## Testing Approach

### Unit Tests

- Test use cases with mocked dependencies
- Test entity behavior and validation
- Test event generation logic
- Property-based tests for deterministic logic

### Coverage Target

**85%+** code coverage

### Example Test

```kotlin
class ProcessPlayerActionTest : FunSpec({
    test("attack action generates AttackResolved event") {
        val rulesEngine = mockk<RulesEngine>()
        val eventRepo = mockk<EventRepository>()
        val useCase = ProcessPlayerAction(rulesEngine, eventRepo)
        
        every { rulesEngine.resolveAttack(any(), any()) } returns outcome
        coEvery { eventRepo.append(any()) } just Runs
        
        val result = useCase(NLAction.Attack(1, 2))
        
        result shouldBeInstanceOf ActionResult.Success::class
        coVerify { eventRepo.append(any<GameEvent.AttackResolved>()) }
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :core:domain:build

# Run tests
./gradlew :core:domain:test

# Run tests with coverage
./gradlew :core:domain:test koverHtmlReport
```

## Package Structure

```
dev.questweaver.domain/
├── entities/           # Domain entities
├── usecases/          # Use case implementations
├── events/            # Event definitions
├── repositories/      # Repository interfaces
└── results/           # Result type definitions
```

## Integration Points

### Consumed By

- `core:data` (implements repository interfaces)
- `core:rules` (uses entities and events)
- `feature:*` (uses use cases and entities)
- `ai:*` (uses entities for context)
- `sync:*` (uses entities for sync)

### Depends On

- None (pure Kotlin only)

## Notes

- This module must remain pure Kotlin with zero Android dependencies
- All entities should be immutable
- Use sealed types for ADTs (events, actions, results)
- Always use exhaustive `when` expressions for sealed types
- Repository interfaces only - implementations belong in `core:data`

---

**Last Updated**: 2025-11-10
