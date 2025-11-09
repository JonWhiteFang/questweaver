# QuestWeaver Project Structure

## Module Organization

QuestWeaver follows a multi-module architecture with clear separation of concerns. Each module has a specific responsibility and well-defined boundaries.

```
questweaver/
├── app/                          # Android application module
├── core/
│   ├── domain/                   # Business logic & entities
│   ├── data/                     # Data layer & repositories
│   └── rules/                    # Deterministic rules engine
├── feature/
│   ├── map/                      # Tactical map UI & rendering
│   ├── encounter/                # Combat & turn management
│   └── character/                # Character sheets & party
├── ai/
│   ├── ondevice/                 # ONNX models & inference
│   └── gateway/                  # Remote AI API client
└── sync/
    └── firebase/                 # Cloud sync & backup
```

## Module Details

### app/
**Purpose**: Android application entry point and DI assembly

**Contents**:
- `MainActivity.kt` - Main Compose activity
- `QuestWeaverApp.kt` - Application class with Koin setup
- `build.gradle.kts` - App-level dependencies
- `proguard-rules.pro` - ProGuard configuration
- `src/main/AndroidManifest.xml` - App manifest

**Dependencies**: All other modules

**Key Responsibilities**:
- Koin module assembly and initialization
- Navigation setup (when implemented)
- Theme and app-level Compose configuration
- Application lifecycle management

---

### core/domain/
**Purpose**: Pure Kotlin business logic, entities, and use cases

**Contents**:
- `entities/` - Domain models (Creature, Campaign, Encounter, etc.)
- `usecases/` - Business operations (ProcessPlayerAction, RunCombatRound, etc.)
- `events/` - Sealed classes for event sourcing (GameEvent hierarchy)
- `repositories/` - Repository interfaces (implementation in core:data)

**Dependencies**: None (pure Kotlin)

**Key Characteristics**:
- No Android dependencies
- Immutable data classes
- Sealed classes for ADTs (actions, events, results)
- Interface-driven design

**Example Structure**:
```
core/domain/src/main/kotlin/dev/questweaver/domain/
├── entities/
│   ├── Creature.kt
│   ├── Campaign.kt
│   ├── Encounter.kt
│   └── MapGrid.kt
├── events/
│   └── GameEvent.kt (sealed interface)
├── usecases/
│   ├── ProcessPlayerAction.kt
│   └── RunCombatRound.kt
└── repositories/
    ├── EventRepository.kt
    └── CreatureRepository.kt
```

---

### core/data/
**Purpose**: Data persistence, Room database, and repository implementations

**Contents**:
- `db/` - Room database and DAOs
- `entities/` - Room entity classes
- `repositories/` - Repository implementations
- `di/` - Koin data module

**Dependencies**: core:domain

**Key Responsibilities**:
- Room database setup with SQLCipher encryption
- Event-sourced persistence (Event table)
- Repository pattern implementation
- Database migrations

**Example Structure**:
```
core/data/src/main/kotlin/dev/questweaver/data/
├── db/
│   ├── AppDatabase.kt
│   ├── EventDao.kt
│   └── CreatureDao.kt
├── entities/
│   ├── EventEntity.kt
│   └── CreatureEntity.kt
├── repositories/
│   ├── EventRepositoryImpl.kt
│   └── CreatureRepositoryImpl.kt
└── di/
    └── DataModule.kt
```

---

### core/rules/
**Purpose**: Deterministic rules engine (no AI, no randomness leaks)

**Contents**:
- `engine/` - Core rules engine
- `dice/` - Seeded dice roller
- `combat/` - Attack, damage, saving throw resolution
- `conditions/` - Status effects and conditions

**Dependencies**: core:domain

**Key Characteristics**:
- Pure Kotlin (no Android dependencies)
- Deterministic with seeded RNG
- SRD-compatible D&D 5e mechanics
- Exhaustive when expressions for sealed types

**Example Structure**:
```
core/rules/src/main/kotlin/dev/questweaver/rules/
├── engine/
│   └── RulesEngine.kt
├── dice/
│   ├── DiceRoller.kt
│   └── DiceRoll.kt
├── combat/
│   ├── AttackResolver.kt
│   └── DamageCalculator.kt
└── conditions/
    └── ConditionManager.kt
```

---

### feature/map/
**Purpose**: Tactical map UI, rendering, and geometry

**Contents**:
- `ui/` - Compose map components
- `render/` - Canvas rendering logic
- `pathfinding/` - A* pathfinding
- `geometry/` - Line-of-effect, range calculations

**Dependencies**: core:domain

**Key Responsibilities**:
- Grid-based map rendering with Compose Canvas
- Token placement and movement visualization
- Pathfinding and movement validation
- Range overlays and AoE templates
- Touch gesture handling

**Example Structure**:
```
feature/map/src/main/kotlin/dev/questweaver/feature/map/
├── ui/
│   ├── TacticalMapScreen.kt
│   └── MapViewModel.kt
├── render/
│   ├── MapRenderer.kt
│   └── TokenRenderer.kt
├── pathfinding/
│   └── AStarPathfinder.kt
└── geometry/
    ├── LineOfEffect.kt
    └── RangeCalculator.kt
```

---

### feature/encounter/
**Purpose**: Combat encounters, turn management, initiative

**Contents**:
- `ui/` - Combat UI screens
- `engine/` - Turn engine and initiative
- `viewmodel/` - Encounter state management

**Dependencies**: core:domain, core:rules, feature:map

**Key Responsibilities**:
- Initiative tracking and turn order
- Combat action processing
- Turn phase management (move/action/bonus/reaction)
- Combat UI and action selection

**Example Structure**:
```
feature/encounter/src/main/kotlin/dev/questweaver/feature/encounter/
├── ui/
│   ├── EncounterScreen.kt
│   └── InitiativeList.kt
├── engine/
│   ├── TurnEngine.kt
│   └── InitiativeTracker.kt
└── viewmodel/
    └── EncounterViewModel.kt
```

---

### feature/character/
**Purpose**: Character sheets, party management, PC/NPC UI

**Contents**:
- `ui/` - Character sheet screens
- `viewmodel/` - Character state management

**Dependencies**: core:domain

**Key Responsibilities**:
- PC character sheet display and editing
- AI party member overview
- Inventory and equipment management
- Character import/export

**Example Structure**:
```
feature/character/src/main/kotlin/dev/questweaver/feature/character/
├── ui/
│   ├── CharacterSheetScreen.kt
│   └── PartyOverview.kt
└── viewmodel/
    └── CharacterViewModel.kt
```

---

### ai/ondevice/
**Purpose**: On-device AI inference with ONNX Runtime

**Contents**:
- `models/` - Model wrappers
- `inference/` - ONNX session management
- `intent/` - Intent classification

**Dependencies**: core:domain

**Key Responsibilities**:
- ONNX model loading and inference
- Intent parsing from natural language
- Model warmup and caching
- Fallback to keyword matching

**Example Structure**:
```
ai/ondevice/src/main/kotlin/dev/questweaver/ai/ondevice/
├── models/
│   └── IntentClassifier.kt
├── inference/
│   └── OnnxSessionManager.kt
└── intent/
    └── IntentParser.kt
```

**Assets**: `app/src/main/assets/models/intent.onnx`

---

### ai/gateway/
**Purpose**: Remote AI API client (optional, for rich narration)

**Contents**:
- `api/` - Retrofit API interfaces
- `dto/` - Data transfer objects
- `cache/` - Response caching

**Dependencies**: core:domain

**Key Responsibilities**:
- Retrofit API client for LLM gateway
- Request/response serialization
- Response caching and rate limiting
- Timeout and fallback handling

**Example Structure**:
```
ai/gateway/src/main/kotlin/dev/questweaver/ai/gateway/
├── api/
│   └── AIGatewayApi.kt
├── dto/
│   ├── NarrateRequest.kt
│   └── NarrateResponse.kt
└── cache/
    └── NarrationCache.kt
```

---

### sync/firebase/
**Purpose**: Cloud backup and synchronization (optional)

**Contents**:
- `auth/` - Firebase authentication
- `storage/` - Cloud Storage integration
- `workers/` - WorkManager backup jobs

**Dependencies**: core:domain, core:data

**Key Responsibilities**:
- Firebase Authentication setup
- Campaign backup to Cloud Storage
- WorkManager periodic sync
- Conflict resolution

**Example Structure**:
```
sync/firebase/src/main/kotlin/dev/questweaver/sync/firebase/
├── auth/
│   └── FirebaseAuthManager.kt
├── storage/
│   └── CloudBackupManager.kt
└── workers/
    └── BackupWorker.kt
```

---

## Dependency Rules

### Allowed Dependencies
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (for map integration)
- `core:data` → `core:domain`
- `core:rules` → `core:domain`
- `ai/*` → `core:domain`
- `sync/*` → `core:domain`, `core:data`

### Forbidden Dependencies
- `core:domain` → any other module (pure Kotlin)
- `core:rules` → any Android dependencies
- `feature/*` → other `feature/*` modules (except encounter→map)
- Circular dependencies between any modules

---

## File Naming Conventions

### Kotlin Files
- **Entities**: `Creature.kt`, `Campaign.kt` (singular, PascalCase)
- **Use Cases**: `ProcessPlayerAction.kt` (verb phrase)
- **ViewModels**: `EncounterViewModel.kt` (suffix with ViewModel)
- **Repositories**: `EventRepository.kt` (interface), `EventRepositoryImpl.kt` (implementation)
- **Screens**: `TacticalMapScreen.kt` (suffix with Screen)
- **DAOs**: `EventDao.kt` (suffix with Dao)

### Packages
- Use lowercase with dots: `dev.questweaver.feature.map.ui`
- Group by feature/layer, not by type
- Keep package depth reasonable (3-4 levels max)

---

## Testing Structure

Each module should have corresponding test directories:

```
module/
├── src/
│   ├── main/kotlin/...
│   └── test/kotlin/...          # Unit tests
│       └── dev/questweaver/...
│           └── ModuleTest.kt
```

### Test Organization
- Mirror main source structure in test directory
- Use `Test` suffix for test classes
- Group related tests in nested contexts (kotest)
- Keep test fixtures in `common/testing` module (when created)

---

## Resource Organization

### app/src/main/res/
```
res/
├── values/
│   ├── strings.xml              # UI strings
│   ├── themes.xml               # Material3 theme
│   └── colors.xml               # Color palette
├── drawable/                    # Vector drawables
└── mipmap/                      # App icons
```

### Assets
```
app/src/main/assets/
└── models/
    ├── intent.onnx              # Intent classification model
    └── tokenizer.json           # Tokenizer config
```

---

## Build Files

### Root Level
- `build.gradle.kts` - Root build configuration
- `settings.gradle.kts` - Module includes and repositories
- `gradle/libs.versions.toml` - Version catalog
- `gradle.properties` - Gradle properties and JVM args

### Module Level
- Each module has `build.gradle.kts`
- Library modules use `com.android.library` plugin
- App module uses `com.android.application` plugin
- All use `kotlin-android` plugin

---

## Key Architectural Patterns

### MVI (Model-View-Intent)
```kotlin
// State: immutable data class
data class EncounterUiState(...)

// Intent: user actions
sealed interface EncounterIntent { ... }

// ViewModel: unidirectional flow
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) { ... }
}
```

### Event Sourcing
```kotlin
// All state mutations produce events
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

// Events are immutable and logged
data class AttackResolved(...) : GameEvent

// State is derived from event replay
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

---

## Navigation Structure (Planned)

```
Home Screen
├── Continue Campaign
├── New Campaign
│   └── Character Import
│       └── Scene/Dialogue
│           └── Combat/Map
│               └── Encounter Screen
├── Settings
└── Import/Export
```

Each screen is a Composable in its respective feature module, with navigation handled in the app module.
