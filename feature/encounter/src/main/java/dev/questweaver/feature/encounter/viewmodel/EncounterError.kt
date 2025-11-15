package dev.questweaver.feature.encounter.viewmodel

/**
 * Sealed interface representing all possible errors in encounter management.
 * Used to convert exceptions to user-friendly error messages.
 */
sealed interface EncounterError {
    
    /**
     * Error during encounter initialization.
     *
     * @property reason Human-readable description of the failure
     */
    data class InitializationFailed(val reason: String) : EncounterError
    
    /**
     * Error during action processing.
     *
     * @property reason Human-readable description of the failure
     */
    data class ActionFailed(val reason: String) : EncounterError
    
    /**
     * Error indicating state corruption.
     *
     * @property reason Human-readable description of the corruption
     */
    data class StateCorrupted(val reason: String) : EncounterError
    
    /**
     * Error during encounter loading.
     *
     * @property sessionId The session ID that failed to load
     * @property reason Human-readable description of the failure
     */
    data class LoadFailed(val sessionId: Long, val reason: String) : EncounterError
}

/**
 * Extension function to convert EncounterError to user-friendly message.
 */
fun EncounterError.toUserMessage(): String = when (this) {
    is EncounterError.InitializationFailed -> "Failed to start encounter: $reason"
    is EncounterError.ActionFailed -> "Action failed: $reason"
    is EncounterError.StateCorrupted -> "Encounter state is corrupted: $reason"
    is EncounterError.LoadFailed -> "Failed to load encounter #$sessionId: $reason"
}
