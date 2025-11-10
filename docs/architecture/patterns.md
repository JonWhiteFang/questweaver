# Architectural Patterns

## MVI (Model-View-Intent)

```kotlin
// State: single immutable data class
data class EncounterUiState(
    val round: Int = 1,
    val activeCreatureId: Long? = null,
    val mapState: MapState = MapState()
)

// Intent: sealed interface for user actions
sealed interface EncounterIntent {
    data class MoveTo(val pos: GridPos) : EncounterIntent
    object EndTurn : EncounterIntent
}

// ViewModel: unidirectional flow
class EncounterViewModel : ViewModel() {
    private val _state = MutableStateFlow(EncounterUiState())
    val state: StateFlow<EncounterUiState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) { /* process */ }
}
```

## Event Sourcing

```kotlin
// All state mutations produce immutable events
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

// State derived from event replay
fun replayEvents(events: List<GameEvent>): EncounterState
```

### Event Design Principles

**✅ CORRECT: Capture intent AND outcome**
```kotlin
data class AttackResolved(
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val modifiers: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent
```

**❌ WRONG: Only captures outcome**
```kotlin
data class HPChanged(val creatureId: Long, val newHP: Int) : GameEvent
```

## Repository Pattern

```kotlin
// Interface in core:domain
interface EventRepository {
    suspend fun append(event: GameEvent)
    suspend fun forSession(sessionId: Long): List<GameEvent>
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}

// Implementation in core:data
class EventRepositoryImpl(private val dao: EventDao) : EventRepository {
    override suspend fun append(event: GameEvent) = dao.insert(event.toEntity())
    override suspend fun forSession(sessionId: Long) = dao.getBySession(sessionId).map { it.toDomain() }
    override fun observeSession(sessionId: Long) = dao.observeBySession(sessionId)
        .map { entities -> entities.map { it.toDomain() } }
}
```

## Use Case Pattern

```kotlin
class ProcessPlayerAction(
    private val rulesEngine: RulesEngine,
    private val eventRepo: EventRepository
) {
    suspend operator fun invoke(action: NLAction): ActionResult {
        // Validate with rules engine
        val outcome = rulesEngine.resolve(action)
        
        // Generate events
        val events = outcome.toEvents()
        
        // Persist events
        events.forEach { eventRepo.append(it) }
        
        return ActionResult.Success(events)
    }
}
```

## Sealed Types for ADTs

```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}

// MUST be exhaustive - no else branch
fun handle(result: ActionResult) = when (result) {
    is ActionResult.Success -> applyEvents(result.events)
    is ActionResult.Failure -> showError(result.reason)
    is ActionResult.RequiresChoice -> promptUser(result.options)
}
```

## Deterministic Behavior

**✅ CORRECT: Seeded RNG**
```kotlin
val roller = DiceRoller(seed = sessionSeed)
val result = roller.d20()
```

**❌ WRONG: Unseeded random**
```kotlin
val result = Random.nextInt(1, 21) // Non-deterministic
```

## State Hoisting in Compose

```kotlin
@Composable
fun TacticalMapScreen(
    state: MapState,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stateless - receives state, emits intents
    Canvas(modifier) {
        state.tokens.forEach { token ->
            drawCircle(/* ... */)
        }
    }
}
```

## Dependency Injection with Koin

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

## Error Handling with Result Types

```kotlin
sealed interface ActionResult {
    data class Success(val events: List<GameEvent>) : ActionResult
    data class Failure(val reason: String) : ActionResult
    data class RequiresChoice(val options: List<ActionOption>) : ActionResult
}
```

## Immutability

- Prefer `val` over `var`
- Use `data class` for entities
- Use sealed types for ADTs
- Require exhaustive `when` expressions

---

**Last Updated**: 2025-11-10
