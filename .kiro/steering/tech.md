---
inclusion: always
---

# QuestWeaver Tech Stack

## Core Stack

**Language**: Kotlin 100% (1.9.24, JVM 17)  
**Build**: Gradle 9.2.0 with Kotlin DSL, versions in `gradle/libs.versions.toml`  
**UI**: Jetpack Compose (BOM 2024.06.00) + Material3  
**Architecture**: Clean Architecture + MVI + Event Sourcing  
**DI**: Koin 3.5.6 (pure Kotlin DSL)  
**Async**: Coroutines 1.8.1 + Flow for reactive state  
**Database**: Room 2.6.1 + SQLCipher 4.5.5 (encrypted)  
**Network**: Retrofit 2.11.0 + OkHttp 4.12.0  
**Serialization**: kotlinx-serialization 1.6.3  
**Testing**: kotest 5.9.1 + MockK 1.13.10

## Module Structure

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
│   ├── ondevice/      # ONNX Runtime 1.16.3 for intent parsing
│   └── gateway/       # Retrofit client for remote LLM (optional)
└── sync/
    └── firebase/      # Cloud backup via WorkManager 2.9.0
```

## Critical Dependency Rules

**ALLOWED:**
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (ONLY exception)
- `core:data` → `core:domain`
- `ai/*`, `sync/*` → `core:domain`

**FORBIDDEN:**
- `core:domain` → any other module (pure Kotlin only)
- `core:rules` → Android dependencies or AI
- `feature/*` → other `feature/*` (except encounter→map)
- Circular dependencies

## Key Patterns

### MVI State Management
```kotlin
// State: single immutable data class
data class EncounterUiState(val round: Int, val activeCreatureId: Long?)

// Intent: sealed interface for user actions
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: unidirectional flow with StateFlow
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    fun handle(intent: EncounterIntent) { /* process */ }
}
```

### Event Sourcing
- All state mutations produce immutable `GameEvent` instances
- Events logged to database for full replay capability
- Use sealed interfaces for event hierarchies
- State derived from event replay, never mutated directly

### Repository Pattern
- Interfaces in `core:domain`, implementations in `core:data`
- Return `Flow` for reactive queries, `suspend fun` for one-shot operations
- Use `sealed interface` for result types (Success/Failure/RequiresChoice)

## Code Style

- **Immutability**: Prefer `val` over `var`, `data class` for entities
- **Sealed Types**: Use for ADTs (actions, events, results)
- **Exhaustive When**: Always use exhaustive `when` for sealed types
- **Naming**: PascalCase for classes, camelCase for functions/properties
- **Packages**: Group by feature/layer, not by type (e.g., `dev.questweaver.feature.map.ui`)

## Common Commands

```bash
# Build & install
gradle assembleDebug
gradle installDebug

# Testing
gradle test                    # All tests
gradle :core:rules:test        # Specific module
gradle lint                    # Lint checks

# Clean
gradle clean
gradle clean build
```

**Note**: This project uses Gradle 9.2.0. You can use either the system-wide installation or the wrapper with `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows).

## Build Configuration

- **compileSdk**: 34
- **minSdk**: 26 (Android 8.0+)
- **targetSdk**: 34
- **ProGuard**: Enabled in release builds (`app/proguard-rules.pro`)

## Performance Targets

- Map render: ≤4ms per frame (60fps)
- AI tactical decision: ≤300ms on-device
- LLM narration: 4s soft timeout (remote)
- Database queries: <50ms typical

## Security Requirements

- **Encryption**: SQLCipher with Android Keystore-wrapped keys for all local data
- **Network**: TLS 1.2+ for all API calls
- **ProGuard**: Obfuscation enabled in release
- **Permissions**: Minimal (no location, camera, etc.)

## AI Integration

- **On-Device**: ONNX Runtime for intent parsing (models in `app/src/main/assets/models/`)
- **Remote**: Optional LLM gateway via Retrofit (4s soft timeout, 8s hard timeout)
- **Fallback**: Template-based narration when AI unavailable
- **Rule**: AI proposes, rules engine validates before commit

## Testing Strategy

- **Unit Tests**: kotest with descriptive test names, MockK for mocking
- **Property-Based**: kotest property testing for deterministic components
- **Integration**: Room in-memory database, mock network responses
- **Coverage Goals**: core/rules 90%+, domain 85%+, data 80%+, UI 60%+

## Development Setup

1. Android Studio Giraffe (2022.3.1) or newer
2. JDK 17 (configured via Gradle toolchain)
3. Android SDK API 34 installed
4. Open project, Gradle sync auto-downloads dependencies
5. Run with Android Studio Run button or `Shift+F10`

## Key Constraints

- `core:domain` and `core:rules` must be pure Kotlin (no Android dependencies)
- Rules engine must be 100% deterministic (seeded RNG only, no AI calls)
- All state mutations must produce `GameEvent` instances
- Feature modules cannot depend on other feature modules (except encounter→map)
- Use event sourcing for all gameplay state changes
