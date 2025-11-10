# Development Setup

## Prerequisites

- **Android Studio**: Giraffe (2022.3.1) or newer
- **JDK**: 17 (configured via Gradle toolchain)
- **Android SDK**: API 34 installed
- **Gradle**: 9.2.0 (system-wide) or use wrapper

## Initial Setup

1. Clone the repository
2. Open project in Android Studio
3. Gradle sync will auto-download dependencies
4. Run with Android Studio Run button or `Shift+F10`

## Build Commands

```bash
# Build & install
gradle assembleDebug
gradle installDebug

# Testing
gradle test                    # All tests
gradle :core:rules:test        # Specific module
gradle lint                    # Lint checks

# Clean build
gradle clean build
```

**Note**: You can use `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows) instead of `gradle` if preferred.

## Project Structure

```
questweaver/
├── app/                    # Android app & DI assembly
├── core/
│   ├── domain/            # Pure Kotlin: entities, use cases, events
│   ├── data/              # Room + SQLCipher, repositories
│   └── rules/             # Deterministic rules engine
├── feature/
│   ├── map/               # Compose Canvas, pathfinding
│   ├── encounter/         # Turn engine, combat UI
│   └── character/         # Character sheets
├── ai/
│   ├── ondevice/          # ONNX Runtime
│   └── gateway/           # Retrofit client
└── sync/
    └── firebase/          # Cloud backup
```

## Configuration Files

- **Version Catalog**: `gradle/libs.versions.toml` - Single source of truth for versions
- **Build Files**: Use Kotlin DSL (`.gradle.kts`)
- **Steering Rules**: `.kiro/steering/*.md` - AI agent guidance

## Running Tests

```bash
# All tests
gradle test

# Specific module
gradle :core:rules:test

# With coverage
gradle test koverHtmlReport
```

## Code Quality

```bash
# Lint
gradle lintDebug

# Detekt
gradle detekt
```

## Common Issues

### Gradle Sync Fails
- Ensure JDK 17 is configured
- Check `gradle/libs.versions.toml` for version conflicts
- Try `gradle clean build`

### Tests Fail
- Ensure seeded RNG is used (not `Random()`)
- Check module dependencies don't violate rules
- Verify Android dependencies aren't in `core:domain` or `core:rules`

---

**Last Updated**: 2025-11-10
