package dev.questweaver.domain.repositories

import dev.questweaver.domain.events.GameEvent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing game events in an event-sourced architecture.
 * 
 * All state mutations in the game produce immutable GameEvent instances that are
 * persisted through this repository for replay capability and audit trails.
 */
interface EventRepository {
    /**
     * Appends a single event to the event store.
     *
     * @param event The game event to persist
     */
    suspend fun append(event: GameEvent)
    
    /**
     * Appends multiple events to the event store in a single operation.
     *
     * @param events The list of game events to persist
     */
    suspend fun appendAll(events: List<GameEvent>)
    
    /**
     * Retrieves all events for a specific game session.
     *
     * @param sessionId The unique identifier of the game session
     * @return List of all events for the session, ordered by timestamp
     */
    suspend fun forSession(sessionId: Long): List<GameEvent>
    
    /**
     * Observes events for a specific game session reactively.
     *
     * @param sessionId The unique identifier of the game session
     * @return Flow emitting the current list of events whenever changes occur
     */
    fun observeSession(sessionId: Long): Flow<List<GameEvent>>
}
