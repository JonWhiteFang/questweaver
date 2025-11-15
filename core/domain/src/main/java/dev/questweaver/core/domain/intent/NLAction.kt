package dev.questweaver.core.domain.intent

/**
 * Represents a structured action derived from natural language input.
 * 
 * This is the result of intent classification and entity extraction,
 * containing all the information needed to validate and execute a player action.
 * 
 * @property intent The type of action the player intends to perform
 * @property originalText The original natural language input from the player
 * @property targetCreatureId The ID of the creature being targeted (if applicable)
 * @property targetLocation The grid location being targeted (if applicable)
 * @property spellName The name of the spell being cast (if applicable)
 * @property itemName The name of the item being used (if applicable)
 * @property confidence The confidence score of the intent classification (0.0-1.0)
 */
data class NLAction(
    val intent: IntentType,
    val originalText: String,
    val targetCreatureId: Long? = null,
    val targetLocation: GridPos? = null,
    val spellName: String? = null,
    val itemName: String? = null,
    val confidence: Float
) {
    init {
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }
}
