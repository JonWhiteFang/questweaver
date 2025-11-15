package dev.questweaver.feature.encounter.state

import dev.questweaver.domain.events.GameEvent
import dev.questweaver.domain.repositories.EventRepository

/**
 * Manages undo/redo functionality for encounter actions.
 * Maintains stacks of events for undo/redo operations with a maximum depth limit.
 */
class UndoRedoManager(
    private val eventRepository: EventRepository
) {
    private val redoStack = mutableListOf<GameEvent>()
    private var lastEventCount = 0
    
    companion object {
        private const val MAX_UNDO_DEPTH = 10
    }
    
    /**
     * Undoes the last action by removing the most recent event.
     *
     * @param sessionId The session ID for the encounter
     * @return Updated list of events after undo
     */
    suspend fun undo(sessionId: Long): List<GameEvent> {
        // Load current events from repository
        val currentEvents = eventRepository.forSession(sessionId).toMutableList()
        
        if (currentEvents.isEmpty()) {
            lastEventCount = 0
            return currentEvents
        }
        
        // Remove most recent event
        val removedEvent = currentEvents.removeLast()
        
        // Push removed event to redo stack (so it can be redone)
        redoStack.add(removedEvent)
        
        // Limit redo stack to maximum depth
        if (redoStack.size > MAX_UNDO_DEPTH) {
            redoStack.removeAt(0)
        }
        
        // Update last event count
        lastEventCount = currentEvents.size
        
        // Note: In a real implementation, we would need to update the repository
        // to reflect the removed event. For now, we return the updated list.
        // The ViewModel will need to handle persisting this change.
        
        return currentEvents
    }
    
    /**
     * Redoes the last undone action by restoring the event.
     *
     * @param sessionId The session ID for the encounter
     * @return Updated list of events after redo
     */
    suspend fun redo(sessionId: Long): List<GameEvent> {
        if (redoStack.isEmpty()) {
            return eventRepository.forSession(sessionId)
        }
        
        // Pop event from redo stack
        val eventToRestore = redoStack.removeLast()
        
        // Append event back to repository
        eventRepository.append(eventToRestore)
        
        // Get updated event list
        val updatedEvents = eventRepository.forSession(sessionId)
        lastEventCount = updatedEvents.size
        
        // Return updated event list
        return updatedEvents
    }
    
    /**
     * Checks if undo is available.
     * Undo is available when there are events in the current session (beyond the initial event).
     *
     * @return True if undo is available
     */
    fun canUndo(): Boolean {
        // Can undo if there are events beyond the initial EncounterStarted event
        return lastEventCount > 1
    }
    
    /**
     * Checks if redo is available.
     *
     * @return True if there are events in the redo stack
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clears the redo stack.
     * Called when a new action is taken after an undo.
     */
    fun clearRedo() {
        redoStack.clear()
    }
    
    /**
     * Updates the internal event count.
     * Should be called after loading or modifying events.
     *
     * @param events The current list of events
     */
    fun updateEventCount(events: List<GameEvent>) {
        lastEventCount = events.size
    }
}
