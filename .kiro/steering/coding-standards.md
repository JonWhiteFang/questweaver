# QuestWeaver Coding Standards

## Kotlin Style

### General
- Follow official Kotlin coding conventions
- Use meaningful names; avoid abbreviations unless domain-standard (HP, AC, DC)
- Prefer immutability; use `val` over `var`
- Use `data class` for value objects
- Use `sealed class/interface` for ADTs (actions, events, results)

### Sealed Classes for Domain Events
```kotlin
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
    
    data class EncounterStarted(
        override val sessionId: Long,
        override val timestamp: Long,
        val mapId: Long,
        val participants: List<Long>,
        val seed: Long
    ) : GameEvent
    
    data class MoveCommitted(
        override val sessionId: Long,
        override val timestamp: Long,
        val creatureId: Long,
        val path: List<GridPos>,
        val cost: Int
    ) : GameEvent
}
```

### Exhaustive When
Always use exhaustive `when` for sealed types:
```kotlin
fun handle(event: GameEvent) = when (event) {
    is GameEvent.EncounterStarted -> handleEncounterStart(event)
    is GameEvent.MoveCommitted -> handleMove(event)
    // Compiler enforces exhaustiveness
}
```

## Architecture Patterns

### MVI (Model-View-Intent)
```kotlin
// State: single immutable data class
data class EncounterUiState(
    val round: Int = 1,
    val activeCreatureId: Long? = null,
    val mapState: MapState = MapState(),
    val turnOrder: List<InitiativeEntry> = emptyList()
)

// Intent: user actions
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    data class Attack(val targetId: Long) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: unidirectional flow
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) {
        viewModelScope.launch {
            when (intent) {
                is EncounterIntent.MoveTo -> processMove(intent.pos)
                // ...
            }
        }
    }
}
```

### Use Cases
- One public suspend function per use case
- Return sealed `Result` types
- No UI dependencies

```kotlin
class ProcessPlayerAction(
    private val rulesEngine: RulesEngine,
    private val eventRepo: EventRepository
) {
    suspend operator fun invoke(input: NLAction): ActionResult {
        // Validate → Execute → Persist → Return
    }
}
```

### Repository Pattern
```kotlin
interface EventRepository {
    suspend fun append(event: GameEvent)
    suspend fun forSession(sessionId: Long): List<GameEvent>
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}
```

## Event Sourcing

### Rules
1. **All state mutations produce events** - never mutate state directly
2. **Events are immutable** - once written, never modified
3. **State is derived** - rebuild from event replay
4. **Deterministic replay** - same events → same state

### Event Design
```kotlin
// Good: captures intent and outcome
data class AttackResolved(
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val modifiers: Int,
    val advantage: Advantage,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// Bad: captures only outcome
data class HPChanged(val creatureId: Long, val newHP: Int) : GameEvent
```

### Seeded Randomness
```kotlin
class DiceRoller(private val seed: Long) {
    private val random = Random(seed)
    
    fun d20(advantage: Advantage = Advantage.NONE): DiceRoll {
        val rolls = when (advantage) {
            Advantage.NONE -> listOf(random.nextInt(1, 21))
            Advantage.ADVANTAGE -> List(2) { random.nextInt(1, 21) }
            Advantage.DISADVANTAGE -> List(2) { random.nextInt(1, 21) }
        }
        return DiceRoll(rolls, advantage)
    }
}
```

## Compose UI

### State Hoisting
```kotlin
@Composable
fun TacticalMapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stateless composable
    TacticalMap(
        state = state,
        onTileTap = { pos -> onIntent(MapIntent.TileTapped(pos)) },
        modifier = modifier
    )
}
```

### Performance
- Use `remember` for expensive calculations
- Use `derivedStateOf` for computed state
- Avoid recomposition with `key()` for lists
- Use `LazyColumn/Row` for large lists

## Testing

### Unit Tests (kotest)
```kotlin
class RulesEngineTest : FunSpec({
    test("attack with advantage rolls twice and takes max") {
        val roller = DiceRoller(seed = 42)
        val result = roller.d20(Advantage.ADVANTAGE)
        
        result.rolls.size shouldBe 2
        result.value shouldBe result.rolls.max()
    }
    
    test("AC 15 blocks attack roll of 14") {
        val engine = RulesEngine()
        val attacker = mockCreature(attackBonus = 5)
        val target = mockCreature(ac = 15)
        
        val outcome = engine.checkAttack(
            attacker, target, mockWeapon(), 
            roll = 9 // 9 + 5 = 14, misses AC 15
        )
        
        outcome.hit shouldBe false
    }
})
```

### Property-Based Tests
```kotlin
class DicePropertyTest : FunSpec({
    test("d20 always returns 1-20") {
        checkAll(Arb.long()) { seed ->
            val roller = DiceRoller(seed)
            val roll = roller.d20()
            roll.value shouldBeInRange 1..20
        }
    }
})
```

## Error Handling

### Result Types
```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}
```

### Logging
```kotlin
// Use structured logging
logger.info { "Attack resolved: attacker=$attackerId, target=$targetId, hit=$hit, damage=$damage" }

// Never log PII
logger.debug { "User action: ${action.type}" } // Good
logger.debug { "User ${user.email} did ${action}" } // Bad
```

## Dependency Injection (Koin)

### Module Organization
```kotlin
val domainModule = module {
    factory { ProcessPlayerAction(get(), get()) }
    factory { RunCombatRound(get(), get()) }
}

val dataModule = module {
    single { provideDatabase(get(), get(named("dbKey"))) }
    single<EventRepository> { EventRepositoryImpl(get()) }
}

val rulesModule = module {
    single { RulesEngine(get()) }
    factory { (seed: Long) -> DiceRoller(seed) }
}
```

### ViewModel Injection
```kotlin
class EncounterViewModel(
    private val runCombatRound: RunCombatRound,
    private val eventRepo: EventRepository
) : ViewModel() {
    // ...
}

// In Koin module
viewModel { EncounterViewModel(get(), get()) }
```

## Performance

### Map Rendering
- Keep draw calls minimal; batch where possible
- Use `drawIntoCanvas` for complex shapes
- Profile with Android Studio GPU Profiler
- Target 60fps (16ms frame budget)

### Database
- Use transactions for multi-event writes
- Index foreign keys and query columns
- Paginate large result sets
- Use `Flow` for reactive queries

### AI Inference
- Warm up models on background thread
- Cache tokenization results
- Batch requests where possible
- Set timeouts and fallbacks

## Security

### Encryption
```kotlin
// Generate key with Android Keystore
val keyStore = KeyStore.getInstance("AndroidKeyStore")
val keyGenerator = KeyGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_AES,
    "AndroidKeyStore"
)

// Wrap SQLCipher passphrase
val passphrase = keyStore.getKey("db_key", null).encoded
val factory = SupportFactory(passphrase)
```

### Input Validation
- Validate all user input before processing
- Sanitize text for AI prompts
- Check bounds for grid positions
- Validate event sequences for consistency

## Documentation

### KDoc for Public APIs
```kotlin
/**
 * Resolves an attack roll against a target.
 *
 * @param attacker The creature making the attack
 * @param target The creature being attacked
 * @param weapon The weapon used for the attack
 * @param advantage Whether the attack has advantage/disadvantage
 * @return [AttackOutcome] with hit status and damage
 */
fun checkAttack(
    attacker: Creature,
    target: Creature,
    weapon: Weapon,
    advantage: Advantage
): AttackOutcome
```

### README per Module
Each feature module should have a README explaining:
- Purpose and responsibilities
- Key classes and interfaces
- Dependencies
- Testing approach
