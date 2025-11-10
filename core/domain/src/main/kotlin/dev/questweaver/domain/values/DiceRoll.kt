package dev.questweaver.domain.values

import kotlinx.serialization.Serializable

/**
 * Represents a dice roll with its result.
 *
 * @property diceType The type of dice (4, 6, 8, 10, 12, 20, or 100)
 * @property count The number of dice rolled
 * @property modifier The modifier added to the roll
 * @property result The final result of the roll
 */
@Serializable
data class DiceRoll(
    val diceType: Int,
    val count: Int,
    val modifier: Int = 0,
    val result: Int
) {
    init {
        require(diceType in VALID_DICE_TYPES) { 
            "Invalid dice type: $diceType. Valid types are: ${VALID_DICE_TYPES.joinToString()}" 
        }
        require(count > 0) { "Dice count must be positive" }
        
        val minPossible = count + modifier
        val maxPossible = (count * diceType) + modifier
        require(result >= minPossible) { 
            "Result $result cannot be less than minimum possible roll $minPossible" 
        }
        require(result <= maxPossible) { 
            "Result $result cannot exceed maximum possible roll $maxPossible" 
        }
    }
    
    /**
     * Returns a readable string representation of the dice roll.
     * Format: "{count}d{diceType}+{modifier} = {result}" or "{count}d{diceType}-{modifier} = {result}"
     */
    override fun toString(): String {
        val modifierStr = when {
            modifier > 0 -> "+$modifier"
            modifier < 0 -> "$modifier"
            else -> ""
        }
        return "${count}d${diceType}${modifierStr} = $result"
    }
    
    companion object {
        /**
         * Valid dice types in D&D 5e
         */
        val VALID_DICE_TYPES = setOf(4, 6, 8, 10, 12, 20, 100)
    }
}
