# AGENTS.md

**AI Agent Instructions for QuestWeaver**

This file provides structured guidance for AI coding agents working on QuestWeaver. It complements the README and existing documentation with agent-specific instructions.

---

## Project Overview

**QuestWeaver** is an offline-first Android app for solo D&D-style RPG gameplay with AI Game Master capabilities.

- **Platform**: Android (API 26+, targetSdk 34)
- **Language**: Kotlin 100% (1.9.24, JVM 17)
- **Architecture**: Clean Architecture + MVI + Event Sourcing
- **Build System**: Gradle 8.5+ with Kotlin DSL

**Core Principle**: Offline-first, deterministic, event-sourced gameplay with strict module boundaries.

---

## Quick Start Commands

```bash
# Build and install
gradle assembleDebug
gradle installDebug

# Run tests
gradle test                    # All tests
gradle :core:rules:test        # Specific module
gradle lint                    # Lint checks

# Clean build
gradle clean build
```

**Note**: This project uses Gradle 9.2.0 installed system-wide. You can also use the Gradle wrapper with `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows) if preferred.

---

## Critical Rules (MUST FOLLOW)

### 1. Module Dependency Rules (STRICTLY ENFORCED)

**ALLOWED:**
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (ONLY exception)
- `core:data` → `core:domain`
- `core:rules` → `core:domain`
- `ai/*`, `sync/*` → `core:domain`

**FORBIDDEN:**
- `core:domain` → any other module (pure Kotlin only, NO Android dependencies)
- `core:rules` → Android dependencies or AI modules
- `feature/*` → other `feature/*` (except encounter→map)
- Circular dependencies

**Before adding any dependency**, verify it doesn't violate these rules.

### 2. Event Sourcing (NON-NEGOTIABLE)

- **All state mutations MUST produce immutable `GameEvent` instances**
- Events are logged to database for full replay capability
- State is derived from event replay, NEVER mutated directly
- Use sealed interfaces for event hierarchies

```kotlin
// ✅ CORRECT: Capture intent AND outcome
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// ❌ WRONG: Only captures outcome
data class HPChanged(val creatureId: Long, val newHP: Int) : GameEvent
```

### 3. Deterministic Behavior (REQUIRED)

- **Always use seeded RNG** for reproducible outcomes
- Same events MUST produce same state on replay
- NO unseeded `Random()` calls in production code

```kotlin
// ✅ CORRECT: Seeded for determinism
val roller = DiceRoller(seed = sessionSeed)
val result = roller.d20()

// ❌ WRONG: Non-deterministic
val result = Random.nextInt(1, 21)
```

### 4. Immutability First

- Prefer `val` over `var`
- Use `data class` for entities
- Use sealed types for ADTs (actions, events, results)
- Require exhaustive `when` expressions (no `else` branch for sealed types)

### 5. MVI Pattern (REQUIRED for ViewModels)

```kotlin
// State: single immutable data class
data class EncounterUiState(
    val round: Int = 1,
    val activeCreatureId: Long? = null
)

// Intent: sealed interface for user actions
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: StateFlow + intent handler
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) {
        viewModelScope.launch { /* process */ }
    }
}
```

---

## Module Structure & Responsibilities

```
app/                    # DI assembly, navigation, theme
├── core/
│   ├── domain/        # Pure Kotlin: entities, use cases, events (NO Android deps)
│   ├── data/          # Room + SQLCipher, repository implementations
│   └── rules/         # Deterministic rules engine (NO Android, NO AI)
├── feature/
│   ├── map/           # Compose Canvas, pathfinding, geometry
│   ├── encounter/     # Turn engine, combat UI (depends on feature:map)
│   └── character/     # Character sheets, party management
├── ai/
│   ├── ondevice/      # ONNX Runtime for intent parsing
│   └── gateway/       # Retrofit client for remote LLM (optional)
└── sync/
    └── firebase/      # Cloud backup via WorkManager
```

### core/domain (Pure Kotlin - NO Android)
- Immutable entities (`Creature`, `Campaign`, `Encounter`, `MapGrid`)
- Use cases with single `suspend operator fun invoke()`
- Event definitions (sealed interfaces)
- Repository interfaces (implementations in `core:data`)

### core/rules (Deterministic - NO AI, NO Android)
- D&D 5e SRD rules engine
- Seeded `DiceRoller` for reproducible randomness
- Combat resolution, saving throws, conditions
- 100% deterministic behavior required

### core/data
- Room database with SQLCipher encryption
- Repository implementations
- Event-sourced persistence
- Separate Room entities from domain entities

### feature/* modules
- Compose UI screens and ViewModels
- MVI pattern with StateFlow
- Can depend on `core:domain` and `core:rules` only
- Exception: `feature/encounter` can depend on `feature:map`

---

## Code Style & Patterns

### Naming Conventions

- **Classes**: PascalCase (`Creature`, `DiceRoller`)
- **Functions/Properties**: camelCase (`rollInitiative`, `currentHP`)
- **Files**: Match class name (`Creature.kt`, `ProcessPlayerAction.kt`)
- **Packages**: Lowercase with dots, group by feature/layer not type
  - ✅ Good: `dev.questweaver.feature.map.ui`
  - ❌ Bad: `dev.questweaver.ui.map`

### Repository Pattern

```kotlin
// Interface in core:domain
interface EventRepository {
    suspend fun append(event: GameEvent)
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}

// Implementation in core:data
class EventRepositoryImpl(private val dao: EventDao) : EventRepository {
    override suspend fun append(event: GameEvent) = dao.insert(event.toEntity())
    override fun observeSession(sessionId: Long) = dao.observeBySession(sessionId)
        .map { entities -> entities.map { it.toDomain() } }
}
```

### Use Case Pattern

```kotlin
class ProcessPlayerAction(
    private val rulesEngine: RulesEngine,
    private val eventRepo: EventRepository
) {
    suspend operator fun invoke(action: NLAction): ActionResult {
        // Validate with rules engine
        val outcome = rulesEngine.resolve(action)
        
        // Generate events
        val events = outcome.toEvents()
        
        // Persist events
        events.forEach { eventRepo.append(it) }
        
        return ActionResult.Success(events)
    }
}
```

### Sealed Types (Exhaustive When Required)

```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}

// MUST be exhaustive - no else branch
fun handle(result: ActionResult) = when (result) {
    is ActionResult.Success -> applyEvents(result.events)
    is ActionResult.Failure -> showError(result.reason)
    is ActionResult.RequiresChoice -> promptUser(result.options)
}
```

---

## Testing Requirements

### Framework: kotest + MockK

```kotlin
class ComponentTest : FunSpec({
    context("feature description") {
        test("specific behavior description") {
            // Arrange
            val input = createTestData()
            
            // Act
            val result = systemUnderTest.process(input)
            
            // Assert
            result shouldBe expectedValue
        }
    }
})
```

### Coverage Targets

- `core/rules`: **90%+** (deterministic, critical)
- `core/domain`: **85%+** (use cases, entities)
- `core/data`: **80%+** (repositories)
- `feature/*`: **60%+** (focus on logic, not UI rendering)

### Testing Rules

1. **Always use seeded RNG** for deterministic tests
2. **Use in-memory Room database** for repository tests
3. **Mock external dependencies** with MockK
4. **Verify event generation** for all state mutations
5. **Property-based tests** for rules engine

```kotlin
// ✅ CORRECT: Deterministic test
test("attack roll with seed produces expected result") {
    val roller = DiceRoller(seed = 42)
    roller.d20().value shouldBe 15 // Reproducible
}

// ❌ WRONG: Non-deterministic test
test("attack roll returns valid range") {
    val result = Random.nextInt(1, 21) // Flaky
    result shouldBeInRange 1..20
}
```

---

## Common Tasks & Patterns

### Adding a New Feature Module

1. Create module directory: `feature/newfeature/`
2. Add `build.gradle.kts` with `com.android.library` plugin
3. Add to `settings.gradle.kts`: `include(":feature:newfeature")`
4. Add dependencies ONLY to `core:domain` and `core:rules`
5. Create package structure: `ui/`, `viewmodel/`, `di/`
6. Implement MVI pattern with StateFlow
7. Add Koin module for DI
8. Write tests with kotest

### Adding a New Event Type

1. Define in `core/domain/events/`
2. Extend sealed interface `GameEvent`
3. Include `sessionId` and `timestamp`
4. Capture both intent AND outcome
5. Add Room entity in `core/data/db/entities/`
6. Update DAO and repository
7. Add event replay logic
8. Write property-based tests

### Adding a New Use Case

1. Create in `core/domain/usecases/`
2. Single `suspend operator fun invoke()` method
3. Return sealed `Result` type
4. Validate with `RulesEngine`
5. Generate and persist events
6. Add to Koin module
7. Write unit tests with mocked dependencies

### Adding UI Screen

1. Create Composable in `feature/*/ui/`
2. Create ViewModel with MVI pattern
3. Define `UiState` data class
4. Define sealed `Intent` interface
5. Expose `StateFlow<UiState>`
6. Handle intents in ViewModel
7. Hoist state to ViewModel (keep Composable stateless)
8. Add navigation in `app` module

---

## Performance Budgets

- **Map render**: ≤4ms per frame (60fps target)
- **AI tactical decision**: ≤300ms on-device
- **LLM narration**: 4s soft timeout, 8s hard timeout
- **Database queries**: <50ms typical

**When implementing features, profile and verify these targets are met.**

---

## Security Requirements

- **Encryption**: All local data encrypted with SQLCipher
- **Keys**: Android Keystore-wrapped keys for database
- **Network**: TLS 1.2+ for all API calls
- **ProGuard**: Obfuscation enabled in release builds
- **Logging**: NEVER log PII (emails, names, etc.)

```kotlin
// ✅ CORRECT: No PII in logs
logger.info { "Attack resolved: attacker=$attackerId, hit=$hit" }

// ❌ WRONG: Logs PII
logger.debug { "User ${user.email} performed ${action}" }
```

---

## Dependency Management

- **Version Catalog**: All versions in `gradle/libs.versions.toml`
- **Single Source of Truth**: Never hardcode versions in module `build.gradle.kts`
- **Update Process**: Update version catalog, then sync Gradle

```kotlin
// ✅ CORRECT: Use version catalog
implementation(libs.androidx.compose.ui)

// ❌ WRONG: Hardcoded version
implementation("androidx.compose.ui:ui:1.5.0")
```

---

## Common Pitfalls to Avoid

1. **Adding Android dependencies to `core:domain` or `core:rules`**
   - These modules MUST be pure Kotlin
   - Check imports before committing

2. **Using unseeded Random() in production code**
   - Always use `DiceRoller` with seed
   - Breaks deterministic replay

3. **Mutating state directly instead of generating events**
   - All mutations MUST produce `GameEvent`
   - State is derived, not mutated

4. **Adding feature-to-feature dependencies**
   - Only `feature/encounter` → `feature/map` allowed
   - Use `core:domain` for shared logic

5. **Non-exhaustive when expressions for sealed types**
   - Compiler enforces exhaustiveness
   - Never add `else` branch for sealed types

6. **Forgetting to update tests after code changes**
   - Run tests before committing
   - Maintain coverage targets

7. **Logging PII or sensitive data**
   - Never log emails, names, personal info
   - Use IDs only in logs

---

## When in Doubt

1. **Check module boundaries**: Does this dependency violate rules?
2. **Is it deterministic?**: Can outcomes be reproduced from events?
3. **Does it work offline?**: Core features must function without network
4. **Is it event-sourced?**: Are events generated for state changes?
5. **Is it tested?**: Does it meet coverage targets?

**Refer to steering files in `.kiro/steering/` for additional context.**

---

## Additional Resources

- **Architecture Design**: `questweaver_architecture_design.md`
- **Steering Rules**: `.kiro/steering/*.md`
- **Version Catalog**: `gradle/libs.versions.toml`
- **Build Configuration**: `build.gradle.kts` (root and module-level)

---

**Last Updated**: 2025-11-10
