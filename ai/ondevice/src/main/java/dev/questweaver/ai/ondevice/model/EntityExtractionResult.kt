package dev.questweaver.ai.ondevice.model

import dev.questweaver.core.domain.intent.GridPos

/**
 * Result of entity extraction from natural language input.
 * 
 * @property creatures List of creatures extracted from the text
 * @property locations List of grid locations extracted from the text
 * @property spells List of spell names extracted from the text
 * @property items List of item names extracted from the text
 */
data class EntityExtractionResult(
    val creatures: List<ExtractedCreature> = emptyList(),
    val locations: List<GridPos> = emptyList(),
    val spells: List<String> = emptyList(),
    val items: List<String> = emptyList()
)

/**
 * Represents a creature entity extracted from text.
 * 
 * @property creatureId The unique ID of the creature
 * @property name The name of the creature
 * @property matchedText The exact text that matched the creature name
 * @property startIndex The starting index of the match in the original text
 * @property endIndex The ending index of the match in the original text
 */
data class ExtractedCreature(
    val creatureId: Long,
    val name: String,
    val matchedText: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Context information needed for entity extraction.
 * 
 * @property creatures List of creatures in the current encounter
 * @property playerSpells List of spells known by the player character
 * @property playerInventory List of items in the player's inventory
 */
data class EncounterContext(
    val creatures: List<CreatureInfo>,
    val playerSpells: List<String>,
    val playerInventory: List<String>
)

/**
 * Minimal creature information needed for entity extraction.
 * 
 * @property id The unique ID of the creature
 * @property name The name of the creature
 */
data class CreatureInfo(
    val id: Long,
    val name: String
)
