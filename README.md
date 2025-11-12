# QuestWeaver

**An offline-first Android app for solo D&D-style RPG gameplay with AI Game Master capabilities.**

QuestWeaver is a single-player tactical RPG where one human player controls their PC while AI manages NPCs, party members, narration, and rules adjudication. Features grid-based tactical combat with deterministic event sourcing and full campaign replay.

## Project Overview

- **Platform**: Android (API 26+, targetSdk 34)
- **Language**: Kotlin 100% (1.9.24, JVM 17)
- **Architecture**: Clean Architecture + MVI + Event Sourcing
- **Build System**: Gradle 9.2.0 with Kotlin DSL
- **UI Framework**: Jetpack Compose + Material3
- **Database**: Room 2.6.1 + SQLCipher 4.5.5 (encrypted)

## Prerequisites

Before you begin, ensure you have the following installed:

### Required

- **Android Studio**: Giraffe (2022.3.1) or newer
  - Download from: https://developer.android.com/studio
- **JDK 17**: Configured via Gradle toolchain
  - Android Studio includes JDK 17, or install separately
- **Android SDK**: API Level 34 (Android 14)
  - Install via Android Studio SDK Manager
  - Required components: Android SDK Platform 34, Android SDK Build-Tools

### Optional

- **Gradle**: 8.5+ (project includes wrapper, but system-wide install is optional)
- **Git**: For version control

### System Requirements

- **OS**: Windows 10+, macOS 10.14+, or Linux
- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 10GB for Android Studio + SDK + project

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd questweaver
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the `questweaver` directory
4. Click **OK**
5. Wait for Gradle sync to complete (may take several minutes on first run)

### 3. Configure Android SDK

If prompted, install missing SDK components:
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android Emulator (if testing on emulator)

### 4. Build the Project

```bash
# Using Gradle wrapper (recommended)
./gradlew build --parallel

# Or using system Gradle
gradle build --parallel
```

### 5. Run the App

**Option A: Android Studio**
1. Select a device/emulator from the device dropdown
2. Click the **Run** button (▶) or press `Shift+F10`

**Option B: Command Line**
```bash
./gradlew installDebug
```

## Module Structure

QuestWeaver uses a multi-module architecture with strict dependency rules:

```
app/                    # Android application module
├── core/
│   ├── domain/        # Pure Kotlin: entities, use cases, events (NO Android deps)
│   ├── data/          # Room + SQLCipher, repository implementations
│   └── rules/         # Deterministic D&D 5e rules engine (NO Android, NO AI)
├── feature/
│   ├── map/           # Tactical map UI with Compose Canvas
│   ├── encounter/     # Combat turn management and UI
│   └── character/     # Character sheets and party management
├── ai/
│   ├── ondevice/      # ONNX Runtime for on-device intent parsing
│   └── gateway/       # Retrofit client for remote LLM (optional)
└── sync/
    └── firebase/      # Cloud backup via WorkManager (optional)
```

### Module Responsibilities

- **app**: DI assembly (Koin), navigation, Material3 theme
- **core:domain**: Business logic, entities, use cases, event definitions (pure Kotlin)
- **core:data**: Database layer with Room + SQLCipher, repository implementations
- **core:rules**: Deterministic D&D 5e SRD rules engine with seeded RNG
- **feature:map**: Grid rendering, pathfinding, geometry calculations
- **feature:encounter**: Turn engine, initiative tracking, combat UI
- **feature:character**: Character sheet display, party management
- **ai:ondevice**: ONNX Runtime integration for intent classification
- **ai:gateway**: Optional remote LLM API client with fallback
- **sync:firebase**: Optional cloud backup and sync

### Dependency Rules

**Allowed:**
- `app` → all modules
- `feature/*` → `core:domain`, `core:rules`
- `feature/encounter` → `feature:map` (only exception)
- `core:data` → `core:domain`
- `ai/*`, `sync/*` → `core:domain`

**Forbidden:**
- `core:domain` → any other module (pure Kotlin only)
- `core:rules` → Android dependencies or AI modules
- `feature/*` → other `feature/*` (except encounter→map)
- Circular dependencies

## Common Commands

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Build with parallel execution and cache
./gradlew build --parallel --build-cache
```

### Testing Commands

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:rules:test
./gradlew :core:domain:test

# Run tests with coverage report
./gradlew test koverHtmlReport

# View coverage report
# Open: build/reports/kover/html/index.html
```

### Code Quality Commands

```bash
# Run Android Lint
./gradlew lintDebug

# Run Detekt (Kotlin static analysis)
./gradlew detekt

# View lint report
# Open: app/build/reports/lint/lint-results-debug.html

# View Detekt report
# Open: build/reports/detekt/detekt.html
```

### Gradle Commands

```bash
# List all tasks
./gradlew tasks

# List project dependencies
./gradlew :app:dependencies

# Refresh dependencies
./gradlew --refresh-dependencies

# Clean all build artifacts
./gradlew clean
```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Follow the coding standards in `.kiro/steering/coding-standards.md`
- Ensure module dependency rules are respected
- Write tests for new functionality

### 3. Run Tests and Checks

```bash
./gradlew test lint detekt
```

### 4. Commit and Push

```bash
git add .
git commit -m "feat: your feature description"
git push origin feature/your-feature-name
```

### 5. Create Pull Request

- Ensure CI/CD pipeline passes
- Request code review

## Architecture Patterns

### MVI (Model-View-Intent)

ViewModels use MVI pattern with unidirectional data flow:

```kotlin
data class EncounterUiState(val round: Int, val activeCreatureId: Long?)

sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    fun handle(intent: EncounterIntent) { /* process */ }
}
```

### Event Sourcing

All state mutations produce immutable events:

```kotlin
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
```

### Repository Pattern

Interfaces in `core:domain`, implementations in `core:data`:

```kotlin
interface EventRepository {
    suspend fun append(event: GameEvent)
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}
```

## Testing

### Framework

- **kotest 5.9.1**: Primary testing framework
- **MockK 1.13.10**: Mocking library

### Coverage Targets

- `core/rules`: 90%+
- `core/domain`: 85%+
- `core/data`: 80%+
- `feature/*`: 60%+

### Running Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :core:rules:test

# With coverage
./gradlew test koverHtmlReport
```

## Troubleshooting

### Gradle Sync Fails

**Problem**: Gradle sync fails with dependency resolution errors

**Solutions**:
1. Check internet connection (dependencies download from Maven Central)
2. Invalidate caches: **File → Invalidate Caches → Invalidate and Restart**
3. Delete `.gradle` folder and sync again
4. Run `./gradlew --refresh-dependencies`

### Build Fails with "Unsupported class file major version"

**Problem**: JDK version mismatch

**Solution**:
1. Ensure JDK 17 is installed
2. In Android Studio: **File → Project Structure → SDK Location**
3. Set **Gradle JDK** to JDK 17
4. Sync Gradle

### App Crashes on Launch

**Problem**: App crashes immediately after installation

**Solutions**:
1. Check Logcat for stack trace
2. Verify minimum SDK version (API 26+)
3. Clean and rebuild: `./gradlew clean build`
4. Uninstall app from device and reinstall

### Tests Fail with "No tests found"

**Problem**: kotest tests not discovered

**Solution**:
1. Ensure `useJUnitPlatform()` is configured in `build.gradle.kts`
2. Verify test class extends `FunSpec` or other kotest spec
3. Invalidate caches and restart Android Studio

### Lint Errors Block Build

**Problem**: Lint errors prevent successful build

**Solutions**:
1. Review lint report: `app/build/reports/lint/lint-results-debug.html`
2. Fix critical and error-level violations
3. Temporarily disable specific checks in `lint.xml` (not recommended)
4. Update baseline: `./gradlew lintDebug --update-baseline`

### Out of Memory During Build

**Problem**: Gradle daemon runs out of memory

**Solution**:
1. Increase heap size in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC
   ```
2. Reduce parallel workers:
   ```properties
   org.gradle.workers.max=8
   ```
3. Restart Android Studio

### Slow Build Times

**Problem**: Builds take too long

**Solutions**:
1. Enable build cache: `./gradlew build --build-cache`
2. Enable parallel execution (already configured in `gradle.properties`)
3. Use incremental builds (avoid `clean` unless necessary)
4. Close unnecessary applications to free RAM
5. Consider upgrading hardware (SSD, more RAM)

### Module Dependency Errors

**Problem**: "Module X cannot depend on module Y"

**Solution**:
1. Review module dependency rules in this README
2. Ensure `core:domain` has no Android dependencies
3. Ensure feature modules don't depend on other features (except encounter→map)
4. Move shared logic to `core:domain`

### SQLCipher Database Errors

**Problem**: Database fails to open or decrypt

**Solutions**:
1. Verify SQLCipher dependency in `gradle/libs.versions.toml`
2. Check passphrase configuration in database initialization
3. Clear app data and reinstall (development only)
4. Ensure Android Keystore is properly configured

### ONNX Model Loading Fails

**Problem**: Intent classifier throws exception on initialization

**Solutions**:
1. Verify `.onnx` model file exists in `app/src/main/assets/models/`
2. Check model file is not corrupted
3. Ensure ONNX Runtime dependency is included
4. Warm up model on background thread, not main thread

## Performance Targets

- **Map render**: ≤4ms per frame (60fps)
- **AI tactical decision**: ≤300ms on-device
- **LLM narration**: 4s soft timeout, 8s hard timeout
- **Database queries**: <50ms typical

## Security

- **Local Data**: Encrypted with SQLCipher using Android Keystore-wrapped keys
- **Network**: TLS 1.2+ for all API calls
- **ProGuard**: Obfuscation enabled in release builds
- **Permissions**: Minimal (no location, camera, etc.)

## Contributing

1. Follow coding standards in `.kiro/steering/coding-standards.md`
2. Respect module dependency rules
3. Write tests for new functionality
4. Ensure CI/CD pipeline passes
5. Update documentation as needed

## Additional Documentation

- **Architecture Design**: `questweaver_architecture_design.md`
- **Agent Instructions**: `AGENTS.md`
- **Steering Rules**: `.kiro/steering/*.md`
- **Gradle Setup**: `.kiro/GRADLE_SETUP.md`

## License

[Add license information here]

## Contact

[Add contact information here]

---

**Last Updated**: 2025-11-10
