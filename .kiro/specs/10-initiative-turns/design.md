# Initiative & Turn Management Design

## Overview

The Initiative & Turn Management system is a pure Kotlin module in `core/rules` that implements D&D 5e SRD-compatible initiative and turn order mechanics. It provides deterministic initiative rolling, turn order management, turn phase tracking, and round progression. The system is stateless and event-sourced, deriving all state from immutable events to ensure reproducible combat sequences.

**Key Design Principles:**
- Pure Kotlin with no Android dependencies
- 100% deterministic using seeded random number generation
- Event-sourced state management (all state derived from events)
- Immutable data structures for initiative order and turn state
- Exhaustive sealed types for all events and turn phases
- SRD-compatible D&D 5e initiative mechanics

## Architecture

### Module Location
`core/rules/src/main/kotlin/dev/questweaver/core/rules/initiative/`

### Package Structure
```
core/rules/initiative/
├── InitiativeRoller.kt        # Rolls initiative for creatures
├── InitiativeTracker.kt       # Manages turn order and progression
├── TurnPhaseManager.kt        # Tracks action economy within a turn
├── SurpriseHandler.kt         # Handles surprise round mechanics
└── models/
    ├── InitiativeEntry.kt     # Initiative score + creature ID
    ├── TurnPhase.kt           # Action/bonus/reaction availability
    ├── TurnState.kt           # Current turn information
    └── RoundState.kt          # Current round information
```

### Dependencies
- `core:domain` - Entity definitions (Creature, GameEvent)
- `04-dice-system` - DiceRoller for seeded initiative rolls
- `05-combat-rules` - Ability modifiers for initiative calculation

## Components and Interfaces

### 1. InitiativeRoller

Rolls initiative for creatures and establishes initial turn order.

```kotlin
class InitiativeRoller(private val diceRoller: DiceRoller) {
    /**
     * Rolls initiative for a single creature.
     *
     * @param creatureId The creature's unique identifier
     * @param dexterityModifier The creature's Dexterity modifier
     * @return InitiativeEntry with roll result and creature ID
     */
    fun rollInitiative(
        creatureId: Long,
        dexterityModifier: Int
    ): InitiativeEntry
    
    /**
     * Rolls initiative for multiple creatures and sorts by score.
     *
     * @param creatures Map of creature ID to Dexterity modifier
     * @return Sorted list of InitiativeEntry (highest first)
     */
    fun rollInitiativeForAll(
        creatures: Map<Long, Int>
    ): List<InitiativeEntry>
}
```

**Algorithm:**
1. For each creature, roll d20 + Dexterity modifier
2. Create InitiativeEntry with creature ID, roll, and modifier
3. Sort entries by total score (descending)
4. For ties, use Dexterity modifier as tiebreaker
5. For identical scores and modifiers, use creature ID (deterministic)
6. Return sorted list

### 2. InitiativeTracker

Manages turn order progression and creature lifecycle during combat.

```kotlin
class InitiativeTracker {
    /**
     * Creates initial turn order from initiative entries.
     *
     * @param initiativeOrder Sorted list of initiative entries
     * @param surprisedCreatures Set of creature IDs that are surprised
     * @return Initial RoundState with turn order established
     */
    fun initialize(
        initiativeOrder: List<InitiativeEntry>,
        surprisedCreatures: Set<Long> = emptySet()
    ): RoundState
    
    /**
     * Advances to the next creature in turn order.
     *
     * @param currentState Current round and turn state
     * @return Updated RoundState with new active creature
     */
    fun advanceTurn(currentState: RoundState): RoundState
    
    /**
     * Adds a creature to combat mid-encounter.
     *
     * @param currentState Current round and turn state
     * @param newEntry Initiative entry for the new creature
     * @return Updated RoundState with creature inserted
     */
    fun addCreature(
        currentState: RoundState,
        newEntry: InitiativeEntry
    ): RoundState
    
    /**
     * Removes a creature from combat (defeated or fled).
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of creature to remove
     * @return Updated RoundState with creature removed
     */
    fun removeCreature(
        currentState: RoundState,
        creatureId: Long
    ): RoundState
    
    /**
     * Delays a creature's turn to later in initiative order.
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of creature delaying
     * @return Updated RoundState with creature removed from current position
     */
    fun delayTurn(
        currentState: RoundState,
        creatureId: Long
    ): RoundState
    
    /**
     * Inserts a delayed creature at the current initiative position.
     *
     * @param currentState Current round and turn state
     * @param creatureId ID of delayed creature
     * @param newInitiative New initiative score for the creature
     * @return Updated RoundState with creature inserted
     */
    fun resumeDelayedTurn(
        currentState: RoundState,
        creatureId: Long,
        newInitiative: Int
    ): RoundState
}
```

**Turn Advancement Algorithm:**
1. Get current turn index in initiative order
2. Increment index
3. If index exceeds order length:
   - Reset index to 0
   - Increment round counter
   - Reset per-round resources (reactions)
4. If surprise round and creature is surprised:
   - Skip creature and recurse
5. Return updated RoundState with new active creature

**Creature Removal Algorithm:**
1. Find creature in initiative order
2. If creature is before current turn index, decrement index
3. Remove creature from order
4. If removed creature was active, advance turn
5. Return updated RoundState

### 3. TurnPhaseManager

Tracks action economy and resource consumption within a turn.

```kotlin
class TurnPhaseManager {
    /**
     * Creates initial turn phase state for a creature's turn.
     *
     * @param creatureId The creature whose turn is starting
     * @param movementSpeed The creature's movement speed
     * @return TurnPhase with all actions available
     */
    fun startTurn(
        creatureId: Long,
        movementSpeed: Int
    ): TurnPhase
    
    /**
     * Consumes movement from the current turn.
     *
     * @param currentPhase Current turn phase state
     * @param movementUsed Amount of movement consumed (in feet)
     * @return Updated TurnPhase with reduced movement
     */
    fun consumeMovement(
        currentPhase: TurnPhase,
        movementUsed: Int
    ): TurnPhase
    
    /**
     * Marks the action phase as consumed.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with action consumed
     */
    fun consumeAction(currentPhase: TurnPhase): TurnPhase
    
    /**
     * Marks the bonus action phase as consumed.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with bonus action consumed
     */
    fun consumeBonusAction(currentPhase: TurnPhase): TurnPhase
    
    /**
     * Marks the reaction as consumed.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with reaction consumed
     */
    fun consumeReaction(currentPhase: TurnPhase): TurnPhase
    
    /**
     * Restores the reaction at the start of a creature's turn.
     *
     * @param currentPhase Current turn phase state
     * @return Updated TurnPhase with reaction restored
     */
    fun restoreReaction(currentPhase: TurnPhase): TurnPhase
    
    /**
     * Checks if a specific action type is available.
     *
     * @param currentPhase Current turn phase state
     * @param actionType Type of action to check
     * @return True if the action is available
     */
    fun isActionAvailable(
        currentPhase: TurnPhase,
        actionType: ActionType
    ): Boolean
}
```

**Action Type Enum:**
```kotlin
enum class ActionType {
    Action,
    BonusAction,
    Reaction,
    Movement,
    FreeAction  // Object interaction, communication
}
```

### 4. SurpriseHandler

Manages surprise round mechanics and surprised condition.

```kotlin
class SurpriseHandler {
    /**
     * Determines if a surprise round should occur.
     *
     * @param surprisedCreatures Set of creature IDs that are surprised
     * @return True if any creatures are surprised
     */
    fun hasSurpriseRound(surprisedCreatures: Set<Long>): Boolean
    
    /**
     * Checks if a creature can act in the surprise round.
     *
     * @param creatureId The creature to check
     * @param surprisedCreatures Set of surprised creature IDs
     * @return True if the creature can act
     */
    fun canActInSurpriseRound(
        creatureId: Long,
        surprisedCreatures: Set<Long>
    ): Boolean
    
    /**
     * Removes surprised condition from all creatures.
     *
     * @param surprisedCreatures Set of surprised creature IDs
     * @return Empty set (all creatures no longer surprised)
     */
    fun endSurpriseRound(
        surprisedCreatures: Set<Long>
    ): Set<Long>
}
```

## Data Models

### Immutable State Classes

```kotlin
/**
 * Initiative entry for a single creature.
 */
data class InitiativeEntry(
    val creatureId: Long,
    val roll: Int,              // d20 roll result
    val modifier: Int,          // Dexterity modifier
    val total: Int              // roll + modifier
) : Comparable<InitiativeEntry> {
    override fun compareTo(other: InitiativeEntry): Int {
        // Sort by total (descending), then modifier (descending), then ID (ascending)
        return compareValuesBy(
            this, other,
            { -it.total },
            { -it.modifier },
            { it.creatureId }
        )
    }
}

/**
 * Turn phase tracking for action economy.
 */
data class TurnPhase(
    val creatureId: Long,
    val movementRemaining: Int,
    val actionAvailable: Boolean,
    val bonusActionAvailable: Boolean,
    val reactionAvailable: Boolean
)

/**
 * Current turn state.
 */
data class TurnState(
    val activeCreatureId: Long,
    val turnPhase: TurnPhase,
    val turnIndex: Int          // Index in initiative order
)

/**
 * Current round state.
 */
data class RoundState(
    val roundNumber: Int,
    val isSurpriseRound: Boolean,
    val initiativeOrder: List<InitiativeEntry>,
    val surprisedCreatures: Set<Long>,
    val delayedCreatures: Map<Long, InitiativeEntry>, // Creatures that delayed
    val currentTurn: TurnState?
)
```

### Event Types

```kotlin
/**
 * Encounter started with initiative rolled.
 */
data class EncounterStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val initiativeRolls: List<InitiativeEntry>,
    val surprisedCreatures: Set<Long>
) : GameEvent

/**
 * New round started.
 */
data class RoundStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val roundNumber: Int
) : GameEvent

/**
 * Creature's turn started.
 */
data class TurnStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val roundNumber: Int,
    val turnIndex: Int
) : GameEvent

/**
 * Creature's turn ended.
 */
data class TurnEnded(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val roundNumber: Int
) : GameEvent

/**
 * Creature used their reaction.
 */
data class ReactionUsed(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val reactionType: String,
    val trigger: String
) : GameEvent

/**
 * Creature delayed their turn.
 */
data class TurnDelayed(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val originalInitiative: Int
) : GameEvent

/**
 * Delayed creature resumed their turn.
 */
data class DelayedTurnResumed(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val newInitiative: Int
) : GameEvent

/**
 * Creature added to combat mid-encounter.
 */
data class CreatureAddedToCombat(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val initiativeEntry: InitiativeEntry
) : GameEvent

/**
 * Creature removed from combat.
 */
data class CreatureRemovedFromCombat(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val reason: String  // "defeated", "fled", "dismissed"
) : GameEvent
```

## Error Handling

The Initiative System uses sealed result types for operations that can fail:

```kotlin
sealed interface InitiativeResult<out T> {
    data class Success<T>(val value: T) : InitiativeResult<T>
    data class InvalidState(val reason: String) : InitiativeResult<Nothing>
}
```

**Validation Rules:**
- Initiative order must not be empty
- Active creature must exist in initiative order
- Turn index must be within bounds
- Movement consumed cannot exceed movement remaining
- Cannot consume action/bonus/reaction twice in same turn
- Cannot delay turn if not active creature
- Cannot remove creature that doesn't exist

## Event Sourcing Integration

### State Derivation from Events

The Initiative System derives all state from events:

```kotlin
class InitiativeStateBuilder {
    /**
     * Rebuilds initiative state from event sequence.
     *
     * @param events Sequence of initiative-related events
     * @return Current RoundState derived from events
     */
    fun buildState(events: List<GameEvent>): RoundState {
        var state = RoundState(
            roundNumber = 0,
            isSurpriseRound = false,
            initiativeOrder = emptyList(),
            surprisedCreatures = emptySet(),
            delayedCreatures = emptyMap(),
            currentTurn = null
        )
        
        events.forEach { event ->
            state = when (event) {
                is EncounterStarted -> handleEncounterStarted(state, event)
                is RoundStarted -> handleRoundStarted(state, event)
                is TurnStarted -> handleTurnStarted(state, event)
                is TurnEnded -> handleTurnEnded(state, event)
                is ReactionUsed -> handleReactionUsed(state, event)
                is TurnDelayed -> handleTurnDelayed(state, event)
                is DelayedTurnResumed -> handleDelayedTurnResumed(state, event)
                is CreatureAddedToCombat -> handleCreatureAdded(state, event)
                is CreatureRemovedFromCombat -> handleCreatureRemoved(state, event)
                else -> state
            }
        }
        
        return state
    }
}
```

### Deterministic Replay

All initiative rolls use seeded DiceRoller, ensuring:
- Same seed → same initiative rolls
- Same events → same turn order
- Same turn progression → same round outcomes

## Testing Strategy

### Unit Tests (kotest)

**Coverage Target:** 90%+

**Test Categories:**

1. **Initiative Rolling Tests**
   - Initiative = d20 + Dexterity modifier
   - Multiple creatures sorted correctly
   - Ties broken by Dexterity modifier
   - Identical scores/modifiers use creature ID
   - Deterministic with same seed

2. **Turn Advancement Tests**
   - Turn advances to next creature
   - Last creature wraps to first and increments round
   - Surprise round skips surprised creatures
   - Surprise condition removed after surprise round

3. **Turn Phase Tests**
   - All actions available at turn start
   - Actions consumed correctly
   - Cannot consume action twice
   - Reaction restored at turn start
   - Movement tracks remaining distance

4. **Creature Lifecycle Tests**
   - Add creature inserts at correct position
   - Remove creature maintains turn order
   - Remove active creature advances turn
   - Remove creature before current turn adjusts index

5. **Delayed Turn Tests**
   - Delay removes creature from order
   - Resume inserts at current position
   - Delayed creature maintains new initiative
   - Round end places delayed creature at end

6. **Event Sourcing Tests**
   - State derived from events matches original
   - Replay produces identical state
   - All events handled exhaustively

### Property-Based Tests

```kotlin
test("initiative with same seed produces same order") {
    checkAll(Arb.long(), Arb.list(Arb.int(-5..10), 2..10)) { seed, modifiers ->
        val creatures1 = modifiers.mapIndexed { i, mod -> i.toLong() to mod }.toMap()
        val creatures2 = modifiers.mapIndexed { i, mod -> i.toLong() to mod }.toMap()
        
        val roller1 = InitiativeRoller(DiceRoller(seed))
        val order1 = roller1.rollInitiativeForAll(creatures1)
        
        val roller2 = InitiativeRoller(DiceRoller(seed))
        val order2 = roller2.rollInitiativeForAll(creatures2)
        
        order1 shouldBe order2
    }
}

test("turn advancement eventually returns to first creature") {
    checkAll(Arb.list(Arb.int(1..20), 2..10)) { initiatives ->
        val entries = initiatives.mapIndexed { i, init ->
            InitiativeEntry(i.toLong(), init, 0, init)
        }
        var state = InitiativeTracker().initialize(entries)
        
        repeat(entries.size) {
            state = InitiativeTracker().advanceTurn(state)
        }
        
        state.roundNumber shouldBe 2
        state.currentTurn?.activeCreatureId shouldBe entries.first().creatureId
    }
}
```

### Determinism Tests

```kotlin
test("event replay produces identical state") {
    val seed = 42L
    val creatures = mapOf(1L to 2, 2L to 3, 3L to 1)
    
    // Original sequence
    val roller1 = InitiativeRoller(DiceRoller(seed))
    val order1 = roller1.rollInitiativeForAll(creatures)
    val state1 = InitiativeTracker().initialize(order1)
    
    // Replay from events
    val event = EncounterStarted(1L, System.currentTimeMillis(), order1, emptySet())
    val state2 = InitiativeStateBuilder().buildState(listOf(event))
    
    state1 shouldBe state2
}
```

## Performance Considerations

**Target:** <1ms per turn advancement operation

**Optimizations:**
- Initiative order stored as immutable list (no sorting on each access)
- Turn index tracked directly (no searching)
- Creature lookup by ID uses Map for O(1) access
- No allocations in hot paths (turn advancement)
- Sealed types enable exhaustive when (no runtime checks)

## Integration Points

### With DiceRoller (04-dice-system)
- InitiativeRoller depends on DiceRoller for d20 rolls
- Must use seeded DiceRoller for deterministic initiative

### With Combat Rules Engine (05-combat-rules)
- Uses Dexterity modifier from creature abilities
- Integrates with condition effects (surprised, incapacitated)

### With Domain Entities (02-core-domain)
- Creature entity provides Dexterity modifier and movement speed
- GameEvent hierarchy includes initiative events
- RoundState and TurnState are domain value objects

### With Action Validation (06-action-validation)
- TurnPhase state used to validate action availability
- Action Validation queries TurnPhaseManager for legality checks

### With Encounter State (12-encounter-state)
- EncounterViewModel uses InitiativeTracker for turn management
- RoundState exposed in EncounterUiState for UI rendering

## Design Decisions

### Why Separate InitiativeRoller and InitiativeTracker?
InitiativeRoller handles the one-time setup (rolling initiative), while InitiativeTracker manages ongoing turn progression. This separation of concerns makes each component simpler and more testable.

### Why Immutable State Classes?
Immutable state enables event sourcing, simplifies concurrency, and makes state changes explicit. All state transitions return new instances rather than mutating existing state.

### Why Track Turn Index?
Tracking the current turn index avoids searching the initiative order on every turn advancement, improving performance and simplifying the logic.

### Why Separate TurnPhaseManager?
Action economy is complex enough to warrant its own component. Separating it allows focused testing and reuse in action validation.

### Why Support Delayed Turns?
Delayed turns are a standard D&D 5e mechanic that adds tactical depth. Supporting them requires tracking delayed creatures separately from the main initiative order.

## Future Enhancements (Out of Scope for v1)

- Lair actions (special actions at initiative count 20)
- Legendary actions (actions between turns)
- Ready action triggers (conditional actions)
- Initiative reroll (e.g., from Alert feat)
- Group initiative (rolling once for groups of creatures)
- Initiative modifiers from spells/effects (e.g., Haste)
