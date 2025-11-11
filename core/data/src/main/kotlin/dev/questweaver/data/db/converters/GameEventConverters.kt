package dev.questweaver.data.db.converters

import androidx.room.TypeConverter
import dev.questweaver.domain.events.GameEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Room type converters for serializing and deserializing polymorphic GameEvent types.
 * 
 * Uses kotlinx-serialization to handle the sealed interface hierarchy with proper
 * type discrimination. The Json configuration ensures forward compatibility by
 * ignoring unknown keys and reduces storage size by not encoding default values.
 */
class GameEventConverters {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "type"
    }
    
    /**
     * Converts a GameEvent domain object to a JSON string for database storage.
     * 
     * @param event The GameEvent to serialize
     * @return JSON string representation of the event
     * @throws SerializationException if the event cannot be serialized
     */
    @TypeConverter
    fun fromGameEvent(event: GameEvent): String {
        return try {
            json.encodeToString(GameEvent.serializer(), event)
        } catch (e: SerializationException) {
            throw SerializationException(
                "Failed to serialize GameEvent of type ${event::class.simpleName}: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Converts a JSON string from the database back to a GameEvent domain object.
     * 
     * @param eventData The JSON string to deserialize
     * @return The deserialized GameEvent instance
     * @throws SerializationException if the JSON cannot be deserialized
     */
    @TypeConverter
    fun toGameEvent(eventData: String): GameEvent {
        return try {
            json.decodeFromString(GameEvent.serializer(), eventData)
        } catch (e: SerializationException) {
            throw SerializationException(
                "Failed to deserialize GameEvent from JSON: ${e.message}",
                e
            )
        }
    }
}
