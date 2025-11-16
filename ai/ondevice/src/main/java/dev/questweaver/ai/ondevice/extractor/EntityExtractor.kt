package dev.questweaver.ai.ondevice.extractor

import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.ai.ondevice.model.EntityExtractionResult
import dev.questweaver.ai.ondevice.model.ExtractedCreature
import dev.questweaver.core.domain.map.GridPos
import org.slf4j.LoggerFactory

/**
 * Extracts game entities (creatures, locations, spells, items) from player text input.
 *
 * This class uses pattern matching and context-aware search to identify:
 * - Creature names from the current encounter
 * - Grid locations (e.g., "E5", "(5,5)")
 * - Spell names from player's known spells
 * - Item names from player's inventory
 */
class EntityExtractor {
    private val logger = LoggerFactory.getLogger(EntityExtractor::class.java)
    
    companion object {
        private const val MIN_PARTIAL_MATCH_LENGTH = 3
    }
    
    /**
     * Extracts entities from text using the provided encounter context.
     *
     * @param text Player input text
     * @param context Current encounter context with creatures, spells, and items
     * @return EntityExtractionResult with all extracted entities
     */
    fun extract(text: String, context: EncounterContext): EntityExtractionResult {
        return try {
            val lowerText = text.lowercase()
            
            val creatures = extractCreatures(lowerText, text, context)
            val locations = extractLocations(text)
            val spells = extractSpells(lowerText, context)
            val items = extractItems(lowerText, context)
            
            logger.debug { 
                "Extracted: ${creatures.size} creatures, ${locations.size} locations, " +
                "${spells.size} spells, ${items.size} items" 
            }
            
            EntityExtractionResult(
                creatures = creatures,
                locations = locations,
                spells = spells,
                items = items
            )
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Entity extraction failed due to invalid argument, returning empty result" }
            EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
        } catch (e: IllegalStateException) {
            logger.warn(e) { "Entity extraction failed due to invalid state, returning empty result" }
            EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
        }
    }
    
    /**
     * Extracts creature references from text.
     *
     * Uses longest-match-first strategy to handle partial name matches.
     * For example, "Goblin Archer" should match before "Goblin".
     */
    private fun extractCreatures(
        lowerText: String,
        originalText: String,
        context: EncounterContext
    ): List<ExtractedCreature> {
        val extracted = mutableListOf<ExtractedCreature>()
        
        // Build case-insensitive name map sorted by length (longest first)
        val creaturesByName = context.creatures
            .sortedByDescending { it.name.length }
            .associateBy { it.name.lowercase() }
        
        // Track matched positions to avoid overlapping matches
        val matchedRanges = mutableListOf<IntRange>()
        
        for ((lowerName, creature) in creaturesByName) {
            findCreatureMatches(lowerText, originalText, lowerName, creature, matchedRanges, extracted)
        }
        
        return extracted
    }
    
    /**
     * Finds all matches for a specific creature name in the text.
     */
    @Suppress("LongParameterList") // Required for efficient matching algorithm
    private fun findCreatureMatches(
        lowerText: String,
        originalText: String,
        lowerName: String,
        creature: dev.questweaver.ai.ondevice.model.CreatureInfo,
        matchedRanges: MutableList<IntRange>,
        extracted: MutableList<ExtractedCreature>
    ) {
        var startIndex = 0
        
        while (true) {
            val index = lowerText.indexOf(lowerName, startIndex)
            if (index == -1) break
            
            val endIndex = index + lowerName.length
            val range = index until endIndex
            
            // Check if this range overlaps with any existing match
            val overlaps = matchedRanges.any { it.intersect(range).isNotEmpty() }
            
            if (!overlaps && isWordBoundary(lowerText, index, endIndex)) {
                extracted.add(
                    ExtractedCreature(
                        creatureId = creature.id,
                        name = creature.name,
                        matchedText = originalText.substring(index, endIndex),
                        startIndex = index,
                        endIndex = endIndex
                    )
                )
                matchedRanges.add(range)
                logger.debug { "Matched creature: ${creature.name} at $index-$endIndex" }
            }
            
            startIndex = index + 1
        }
    }
    
    /**
     * Extracts grid locations from text.
     *
     * Supports two formats:
     * - Grid notation: "E5", "A1", etc. (letter + number)
     * - Coordinate notation: "(5,5)", "(10, 3)", etc.
     */
    private fun extractLocations(text: String): List<GridPos> {
        val locations = mutableListOf<GridPos>()
        
        // Pattern 1: Grid notation (e.g., "E5")
        val gridPattern = Regex("""([A-Z])(\d+)""")
        gridPattern.findAll(text).forEach { match ->
            val letter = match.groupValues[1][0]
            val number = match.groupValues[2].toIntOrNull()
            
            if (number != null) {
                // Convert letter to 0-indexed column (A=0, B=1, etc.)
                val col = letter - 'A'
                // Convert number to 0-indexed row (1=0, 2=1, etc.)
                val row = number - 1
                
                if (col >= 0 && row >= 0) {
                    locations.add(GridPos(col, row))
                    logger.debug { "Matched grid location: $letter$number -> ($col, $row)" }
                }
            }
        }
        
        // Pattern 2: Coordinate notation (e.g., "(5,5)")
        val coordPattern = Regex("""\((\d+)\s*,\s*(\d+)\)""")
        coordPattern.findAll(text).forEach { match ->
            val x = match.groupValues[1].toIntOrNull()
            val y = match.groupValues[2].toIntOrNull()
            
            if (x != null && y != null) {
                locations.add(GridPos(x, y))
                logger.debug { "Matched coordinate location: ($x, $y)" }
            }
        }
        
        return locations.distinct()
    }
    
    /**
     * Extracts spell names from text.
     *
     * Uses case-insensitive matching against player's known spells.
     * Handles partial matches (e.g., "fireball" matches "Fireball").
     */
    private fun extractSpells(lowerText: String, context: EncounterContext): List<String> {
        val spells = mutableListOf<String>()
        
        for (spell in context.playerSpells) {
            val lowerSpell = spell.lowercase()
            
            if (lowerText.contains(lowerSpell)) {
                // Check word boundaries for better accuracy
                val pattern = Regex("""\b${Regex.escape(lowerSpell)}\b""")
                if (pattern.containsMatchIn(lowerText)) {
                    spells.add(spell)
                    logger.debug { "Matched spell: $spell" }
                }
            }
        }
        
        return spells.distinct()
    }
    
    /**
     * Extracts item names from text.
     *
     * Uses case-insensitive matching against player's inventory.
     * Handles articles and plurals (e.g., "a potion" matches "Potion of Healing").
     */
    private fun extractItems(lowerText: String, context: EncounterContext): List<String> {
        val items = mutableListOf<String>()
        
        for (item in context.playerInventory) {
            val lowerItem = item.lowercase()
            
            // Try exact match first
            if (lowerText.contains(lowerItem)) {
                val pattern = Regex("""\b${Regex.escape(lowerItem)}\b""")
                if (pattern.containsMatchIn(lowerText)) {
                    items.add(item)
                    logger.debug { "Matched item (exact): $item" }
                    continue
                }
            }
            
            // Try partial match (first word of item name)
            val firstWord = lowerItem.split(" ").firstOrNull()
            if (firstWord != null && firstWord.length > MIN_PARTIAL_MATCH_LENGTH) {
                val pattern = Regex("""\b${Regex.escape(firstWord)}\b""")
                if (pattern.containsMatchIn(lowerText)) {
                    items.add(item)
                    logger.debug { "Matched item (partial): $item" }
                }
            }
        }
        
        return items.distinct()
    }
    
    /**
     * Checks if a match is at word boundaries.
     *
     * This helps avoid false positives like matching "arc" in "march".
     */
    private fun isWordBoundary(text: String, startIndex: Int, endIndex: Int): Boolean {
        val beforeIsWordChar = startIndex > 0 && text[startIndex - 1].isLetterOrDigit()
        val afterIsWordChar = endIndex < text.length && text[endIndex].isLetterOrDigit()
        
        return !beforeIsWordChar && !afterIsWordChar
    }
}
