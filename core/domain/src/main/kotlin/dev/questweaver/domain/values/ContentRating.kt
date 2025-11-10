package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents content rating levels for campaign content.
 */
@Serializable
enum class ContentRating {
    /** Content suitable for all ages */
    EVERYONE,
    
    /** Content suitable for teenagers and above */
    TEEN,
    
    /** Content suitable for mature audiences only */
    MATURE
}
