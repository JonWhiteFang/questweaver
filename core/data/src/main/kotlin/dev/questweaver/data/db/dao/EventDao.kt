package dev.questweaver.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.questweaver.data.db.entities.EventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for EventEntity operations.
 * 
 * Provides Room database operations for event-sourced persistence, including
 * single and batch insertions, session-based queries, and reactive observation
 * via Flow. All queries leverage composite indices for optimal performance.
 * 
 * Requirements: 3.2, 3.3
 */
@Dao
interface EventDao {
    
    /**
     * Inserts a single event into the database.
     * 
     * @param event The event entity to insert
     * @return The row ID of the inserted event
     * @throws SQLiteConstraintException if a conflict occurs (ABORT strategy)
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: EventEntity): Long
    
    /**
     * Inserts multiple events in a single transaction.
     * 
     * This method is transactional by default in Room, ensuring all events
     * are inserted atomically or none at all.
     * 
     * @param events List of event entities to insert
     * @return List of row IDs for the inserted events
     * @throws SQLiteConstraintException if any conflict occurs (ABORT strategy)
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<EventEntity>): List<Long>
    
    /**
     * Retrieves all events for a given session, ordered by timestamp.
     * 
     * Uses the composite index (session_id, timestamp) for optimal query performance.
     * 
     * @param sessionId The session ID to query events for
     * @return List of events ordered chronologically
     */
    @Query("SELECT * FROM events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<EventEntity>
    
    /**
     * Observes events for a given session reactively via Flow.
     * 
     * Emits a new list whenever events are inserted, updated, or deleted for the session.
     * Uses the composite index (session_id, timestamp) for optimal query performance.
     * 
     * @param sessionId The session ID to observe events for
     * @return Flow emitting lists of events ordered chronologically
     */
    @Query("SELECT * FROM events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: Long): Flow<List<EventEntity>>
    
    /**
     * Counts the number of events for a given session.
     * 
     * Uses the single session_id index for optimal count performance.
     * 
     * @param sessionId The session ID to count events for
     * @return The number of events in the session
     */
    @Query("SELECT COUNT(*) FROM events WHERE session_id = :sessionId")
    suspend fun countBySession(sessionId: Long): Int
    
    /**
     * Deletes all events for a given session.
     * 
     * Uses the single session_id index for optimal delete performance.
     * 
     * @param sessionId The session ID to delete events for
     * @return The number of events deleted
     */
    @Query("DELETE FROM events WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long): Int
}
