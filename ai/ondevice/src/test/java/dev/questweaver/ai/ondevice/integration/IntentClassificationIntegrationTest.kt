package dev.questweaver.ai.ondevice.integration

import dev.questweaver.ai.ondevice.classifier.OnnxIntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.model.CreatureInfo
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.ai.ondevice.tokenizer.SimpleTokenizer
import dev.questweaver.ai.ondevice.usecase.IntentClassificationUseCase
import dev.questweaver.core.domain.action.ActionResult
import dev.questweaver.core.domain.intent.IntentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Integration tests for the full intent classification pipeline.
 *
 * Tests the complete flow from text input to NLAction, including:
 * - Intent classification (with mocked ONNX for deterministic testing)
 * - Entity extraction
 * - Use case orchestration
 *
 * Requirements: 8.1 - End-to-end classification test
 */
class IntentClassificationIntegrationTest : FunSpec({
    
    context("end-to-end classification with real components") {
        test("should classify attack intent and extract creature target") {
            // Arrange - Create real components with mocked ONNX session
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.ATTACK.ordinal,
                confidence = 0.95f
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin"),
                    CreatureInfo(id = 2, name = "Orc Warrior")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack the goblin", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.ATTACK
            action.targetCreatureId shouldBe 1
            action.originalText shouldBe "attack the goblin"
            action.confidence shouldBe 0.95f
        }
        
        test("should classify spell casting and extract spell name and target") {
            // Arrange
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.CAST_SPELL.ordinal,
                confidence = 0.92f
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin"),
                    CreatureInfo(id = 2, name = "Orc")
                ),
                playerSpells = listOf("Fire Bolt", "Magic Missile", "Shield"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("cast fire bolt at the goblin", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.CAST_SPELL
            action.spellName shouldBe "Fire Bolt"
            action.targetCreatureId shouldBe 1
            action.confidence shouldBe 0.92f
        }
        
        test("should classify movement and extract location") {
            // Arrange
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.MOVE.ordinal,
                confidence = 0.88f
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("move to E5", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.MOVE
            action.targetLocation shouldNotBe null
            action.targetLocation?.x shouldBe 4 // E = 4 (0-indexed)
            action.targetLocation?.y shouldBe 4 // 5 = 4 (0-indexed)
        }
        
        test("should handle complex input with multiple entities") {
            // Arrange
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.CAST_SPELL.ordinal,
                confidence = 0.91f
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin Archer"),
                    CreatureInfo(id = 2, name = "Orc Warrior"),
                    CreatureInfo(id = 3, name = "Goblin Shaman")
                ),
                playerSpells = listOf("Fire Bolt", "Magic Missile", "Burning Hands"),
                playerInventory = listOf("Potion of Healing", "Rope")
            )
            
            // Act
            val result = runTest {
                useCase("cast magic missile at the orc warrior", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.CAST_SPELL
            action.spellName shouldBe "Magic Missile"
            action.targetCreatureId shouldBe 2 // Orc Warrior
            action.confidence shouldBe 0.91f
        }
        
        test("should fall back to keywords when ONNX confidence is low") {
            // Arrange - Mock low confidence from ONNX
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.ATTACK.ordinal,
                confidence = 0.4f // Below threshold
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack the goblin", context)
            }
            
            // Assert - Should still succeed using keyword fallback
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.ATTACK
            action.targetCreatureId shouldBe 1
            action.confidence shouldBe 0.5f // Keyword fallback confidence
        }
        
        test("should handle item usage with entity extraction") {
            // Arrange
            val mockSessionManager = createMockSessionManager(
                intentIndex = IntentType.USE_ITEM.ordinal,
                confidence = 0.87f
            )
            
            val tokenizer = SimpleTokenizer(
                vocabulary = createTestVocabulary(),
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
            
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = listOf("Potion of Healing", "Rope", "Torch")
            )
            
            // Act
            val result = runTest {
                useCase("use potion of healing", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.USE_ITEM
            action.itemName shouldBe "Potion of Healing"
            action.confidence shouldBe 0.87f
        }
    }
})

/**
 * Creates a mock ONNX session manager that returns deterministic probabilities.
 */
private fun createMockSessionManager(intentIndex: Int, confidence: Float): OnnxSessionManager {
    val mockSessionManager = mockk<OnnxSessionManager>()
    
    // Create probability array with high confidence for the specified intent
    val probabilities = FloatArray(12) { 0.01f } // 12 intent types
    probabilities[intentIndex] = confidence
    
    coEvery { mockSessionManager.infer(any()) } returns probabilities
    every { mockSessionManager.isReady() } returns true
    
    return mockSessionManager
}

/**
 * Creates a test vocabulary with common D&D terms.
 */
private fun createTestVocabulary(): Map<String, Int> {
    return mapOf(
        // Special tokens
        "[UNK]" to 0,
        "[PAD]" to 1,
        "[CLS]" to 2,
        "[SEP]" to 3,
        
        // Common action words
        "attack" to 10,
        "move" to 11,
        "cast" to 12,
        "use" to 13,
        "dash" to 14,
        "dodge" to 15,
        "hide" to 16,
        "help" to 17,
        
        // Common targets
        "the" to 20,
        "a" to 21,
        "at" to 22,
        "to" to 23,
        "on" to 24,
        
        // Creatures
        "goblin" to 30,
        "orc" to 31,
        "warrior" to 32,
        "archer" to 33,
        "shaman" to 34,
        
        // Spells
        "fire" to 40,
        "bolt" to 41,
        "magic" to 42,
        "missile" to 43,
        "burning" to 44,
        "hands" to 45,
        "shield" to 46,
        
        // Items
        "potion" to 50,
        "of" to 51,
        "healing" to 52,
        "rope" to 53,
        "torch" to 54,
        
        // Locations
        "e5" to 60,
        "a1" to 61
    )
}
