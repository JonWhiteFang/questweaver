---
inclusion: always
---

# Build & Test Guidelines

## Build Commands

```bash
gradle assembleDebug          # Build debug APK
gradle installDebug            # Install on device
gradle test                    # Run all tests
gradle :core:rules:test        # Test specific module
gradle koverHtmlReport         # Generate coverage report
gradle clean build             # Clean build
```

**Note**: This project uses Gradle 9.2.0. You can use either the system-wide installation or the wrapper with `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows).

## Gradle Configuration Rules

- **Versions**: Centralize in `gradle/libs.versions.toml` (single source of truth)
- **Build Files**: Use Kotlin DSL (`.gradle.kts`)
- **Module Dependencies**: Feature modules CANNOT depend on other feature modules (except `feature:encounter` → `feature:map`)
- **Circular Dependencies**: Forbidden

```kotlin
// ✅ Correct: Feature depends on domain
implementation(project(":core:domain"))

// ❌ Wrong: Feature depends on another feature
implementation(project(":feature:character"))
```

## Testing Framework: kotest + MockK

### Test Structure Pattern

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

### Test Naming
- **Class**: `<ClassUnderTest>Test` (e.g., `DiceRollerTest`)
- **Method**: Descriptive sentences explaining behavior
- Use `context()` to group related tests

### Test Pyramid Distribution
- **70% Unit Tests**: Fast, isolated, deterministic
- **20% Integration Tests**: Module boundaries, database, repositories
- **10% UI Tests**: Critical user flows only

## Critical Testing Rules

### 1. Deterministic Tests Required
```kotlin
// ✅ Use seeded RNG for reproducibility
val roller = DiceRoller(seed = 42)
val result = roller.d20()

// ❌ Never use unseeded random
val result = Random.nextInt(1, 21) // Non-deterministic
```

### 2. Property-Based Testing for Rules Engine
```kotlin
test("d20 always returns 1-20 for any seed") {
    checkAll(Arb.long()) { seed ->
        val roller = DiceRoller(seed)
        roller.d20().value shouldBeInRange 1..20
    }
}
```

### 3. Event Sourcing Verification
```kotlin
test("action generates correct events") {
    val result = useCase(action)
    
    result shouldBeInstanceOf ActionResult.Success::class
    val events = (result as ActionResult.Success).events
    events.first() shouldBeInstanceOf GameEvent.AttackResolved::class
}
```

### 4. Repository Tests Use In-Memory Database
```kotlin
lateinit var database: AppDatabase

beforeTest {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()
}

afterTest {
    database.close()
}
```

### 5. Mock External Dependencies with MockK
```kotlin
val rulesEngine = mockk<RulesEngine>()
val eventRepo = mockk<EventRepository>()

every { rulesEngine.resolveAttack(any(), any()) } returns outcome
coEvery { eventRepo.append(any()) } just Runs

// Verify interactions
coVerify { eventRepo.append(any<GameEvent.AttackResolved>()) }
```

## Test Fixtures & Builders

Use test fixtures for consistent test data:

```kotlin
// Fixtures.kt
object Fixtures {
    fun mockCreature(
        id: Long = 1,
        ac: Int = 15,
        hpCurrent: Int = 20,
        hpMax: Int = 20
    ) = Creature(id, "Test", ac, hpCurrent, hpMax, 30, Abilities())
}

// Builder pattern for complex objects
class CreatureBuilder {
    private var ac: Int = 15
    fun withAC(ac: Int) = apply { this.ac = ac }
    fun build() = Creature(1, "Test", ac, 20, 20, 30, Abilities())
}
```

## Coverage Requirements

When writing tests, target these coverage levels:

- `core/rules`: **90%+** (deterministic, critical)
- `core/domain`: **85%+** (use cases, entities)
- `core/data`: **80%+** (repositories)
- `feature/*`: **60%+** (focus on logic, not UI rendering)
- `ai/*`: **70%+** (behavior validation)

**Check Coverage**: `./gradlew koverHtmlReport` → `build/reports/kover/html/index.html`

## Performance Testing

### Map Rendering Budget
Target: ≤4ms per frame (60fps)

```kotlin
@Test
fun mapRenderingPerformance() {
    val duration = measureTimeMillis {
        repeat(100) { renderMap(state) }
    }
    (duration / 100) shouldBeLessThan 4
}
```

### AI Decision Budget
Target: ≤300ms on-device

```kotlin
test("tactical decision completes within budget") {
    val duration = measureTimeMillis {
        agent.decide(state, creature)
    }
    duration shouldBeLessThan 300
}
```

## Common Testing Patterns

### Testing Sealed Types (Exhaustive When)
```kotlin
test("handles all event types") {
    val events = listOf(
        GameEvent.EncounterStarted(...),
        GameEvent.MoveCommitted(...)
    )
    
    events.forEach { event ->
        when (event) {
            is GameEvent.EncounterStarted -> handleStart(event)
            is GameEvent.MoveCommitted -> handleMove(event)
            // Compiler enforces exhaustiveness
        }
    }
}
```

### Testing MVI ViewModels
```kotlin
test("intent updates state correctly") {
    val viewModel = EncounterViewModel(...)
    
    viewModel.handle(EncounterIntent.MoveTo(GridPos(5, 5)))
    
    viewModel.state.value.activeCreature?.position shouldBe GridPos(5, 5)
}
```

### Testing Use Cases
```kotlin
test("use case returns success with events") {
    val useCase = ProcessPlayerAction(rulesEngine, eventRepo)
    val action = NLAction.Attack(attackerId = 1, targetId = 2)
    
    val result = useCase(action)
    
    result shouldBeInstanceOf ActionResult.Success::class
}
```

## Debugging Tests

```kotlin
// Temporarily disable test
test("failing test").config(enabled = false) { }

// Or use xtest
xtest("skipped test") { }

// Enable debug logging
beforeSpec {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
}
```

## Best Practices Summary

1. **Always use seeded RNG** for deterministic tests
2. **Test behavior, not implementation** - focus on what, not how
3. **One assertion per test** - keep tests focused
4. **Mock external dependencies** - database, network, AI services
5. **Clean up resources** - close databases in `afterTest`
6. **Use property-based tests** for rules engine and deterministic components
7. **Verify event generation** for all state mutations
8. **Test edge cases** - null, empty, boundary values
9. **Keep tests fast** - unit tests in milliseconds
10. **Use descriptive test names** - explain the scenario being tested

## Koin Module Verification

Always verify DI modules are correctly wired:

```kotlin
class ModuleVerificationTest : KoinTest {
    @Test
    fun `verify all modules`() {
        koinApplication {
            modules(domainModule, dataModule, rulesModule)
        }.verify()
    }
}
```

This catches missing dependencies at test time, not runtime.
