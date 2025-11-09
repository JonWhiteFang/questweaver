---
inclusion: always
---

# QuestWeaver Project Structure

## Module Architecture

Multi-module Clean Architecture with strict dependency rules:

```
app/                    # DI assembly, navigation, theme
├── core/
│   ├── domain/        # Pure Kotlin: entities, use cases, events (NO dependencies)
│   ├── data/          # Room + SQLCipher, repository implementations
│   └── rules/         # Deterministic rules engine (NO Android, NO AI)
├── feature/
│   ├── map/           # Compose Canvas rendering, pathfinding, geometry
│   ├── encounter/     # Turn engine, initiative, combat UI
│   └── character/     # Character sheets, party management
├── ai/
│   ├── ondevice/      # ONNX Runtime for intent parsing
│   └── gateway/       # Retrofit client for remote LLM (optional)
└── sync/
    └── firebase/      # Cloud backup via WorkManager
```

## Critical Dependency Rules

**ALLOWED:**
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (only exception)
- `core:data` → `core:domain`
- `core:rules` → `core:domain`
- `ai/*` → `core:domain`
- `sync/*` → `core:domain`, `core:data`

**FORBIDDEN:**
- `core:domain` → any other module (pure Kotlin only)
- `core:rules` → Android dependencies or AI
- `feature/*` → other `feature/*` (except encounter→map)
- Circular dependencies

## Module Responsibilities

### core/domain/
**Pure Kotlin business logic - NO Android dependencies**

Structure:
- `entities/` - Immutable data classes (Creature, Campaign, Encounter, MapGrid)
- `usecases/` - Single-responsibility operations (ProcessPlayerAction, RunCombatRound)
- `events/` - Sealed interfaces for event sourcing (GameEvent hierarchy)
- `repositories/` - Interfaces only (implementations in core:data)

Key rules:
- All entities are immutable (`data class` with `val`)
- Use sealed classes/interfaces for ADTs (actions, events, results)
- No Android imports allowed

### core/data/
**Persistence layer with Room + SQLCipher**

Structure:
- `db/` - AppDatabase, DAOs
- `entities/` - Room entity classes (separate from domain entities)
- `repositories/` - Repository implementations
- `di/` - Koin module for data layer

Key rules:
- All saves encrypted with SQLCipher
- Event-sourced persistence (Event table logs all mutations)
- Repository pattern: interface in domain, implementation here

### core/rules/
**Deterministic D&D 5e SRD rules engine**

Structure:
- `engine/` - RulesEngine (validates actions, resolves outcomes)
- `dice/` - Seeded DiceRoller (reproducible randomness)
- `combat/` - Attack, damage, saving throw resolution
- `conditions/` - Status effects

Key rules:
- 100% deterministic (seeded RNG only)
- NO AI calls in rules engine
- NO Android dependencies
- Exhaustive `when` for sealed types

### feature/map/
**Tactical map rendering and geometry**

Structure:
- `ui/` - Compose screens and ViewModels
- `render/` - Canvas drawing logic
- `pathfinding/` - A* pathfinding
- `geometry/` - Line-of-effect, range calculations

Key rules:
- Target ≤4ms render time per frame
- Use Compose Canvas for grid rendering
- Keep rendering logic separate from UI state

### feature/encounter/
**Combat turn management**

Structure:
- `ui/` - Combat screens, initiative list
- `engine/` - TurnEngine, InitiativeTracker
- `viewmodel/` - EncounterViewModel (MVI pattern)

Key rules:
- Depends on feature:map for tactical integration
- MVI pattern: unidirectional data flow
- All actions validated by core:rules before commit

### feature/character/
**Character sheets and party management**

Structure:
- `ui/` - Character sheet screens, party overview
- `viewmodel/` - CharacterViewModel

Key rules:
- Display only, no rules logic here
- Import/export character data

### ai/ondevice/
**ONNX Runtime for on-device inference**

Structure:
- `models/` - Model wrappers (IntentClassifier)
- `inference/` - OnnxSessionManager
- `intent/` - Intent parsing with fallback to keywords

Key rules:
- Models in `app/src/main/assets/models/`
- Warm up models on background thread
- Always provide fallback (keyword matching)

### ai/gateway/
**Optional remote LLM client**

Structure:
- `api/` - Retrofit interfaces
- `dto/` - Request/response DTOs
- `cache/` - LRU cache for narration

Key rules:
- 4s soft timeout, 8s hard timeout
- Cache responses (hash context + action)
- Fallback to template narration on failure

### sync/firebase/
**Optional cloud backup**

Structure:
- `auth/` - Firebase Authentication
- `storage/` - Cloud Storage integration
- `workers/` - WorkManager periodic sync

Key rules:
- Opt-in only (offline-first)
- Encrypt before upload
- Handle conflicts with event sourcing

## File Naming Conventions

- **Entities**: `Creature.kt` (singular, PascalCase)
- **Use Cases**: `ProcessPlayerAction.kt` (verb phrase)
- **ViewModels**: `EncounterViewModel.kt` (suffix)
- **Repositories**: `EventRepository.kt` (interface), `EventRepositoryImpl.kt` (impl)
- **Screens**: `TacticalMapScreen.kt` (suffix)
- **DAOs**: `EventDao.kt` (suffix)

**Packages**: Lowercase with dots, group by feature/layer not type
- Good: `dev.questweaver.feature.map.ui`
- Bad: `dev.questweaver.ui.map`

## Architectural Patterns

### MVI (Model-View-Intent)
```kotlin
// State: single immutable data class
data class EncounterUiState(
    val round: Int = 1,
    val activeCreatureId: Long? = null,
    val mapState: MapState = MapState()
)

// Intent: sealed interface for user actions
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: unidirectional flow
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) { /* process */ }
}
```

### Event Sourcing
```kotlin
// All state mutations produce immutable events
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// State derived from event replay
fun replayEvents(events: List<GameEvent>): EncounterState
```

### Repository Pattern
```kotlin
// Interface in core:domain
interface EventRepository {
    suspend fun append(event: GameEvent)
    suspend fun forSession(sessionId: Long): List<GameEvent>
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}

// Implementation in core:data
class EventRepositoryImpl(private val dao: EventDao) : EventRepository
```

## Package Structure Template

```
module/src/main/kotlin/dev/questweaver/{module}/
├── {feature}/           # Group by feature
│   ├── ui/             # Composables, screens
│   ├── viewmodel/      # ViewModels
│   └── domain/         # Feature-specific logic
└── di/                 # Koin module
```

## Resource Organization

**App resources**: `app/src/main/res/`
- `values/strings.xml` - UI strings
- `values/themes.xml` - Material3 theme
- `drawable/` - Vector drawables
- `mipmap/` - App icons

**Assets**: `app/src/main/assets/`
- `models/intent.onnx` - Intent classifier (~80MB)
- `models/tokenizer.json` - Tokenizer config

## Build Files

**Root level**:
- `build.gradle.kts` - Root configuration
- `settings.gradle.kts` - Module includes
- `gradle/libs.versions.toml` - Version catalog (single source of truth)
- `gradle.properties` - JVM args, build flags

**Module level**:
- Each module has `build.gradle.kts`
- Libraries use `com.android.library`
- App uses `com.android.application`
- All use `kotlin-android` plugin

## Testing Structure

Mirror main source structure in test directories:

```
module/src/
├── main/kotlin/dev/questweaver/{module}/
└── test/kotlin/dev/questweaver/{module}/
    └── {Feature}Test.kt
```

**Test naming**: `{ClassUnderTest}Test.kt`

## Key Constraints When Creating Files

1. **core/domain**: Pure Kotlin only, no Android imports
2. **core/rules**: Deterministic only, no AI, no Android
3. **feature modules**: Cannot depend on other features (except encounter→map)
4. **Event sourcing**: All state mutations must produce GameEvent
5. **MVI pattern**: ViewModels expose StateFlow, handle sealed Intent types
6. **Repository pattern**: Interfaces in domain, implementations in data
7. **Sealed types**: Use exhaustive `when` expressions
8. **Immutability**: Prefer `val` and `data class` for entities
