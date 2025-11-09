package dev.questweaver.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val idx: Int,
    val type: String,
    val payload: String,
    val ts: Long
)
