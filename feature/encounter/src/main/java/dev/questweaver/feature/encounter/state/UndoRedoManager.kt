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
    private val undoStack = mutableListOf<GameEvent>()
    private val redoStack = mutableListOf<GameEvent>()
    
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
            return currentEvents
        }
        
        // Remove most recent event
        val removedEvent = currentEvents.removeLast()
        
        // Push removed event to undo stack
        undoStack.add(removedEvent)
        
        // Limit undo stack to maximum depth
        if (undoStack.size > MAX_UNDO_DEPTH) {
            undoStack.removeAt(0)
        }
        
        // Clear redo stack when new action is taken
        redoStack.clear()
        
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
        if (undoStack.isEmpty()) {
            return eventRepository.forSession(sessionId)
        }
        
        // Pop event from undo stack
        val eventToRestore = undoStack.removeLast()
        
        // Append event back to repository
        eventRepository.append(eventToRestore)
        
        // Return updated event list
        return eventRepository.forSession(sessionId)
    }
    
    /**
     * Checks if undo is available.
     *
     * @return True if there are events in the undo stack
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
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
}
