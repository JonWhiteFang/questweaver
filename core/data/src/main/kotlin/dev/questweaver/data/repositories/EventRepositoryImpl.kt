package dev.questweaver.data.repositories

import dev.questweaver.data.db.dao.EventDao
import dev.questweaver.data.db.entities.EventEntity
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.repositories.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Implementation of EventRepository using Room database for persistence.
 * 
 * This implementation provides event-sourced storage with automatic mapping between
 * domain GameEvent objects and Room EventEntity objects. All database operations
 * are performed through the EventDao, with serialization handled via kotlinx-serialization.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
class EventRepositoryImpl(
    private val eventDao: EventDao
) : EventRepository {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "type"
    }
    
    /**
     * Appends a single event to the event store.
     * 
     * Converts the domain GameEvent to an EventEntity and persists it to the database.
     * 
     * @param event The game event to persist
     * @throws SerializationException if the event cannot be serialized
     */
    override suspend fun append(event: GameEvent) {
        val entity = event.toEntity()
        eventDao.insert(entity)
    }
    
    /**
     * Appends multiple events to the event store in a single transaction.
     * 
     * Room automatically wraps the insertAll operation in a transaction,
     * ensuring atomicity - either all events are persisted or none are.
     * 
     * @param events The list of game events to persist
     * @throws SerializationException if any event cannot be serialized
     */
    override suspend fun appendAll(events: List<GameEvent>) {
        val entities = events.map { it.toEntity() }
        eventDao.insertAll(entities)
    }
    
    /**
     * Retrieves all events for a specific game session.
     * 
     * Events are returned in chronological order (by timestamp ascending).
     * 
     * @param sessionId The unique identifier of the game session
     * @return List of all events for the session, ordered by timestamp
     * @throws SerializationException if any event cannot be deserialized
     */
    override suspend fun forSession(sessionId: Long): List<GameEvent> {
        return eventDao.getBySession(sessionId).map { it.toDomain() }
    }
    
    /**
     * Observes events for a specific game session reactively.
     * 
     * The returned Flow emits a new list of events whenever the database changes
     * (events inserted, updated, or deleted for this session). Events are always
     * ordered chronologically by timestamp.
     * 
     * @param sessionId The unique identifier of the game session
     * @return Flow emitting the current list of events whenever changes occur
     * @throws SerializationException if any event cannot be deserialized
     */
    override fun observeSession(sessionId: Long): Flow<List<GameEvent>> {
        return eventDao.observeBySession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    /**
     * Extension function to convert a GameEvent domain object to an EventEntity.
     * 
     * Extracts the event type discriminator from the class name and serializes
     * the full event to JSON for storage.
     * 
     * Requirements: 3.3, 3.4, 4.5
     */
    private fun GameEvent.toEntity(): EventEntity {
        val eventType = this::class.simpleName ?: "Unknown"
        val eventData = json.encodeToString(GameEvent.serializer(), this)
        
        return EventEntity(
            sessionId = this.sessionId,
            timestamp = this.timestamp,
            eventType = eventType,
            eventData = eventData
        )
    }
    
    /**
     * Extension function to convert an EventEntity to a GameEvent domain object.
     * 
     * Deserializes the JSON event data back to the appropriate GameEvent subtype
     * using kotlinx-serialization's polymorphic deserialization.
     * 
     * Requirements: 3.3, 3.4, 4.5
     */
    private fun EventEntity.toDomain(): GameEvent {
        return json.decodeFromString(GameEvent.serializer(), this.eventData)
    }
}
