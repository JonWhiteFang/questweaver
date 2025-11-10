package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents campaign difficulty levels.
 */
@Serializable
enum class Difficulty {
    /** Easy difficulty with reduced challenge */
    EASY,
    
    /** Normal difficulty with balanced challenge */
    NORMAL,
    
    /** Hard difficulty with increased challenge */
    HARD,
    
    /** Deadly difficulty with maximum challenge */
    DEADLY
}
