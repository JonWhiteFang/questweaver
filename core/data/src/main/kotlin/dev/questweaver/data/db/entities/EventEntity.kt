package dev.questweaver.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted GameEvent.
 * 
 * This entity stores events in an event-sourced architecture where all state mutations
 * are captured as immutable events. The composite index on (session_id, timestamp)
 * optimizes session replay queries, while the single session_id index supports
 * count and delete operations.
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["session_id", "timestamp"], name = "index_events_session_id_timestamp"),
        Index(value = ["session_id"], name = "index_events_session_id")
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "event_type")
    val eventType: String,
    
    @ColumnInfo(name = "event_data")
    val eventData: String
)
