---
inclusion: fileMatch
fileMatchPattern: '**/*Event*.kt'
---

# Kotlinx Serialization Patterns for Event Sourcing

## Sealed Class Serialization

### Basic Setup
```kotlin
@Serializable
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

@Serializable
@SerialName("encounter_started")
data class EncounterStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val mapId: Long,
    val participants: List<Long>,
    val seed: Long
) : GameEvent

@Serializable
@SerialName("move_committed")
data class MoveCommitted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val path: List<GridPos>,
    val cost: Int
) : GameEvent
```

**Key Points:**
- Mark sealed hierarchy with `@Serializable`
- Use `@SerialName` for stable serialization (independent of class names)
- All subclasses must be `@Serializable`
- Automatically adds `type` discriminator to JSON

### JSON Output
```json
{
  "type": "encounter_started",
  "sessionId": 1,
  "timestamp": 1699564800000,
  "mapId": 42,
  "participants": [1, 2, 3],
  "seed": 12345
}
```

## Custom Serial Names

### Why Use @SerialName?
```kotlin
// ❌ Bad: Refactoring class name breaks deserialization
@Serializable
data class PlayerMoveAction(...) : GameEvent

// ✅ Good: Stable serial name survives refactoring
@Serializable
@SerialName("player_move")
data class PlayerMoveAction(...) : GameEvent
```

**Benefits:**
- Refactor-safe: Rename classes without breaking saved data
- Version-stable: Old saves can still be loaded
- Human-readable: Clear event types in JSON

## Polymorphic Serialization

### For Abstract Classes (Not Sealed)
```kotlin
@Serializable
abstract class Action {
    abstract val actorId: Long
}

@Serializable
@SerialName("attack")
data class AttackAction(
    override val actorId: Long,
    val targetId: Long,
    val weaponId: Long
) : Action()

@Serializable
@SerialName("cast_spell")
data class CastSpellAction(
    override val actorId: Long,
    val spellId: Long,
    val targets: List<Long>
) : Action()

// Register subclasses in SerializersModule
val actionModule = SerializersModule {
    polymorphic(Action::class) {
        subclass(AttackAction::class)
        subclass(CastSpellAction::class)
    }
}

val json = Json {
    serializersModule = actionModule
}
```

## Serializing Objects in Hierarchies

```kotlin
@Serializable
sealed interface Response

@Serializable
@SerialName("empty")
object EmptyResponse : Response

@Serializable
@SerialName("text")
data class TextResponse(val text: String) : Response

// Objects serialize as empty classes
val responses = listOf(EmptyResponse, TextResponse("OK"))
println(Json.encodeToString(responses))
// [{"type":"empty"},{"type":"text","text":"OK"}]
```

## Concrete Properties in Base Class

```kotlin
@Serializable
sealed class GameEvent {
    abstract val sessionId: Long
    var metadata: String = "" // Concrete property with backing field
}

@Serializable
@SerialName("damage_dealt")
data class DamageDealt(
    override val sessionId: Long,
    val targetId: Long,
    val amount: Int
) : GameEvent()

// Concrete properties serialize before subclass properties
val json = Json { encodeDefaults = true }
val event = DamageDealt(1, 2, 10).apply { metadata = "critical" }
println(json.encodeToString(event))
// {"type":"damage_dealt","metadata":"critical","sessionId":1,"targetId":2,"amount":10}
```

## Handling Nothing Type Parameter

```kotlin
@Serializable
sealed class Result<out T> {
    @Serializable
    @SerialName("success")
    data class Success<T>(val value: T) : Result<T>()
    
    @Serializable
    @SerialName("error")
    data class Error(val message: String) : Result<Nothing>()
}

// Nothing is used when no type parameter is needed
val error: Result<Nothing> = Result.Error("Not found")
println(Json.encodeToString(error))
// {"type":"error","message":"Not found"}
```

## Event Sourcing Best Practices

### Event Design
```kotlin
// ✅ Good: Captures intent and outcome
@Serializable
@SerialName("attack_resolved")
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val modifiers: Int,
    val advantage: Advantage,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// ❌ Bad: Only captures outcome
@Serializable
@SerialName("hp_changed")
data class HPChanged(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val newHP: Int
) : GameEvent
```

### Versioning Events

#### Option 1: New Event Types
```kotlin
@Serializable
@SerialName("move_v1")
data class MoveCommittedV1(...) : GameEvent

@Serializable
@SerialName("move_v2")
data class MoveCommittedV2(...) : GameEvent
```

#### Option 2: Optional Fields
```kotlin
@Serializable
@SerialName("move")
data class MoveCommitted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val path: List<GridPos>,
    val cost: Int,
    val isDiagonal: Boolean = false // Added in v2
) : GameEvent
```

## JSON Configuration

### Recommended Settings
```kotlin
val json = Json {
    // Ignore unknown fields (forward compatibility)
    ignoreUnknownKeys = true
    
    // Pretty print for debugging
    prettyPrint = true
    
    // Encode default values
    encodeDefaults = false // Save space
    
    // Allow structured map keys
    allowStructuredMapKeys = true
    
    // Class discriminator
    classDiscriminator = "type"
}
```

### For Event Storage
```kotlin
val eventJson = Json {
    ignoreUnknownKeys = true // Load old events with new code
    encodeDefaults = false // Minimize storage
    prettyPrint = false // Compact storage
}
```

## Retrofit Integration

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com")
    .addConverterFactory(
        json.asConverterFactory("application/json".toMediaType())
    )
    .build()
```

**Note:** Add kotlinx-serialization converter last if mixing with other converters.

## Error Handling

### Deserialization Errors
```kotlin
try {
    val event = json.decodeFromString<GameEvent>(jsonString)
} catch (e: SerializationException) {
    logger.error { "Failed to deserialize event: ${e.message}" }
    // Handle unknown event type or malformed JSON
} catch (e: IllegalArgumentException) {
    logger.error { "Invalid event data: ${e.message}" }
    // Handle validation errors
}
```

### Custom Serializers for Complex Types
```kotlin
object GridPosSerializer : KSerializer<GridPos> {
    override val descriptor = PrimitiveSerialDescriptor("GridPos", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: GridPos) {
        encoder.encodeString("${value.x},${value.y}")
    }
    
    override fun deserialize(decoder: Decoder): GridPos {
        val (x, y) = decoder.decodeString().split(",").map { it.toInt() }
        return GridPos(x, y)
    }
}

@Serializable
data class GridPos(
    val x: Int,
    val y: Int
) {
    companion object {
        @Serializer(forClass = GridPos::class)
        val serializer = GridPosSerializer
    }
}
```

## Testing Serialization

```kotlin
class EventSerializationTest : FunSpec({
    val json = Json { prettyPrint = true }
    
    test("EncounterStarted serializes correctly") {
        val event = GameEvent.EncounterStarted(
            sessionId = 1,
            timestamp = 1699564800000,
            mapId = 42,
            participants = listOf(1, 2, 3),
            seed = 12345
        )
        
        val jsonString = json.encodeToString(event)
        val decoded = json.decodeFromString<GameEvent>(jsonString)
        
        decoded shouldBe event
    }
    
    test("unknown event type is ignored") {
        val jsonString = """
            {
                "type": "unknown_event",
                "sessionId": 1,
                "timestamp": 1699564800000
            }
        """.trimIndent()
        
        shouldThrow<SerializationException> {
            json.decodeFromString<GameEvent>(jsonString)
        }
    }
    
    test("forward compatibility with new fields") {
        val jsonString = """
            {
                "type": "move_committed",
                "sessionId": 1,
                "timestamp": 1699564800000,
                "creatureId": 5,
                "path": [{"x":0,"y":0}],
                "cost": 5,
                "newField": "ignored"
            }
        """.trimIndent()
        
        val decoded = json.decodeFromString<GameEvent>(jsonString)
        decoded shouldBeInstanceOf GameEvent.MoveCommitted::class
    }
})
```

## QuestWeaver Event Hierarchy

```kotlin
@Serializable
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
    
    // Combat Events
    @Serializable @SerialName("encounter_started")
    data class EncounterStarted(...) : GameEvent
    
    @Serializable @SerialName("turn_advanced")
    data class TurnAdvanced(...) : GameEvent
    
    @Serializable @SerialName("move_committed")
    data class MoveCommitted(...) : GameEvent
    
    @Serializable @SerialName("attack_rolled")
    data class AttackRolled(...) : GameEvent
    
    @Serializable @SerialName("damage_applied")
    data class DamageApplied(...) : GameEvent
    
    @Serializable @SerialName("condition_applied")
    data class ConditionApplied(...) : GameEvent
    
    @Serializable @SerialName("spell_cast")
    data class SpellCast(...) : GameEvent
    
    // Narrative Events
    @Serializable @SerialName("journal_added")
    data class JournalAdded(...) : GameEvent
    
    @Serializable @SerialName("dialogue_started")
    data class DialogueStarted(...) : GameEvent
    
    // Campaign Events
    @Serializable @SerialName("campaign_created")
    data class CampaignCreated(...) : GameEvent
    
    @Serializable @SerialName("session_started")
    data class SessionStarted(...) : GameEvent
}
```

## Storage in Room

```kotlin
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val idx: Int,
    val type: String, // Extracted from @SerialName
    val payload: String, // JSON via kotlinx-serialization
    val ts: Long
)

// Converter
class EventConverter(private val json: Json) {
    fun toEntity(event: GameEvent, idx: Int): EventEntity {
        return EventEntity(
            sessionId = event.sessionId,
            idx = idx,
            type = event::class.simpleName ?: "Unknown",
            payload = json.encodeToString(event),
            ts = event.timestamp
        )
    }
    
    fun fromEntity(entity: EventEntity): GameEvent {
        return json.decodeFromString(entity.payload)
    }
}
```
