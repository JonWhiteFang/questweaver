package dev.questweaver.domain

/**
 * Sealed class representing domain-level errors.
 * Provides structured error handling with context for different failure scenarios.
 */
sealed class DomainError : Exception() {
    /**
     * Error indicating an entity failed validation or is in an invalid state.
     *
     * @property entityType The type of entity that is invalid (e.g., "Creature", "Campaign")
     * @property reason Human-readable explanation of why the entity is invalid
     */
    data class InvalidEntity(
        val entityType: String,
        val reason: String
    ) : DomainError() {
        override val message: String
            get() = "Invalid $entityType: $reason"
    }

    /**
     * Error indicating an operation cannot be performed due to business rule violations.
     *
     * @property operation The name of the operation that failed (e.g., "AttackCreature", "MoveToPosition")
     * @property reason Human-readable explanation of why the operation is invalid
     */
    data class InvalidOperation(
        val operation: String,
        val reason: String
    ) : DomainError() {
        override val message: String
            get() = "Invalid operation '$operation': $reason"
    }

    /**
     * Error indicating a requested entity was not found.
     *
     * @property entityType The type of entity that was not found (e.g., "Creature", "Campaign")
     * @property id The identifier of the entity that was not found
     */
    data class NotFound(
        val entityType: String,
        val id: Long
    ) : DomainError() {
        override val message: String
            get() = "$entityType with id $id not found"
    }
}
