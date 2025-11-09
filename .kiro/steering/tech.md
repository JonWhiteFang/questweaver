# QuestWeaver Tech Stack

## Build System

- **Build Tool**: Gradle 8.5+ with Kotlin DSL
- **Version Management**: Centralized in `gradle/libs.versions.toml`
- **Android Gradle Plugin**: 8.5.0
- **Kotlin**: 1.9.24 with JVM toolchain 17

## Core Technologies

### Language & Runtime
- **Kotlin**: 100% Kotlin codebase
- **Coroutines**: 1.8.1 for async operations
- **Flow**: Reactive streams for state management
- **kotlinx-serialization**: 1.6.3 for JSON serialization

### UI Framework
- **Jetpack Compose**: BOM 2024.06.00
- **Material3**: Design system
- **Compose Canvas**: For tactical map rendering
- **Activity Compose**: 1.9.0

### Architecture
- **Pattern**: Clean Architecture + MVI (Model-View-Intent)
- **DI**: Koin 3.5.6 (pure Kotlin DSL)
- **State Management**: Unidirectional data flow with StateFlow

### Persistence
- **Database**: Room 2.6.1 with SQLCipher 4.5.5
- **Encryption**: SQLCipher for local data encryption
- **SQLite KTX**: 2.4.0 for enhanced SQLite support
- **Event Sourcing**: All state mutations logged as events

### Networking
- **HTTP Client**: Retrofit 2.11.0 + OkHttp 4.12.0
- **Logging**: OkHttp logging interceptor
- **Serialization**: kotlinx-serialization converter for Retrofit

### Background Work
- **WorkManager**: 2.9.0 for cloud sync and background tasks

### AI/ML
- **On-Device**: ONNX Runtime 1.16.3 for intent parsing
- **Remote**: Optional gateway via Retrofit (Firebase Functions or Ktor)

### Testing
- **Unit Tests**: kotest 5.9.1 + MockK 1.13.10
- **Screenshot Tests**: Paparazzi 1.3.3 (planned)
- **Property-Based**: kotest property testing

## Common Commands

### Build & Run
```bash
# Sync Gradle dependencies
./gradlew --refresh-dependencies

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run app on device/emulator
# Use Android Studio Run button or:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run tests for specific module
./gradlew :core:rules:test

# Run tests with coverage (when configured)
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lint
```

### Code Quality
```bash
# Run lint on all modules
./gradlew lintDebug

# Generate lint report
./gradlew lintDebug --continue

# Check for dependency updates
./gradlew dependencyUpdates
```

### Database
```bash
# Export Room schema (configured in build.gradle.kts)
# Schemas exported to: core/data/schemas/

# View database on device (requires root or debuggable app)
adb shell
run-as dev.questweaver
cd databases
sqlite3 questweaver.db
```

### Clean & Rebuild
```bash
# Clean build artifacts
./gradlew clean

# Clean + rebuild
./gradlew clean build

# Clean specific module
./gradlew :app:clean
```

## Module Dependencies

### Dependency Graph
```
app
├── core:domain (entities, use cases, events)
├── core:data (repositories, Room, DAOs)
├── core:rules (deterministic rules engine)
├── feature:map (map UI, pathfinding, geometry)
├── feature:encounter (combat, turn engine)
├── feature:character (PC sheet, party management)
├── ai:ondevice (ONNX models, wrappers)
├── ai:gateway (Retrofit API, DTOs)
└── sync:firebase (cloud backup, WorkManager)
```

### Module Isolation Rules
- Feature modules do NOT depend on other feature modules
- All features depend on `core:domain`
- Data layer accessed via repositories in `core:data`
- Rules engine is pure Kotlin with no Android dependencies

## Key Libraries

### Compose
- `androidx.compose:compose-bom:2024.06.00`
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`
- `androidx.activity:activity-compose:1.9.0`

### Dependency Injection
- `io.insert-koin:koin-core:3.5.6`
- `io.insert-koin:koin-android:3.5.6`

### Networking
- `com.squareup.retrofit2:retrofit:2.11.0`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`

### Database
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-ktx:2.6.1`
- `net.zetetic:android-database-sqlcipher:4.5.5`

### AI/ML
- `com.microsoft.onnxruntime:onnxruntime-android:1.16.3`

## Build Configuration

### Compile & Target SDK
- **compileSdk**: 34
- **minSdk**: 26 (Android 8.0+)
- **targetSdk**: 34

### Build Types
- **debug**: No minification, debuggable
- **release**: ProGuard enabled, optimized

### ProGuard
- Rules in `app/proguard-rules.pro`
- Keeps kotlinx-serialization annotations
- Optimized for release builds

## Development Setup

1. **Android Studio**: Giraffe (2022.3.1) or newer
2. **JDK**: 17 (configured via Gradle toolchain)
3. **Android SDK**: API 34 installed
4. **Gradle**: Wrapper included (8.5+)

### First-Time Setup
```bash
# Clone repository
git clone <repo-url>
cd questweaver

# Open in Android Studio
# File > Open > select questweaver folder

# Gradle sync will download dependencies automatically

# Run on emulator or device
# Click Run button or Shift+F10
```

## Performance Targets

- **Map Render**: ≤4ms per frame (60fps)
- **AI Tactical Decision**: ≤300ms on-device
- **LLM Narration**: 4s soft timeout (remote)
- **Database Queries**: <50ms for typical operations

## Security

- **Encryption**: SQLCipher with Android Keystore-wrapped keys
- **Network**: TLS 1.2+ for all API calls
- **ProGuard**: Obfuscation enabled in release builds
- **Permissions**: Minimal (no location, camera, etc.)
