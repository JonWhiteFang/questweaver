# Design Document: Project Setup & Build Configuration

## Overview

This design establishes the foundational build infrastructure for QuestWeaver using Gradle 8.5+ with Kotlin DSL, multi-module architecture, and optimized build performance. The system provides centralized dependency management through version catalogs, automated code quality checks, and CI/CD integration to support efficient development across a 16-week timeline to v1.0 release.

## Architecture

### Module Structure

```
questweaver/
├── app/                          # Android application module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       └── test/kotlin/
├── core/
│   ├── domain/                   # Pure Kotlin business logic
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   ├── data/                     # Data layer & repositories
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   └── rules/                    # Deterministic rules engine
│       ├── build.gradle.kts
│       └── src/main/kotlin/
├── feature/
│   ├── map/                      # Tactical map UI
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   ├── encounter/                # Combat & turn management
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   └── character/                # Character sheets & party
│       ├── build.gradle.kts
│       └── src/main/kotlin/
├── ai/
│   ├── ondevice/                 # ONNX models & inference
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   └── gateway/                  # Remote AI API client
│       ├── build.gradle.kts
│       └── src/main/kotlin/
├── sync/
│   └── firebase/                 # Cloud sync & backup
│       ├── build.gradle.kts
│       └── src/main/kotlin/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Module includes
├── gradle.properties             # Gradle properties
└── gradle/
    └── libs.versions.toml        # Version catalog
```

### Dependency Graph

```
app
├── core:domain
├── core:data
├── core:rules
├── feature:map
├── feature:encounter
├── feature:character
├── ai:ondevice
├── ai:gateway
└── sync:firebase

feature:map → core:domain
feature:encounter → core:domain, core:rules, feature:map
feature:character → core:domain

core:data → core:domain
core:rules → core:domain

ai:ondevice → core:domain
ai:gateway → core:domain

sync:firebase → core:domain, core:data
```

## Components and Interfaces

### 1. Root Build Configuration (`build.gradle.kts`)

**Purpose:** Define project-wide plugins, repositories, and common configurations

**Key Elements:**
- Plugin management for Android, Kotlin, and code quality tools
- Common repository declarations (Google, Maven Central)
- Subproject configuration for shared settings
- Clean task for removing build artifacts

**Example Structure:**
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

### 2. Settings Configuration (`settings.gradle.kts`)

**Purpose:** Define module includes and plugin management

**Key Elements:**
- Plugin management with version catalog references
- Module includes for all subprojects
- Dependency resolution management
- Build cache configuration

**Example Structure:**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuestWeaver"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:rules")
include(":feature:map")
include(":feature:encounter")
include(":feature:character")
include(":ai:ondevice")
include(":ai:gateway")
include(":sync:firebase")
```

### 3. Version Catalog (`gradle/libs.versions.toml`)

**Purpose:** Centralize all dependency versions and provide type-safe accessors

**Structure:**
```toml
[versions]
kotlin = "1.9.24"
agp = "8.5.0"
compose-bom = "2024.06.00"
room = "2.6.1"
koin = "3.5.6"
retrofit = "2.11.0"
okhttp = "4.12.0"
onnx = "1.16.3"
kotest = "5.9.1"
mockk = "1.13.10"
detekt = "1.23.6"

[libraries]
# Kotlin
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.8.1" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version = "1.8.1" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version = "1.6.3" }

# Compose
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }

# Networking
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { module = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter", version = "1.0.0" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# AI/ML
onnx-runtime = { module = "com.microsoft.onnxruntime:onnxruntime-android", version.ref = "onnx" }

# Testing
kotest-runner = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-property = { module = "io.kotest:kotest-property", version.ref = "kotest" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }

[bundles]
compose = ["compose-ui", "compose-material3", "compose-ui-tooling-preview"]
room = ["room-runtime", "room-ktx"]
koin = ["koin-core", "koin-android"]
retrofit = ["retrofit", "retrofit-kotlinx-serialization", "okhttp", "okhttp-logging"]
kotest = ["kotest-runner", "kotest-assertions", "kotest-property"]

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### 4. Gradle Properties (`gradle.properties`)

**Purpose:** Configure Gradle daemon and build performance settings

**Key Settings:**
```properties
# Gradle Daemon
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
org.gradle.daemon=true
org.gradle.daemon.idletimeout=10800000

# Build Performance
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.workers.max=14

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true

# Android
android.useAndroidX=true
android.enableJetifier=false
android.nonTransitiveRClass=true
```

### 5. App Module Build Configuration

**Purpose:** Configure the Android application module with dependencies and build variants

**Key Elements:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.questweaver"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "dev.questweaver"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:rules"))
    implementation(project(":feature:map"))
    implementation(project(":feature:encounter"))
    implementation(project(":feature:character"))
    implementation(project(":ai:ondevice"))
    implementation(project(":ai:gateway"))
    implementation(project(":sync:firebase"))
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    
    // Koin
    implementation(libs.bundles.koin)
    
    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
```

### 6. Library Module Build Configuration Template

**Purpose:** Provide consistent configuration for library modules

**Key Elements:**
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.questweaver.MODULE_NAME"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Module-specific dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
```

### 7. Pure Kotlin Module Build Configuration

**Purpose:** Configure modules without Android dependencies (core:domain, core:rules)

**Key Elements:**
```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## Data Models

### Build Configuration Model

```kotlin
// Conceptual model - not actual code
data class BuildConfiguration(
    val gradleVersion: String = "8.5",
    val kotlinVersion: String = "1.9.24",
    val jvmTarget: String = "17",
    val compileSdk: Int = 34,
    val minSdk: Int = 26,
    val targetSdk: Int = 34,
    val buildCacheEnabled: Boolean = true,
    val parallelExecutionEnabled: Boolean = true,
    val maxWorkers: Int = 14,
    val daemonMaxHeap: String = "4g"
)

data class ModuleConfiguration(
    val name: String,
    val type: ModuleType,
    val dependencies: List<String>,
    val hasAndroidDependencies: Boolean
)

enum class ModuleType {
    APPLICATION,
    ANDROID_LIBRARY,
    KOTLIN_LIBRARY
}
```

## Error Handling

### Build Failures

**Compilation Errors:**
- Gradle will report compilation errors with file location and line number
- Kotlin compiler provides detailed error messages
- Build fails immediately on first error in non-parallel mode

**Dependency Resolution Errors:**
- Version conflicts reported with dependency tree
- Missing dependencies fail with clear error message
- Circular dependencies detected and reported

**Configuration Errors:**
- Invalid plugin configurations fail during configuration phase
- Missing required properties reported with suggestions
- Syntax errors in build scripts reported with line numbers

### Code Quality Violations

**Lint Errors:**
- Critical and error-level violations fail the build
- Warning-level violations reported but don't fail build
- HTML reports generated in `build/reports/lint/`

**Detekt Violations:**
- Configurable threshold for failing build
- Reports generated in `build/reports/detekt/`
- Can be configured to fail on specific rule violations

## Testing Strategy

### Build Configuration Testing

**Verification Approach:**
1. Manual verification of successful project creation
2. Test builds with clean, build, and rebuild tasks
3. Verify incremental compilation works correctly
4. Test parallel execution with multiple modules
5. Verify cache effectiveness with repeated builds

**Performance Testing:**
1. Measure clean build time (baseline)
2. Measure incremental build time (single file change)
3. Measure build time with cache (after clean)
4. Verify parallel execution reduces build time
5. Monitor memory usage during builds

### Module Dependency Testing

**Verification Approach:**
1. Attempt to create invalid dependencies (should fail)
2. Verify feature modules cannot depend on each other
3. Verify core:domain has no Android dependencies
4. Test circular dependency detection
5. Verify dependency graph matches design

### CI/CD Testing

**Verification Approach:**
1. Test pipeline execution on push
2. Test pipeline execution on pull request
3. Verify test reports are uploaded on failure
4. Verify cache is used between runs
5. Test pipeline with intentional test failures

## Implementation Notes

### Module Creation Order

1. Create root project structure
2. Create `gradle/libs.versions.toml`
3. Create `settings.gradle.kts` with module includes
4. Create root `build.gradle.kts`
5. Create `gradle.properties`
6. Create `core:domain` (pure Kotlin, no dependencies)
7. Create `core:rules` (depends on core:domain)
8. Create `core:data` (depends on core:domain)
9. Create feature modules (depend on core modules)
10. Create ai modules (depend on core:domain)
11. Create sync modules (depend on core modules)
12. Create app module (depends on all modules)

### Build Performance Optimization

**Incremental Compilation:**
- Kotlin incremental compilation enabled by default
- Classpath snapshot improves incremental build accuracy
- Annotation processors configured for incremental processing

**Parallel Execution:**
- Set `org.gradle.workers.max` to CPU cores - 2
- Ensure modules have minimal inter-dependencies
- Use `--parallel` flag for command-line builds

**Build Cache:**
- Local build cache enabled by default
- Remote build cache can be configured for team sharing
- Cache directory: `~/.gradle/caches/build-cache-1`

**Configuration Cache:**
- Enabled with `org.gradle.configuration-cache=true`
- Reduces configuration time by 50-90%
- May require plugin compatibility updates

### Code Quality Configuration

**Android Lint:**
- Custom lint rules in `lint.xml`
- Baseline file to track existing violations
- HTML reports for easy review

**Detekt:**
- Configuration in `detekt.yml`
- Custom rule sets for project-specific patterns
- Integration with IDE for real-time feedback

### CI/CD Pipeline

**GitHub Actions Workflow:**
```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build --parallel --build-cache
    
    - name: Run tests
      run: ./gradlew test --parallel
    
    - name: Upload test reports
      if: failure()
      uses: actions/upload-artifact@v4
      with:
        name: test-reports
        path: '**/build/reports/tests/'
    
    - name: Run lint
      run: ./gradlew lintDebug
    
    - name: Run Detekt
      run: ./gradlew detekt
```

## Documentation Requirements

### README.md

**Required Sections:**
1. Project Overview
2. Prerequisites (Android Studio, JDK, Android SDK)
3. Setup Instructions
4. Module Structure
5. Common Commands
6. Troubleshooting
7. Contributing Guidelines

### Module READMEs

Each module should have a README explaining:
- Module purpose and responsibilities
- Key classes and interfaces
- Dependencies
- Testing approach

## Migration Path

This is a new project, so no migration is required. However, the design supports future migrations:

1. **Gradle Version Updates:** Version catalog makes updates straightforward
2. **Dependency Updates:** Centralized versions simplify updates
3. **Module Refactoring:** Clear boundaries enable module splitting/merging
4. **Build System Changes:** Kotlin DSL is future-proof and maintainable
