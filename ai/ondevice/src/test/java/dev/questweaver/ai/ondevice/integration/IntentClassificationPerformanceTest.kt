package dev.questweaver.ai.ondevice.integration

import dev.questweaver.ai.ondevice.classifier.OnnxIntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.model.CreatureInfo
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.ai.ondevice.tokenizer.SimpleTokenizer
import dev.questweaver.ai.ondevice.usecase.IntentClassificationUseCase
import dev.questweaver.core.domain.intent.IntentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThan as shouldBeLessThanDouble
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for intent classification.
 *
 * Verifies that the system meets performance budgets:
 * - Full classification: ≤300ms
 * - Tokenization: ≤20ms
 * - Entity extraction: ≤100ms
 *
 * Requirements: 1.2 - Performance budgets
 */
class IntentClassificationPerformanceTest : FunSpec({
    
    context("performance benchmarks") {
        test("full classification pipeline should complete within 300ms budget") {
            // Arrange
            val mockSessionManager = createFastMockSessionManager()
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createLargeVocabulary(),
                maxLength = 128
            )
            
            val keywordFallback = KeywordFallback()
            
            val intentClassifier = OnnxIntentClassifier(
                sessionManager = mockSessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            val entityExtractor = EntityExtractor()
            
            val useCase = IntentClassificationUseCase(
                intentClassifier = intentClassifier,
                entityExtractor = entityExtractor
            )
            
            val context = createLargeEncounterContext()
            
            // Act - Measure time for full pipeline
            val duration = measureTimeMillis {
                runTest {
                    useCase("cast fireball at the goblin archer near the orc warrior", context)
                }
            }
            
            // Assert - Should complete within 300ms budget
            duration shouldBeLessThan 300L
        }
        
        test("tokenization should complete within 20ms budget") {
            // Arrange
            val tokenizer = SimpleTokenizer(
                vocabulary = createLargeVocabulary(),
                maxLength = 128
            )
            
            val longInput = "I want to cast a powerful fireball spell at the group of goblin archers " +
                    "standing near the orc warrior and the troll shaman who are blocking the path " +
                    "to the treasure chest in the corner of the dungeon room"
            
            // Act - Measure tokenization time
            val duration = measureTimeMillis {
                repeat(10) {
                    tokenizer.tokenize(longInput)
                }
            }
            
            val averageDuration = duration / 10
            
            // Assert - Should complete within 20ms budget
            averageDuration shouldBeLessThan 20L
        }
        
        test("entity extraction should complete within 100ms budget") {
            // Arrange
            val entityExtractor = EntityExtractor()
            val context = createLargeEncounterContext()
            
            val complexInput = "cast magic missile at the goblin archer, then move to E5 near the orc warrior, " +
                    "and use my potion of healing if needed"
            
            // Act - Measure entity extraction time
            val duration = measureTimeMillis {
                repeat(10) {
                    entityExtractor.extract(complexInput, context)
                }
            }
            
            val averageDuration = duration / 10
            
            // Assert - Should complete within 100ms budget
            averageDuration shouldBeLessThan 100L
        }
        
        test("classification with keyword fallback should be fast") {
            // Arrange
            val keywordFallback = KeywordFallback()
            
            val inputs = listOf(
                "attack the goblin",
                "move to E5",
                "cast fireball",
                "use potion",
                "dash forward",
                "dodge the attack",
                "help my ally",
                "hide behind cover"
            )
            
            // Act - Measure keyword fallback time
            val duration = measureTimeMillis {
                inputs.forEach { input ->
                    keywordFallback.classify(input)
                }
            }
            
            val averageDuration = duration / inputs.size
            
            // Assert - Keyword fallback should be very fast (< 10ms)
            averageDuration shouldBeLessThan 10L
        }
        
        test("repeated classifications should maintain performance") {
            // Arrange
            val mockSessionManager = createFastMockSessionManager()
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createLargeVocabulary(),
                maxLength = 128
            )
            
            val keywordFallback = KeywordFallback()
            
            val intentClassifier = OnnxIntentClassifier(
                sessionManager = mockSessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            val entityExtractor = EntityExtractor()
            
            val useCase = IntentClassificationUseCase(
                intentClassifier = intentClassifier,
                entityExtractor = entityExtractor
            )
            
            val context = createLargeEncounterContext()
            
            val inputs = listOf(
                "attack the goblin",
                "cast fire bolt at the orc",
                "move to E5",
                "use potion of healing",
                "dash to the door"
            )
            
            // Act - Measure time for multiple classifications
            val durations = mutableListOf<Long>()
            inputs.forEach { input ->
                val duration = measureTimeMillis {
                    runTest {
                        useCase(input, context)
                    }
                }
                durations.add(duration)
            }
            
            // Assert - All classifications should be within budget
            durations.forEach { duration ->
                duration shouldBeLessThan 300L
            }
            
            // Average should be well within budget
            val averageDuration = durations.average()
            averageDuration shouldBeLessThanDouble 250.0
        }
        
        test("entity extraction with many creatures should remain performant") {
            // Arrange
            val entityExtractor = EntityExtractor()
            
            // Create context with many creatures
            val creatures = (1..50).map { i ->
                CreatureInfo(id = i.toLong(), name = "Creature $i")
            }
            
            val context = EncounterContext(
                creatures = creatures,
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            val input = "attack creature 25"
            
            // Act - Measure extraction time with many creatures
            val duration = measureTimeMillis {
                repeat(10) {
                    entityExtractor.extract(input, context)
                }
            }
            
            val averageDuration = duration / 10
            
            // Assert - Should still be within budget
            averageDuration shouldBeLessThan 100L
        }
        
        test("tokenization of maximum length input should be within budget") {
            // Arrange
            val tokenizer = SimpleTokenizer(
                vocabulary = createLargeVocabulary(),
                maxLength = 128
            )
            
            // Create input that will exceed max length
            val longInput = (1..200).joinToString(" ") { "word$it" }
            
            // Act - Measure tokenization time for long input
            val duration = measureTimeMillis {
                repeat(10) {
                    tokenizer.tokenize(longInput)
                }
            }
            
            val averageDuration = duration / 10
            
            // Assert - Should handle truncation efficiently
            averageDuration shouldBeLessThan 20L
        }
    }
})

/**
 * Creates a fast mock ONNX session manager for performance testing.
 */
private fun createFastMockSessionManager(): OnnxSessionManager {
    val mockSessionManager = mockk<OnnxSessionManager>()
    
    // Simulate fast inference (< 100ms)
    val probabilities = FloatArray(12) { 0.01f }
    probabilities[IntentType.ATTACK.ordinal] = 0.95f
    
    coEvery { mockSessionManager.infer(any()) } returns probabilities
    every { mockSessionManager.isReady() } returns true
    
    return mockSessionManager
}

/**
 * Creates a large vocabulary for performance testing.
 */
private fun createLargeVocabulary(): Map<String, Int> {
    val vocab = mutableMapOf<String, Int>()
    
    // Special tokens
    vocab["[UNK]"] = 0
    vocab["[PAD]"] = 1
    vocab["[CLS]"] = 2
    vocab["[SEP]"] = 3
    
    // Add 5000 common words
    var index = 4
    
    // Action words
    listOf("attack", "move", "cast", "use", "dash", "dodge", "help", "hide", "disengage", "ready", "search").forEach {
        vocab[it] = index++
    }
    
    // Common words
    listOf("the", "a", "an", "at", "to", "on", "in", "with", "from", "by", "for", "of", "and", "or").forEach {
        vocab[it] = index++
    }
    
    // Creatures
    (1..100).forEach { i ->
        vocab["creature$i"] = index++
    }
    
    // Spells
    listOf("fire", "bolt", "magic", "missile", "fireball", "shield", "healing", "light", "darkness").forEach {
        vocab[it] = index++
    }
    
    // Items
    listOf("potion", "sword", "shield", "armor", "rope", "torch", "healing", "mana").forEach {
        vocab[it] = index++
    }
    
    // Fill remaining vocabulary with generic words
    (index until 5000).forEach { i ->
        vocab["word$i"] = i
    }
    
    return vocab
}

/**
 * Creates a large encounter context for performance testing.
 */
private fun createLargeEncounterContext(): EncounterContext {
    return EncounterContext(
        creatures = listOf(
            CreatureInfo(id = 1, name = "Goblin Archer"),
            CreatureInfo(id = 2, name = "Orc Warrior"),
            CreatureInfo(id = 3, name = "Troll Shaman"),
            CreatureInfo(id = 4, name = "Goblin Scout"),
            CreatureInfo(id = 5, name = "Orc Berserker"),
            CreatureInfo(id = 6, name = "Hobgoblin Captain"),
            CreatureInfo(id = 7, name = "Bugbear Brute"),
            CreatureInfo(id = 8, name = "Kobold Sorcerer"),
            CreatureInfo(id = 9, name = "Gnoll Hunter"),
            CreatureInfo(id = 10, name = "Ogre")
        ),
        playerSpells = listOf(
            "Fire Bolt",
            "Magic Missile",
            "Shield",
            "Mage Armor",
            "Burning Hands",
            "Thunderwave",
            "Detect Magic",
            "Light",
            "Prestidigitation",
            "Ray of Frost"
        ),
        playerInventory = listOf(
            "Potion of Healing",
            "Potion of Greater Healing",
            "Rope (50 feet)",
            "Torch",
            "Rations (5 days)",
            "Waterskin",
            "Bedroll",
            "Tinderbox",
            "Crowbar",
            "Hammer"
        )
    )
}
