package dev.questweaver.domain.repositories

import dev.questweaver.domain.entities.Creature
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Creature entities.
 * 
 * Provides CRUD operations for creatures (player characters, NPCs, and monsters)
 * with reactive observation capabilities.
 */
interface CreatureRepository {
    /**
     * Retrieves a creature by its unique identifier.
     *
     * @param id The unique identifier of the creature
     * @return The creature if found, null otherwise
     */
    suspend fun getById(id: Long): Creature?
    
    /**
     * Retrieves all creatures.
     *
     * @return List of all creatures in the repository
     */
    suspend fun getAll(): List<Creature>
    
    /**
     * Inserts a new creature into the repository.
     *
     * @param creature The creature to insert
     * @return The unique identifier assigned to the inserted creature
     */
    suspend fun insert(creature: Creature): Long
    
    /**
     * Updates an existing creature in the repository.
     *
     * @param creature The creature with updated values
     */
    suspend fun update(creature: Creature)
    
    /**
     * Deletes a creature from the repository.
     *
     * @param id The unique identifier of the creature to delete
     */
    suspend fun delete(id: Long)
    
    /**
     * Observes a specific creature reactively.
     *
     * @param id The unique identifier of the creature to observe
     * @return Flow emitting the current creature state whenever it changes, or null if deleted
     */
    fun observe(id: Long): Flow<Creature?>
}
