package dev.questweaver.data.repositories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.questweaver.data.db.AppDatabase
import dev.questweaver.data.db.dao.EventDao
import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.MoveCommitted
import dev.questweaver.domain.values.DiceRoll
import dev.questweaver.domain.values.GridPos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for EventRepositoryImpl using in-memory Room database.
 * 
 * Verifies event insertion, retrieval, ordering, and reactive observation
 * through the repository layer with actual database operations.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@RunWith(AndroidJUnit4::class)
class EventRepositoryImplTest {
    
    private lateinit var database: AppDatabase
    private lateinit var eventDao: EventDao
    private lateinit var repository: EventRepositoryImpl
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        
        eventDao = database.eventDao()
        repository = EventRepositoryImpl(eventDao)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveSingleEvent() = runBlocking {
        val event = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        
        repository.append(event)
        
        val retrieved = repository.forSession(1L)
        assertEquals(1, retrieved.size)
        assertEquals(event, retrieved.first())
    }
    
    @Test
    fun retrieveEmptyListForNonExistentSession() = runBlocking {
        val retrieved = repository.forSession(999L)
        assertTrue(retrieved.isEmpty())
    }
    
    @Test
    fun insertMultipleEventsForSameSession() = runBlocking {
        val event1 = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        val event2 = createMoveEvent(sessionId = 1L, timestamp = 2000L)
        val event3 = createEncounterEvent(sessionId = 1L, timestamp = 3000L)
        
        repository.append(event1)
        repository.append(event2)
        repository.append(event3)
        
        val retrieved = repository.forSession(1L)
        assertEquals(3, retrieved.size)
    }
    
    @Test
    fun isolateEventsBySessionId() = runBlocking {
        val session1Event = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        val session2Event = createAttackEvent(sessionId = 2L, timestamp = 1000L)
        
        repository.append(session1Event)
        repository.append(session2Event)
        
        val session1Events = repository.forSession(1L)
        val session2Events = repository.forSession(2L)
        
        assertEquals(1, session1Events.size)
        assertEquals(1, session2Events.size)
        assertEquals(session1Event, session1Events.first())
        assertEquals(session2Event, session2Events.first())
    }
    
    @Test
    fun maintainChronologicalOrderWhenInsertedInOrder() = runBlocking {
        val event1 = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        val event2 = createMoveEvent(sessionId = 1L, timestamp = 2000L)
        val event3 = createEncounterEvent(sessionId = 1L, timestamp = 3000L)
        
        repository.append(event1)
        repository.append(event2)
        repository.append(event3)
        
        val retrieved = repository.forSession(1L)
        assertEquals(listOf(event1, event2, event3), retrieved)
    }
    
    @Test
    fun sortByTimestampWhenInsertedOutOfOrder() = runBlocking {
        val event1 = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        val event2 = createMoveEvent(sessionId = 1L, timestamp = 2000L)
        val event3 = createEncounterEvent(sessionId = 1L, timestamp = 3000L)
        
        repository.append(event3)
        repository.append(event1)
        repository.append(event2)
        
        val retrieved = repository.forSession(1L)
        assertEquals(listOf(event1, event2, event3), retrieved)
    }
    
    @Test
    fun emitInitialEmptyListForNonExistentSession() = runBlocking {
        val flow = repository.observeSession(1L)
        val initial = flow.first()
        
        assertTrue(initial.isEmpty())
    }
    
    @Test
    fun emitUpdatedListWhenNewEventInserted() = runBlocking {
        val event1 = createAttackEvent(sessionId = 1L, timestamp = 1000L)
        
        repository.append(event1)
        
        val flow = repository.observeSession(1L)
        val emitted = flow.first()
        
        assertEquals(1, emitted.size)
        assertEquals(event1, emitted.first())
    }
    
    @Test
    fun batchInsertMultipleEvents() = runBlocking {
        val events = listOf(
            createAttackEvent(sessionId = 1L, timestamp = 1000L),
            createMoveEvent(sessionId = 1L, timestamp = 2000L),
            createEncounterEvent(sessionId = 1L, timestamp = 3000L)
        )
        
        repository.appendAll(events)
        
        val retrieved = repository.forSession(1L)
        assertEquals(3, retrieved.size)
        assertEquals(events, retrieved)
    }
    
    @Test
    fun handleEmptyBatchInsertion() = runBlocking {
        repository.appendAll(emptyList())
        
        val retrieved = repository.forSession(1L)
        assertTrue(retrieved.isEmpty())
    }
    
    private fun createAttackEvent(sessionId: Long, timestamp: Long): AttackResolved {
        return AttackResolved(
            sessionId = sessionId,
            timestamp = timestamp,
            attackerId = 1L,
            targetId = 2L,
            attackRoll = DiceRoll(d20 = 15, modifier = 5, total = 20),
            targetAC = 18,
            hit = true,
            critical = false
        )
    }
    
    private fun createMoveEvent(sessionId: Long, timestamp: Long): MoveCommitted {
        return MoveCommitted(
            sessionId = sessionId,
            timestamp = timestamp,
            creatureId = 1L,
            fromPos = GridPos(0, 0),
            toPos = GridPos(5, 5),
            path = listOf(GridPos(0, 0), GridPos(5, 5)),
            movementCost = 5
        )
    }
    
    private fun createEncounterEvent(sessionId: Long, timestamp: Long): EncounterStarted {
        return EncounterStarted(
            sessionId = sessionId,
            timestamp = timestamp,
            encounterId = 1L,
            participants = listOf(1L, 2L),
            initiativeOrder = emptyList()
        )
    }
}
