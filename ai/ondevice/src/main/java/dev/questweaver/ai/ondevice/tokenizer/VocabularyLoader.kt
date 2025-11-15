package dev.questweaver.ai.ondevice.tokenizer

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import java.io.IOException

/**
 * Loads vocabulary from JSON assets for tokenization.
 */
class VocabularyLoader(private val context: Context) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Loads vocabulary from a JSON file in assets.
     * 
     * Expected JSON format: {"token": index, ...}
     * Example: {"attack": 10, "the": 20, "goblin": 30}
     * 
     * @param path Path to the vocabulary JSON file in assets (e.g., "models/vocabulary.json")
     * @return Map of tokens to their vocabulary indices
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the JSON format is invalid
     */
    fun loadVocabulary(path: String): Map<String, Int> {
        return try {
            val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
            val jsonElement = json.parseToJsonElement(jsonString)
            
            jsonElement.jsonObject.mapValues { (_, value) ->
                value.jsonPrimitive.int
            }
        } catch (e: IOException) {
            throw IOException("Failed to load vocabulary from $path", e)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw IllegalArgumentException("Invalid vocabulary JSON format in $path", e)
        }
    }
    
    /**
     * Creates a default vocabulary with common D&D terms.
     * Used as fallback when vocabulary file is missing.
     */
    @Suppress("MagicNumber")
    fun createDefaultVocabulary(): Map<String, Int> {
        return mapOf(
            // Special tokens
            "[UNK]" to 0,
            "[PAD]" to 1,
            "[CLS]" to 2,
            "[SEP]" to 3,
            
            // Common action verbs
            "attack" to 10,
            "hit" to 11,
            "strike" to 12,
            "shoot" to 13,
            "move" to 20,
            "go" to 21,
            "walk" to 22,
            "run" to 23,
            "cast" to 30,
            "spell" to 31,
            "magic" to 32,
            "use" to 40,
            "item" to 41,
            "potion" to 42,
            "dash" to 50,
            "dodge" to 51,
            "help" to 52,
            "hide" to 53,
            "disengage" to 54,
            "ready" to 55,
            "search" to 56,
            
            // Common targets
            "the" to 100,
            "a" to 101,
            "an" to 102,
            "to" to 103,
            "at" to 104,
            "with" to 105,
            "my" to 106,
            "goblin" to 200,
            "orc" to 201,
            "dragon" to 202,
            "skeleton" to 203,
            "zombie" to 204,
            
            // Common spells
            "fireball" to 300,
            "fire" to 301,
            "bolt" to 302,
            "magic" to 303,
            "missile" to 304,
            "cure" to 305,
            "wounds" to 306,
            "light" to 307,
            
            // Common items
            "sword" to 400,
            "bow" to 401,
            "arrow" to 402,
            "shield" to 403,
            "armor" to 404
        )
    }
}
