---
inclusion: fileMatch
fileMatchPattern: ['**/*Event*.kt', '**/*Serializ*.kt', '**/EventEntity.kt']
---

# Kotlinx Serialization for Event Sourcing

## Mandatory Rules

1. **Always use `@SerialName`** - Never rely on class names for serialization
2. **Mark sealed hierarchies** - Both parent and all children need `@Serializable`
3. **Capture intent + outcome** - Events must include context, not just final state
4. **Use snake_case for serial names** - Consistent with JSON conventions
5. **Test round-trip serialization** - Encode then decode must equal original

## Sealed Event Hierarchy Pattern

```kotlin
@Serializable
sealed interface GameEvent {
    val sessionId: Long
    val timestamp: Long
}

@Serializable
@SerialName("encounter_started")  // REQUIRED: Stable identifier
data class EncounterStarted(
    override val sessionId: Long,
    override val timestamp: Long,
    val mapId: Long,
    val participants: List<Long>,
    val seed: Long
) : GameEvent
```

**Why `@SerialName`?**
- Refactoring class names won't break saved data
- Old events remain loadable after code changes
- Human-readable event types in JSON/database

## Event Design Best Practices

```kotlin
// ✅ GOOD: Captures full context for replay
@Serializable
@SerialName("attack_resolved")
data class AttackResolved(
    override val sessionId: Long,
    override val timestamp: Long,
    val attackerId: Long,
    val targetId: Long,
    val roll: Int,
    val modifiers: Int,
    val hit: Boolean,
    val damage: Int?
) : GameEvent

// ❌ BAD: Only captures outcome, loses context
@Serializable
@SerialName("hp_changed")
data class HPChanged(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val newHP: Int
) : GameEvent
```

## Event Versioning Strategies

**Option 1: Optional fields (preferred for minor changes)**
```kotlin
@Serializable
@SerialName("move_committed")
data class MoveCommitted(
    override val sessionId: Long,
    override val timestamp: Long,
    val creatureId: Long,
    val path: List<GridPos>,
    val cost: Int,
    val isDiagonal: Boolean = false  // Added in v2, defaults for old events
) : GameEvent
```

**Option 2: New event types (for breaking changes)**
```kotlin
@Serializable
@SerialName("move_v1")
data class MoveCommittedV1(...) : GameEvent

@Serializable
@SerialName("move_v2")
data class MoveCommittedV2(...) : GameEvent
```

## JSON Configuration

```kotlin
// For event storage (compact, forward-compatible)
val eventJson = Json {
    ignoreUnknownKeys = true    // Load old events with new code
    encodeDefaults = false      // Minimize storage size
    prettyPrint = false         // Compact storage
    classDiscriminator = "type" // Default, explicit for clarity
}

// For debugging (human-readable)
val debugJson = Json {
    prettyPrint = true
    encodeDefaults = true
}
```

## Room Database Integration

```kotlin
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val idx: Int,
    val type: String,    // Event discriminator for queries
    val payload: String, // Full JSON via kotlinx-serialization
    val ts: Long
)

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

## Error Handling

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

## Testing Requirements

```kotlin
class EventSerializationTest : FunSpec({
    val json = Json { prettyPrint = true }
    
    test("round-trip serialization preserves data") {
        val event = EncounterStarted(
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
    
    test("forward compatibility ignores new fields") {
        val jsonWithNewField = """
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
        
        val decoded = json.decodeFromString<GameEvent>(jsonWithNewField)
        decoded shouldBeInstanceOf MoveCommitted::class
    }
})
```

## Quick Reference

**Every event class needs:**
1. `@Serializable` annotation
2. `@SerialName("snake_case_name")` annotation
3. Override `sessionId` and `timestamp` from `GameEvent`
4. All fields needed to replay the event

**JSON config for storage:**
```kotlin
Json { ignoreUnknownKeys = true; encodeDefaults = false; prettyPrint = false }
```

**Testing checklist:**
- Round-trip serialization (encode → decode → equals original)
- Forward compatibility (old code loads new events with extra fields)
- Backward compatibility (new code loads old events with missing optional fields)
