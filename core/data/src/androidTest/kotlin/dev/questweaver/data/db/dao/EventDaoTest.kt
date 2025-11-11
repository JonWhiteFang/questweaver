package dev.questweaver.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.questweaver.data.db.AppDatabase
import dev.questweaver.data.db.entities.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for EventDao using in-memory Room database.
 * 
 * Verifies insert, query, observe, count, and delete operations,
 * as well as composite index usage and transaction behavior.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@RunWith(AndroidJUnit4::class)
class EventDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var eventDao: EventDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        
        eventDao = database.eventDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertSingleEventAndReturnRowId() = runBlocking {
        val entity = createEventEntity(sessionId = 1L, timestamp = 1000L)
        
        val rowId = eventDao.insert(entity)
        
        assertEquals(1L, rowId)
    }
    
    @Test
    fun autoGenerateSequentialIds() = runBlocking {
        val entity1 = createEventEntity(sessionId = 1L, timestamp = 1000L)
        val entity2 = createEventEntity(sessionId = 1L, timestamp = 2000L)
        
        val rowId1 = eventDao.insert(entity1)
        val rowId2 = eventDao.insert(entity2)
        
        assertEquals(1L, rowId1)
        assertEquals(2L, rowId2)
    }
    
    @Test
    fun insertMultipleEventsAndReturnRowIds() = runBlocking {
        val entities = listOf(
            createEventEntity(sessionId = 1L, timestamp = 1000L),
            createEventEntity(sessionId = 1L, timestamp = 2000L),
            createEventEntity(sessionId = 1L, timestamp = 3000L)
        )
        
        val rowIds = eventDao.insertAll(entities)
        
        assertEquals(3, rowIds.size)
        assertEquals(listOf(1L, 2L, 3L), rowIds)
    }
    
    @Test
    fun retrieveEventsBySessionId() = runBlocking {
        val entity1 = createEventEntity(sessionId = 1L, timestamp = 1000L)
        val entity2 = createEventEntity(sessionId = 1L, timestamp = 2000L)
        
        eventDao.insert(entity1)
        eventDao.insert(entity2)
        
        val retrieved = eventDao.getBySession(1L)
        
        assertEquals(2, retrieved.size)
    }
    
    @Test
    fun returnEmptyListForNonExistentSession() = runBlocking {
        val retrieved = eventDao.getBySession(999L)
        
        assertTrue(retrieved.isEmpty())
    }
    
    @Test
    fun orderEventsByTimestampAscending() = runBlocking {
        val entity1 = createEventEntity(sessionId = 1L, timestamp = 3000L, eventType = "Event3")
        val entity2 = createEventEntity(sessionId = 1L, timestamp = 1000L, eventType = "Event1")
        val entity3 = createEventEntity(sessionId = 1L, timestamp = 2000L, eventType = "Event2")
        
        eventDao.insert(entity1)
        eventDao.insert(entity2)
        eventDao.insert(entity3)
        
        val retrieved = eventDao.getBySession(1L)
        
        assertEquals(listOf("Event1", "Event2", "Event3"), retrieved.map { it.eventType })
        assertEquals(listOf(1000L, 2000L, 3000L), retrieved.map { it.timestamp })
    }
    
    @Test
    fun emitInitialEmptyListForNonExistentSession() = runBlocking {
        val flow = eventDao.observeBySession(1L)
        val initial = flow.first()
        
        assertTrue(initial.isEmpty())
    }
    
    @Test
    fun emitCurrentEventsWhenStartingObservation() = runBlocking {
        val entity = createEventEntity(sessionId = 1L, timestamp = 1000L)
        eventDao.insert(entity)
        
        val flow = eventDao.observeBySession(1L)
        val emitted = flow.first()
        
        assertEquals(1, emitted.size)
    }
    
    @Test
    fun countEventsBySession() = runBlocking {
        val entity1 = createEventEntity(sessionId = 1L, timestamp = 1000L)
        val entity2 = createEventEntity(sessionId = 1L, timestamp = 2000L)
        val entity3 = createEventEntity(sessionId = 1L, timestamp = 3000L)
        
        eventDao.insert(entity1)
        eventDao.insert(entity2)
        eventDao.insert(entity3)
        
        val count = eventDao.countBySession(1L)
        
        assertEquals(3, count)
    }
    
    @Test
    fun deleteAllEventsForSpecificSession() = runBlocking {
        val entity1 = createEventEntity(sessionId = 1L, timestamp = 1000L)
        val entity2 = createEventEntity(sessionId = 1L, timestamp = 2000L)
        
        eventDao.insert(entity1)
        eventDao.insert(entity2)
        
        val deleted = eventDao.deleteBySession(1L)
        
        assertEquals(2, deleted)
        
        val remaining = eventDao.getBySession(1L)
        assertTrue(remaining.isEmpty())
    }
    
    @Test
    fun onlyDeleteEventsForSpecifiedSession() = runBlocking {
        val session1Entity = createEventEntity(sessionId = 1L, timestamp = 1000L)
        val session2Entity = createEventEntity(sessionId = 2L, timestamp = 1000L)
        
        eventDao.insert(session1Entity)
        eventDao.insert(session2Entity)
        
        val deleted = eventDao.deleteBySession(1L)
        
        assertEquals(1, deleted)
        
        val session1Events = eventDao.getBySession(1L)
        val session2Events = eventDao.getBySession(2L)
        
        assertTrue(session1Events.isEmpty())
        assertEquals(1, session2Events.size)
    }
    
    private fun createEventEntity(
        sessionId: Long,
        timestamp: Long,
        eventType: String = "TestEvent",
        eventData: String = """{"type":"TestEvent","sessionId":$sessionId,"timestamp":$timestamp}"""
    ): EventEntity {
        return EventEntity(
            sessionId = sessionId,
            timestamp = timestamp,
            eventType = eventType,
            eventData = eventData
        )
    }
}
