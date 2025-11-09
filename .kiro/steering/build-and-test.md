# QuestWeaver Build & Test Guidelines

## Build Configuration

### Gradle Setup
- Use Kotlin DSL for all build files
- Centralize versions in `gradle/libs.versions.toml`
- Enable build cache and configuration cache for faster builds
- Use parallel execution: `org.gradle.parallel=true`

### Build Performance
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true
```

### Module Dependencies
- Keep modules loosely coupled
- Use interfaces for cross-module communication
- Avoid circular dependencies
- Feature modules should not depend on each other

```kotlin
// Good: Feature depends on domain
implementation(project(":core:domain"))

// Bad: Feature depends on another feature
implementation(project(":feature:character")) // Avoid
```

## Testing Strategy

### Test Pyramid
- **70% Unit Tests**: Fast, isolated, deterministic
- **20% Integration Tests**: Module boundaries, database, AI agents
- **10% UI Tests**: Critical user flows only

### Unit Testing with kotest

#### Basic Structure
```kotlin
class DiceRollerTest : FunSpec({
    test("d20 returns value between 1 and 20") {
        val roller = DiceRoller(seed = 42)
        val result = roller.d20()
        
        result.value shouldBeInRange 1..20
    }
    
    test("advantage rolls twice and takes maximum") {
        val roller = DiceRoller(seed = 42)
        val result = roller.d20(Advantage.ADVANTAGE)
        
        result.rolls.size shouldBe 2
        result.value shouldBe result.rolls.max()
    }
})
```

#### Property-Based Testing
```kotlin
class DicePropertyTest : FunSpec({
    test("d20 always returns 1-20 for any seed") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val roll = roller.d20()
            roll.value shouldBeInRange 1..20
        }
    }
    
    test("advantage always >= normal roll") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val normal = roller.d20(Advantage.NONE)
            
            val roller2 = DiceRoller(seed)
            val advantage = roller2.d20(Advantage.ADVANTAGE)
            
            advantage.value shouldBeGreaterThanOrEqual normal.value
        }
    }
})
```

### Integration Testing

#### Repository Tests
```kotlin
class EventRepositoryTest : FunSpec({
    lateinit var database: AppDatabase
    lateinit var repository: EventRepository
    
    beforeTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = EventRepositoryImpl(database.eventDao())
    }
    
    afterTest {
        database.close()
    }
    
    test("events are retrieved in order") {
        val sessionId = 1L
        val events = listOf(
            GameEvent.EncounterStarted(sessionId, 100, 1, listOf(), 42),
            GameEvent.MoveCommitted(sessionId, 200, 1, listOf(), 5)
        )
        
        events.forEach { repository.append(it) }
        
        val retrieved = repository.forSession(sessionId)
        retrieved shouldBe events
    }
})
```

#### AI Agent Tests
```kotlin
class TacticalAgentTest : FunSpec({
    test("always selects legal actions") {
        checkAll(Arb.encounterState(), Arb.creature()) { state, creature ->
            val agent = TacticalAgent()
            val action = agent.decide(state, creature)
            
            val validation = rulesEngine.validate(action, state)
            validation.isLegal shouldBe true
        }
    }
    
    test("low HP creatures disengage") {
        val creature = mockCreature(hpCurrent = 5, hpMax = 20) // 25% HP
        val state = mockEncounterState(creature)
        
        val agent = TacticalAgent()
        val action = agent.decide(state, creature)
        
        action shouldBeInstanceOf ActionType.Disengage::class
    }
})
```

### Mocking with MockK

```kotlin
class ProcessPlayerActionTest : FunSpec({
    val rulesEngine = mockk<RulesEngine>()
    val eventRepo = mockk<EventRepository>()
    val useCase = ProcessPlayerAction(rulesEngine, eventRepo)
    
    test("successful attack generates events") {
        val action = NLAction.Attack(attackerId = 1, targetId = 2)
        val outcome = AttackOutcome(hit = true, damage = 10)
        
        every { rulesEngine.resolveAttack(any(), any()) } returns outcome
        coEvery { eventRepo.append(any()) } just Runs
        
        val result = useCase(action)
        
        result shouldBeInstanceOf ActionResult.Success::class
        coVerify { eventRepo.append(any<GameEvent.AttackResolved>()) }
    }
})
```

### Compose UI Testing with Paparazzi

```kotlin
class MapScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight"
    )
    
    @Test
    fun mapWithTokens() {
        paparazzi.snapshot {
            TacticalMap(
                state = MapState(
                    w = 10, h = 10, tileSize = 50f,
                    tokens = listOf(
                        Token(1, GridPos(2, 3), isEnemy = false, hpPct = 0.8f),
                        Token(2, GridPos(7, 5), isEnemy = true, hpPct = 0.6f)
                    )
                ),
                onTap = {}
            )
        }
    }
}
```

### Test Fixtures

```kotlin
// common/testing/src/main/kotlin/Fixtures.kt
object Fixtures {
    fun mockCreature(
        id: Long = 1,
        name: String = "Test Creature",
        ac: Int = 15,
        hpCurrent: Int = 20,
        hpMax: Int = 20,
        speed: Int = 30
    ) = Creature(
        id = id,
        name = name,
        ac = ac,
        hpCurrent = hpCurrent,
        hpMax = hpMax,
        speed = speed,
        abilities = Abilities(10, 10, 10, 10, 10, 10)
    )
    
    fun mockEncounterState(
        vararg creatures: Creature,
        round: Int = 1
    ) = EncounterState(
        id = 1,
        round = round,
        creatures = creatures.toList(),
        initiative = creatures.mapIndexed { idx, c -> 
            InitiativeEntry(idx, c.id, 10) 
        }
    )
}
```

### Arbitrary Generators for Property Tests

```kotlin
// common/testing/src/main/kotlin/Arbitraries.kt
object Arbitraries {
    fun Arb.Companion.creature() = arbitrary {
        Creature(
            id = Arb.long(1..1000).bind(),
            name = Arb.string(5..20).bind(),
            ac = Arb.int(10..20).bind(),
            hpCurrent = Arb.int(1..100).bind(),
            hpMax = Arb.int(1..100).bind(),
            speed = Arb.int(20..40).bind(),
            abilities = Arb.abilities().bind()
        )
    }
    
    fun Arb.Companion.encounterState() = arbitrary {
        val creatures = Arb.list(Arb.creature(), 1..6).bind()
        EncounterState(
            id = Arb.long().bind(),
            round = Arb.int(1..10).bind(),
            creatures = creatures,
            initiative = creatures.mapIndexed { idx, c ->
                InitiativeEntry(idx, c.id, Arb.int(1..20).bind())
            }
        )
    }
}
```

## Test Organization

### Naming Conventions
- Test classes: `<ClassUnderTest>Test`
- Test methods: Descriptive sentences in backticks or strings
- Use `should` or `when...then` patterns

```kotlin
class RulesEngineTest : FunSpec({
    context("attack resolution") {
        test("attack roll + modifier >= AC results in hit") { }
        test("natural 20 always hits") { }
        test("natural 1 always misses") { }
    }
    
    context("saving throws") {
        test("roll + modifier >= DC results in success") { }
        test("advantage rolls twice and takes max") { }
    }
})
```

### Test Data Builders

```kotlin
class CreatureBuilder {
    private var id: Long = 1
    private var name: String = "Test"
    private var ac: Int = 15
    private var hp: Int = 20
    
    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withAC(ac: Int) = apply { this.ac = ac }
    fun withHP(hp: Int) = apply { this.hp = hp }
    
    fun build() = Creature(id, name, ac, hp, hp, 30, Abilities())
}

// Usage
val creature = CreatureBuilder()
    .withName("Goblin")
    .withAC(13)
    .withHP(7)
    .build()
```

## Continuous Integration

### GitHub Actions Workflow
```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
      
      - name: Run tests
        run: ./gradlew test --parallel
      
      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'
```

## Code Coverage

### Kover Configuration
```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
}

kover {
    reports {
        filters {
            excludes {
                classes("*.BuildConfig", "*_Factory", "*_MembersInjector")
                packages("*.di", "*.generated")
            }
        }
        
        verify {
            rule {
                minBound(80) // 80% minimum coverage
            }
        }
    }
}
```

### Coverage Goals
- **Core/Rules**: 90%+ (deterministic, critical)
- **Domain/Use Cases**: 85%+
- **Data/Repositories**: 80%+
- **UI/Compose**: 60%+ (focus on logic, not rendering)
- **AI Agents**: 70%+ (behavior trees, scoring)

## Performance Testing

### Benchmark with JMH
```kotlin
@State(Scope.Benchmark)
class PathfindingBenchmark {
    private lateinit var grid: MapGrid
    private lateinit var pathfinder: Pathfinder
    
    @Setup
    fun setup() {
        grid = MapGrid(50, 50, IntArray(2500))
        pathfinder = AStarPathfinder()
    }
    
    @Benchmark
    fun findPath() {
        pathfinder.findPath(
            start = GridPos(0, 0),
            goal = GridPos(49, 49),
            grid = grid
        )
    }
}
```

### Profile Map Rendering
```kotlin
@Test
fun mapRenderingPerformance() {
    val state = MapState(w = 50, h = 50, tileSize = 50f)
    
    val startTime = System.nanoTime()
    repeat(100) {
        // Simulate rendering
        Canvas(Modifier.size(2500.dp, 2500.dp)) {
            // Draw grid and tokens
        }
    }
    val duration = (System.nanoTime() - startTime) / 1_000_000
    
    // Should average < 4ms per frame
    (duration / 100) shouldBeLessThan 4
}
```

## Test Execution

### Run All Tests
```bash
./gradlew test
```

### Run Specific Module
```bash
./gradlew :core:rules:test
```

### Run with Coverage
```bash
./gradlew koverHtmlReport
# Open build/reports/kover/html/index.html
```

### Run Property Tests with More Iterations
```kotlin
test("property test with 10000 iterations").config(
    invocations = 10000
) {
    checkAll(Arb.creature()) { creature ->
        // assertions
    }
}
```

## Debugging Tests

### Enable Logging
```kotlin
class MyTest : FunSpec({
    beforeSpec {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
    }
    
    test("with logging") {
        logger.debug { "Test state: $state" }
        // test code
    }
})
```

### Isolate Failing Tests
```kotlin
test("failing test").config(enabled = false) {
    // Temporarily disable
}

xtest("skipped test") {
    // Alternative syntax
}
```

## Best Practices

1. **Test Behavior, Not Implementation**: Focus on what the code does, not how
2. **One Assertion Per Test**: Keep tests focused and easy to debug
3. **Use Descriptive Names**: Test names should explain the scenario
4. **Avoid Test Interdependence**: Each test should run independently
5. **Mock External Dependencies**: Database, network, AI services
6. **Test Edge Cases**: Null, empty, boundary values
7. **Verify Event Sourcing**: Test event generation and replay
8. **Deterministic Tests**: Use seeded RNG, fixed timestamps
9. **Fast Tests**: Unit tests should run in milliseconds
10. **Clean Up Resources**: Close databases, files, connections

## Module Verification

### Koin Module Verification
```kotlin
class ModuleVerificationTest : KoinTest {
    @Test
    fun `verify all modules`() {
        koinApplication {
            modules(
                domainModule,
                dataModule,
                rulesModule,
                aiModule,
                mapModule
            )
        }.verify()
    }
}
```

This ensures all Koin dependencies are correctly wired before runtime.
