# QuestWeaver Quick Reference

**Fast lookup for common patterns, commands, and constraints**

## Module Dependency Rules (CRITICAL)

```
✅ ALLOWED:
app → all modules
feature/* → core:domain, core:rules
feature/encounter → feature:map (ONLY exception)
core:data → core:domain
ai/*, sync/* → core:domain

❌ FORBIDDEN:
core:domain → any other module (pure Kotlin only)
core:rules → Android deps or AI
feature/* → other feature/* (except encounter→map)
Circular dependencies
```

## Common Commands

```bash
# Build & Run
gradle assembleDebug
gradle installDebug

# Testing
gradle test                    # All tests
gradle :core:rules:test        # Specific module
gradle test koverHtmlReport    # With coverage

# Code Quality
gradle lintDebug
gradle detekt

# Clean
gradle clean build
```

## Performance Budgets

| Component | Target | Hard Limit |
|-----------|--------|------------|
| Map render | ≤4ms/frame | 16ms (60fps) |
| AI tactical decision | ≤300ms | 500ms |
| LLM narration | 4s soft | 8s hard |
| Database queries | <50ms | 100ms |

## Event Sourcing Pattern

```kotlin
// ✅ CORRECT: Capture intent + outcome
@Serializable
@SerialName("attack_resolved")
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// ❌ WRONG: Only outcome
data class HPChanged(val creatureId: Long, val newHP: Int) : GameEvent
```

## MVI Pattern

```kotlin
// State: immutable data class
data class EncounterUiState(val round: Int, val activeCreatureId: Long?)

// Intent: sealed interface
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: StateFlow + handler
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    fun handle(intent: EncounterIntent) { /* process */ }
}
```

## Repository Pattern

```kotlin
// Interface in core:domain
interface EventRepository {
    suspend fun append(event: GameEvent)
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}

// Implementation in core:data
class EventRepositoryImpl(private val dao: EventDao) : EventRepository
```

## Koin DI Scopes

| Scope | Use Case | Example |
|-------|----------|---------|
| `single` | Shared, expensive | Database, Repository, RulesEngine |
| `factory` | New instance | Use cases, helpers |
| `viewModel` | Lifecycle-aware | ViewModels |

## Deterministic Rules

```kotlin
// ✅ CORRECT: Seeded RNG
val roller = DiceRoller(seed = sessionSeed)
val result = roller.d20()

// ❌ WRONG: Unseeded random
val result = Random.nextInt(1, 21) // Non-deterministic
```

## Compose Performance

```kotlin
// ✅ Read state in Canvas (draw phase)
Canvas(modifier) {
    state.tokens.forEach { token ->
        drawCircle(/* ... */)
    }
}

// ✅ Use lambda modifiers for animation
Column(Modifier.offset { IntOffset(0, scroll.value) })

// ✅ Use derivedStateOf for computed state
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

## AI Integration

```kotlin
// ✅ CORRECT: AI proposes, rules validate
val aiAction = tacticalAgent.decide(state, creature)
val validated = rulesEngine.validate(aiAction)
if (validated.isLegal) commit(validated.events)

// ❌ WRONG: Direct mutation
creature.hp -= aiAction.damage
```

## Serialization

```kotlin
// ✅ CORRECT: Use @SerialName
@Serializable
@SerialName("encounter_started")
data class EncounterStarted(...) : GameEvent

// JSON config for storage
val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    prettyPrint = false
}
```

## Testing

```kotlin
// kotest structure
class ComponentTest : FunSpec({
    test("descriptive behavior") {
        // Arrange
        val input = createTestData()
        
        // Act
        val result = systemUnderTest.process(input)
        
        // Assert
        result shouldBe expectedValue
    }
})

// Property-based for rules
test("d20 always returns 1-20") {
    checkAll(Arb.long()) { seed ->
        DiceRoller(seed).d20() shouldBeInRange 1..20
    }
}
```

## Coverage Targets

| Module | Target |
|--------|--------|
| core/rules | 90%+ |
| core/domain | 85%+ |
| core/data | 80%+ |
| feature/* | 60%+ |

## File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Entity | Singular, PascalCase | `Creature.kt` |
| Use Case | Verb phrase | `ProcessPlayerAction.kt` |
| ViewModel | Suffix | `EncounterViewModel.kt` |
| Repository | Interface + Impl | `EventRepository.kt`, `EventRepositoryImpl.kt` |
| Screen | Suffix | `TacticalMapScreen.kt` |
| DAO | Suffix | `EventDao.kt` |

## Package Structure

```
✅ Good: dev.questweaver.feature.map.ui
❌ Bad:  dev.questweaver.ui.map
```

Group by feature/layer, not by type.

## Critical Constraints

1. **Offline-First**: Core gameplay must work without network
2. **Deterministic**: Same events → same outcomes (seeded RNG)
3. **Event-Sourced**: All mutations produce GameEvent instances
4. **Module Boundaries**: core:domain and core:rules are pure Kotlin (NO Android)
5. **AI Proposes, Rules Validate**: AI never directly mutates state

## Common Mistakes

```kotlin
// ❌ Android deps in core:domain
import android.content.Context // FORBIDDEN

// ❌ Unseeded random in rules
val roll = Random.nextInt(1, 21) // Non-deterministic

// ❌ Direct state mutation
creature.hp -= damage // No events generated

// ❌ Feature-to-feature dependency
implementation(project(":feature:character")) // FORBIDDEN

// ❌ Non-exhaustive when for sealed types
when (event) {
    is GameEvent.AttackResolved -> handle(event)
    else -> {} // FORBIDDEN - must be exhaustive
}
```

## Version Catalog Location

All versions in `gradle/libs.versions.toml` - single source of truth

## Key Tech Stack

- **Language**: Kotlin 1.9.24, JVM 17
- **UI**: Compose BOM 2024.06.00 + Material3
- **DI**: Koin 3.5.6
- **Database**: Room 2.6.1 + SQLCipher 4.5.5
- **Network**: Retrofit 2.11.0 + OkHttp 4.12.0
- **Serialization**: kotlinx-serialization 1.6.3
- **Testing**: kotest 5.9.1 + MockK 1.13.10
- **AI**: ONNX Runtime 1.16.3

## Build Configuration

- **compileSdk**: 34
- **minSdk**: 26 (Android 8.0+)
- **targetSdk**: 34
- **JVM**: 17

## Security

- **Local Data**: SQLCipher with Android Keystore-wrapped keys
- **Network**: TLS 1.2+
- **ProGuard**: Enabled in release
- **Logging**: NEVER log PII

## When in Doubt

Ask these questions:
1. Does it work offline?
2. Is it deterministic?
3. Does it respect module boundaries?
4. Are events generated for state changes?
5. Is it tested?

## Documentation Locations

- **Architecture**: `questweaver_architecture_design.md`
- **Steering Rules**: `.kiro/steering/*.md`
- **Specs**: `.kiro/specs/*/`
- **Agent Guide**: `AGENTS.md`
- **README**: `README.md`
