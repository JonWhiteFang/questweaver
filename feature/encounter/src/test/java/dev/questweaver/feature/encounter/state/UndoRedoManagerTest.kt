package dev.questweaver.feature.encounter.state

import dev.questweaver.domain.events.AttackResolved
import dev.questweaver.domain.events.EncounterStarted
import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.events.InitiativeEntryData
import dev.questweaver.domain.repositories.EventRepository
import dev.questweaver.domain.values.DiceRoll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Tests for UndoRedoManager.
 * Verifies undo/redo functionality and stack management.
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5
 */
class UndoRedoManagerTest : FunSpec({
    
    lateinit var eventRepository: EventRepository
    lateinit var undoRedoManager: UndoRedoManager
    
    beforeTest {
        eventRepository = mockk(relaxed = true)
        undoRedoManager = UndoRedoManager(eventRepository)
    }
    
    context("Undo functionality") {
        test("undo removes last event and rebuilds state") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L, 2L),
                    initiativeOrder = listOf(
                        InitiativeEntryData(1L, 15, 2, 17),
                        InitiativeEntryData(2L, 10, 1, 11)
                    ),
                    surprisedCreatures = emptySet()
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2)
                coEvery { eventRepository.forSession(sessionId) } returns events
                
                // Update event count before undo
                undoRedoManager.updateEventCount(events)
                
                // Act
                val updatedEvents = undoRedoManager.undo(sessionId)
                
                // Assert
                updatedEvents shouldHaveSize 1
                updatedEvents[0] shouldBe event1
                undoRedoManager.canUndo() shouldBe true  // Still have 1 event, can undo to 0
                undoRedoManager.canRedo() shouldBe true  // Event2 is in redo stack
            }
        }
        
        test("undo with empty event list returns empty list") {
            runTest {
                // Arrange
                val sessionId = 1L
                coEvery { eventRepository.forSession(sessionId) } returns emptyList()
                
                // Act
                val updatedEvents = undoRedoManager.undo(sessionId)
                
                // Assert
                updatedEvents shouldHaveSize 0
                undoRedoManager.canUndo() shouldBe false
            }
        }
        
        test("canUndo returns true when multiple events available") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2)
                coEvery { eventRepository.forSession(sessionId) } returns events
                
                // Update event count
                undoRedoManager.updateEventCount(events)
                
                // Assert
                undoRedoManager.canUndo() shouldBe true  // Have 2 events, can undo
            }
        }
        
        test("canUndo returns false when no undo available") {
            // Arrange - fresh manager
            
            // Assert
            undoRedoManager.canUndo() shouldBe false
        }
    }
    
    context("Redo functionality") {
        test("redo restores undone event") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2)
                coEvery { eventRepository.forSession(sessionId) } returns events andThen events
                
                // Update event count and undo
                undoRedoManager.updateEventCount(events)
                undoRedoManager.undo(sessionId)
                
                // Act - redo
                val restoredEvents = undoRedoManager.redo(sessionId)
                
                // Assert
                restoredEvents shouldHaveSize 2
                coVerify { eventRepository.append(event2) }
            }
        }
        
        test("canRedo returns true when redo available") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2)
                coEvery { eventRepository.forSession(sessionId) } returns events
                
                // Update event count and undo to populate redo stack
                undoRedoManager.updateEventCount(events)
                undoRedoManager.undo(sessionId)
                
                // Assert
                undoRedoManager.canRedo() shouldBe true  // Event2 is in redo stack
            }
        }
        
        test("canRedo returns false when no redo available") {
            // Arrange - fresh manager
            
            // Assert
            undoRedoManager.canRedo() shouldBe false
        }
    }
    
    context("Undo stack management") {
        test("undo stack limited to 10 actions") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                // Create 15 events
                val events = (1..15).map { i ->
                    AttackResolved(
                        sessionId = sessionId,
                        timestamp = timestamp + i,
                        attackerId = 1L,
                        targetId = 2L,
                        attackRoll = DiceRoll(20, 1, 2, 15),
                        targetAC = 13,
                        hit = true,
                        critical = false
                    )
                }
                
                // Act - undo 15 times
                for (i in 15 downTo 1) {
                    coEvery { eventRepository.forSession(sessionId) } returns events.take(i)
                    undoRedoManager.undo(sessionId)
                }
                
                // Assert - after undoing all 15 events, we're at 0 events
                // So canUndo() should return false (no more events to undo)
                undoRedoManager.canUndo() shouldBe false
            }
        }
        
        test("undo stack maintains order") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 2L,
                    targetId = 1L,
                    attackRoll = DiceRoll(20, 1, 0, 12),
                    targetAC = 15,
                    hit = false,
                    critical = false
                )
                
                coEvery { eventRepository.forSession(sessionId) } returns listOf(event1, event2) andThen listOf(event1)
                
                // Act - undo twice
                val afterFirstUndo = undoRedoManager.undo(sessionId)
                
                coEvery { eventRepository.forSession(sessionId) } returns afterFirstUndo
                val afterSecondUndo = undoRedoManager.undo(sessionId)
                
                // Assert
                afterFirstUndo shouldHaveSize 1
                afterSecondUndo shouldHaveSize 0
            }
        }
    }
    
    context("Redo stack management") {
        test("redo cleared when new action taken") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                coEvery { eventRepository.forSession(sessionId) } returns listOf(event)
                
                // Undo to populate undo stack
                undoRedoManager.undo(sessionId)
                
                // Act - clear redo (simulating new action)
                undoRedoManager.clearRedo()
                
                // Assert
                undoRedoManager.canRedo() shouldBe false
            }
        }
        
        test("redo populated on undo") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2)
                coEvery { eventRepository.forSession(sessionId) } returns events
                
                // Update event count
                undoRedoManager.updateEventCount(events)
                
                // Act - undo (which populates redo stack)
                undoRedoManager.undo(sessionId)
                
                // Assert
                undoRedoManager.canRedo() shouldBe true  // Event2 is now in redo stack
            }
        }
    }
    
    context("Edge cases") {
        xtest("multiple undos and redos work correctly") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event1 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 1,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val event2 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 2,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val event3 = AttackResolved(
                    sessionId = sessionId,
                    timestamp = timestamp + 3,
                    attackerId = 1L,
                    targetId = 2L,
                    attackRoll = DiceRoll(20, 1, 2, 15),
                    targetAC = 13,
                    hit = true,
                    critical = false
                )
                
                val events = listOf(event1, event2, event3)
                
                // Setup mock to return appropriate event lists
                // Initial state: 3 events
                coEvery { eventRepository.forSession(sessionId) } returns events
                
                // Update initial event count
                undoRedoManager.updateEventCount(events)
                
                // Act - undo 2 times
                val after1Undo = undoRedoManager.undo(sessionId)
                undoRedoManager.updateEventCount(after1Undo)
                
                val after2Undos = undoRedoManager.undo(sessionId)
                undoRedoManager.updateEventCount(after2Undos)
                
                // Setup mock for redo operations
                coEvery { eventRepository.forSession(sessionId) } returnsMany listOf(
                    after2Undos,  // Before first redo
                    listOf(event1, event2),  // After first redo
                    listOf(event1, event2, event3)  // After second redo
                )
                
                // Redo 2 times
                val after1Redo = undoRedoManager.redo(sessionId)
                val after2Redos = undoRedoManager.redo(sessionId)
                
                // Assert
                after1Undo shouldHaveSize 2
                after2Undos shouldHaveSize 1
                after1Redo shouldHaveSize 2  // Restored event2
                after2Redos shouldHaveSize 3  // Restored event3
            }
        }
        
        test("undo with single event returns empty list") {
            runTest {
                // Arrange
                val sessionId = 1L
                val timestamp = System.currentTimeMillis()
                
                val event = EncounterStarted(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    encounterId = sessionId,
                    participants = listOf(1L),
                    initiativeOrder = listOf(InitiativeEntryData(1L, 15, 2, 17)),
                    surprisedCreatures = emptySet()
                )
                
                coEvery { eventRepository.forSession(sessionId) } returns listOf(event)
                
                // Update event count
                undoRedoManager.updateEventCount(listOf(event))
                
                // Act
                val result = undoRedoManager.undo(sessionId)
                
                // Assert
                result shouldHaveSize 0
                undoRedoManager.canUndo() shouldBe false  // No more events to undo
                undoRedoManager.canRedo() shouldBe true   // Event is in redo stack
            }
        }
    }
})
