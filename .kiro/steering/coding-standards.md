---
inclusion: always
---

# Coding Standards

## Kotlin Conventions

**Immutability First**: Use `val` over `var`, `data class` for value objects, sealed types for ADTs

**Naming**: Meaningful names, avoid abbreviations except domain terms (HP, AC, DC)

**Sealed Types**: Always use for events, actions, results. Require exhaustive `when` expressions:
```kotlin
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

fun handle(event: GameEvent) = when (event) {
    is GameEvent.EncounterStarted -> handleStart(event)
    is GameEvent.MoveCommitted -> handleMove(event)
    // Compiler enforces exhaustiveness - no else branch
}
```

## Architecture Patterns

### MVI (Model-View-Intent)
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

### Use Cases
- Single public `suspend operator fun invoke()` per use case
- Return sealed `Result` types
- No Android/UI dependencies
```kotlin
class ProcessPlayerAction(
    private val rulesEngine: RulesEngine,
    private val eventRepo: EventRepository
) {
    suspend operator fun invoke(input: NLAction): ActionResult
}
```

### Repository Pattern
- Interfaces in `core:domain`, implementations in `core:data`
- Return `Flow` for reactive queries, `suspend fun` for one-shot operations
```kotlin
interface EventRepository {
    suspend fun append(event: GameEvent)
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}
```

## Event Sourcing Rules

1. **All state mutations produce events** - never mutate state directly
2. **Events are immutable** - once written, never modified
3. **State is derived** - rebuild from event replay
4. **Deterministic replay** - same events → same state

**Event Design**: Capture intent AND outcome, not just final state
```kotlin
// ✅ Good: captures full context
data class AttackResolved(
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val modifiers: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// ❌ Bad: only captures outcome
data class HPChanged(val creatureId: Long, val newHP: Int) : GameEvent
```

**Seeded Randomness**: Always use seeded RNG for deterministic replay
```kotlin
class DiceRoller(private val seed: Long) {
    private val random = Random(seed)
    fun d20(): Int = random.nextInt(1, 21)
}
```

## Compose UI

**State Hoisting**: Keep composables stateless, hoist state to ViewModels
```kotlin
@Composable
fun TacticalMapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stateless - receives state, emits intents
}
```

**Performance**:
- Use `remember` for expensive calculations
- Use `derivedStateOf` for computed state
- Use `key()` to avoid unnecessary recomposition
- Use `LazyColumn/Row` for large lists

## Dependency Injection (Koin)

**Module Organization**: Separate by layer (domain, data, rules, ui)
```kotlin
val domainModule = module {
    factory { ProcessPlayerAction(get(), get()) }
}

val dataModule = module {
    single<EventRepository> { EventRepositoryImpl(get()) }
}

// ViewModel injection
viewModel { EncounterViewModel(get(), get()) }
```

## Error Handling

**Result Types**: Use sealed interfaces for operation results
```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}
```

**Logging**: Structured logging, never log PII
```kotlin
logger.info { "Attack resolved: attacker=$attackerId, hit=$hit" } // ✅
logger.debug { "User ${user.email} did ${action}" } // ❌ Never log PII
```

## Testing (kotest + MockK)

**Test Structure**:
```kotlin
class ComponentTest : FunSpec({
    test("descriptive behavior description") {
        // Arrange
        val input = createTestData()
        
        // Act
        val result = systemUnderTest.process(input)
        
        // Assert
        result shouldBe expectedValue
    }
})
```

**Deterministic Tests**: Always use seeded RNG
```kotlin
val roller = DiceRoller(seed = 42) // ✅ Reproducible
val result = Random.nextInt(1, 21) // ❌ Non-deterministic
```

**Property-Based Tests**: Use for rules engine
```kotlin
test("d20 always returns 1-20") {
    checkAll(Arb.long()) { seed ->
        DiceRoller(seed).d20() shouldBeInRange 1..20
    }
}
```

**Mocking**: Use MockK for external dependencies
```kotlin
val repo = mockk<EventRepository>()
coEvery { repo.append(any()) } just Runs
coVerify { repo.append(any<GameEvent.AttackResolved>()) }
```

## Performance Targets

- **Map render**: ≤4ms per frame (60fps)
- **AI decision**: ≤300ms on-device
- **Database queries**: <50ms typical

**Optimization**:
- Batch draw calls in map rendering
- Use transactions for multi-event writes
- Index foreign keys and query columns
- Warm up AI models on background thread

## Security

**Encryption**: SQLCipher with Android Keystore-wrapped keys
```kotlin
val passphrase = keyStore.getKey("db_key", null).encoded
val factory = SupportFactory(passphrase)
```

**Input Validation**: Validate all user input, sanitize AI prompts, check bounds

## Documentation

**KDoc for Public APIs**: Document parameters, return values, behavior
```kotlin
/**
 * Resolves an attack roll against a target.
 *
 * @param attacker The creature making the attack
 * @param target The creature being attacked
 * @return [AttackOutcome] with hit status and damage
 */
fun checkAttack(attacker: Creature, target: Creature): AttackOutcome
```

**Module READMEs**: Each feature module should document purpose, key classes, dependencies, testing approach

## Critical Rules Summary

1. **Immutability**: Prefer `val`, `data class`, sealed types
2. **Exhaustive when**: Required for sealed types, no else branch
3. **Event sourcing**: All mutations produce events, state is derived
4. **Deterministic**: Use seeded RNG, ensure reproducible outcomes
5. **MVI pattern**: StateFlow + sealed intents + unidirectional flow
6. **Repository pattern**: Interfaces in domain, implementations in data
7. **State hoisting**: Keep composables stateless
8. **Testing**: Use seeded RNG, property-based tests for rules
9. **Security**: Encrypt local data, validate all inputs
10. **Performance**: Target 60fps rendering, <300ms AI decisions
