# feature:encounter

**Combat turn management and encounter UI**

## Purpose

The `feature:encounter` module manages combat encounters, turn order, initiative tracking, and combat UI. It integrates with `feature:map` for tactical visualization and `core:rules` for combat resolution, providing the main interface for players to engage in grid-based tactical combat.

## Responsibilities

- Manage combat turn order and initiative
- Track encounter state (round, active creature, conditions)
- Provide combat UI (initiative list, action buttons, status display)
- Integrate tactical map for movement and targeting
- Process combat actions through rules engine
- Generate and persist combat events
- Display combat log and narration

## Key Classes and Interfaces

### UI Components (Placeholder)

- `EncounterScreen`: Main Composable for combat UI
- `InitiativeList`: Displays turn order
- `ActionPanel`: Combat action buttons
- `CombatLog`: Scrollable combat event log

### ViewModels (Placeholder)

- `EncounterViewModel`: MVI ViewModel for encounter state
- `EncounterState`: Immutable encounter state
- `EncounterIntent`: Sealed interface for combat actions

### Turn Management (Placeholder)

- `TurnEngine`: Manages turn progression
- `InitiativeTracker`: Tracks initiative order
- `RoundManager`: Manages combat rounds

## Dependencies

### Production

- `core:domain`: Domain entities and use cases
- `core:rules`: Rules engine for combat resolution
- `feature:map`: Tactical map display (ONLY feature dependency allowed)
- `compose-ui`: Jetpack Compose UI
- `compose-material3`: Material3 components
- `kotlinx-coroutines-android`: Coroutines for Android

### Test

- `kotest-runner-junit5`: Testing framework
- `kotest-assertions-core`: Assertion library
- `mockk`: Mocking library

## Module Rules

### ✅ Allowed

- Compose UI for combat screens
- Dependencies on `core:domain`, `core:rules`, and `feature:map`
- Combat state management
- Turn order logic

### ❌ Forbidden

- Dependencies on other feature modules (except `feature:map`)
- Business logic (belongs in `core:domain` or `core:rules`)
- Direct database access (use repositories)

## Architecture Patterns

### MVI Pattern

```kotlin
data class EncounterState(
    val encounterId: Long,
    val round: Int = 1,
    val turnOrder: List<Long> = emptyList(),
    val activeCreatureId: Long? = null,
    val creatures: Map<Long, CreatureState> = emptyMap(),
    val mapState: MapState = MapState(),
    val combatLog: List<CombatLogEntry> = emptyList()
)

sealed interface EncounterIntent {
    data class StartEncounter(val creatures: List<Creature>) : EncounterIntent
    data class MoveTo(val pos: GridPos) : EncounterIntent
    data class Attack(val targetId: Long) : EncounterIntent
    data class CastSpell(val spellId: String, val targetId: Long?) : EncounterIntent
    object EndTurn : EncounterIntent
    object EndEncounter : EncounterIntent
}

class EncounterViewModel(
    private val processAction: ProcessPlayerAction,
    private val rulesEngine: RulesEngine
) : ViewModel() {
    private val _state = MutableStateFlow(EncounterState())
    val state: StateFlow<EncounterState> = _state.asStateFlow()
    
    fun handle(intent: EncounterIntent) {
        viewModelScope.launch {
            when (intent) {
                is EncounterIntent.StartEncounter -> startEncounter(intent.creatures)
                is EncounterIntent.MoveTo -> handleMove(intent.pos)
                is EncounterIntent.Attack -> handleAttack(intent.targetId)
                is EncounterIntent.CastSpell -> handleSpell(intent.spellId, intent.targetId)
                is EncounterIntent.EndTurn -> endTurn()
                is EncounterIntent.EndEncounter -> endEncounter()
            }
        }
    }
}
```

### Turn Engine

Manage turn progression:

```kotlin
class TurnEngine(
    private val initiativeTracker: InitiativeTracker
) {
    fun startEncounter(creatures: List<Creature>): TurnOrder {
        val initiatives = creatures.map { creature ->
            val roll = rollInitiative(creature)
            creature.id to roll
        }.sortedByDescending { it.second }
        
        return TurnOrder(initiatives.map { it.first })
    }
    
    fun nextTurn(currentOrder: TurnOrder): TurnOrder {
        val nextIndex = (currentOrder.currentIndex + 1) % currentOrder.creatures.size
        return currentOrder.copy(currentIndex = nextIndex)
    }
}
```

### Combat Integration

Integrate map and rules:

```kotlin
private suspend fun handleAttack(targetId: Long) {
    val attacker = state.value.creatures[state.value.activeCreatureId] ?: return
    val target = state.value.creatures[targetId] ?: return
    
    // Validate action with rules engine
    val action = NLAction.Attack(attacker.id, target.id)
    val result = processAction(action)
    
    when (result) {
        is ActionResult.Success -> {
            // Update state with events
            applyEvents(result.events)
            // Update combat log
            addToCombatLog(result.events)
        }
        is ActionResult.Failure -> {
            // Show error message
            showError(result.reason)
        }
        is ActionResult.RequiresChoice -> {
            // Prompt user for choice
            promptChoice(result.options)
        }
    }
}
```

## Testing Approach

### Unit Tests

- Test turn order logic
- Test initiative calculation
- Test ViewModel state transitions
- Test combat action processing

### Integration Tests

- Test encounter flow end-to-end
- Test integration with rules engine
- Test event generation

### Coverage Target

**60%+** code coverage (focus on logic, not UI rendering)

### Example Test

```kotlin
class TurnEngineTest : FunSpec({
    test("next turn advances to next creature") {
        val creatures = listOf(
            Fixtures.mockCreature(id = 1),
            Fixtures.mockCreature(id = 2),
            Fixtures.mockCreature(id = 3)
        )
        val engine = TurnEngine(InitiativeTracker())
        
        val order = engine.startEncounter(creatures)
        val nextOrder = engine.nextTurn(order)
        
        nextOrder.currentIndex shouldBe 1
        nextOrder.activeCreatureId shouldBe creatures[1].id
    }
})
```

## Building and Testing

```bash
# Build module
./gradlew :feature:encounter:build

# Run tests
./gradlew :feature:encounter:test

# Run tests with coverage
./gradlew :feature:encounter:test koverHtmlReport
```

## Package Structure

```
dev.questweaver.feature.encounter/
├── ui/
│   ├── EncounterScreen.kt
│   ├── InitiativeList.kt
│   ├── ActionPanel.kt
│   └── CombatLog.kt
├── viewmodel/
│   └── EncounterViewModel.kt
├── engine/
│   ├── TurnEngine.kt
│   ├── InitiativeTracker.kt
│   └── RoundManager.kt
└── di/
    └── EncounterModule.kt
```

## Integration Points

### Consumed By

- `app` (navigation to encounter screen)

### Depends On

- `core:domain` (entities, use cases, events)
- `core:rules` (combat resolution)
- `feature:map` (tactical map display)

## Features

### Current (Placeholder)

- Initiative tracking
- Turn order management
- Basic combat UI

### Planned

- Attack actions with targeting
- Movement with pathfinding
- Spell casting
- Condition tracking
- Combat log with narration
- AI opponent turns
- End-of-encounter summary

## UI/UX Considerations

- **Initiative List**: Always visible, shows turn order and HP
- **Action Panel**: Context-sensitive actions based on active creature
- **Combat Log**: Auto-scrolls to latest entry
- **Map Integration**: Seamless integration with tactical map
- **Feedback**: Clear visual feedback for actions and outcomes

## Performance Considerations

- **State Updates**: Batch state updates to minimize recomposition
- **Event Processing**: Process events asynchronously
- **Map Rendering**: Delegate to `feature:map` for optimization

## Notes

- This is the ONLY feature module allowed to depend on another feature (`feature:map`)
- All combat logic should go through `core:rules` for validation
- Use event sourcing - generate events for all state changes
- Keep UI logic separate from business logic
- Test turn management and initiative logic thoroughly

---

**Last Updated**: 2025-11-10
