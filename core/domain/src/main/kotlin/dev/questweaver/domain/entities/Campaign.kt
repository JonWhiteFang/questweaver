package dev.questweaver.domain.entities

import dev.questweaver.domain.values.ContentRating
import dev.questweaver.domain.values.Difficulty
import kotlinx.serialization.Serializable

/**
 * Represents a game campaign with metadata and settings.
 *
 * @property id Unique identifier for the campaign
 * @property name The campaign's name
 * @property createdTimestamp Unix timestamp when the campaign was created
 * @property lastPlayedTimestamp Unix timestamp when the campaign was last played
 * @property playerCharacterId ID of the player's character in this campaign
 * @property settings Campaign settings including difficulty and content rating
 */
@Serializable
data class Campaign(
    val id: Long,
    val name: String,
    val createdTimestamp: Long,
    val lastPlayedTimestamp: Long,
    val playerCharacterId: Long,
    val settings: CampaignSettings
) {
    init {
        require(id > 0) { "Campaign id must be positive" }
        require(name.isNotBlank()) { "Campaign name cannot be blank" }
        require(createdTimestamp > 0) { "Created timestamp must be positive" }
        require(lastPlayedTimestamp >= createdTimestamp) { 
            "Last played timestamp cannot be before created timestamp" 
        }
        require(playerCharacterId > 0) { "Player character id must be positive" }
    }
}

/**
 * Settings for a campaign.
 *
 * @property difficulty The difficulty level of the campaign
 * @property contentRating The content rating for the campaign
 */
@Serializable
data class CampaignSettings(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val contentRating: ContentRating = ContentRating.TEEN
)
