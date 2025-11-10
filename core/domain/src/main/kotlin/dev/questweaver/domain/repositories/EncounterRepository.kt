package dev.questweaver.domain.repositories

import dev.questweaver.domain.entities.Encounter
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Encounter entities.
 * 
 * Provides CRUD operations for combat encounters with reactive observation capabilities
 * and campaign-specific queries.
 */
interface EncounterRepository {
    /**
     * Retrieves an encounter by its unique identifier.
     *
     * @param id The unique identifier of the encounter
     * @return The encounter if found, null otherwise
     */
    suspend fun getById(id: Long): Encounter?
    
    /**
     * Retrieves all encounters for a specific campaign.
     *
     * @param campaignId The unique identifier of the campaign
     * @return List of all encounters belonging to the campaign
     */
    suspend fun getByCampaign(campaignId: Long): List<Encounter>
    
    /**
     * Inserts a new encounter into the repository.
     *
     * @param encounter The encounter to insert
     * @return The unique identifier assigned to the inserted encounter
     */
    suspend fun insert(encounter: Encounter): Long
    
    /**
     * Updates an existing encounter in the repository.
     *
     * @param encounter The encounter with updated values
     */
    suspend fun update(encounter: Encounter)
    
    /**
     * Deletes an encounter from the repository.
     *
     * @param id The unique identifier of the encounter to delete
     */
    suspend fun delete(id: Long)
    
    /**
     * Observes a specific encounter reactively.
     *
     * @param id The unique identifier of the encounter to observe
     * @return Flow emitting the current encounter state whenever it changes, or null if deleted
     */
    fun observe(id: Long): Flow<Encounter?>
}
