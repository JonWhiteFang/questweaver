package dev.questweaver.domain.repositories

import dev.questweaver.domain.entities.Campaign
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Campaign entities.
 * 
 * Provides CRUD operations for campaigns with reactive observation capabilities
 * for tracking all campaigns in the system.
 */
interface CampaignRepository {
    /**
     * Retrieves a campaign by its unique identifier.
     *
     * @param id The unique identifier of the campaign
     * @return The campaign if found, null otherwise
     */
    suspend fun getById(id: Long): Campaign?
    
    /**
     * Retrieves all campaigns.
     *
     * @return List of all campaigns in the repository
     */
    suspend fun getAll(): List<Campaign>
    
    /**
     * Inserts a new campaign into the repository.
     *
     * @param campaign The campaign to insert
     * @return The unique identifier assigned to the inserted campaign
     */
    suspend fun insert(campaign: Campaign): Long
    
    /**
     * Updates an existing campaign in the repository.
     *
     * @param campaign The campaign with updated values
     */
    suspend fun update(campaign: Campaign)
    
    /**
     * Deletes a campaign from the repository.
     *
     * @param id The unique identifier of the campaign to delete
     */
    suspend fun delete(id: Long)
    
    /**
     * Observes all campaigns reactively.
     *
     * @return Flow emitting the current list of all campaigns whenever changes occur
     */
    fun observeAll(): Flow<List<Campaign>>
}
