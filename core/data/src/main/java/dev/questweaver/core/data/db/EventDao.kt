package dev.questweaver.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY idx ASC")
    suspend fun forSession(sessionId: Long): List<EventEntity>

    @Insert
    suspend fun insertAll(events: List<EventEntity>)
}
