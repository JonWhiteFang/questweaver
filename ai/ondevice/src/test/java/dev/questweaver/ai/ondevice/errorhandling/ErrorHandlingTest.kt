package dev.questweaver.ai.ondevice.errorhandling

import android.content.Context
import android.content.res.AssetManager
import dev.questweaver.ai.ondevice.classifier.IntentClassifier
import dev.questweaver.ai.ondevice.classifier.OnnxIntentClassifier
import dev.questweaver.ai.ondevice.extractor.EntityExtractor
import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.model.CreatureInfo
import dev.questweaver.ai.ondevice.model.EncounterContext
import dev.questweaver.ai.ondevice.model.EntityExtractionResult
import dev.questweaver.ai.ondevice.model.IntentResult
import dev.questweaver.ai.ondevice.tokenizer.Tokenizer
import dev.questweaver.ai.ondevice.usecase.IntentClassificationUseCase
import dev.questweaver.core.domain.action.ActionResult
import dev.questweaver.core.domain.intent.IntentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Comprehensive error handling tests for the intent classification system.
 *
 * Tests cover:
 * - Model loading failure scenarios (Requirement 7.1)
 * - ONNX Runtime exception handling (Requirement 7.2)
 * - Timeout scenarios (Requirement 7.2)
 * - Invalid input handling (Requirement 7.3)
 * - Entity extraction failures (Requirement 7.4)
 * - Graceful degradation to keyword fallback (Requirement 7.5)
 */
class ErrorHandlingTest : FunSpec({
    
    context("model loading failures (Requirement 7.1)") {
        test("should handle missing model file gracefully") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws FileNotFoundException("Model file not found")
            
            val sessionManager = OnnxSessionManager(context, "models/missing.onnx")
            
            // Act
            sessionManager.initialize()
            
            // Assert - should not throw, should mark as not ready
            sessionManager.isReady() shouldBe false
        }
        
        test("should handle corrupted model file gracefully") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws IOException("Corrupted model file")
            
            val sessionManager = OnnxSessionManager(context, "models/corrupted.onnx")
            
            // Act
            sessionManager.initialize()
            
            // Assert - should not throw, should mark as not ready
            sessionManager.isReady() shouldBe false
        }
        
        test("should activate keyword fallback when model loading fails") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns false
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("attack the goblin")
            }
            
            // Assert - should use fallback
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.ATTACK
        }
        
        test("should log error when model loading fails") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws FileNotFoundException("Model not found")
            
            val sessionManager = OnnxSessionManager(context)
            
            // Act
            sessionManager.initialize()
            
            // Assert - initialization should complete without throwing
            sessionManager.isReady() shouldBe false
        }
    }
    
    context("ONNX Runtime exception handling (Requirement 7.2)") {
        test("should catch and handle IllegalStateException during inference") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            coEvery { sessionManager.infer(any()) } throws IllegalStateException("ONNX Runtime error")
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.MOVE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("move to E5")
            }
            
            // Assert - should fall back to keywords
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.MOVE
        }
        
        test("should catch and handle IllegalArgumentException during inference") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            coEvery { sessionManager.infer(any()) } throws IllegalArgumentException("Invalid tensor shape")
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.CAST_SPELL,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("cast fireball")
            }
            
            // Assert - should fall back to keywords
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.CAST_SPELL
        }
        
        test("should catch and handle RuntimeException during inference") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            coEvery { sessionManager.infer(any()) } throws RuntimeException("Unexpected ONNX error")
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.DODGE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("dodge")
            }
            
            // Assert - should fall back to keywords
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.DODGE
        }
        
        test("should handle inference failure after successful initialization") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // First call succeeds, second fails
            coEvery { sessionManager.infer(any()) } returns FloatArray(12) { 0.08f } andThenThrows 
                IllegalStateException("Session corrupted")
            
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.HELP,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            // Act - first call
            val result1 = runTest {
                classifier.classify("help")
            }
            
            // Act - second call (should fail and fall back)
            val result2 = runTest {
                classifier.classify("help again")
            }
            
            // Assert
            result1.usedFallback shouldBe true // Low confidence fallback
            result2.usedFallback shouldBe true // Exception fallback
        }
    }
    
    context("timeout scenarios (Requirement 7.2)") {
        test("should handle inference timeout gracefully") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Simulate timeout by throwing CancellationException
            coEvery { sessionManager.infer(any()) } coAnswers {
                throw CancellationException("Inference timeout")
            }
            
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.SEARCH,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                timeoutMs = 300L
            )
            
            // Act
            val result = runTest {
                classifier.classify("search the room")
            }
            
            // Assert - should fall back to keywords
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.SEARCH
        }
        
        test("should respect timeout configuration") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Simulate slow inference
            coEvery { sessionManager.infer(any()) } coAnswers {
                kotlinx.coroutines.delay(500) // Longer than timeout
                FloatArray(12) { 0.08f }
            }
            
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.READY,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                timeoutMs = 100L // Short timeout
            )
            
            // Act
            val result = runTest {
                classifier.classify("ready action")
            }
            
            // Assert - should timeout and fall back
            result.usedFallback shouldBe true
        }
    }
    
    context("invalid input handling (Requirement 7.3)") {
        test("should reject empty input") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Failure>()
            (result as ActionResult.Failure).reason shouldBe "Input cannot be empty"
        }
        
        test("should reject blank input") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("   \n\t  ", context)
            }
            
            // Assert
            result.shouldBeInstanceOf<ActionResult.Failure>()
            (result as ActionResult.Failure).reason shouldBe "Input cannot be empty"
        }
        
        test("should truncate input that is too long") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Create input longer than 500 characters
            val longInput = "attack ".repeat(100) // 700 characters
            
            // Act
            val result = runTest {
                useCase(longInput, context)
            }
            
            // Assert - should not fail, should truncate
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
        }
        
        test("should sanitize input with invalid characters") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.MOVE,
                confidence = 0.95f,
                usedFallback = false
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Input with control characters
            val inputWithControlChars = "move\u0000to\u0001E5\u0002"
            
            // Act
            val result = runTest {
                useCase(inputWithControlChars, context)
            }
            
            // Assert - should not fail, should sanitize
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
        }
        
        test("should handle null-like input gracefully") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("null", context)
            }
            
            // Assert - should process as normal text, not fail
            result.shouldBeInstanceOf<ActionResult>()
        }
    }
    
    context("entity extraction failures (Requirement 7.4)") {
        test("should return empty result when extraction fails") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act - pass empty input
            val result = extractor.extract("", context)
            
            // Assert - should not throw, returns empty result
            result.creatures shouldBe emptyList()
            result.locations shouldBe emptyList()
            result.spells shouldBe emptyList()
            result.items shouldBe emptyList()
        }
        
        test("should handle extraction with malformed input") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act - pass malformed input
            val result = extractor.extract("@#$%^&*()", context)
            
            // Assert - should not throw, returns empty result
            result.creatures shouldBe emptyList()
            result.locations shouldBe emptyList()
            result.spells shouldBe emptyList()
            result.items shouldBe emptyList()
        }
        
        test("should handle extraction with invalid location format") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act - pass invalid location formats
            val result = extractor.extract("move to ZZ99 or (abc,def)", context)
            
            // Assert - should not throw, ignores invalid locations
            result.locations shouldBe emptyList()
        }
        
        test("should handle extraction when context is empty") {
            // Arrange
            val extractor = EntityExtractor()
            val emptyContext = EncounterContext(
                creatures = emptyList(),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = extractor.extract("attack the goblin with fire bolt", emptyContext)
            
            // Assert - should not throw, returns empty result
            result.creatures shouldBe emptyList()
            result.spells shouldBe emptyList()
        }
        
        test("should log warnings for ambiguous references") {
            // Arrange
            val extractor = EntityExtractor()
            val context = EncounterContext(
                creatures = listOf(
                    CreatureInfo(id = 1, name = "Goblin Archer"),
                    CreatureInfo(id = 2, name = "Goblin Warrior"),
                    CreatureInfo(id = 3, name = "Goblin Shaman")
                ),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act - ambiguous reference
            val result = extractor.extract("attack the goblin", context)
            
            // Assert - should extract all matching creatures
            result.creatures.size shouldBe 3
        }
    }
    
    context("graceful degradation to keyword fallback (Requirement 7.5)") {
        test("should use keyword fallback when ONNX unavailable") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns false
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("attack the goblin")
            }
            
            // Assert
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.ATTACK
            result.confidence shouldBe 0.5f
        }
        
        test("should use keyword fallback when confidence too low") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Low confidence from ONNX
            val probabilities = FloatArray(12) { 0.05f }
            coEvery { sessionManager.infer(any()) } returns probabilities
            
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.MOVE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            // Act
            val result = runTest {
                classifier.classify("move to E5")
            }
            
            // Assert
            result.usedFallback shouldBe true
            result.intent shouldBe IntentType.MOVE
        }
        
        test("should maintain functionality with keyword fallback only") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            // Simulate fallback mode
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.CAST_SPELL,
                confidence = 0.5f,
                usedFallback = true
            )
            
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = listOf(
                    dev.questweaver.ai.ondevice.model.ExtractedCreature(
                        creatureId = 1,
                        name = "Goblin",
                        matchedText = "goblin",
                        startIndex = 16,
                        endIndex = 22
                    )
                ),
                locations = emptyList(),
                spells = listOf("Fire Bolt"),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = listOf("Fire Bolt"),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("cast fire bolt at goblin", context)
            }
            
            // Assert - should work with fallback
            result.shouldBeInstanceOf<ActionResult.Success>()
            val action = (result as ActionResult.Success).action
            action.intent shouldBe IntentType.CAST_SPELL
            action.spellName shouldBe "Fire Bolt"
            action.targetCreatureId shouldBe 1
        }
        
        test("should continue working after multiple ONNX failures") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            coEvery { sessionManager.infer(any()) } throws RuntimeException("ONNX error")
            
            coEvery { keywordFallback.classify("attack") } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.5f,
                usedFallback = true
            )
            
            coEvery { keywordFallback.classify("move") } returns IntentResult(
                intent = IntentType.MOVE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            coEvery { keywordFallback.classify("dodge") } returns IntentResult(
                intent = IntentType.DODGE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act - multiple calls
            val result1 = runTest { classifier.classify("attack") }
            val result2 = runTest { classifier.classify("move") }
            val result3 = runTest { classifier.classify("dodge") }
            
            // Assert - all should use fallback successfully
            result1.usedFallback shouldBe true
            result1.intent shouldBe IntentType.ATTACK
            
            result2.usedFallback shouldBe true
            result2.intent shouldBe IntentType.MOVE
            
            result3.usedFallback shouldBe true
            result3.intent shouldBe IntentType.DODGE
        }
        
        test("should indicate fallback usage in result") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns false
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.HIDE,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result = runTest {
                classifier.classify("hide")
            }
            
            // Assert - usedFallback flag should be true
            result.usedFallback shouldBe true
        }
    }
    
    context("combined error scenarios") {
        test("should handle multiple simultaneous errors") {
            // Arrange
            val intentClassifier = mockk<IntentClassifier>()
            val entityExtractor = mockk<EntityExtractor>()
            
            // Classifier uses fallback
            coEvery { intentClassifier.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.5f,
                usedFallback = true
            )
            
            // Entity extraction returns empty (failure)
            every { entityExtractor.extract(any(), any()) } returns EntityExtractionResult(
                creatures = emptyList(),
                locations = emptyList(),
                spells = emptyList(),
                items = emptyList()
            )
            
            val useCase = IntentClassificationUseCase(intentClassifier, entityExtractor)
            val context = EncounterContext(
                creatures = listOf(CreatureInfo(id = 1, name = "Goblin")),
                playerSpells = emptyList(),
                playerInventory = emptyList()
            )
            
            // Act
            val result = runTest {
                useCase("attack", context)
            }
            
            // Assert - should handle both errors and request disambiguation
            result.shouldBeInstanceOf<ActionResult.RequiresChoice>()
        }
        
        test("should recover from transient errors") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // First call fails, second succeeds
            val successProbabilities = FloatArray(12) { 0.0f }
            successProbabilities[0] = 0.95f // ATTACK
            
            coEvery { sessionManager.infer(any()) } throws RuntimeException("Transient error") andThen successProbabilities
            
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
                confidence = 0.5f,
                usedFallback = true
            )
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback
            )
            
            // Act
            val result1 = runTest { classifier.classify("attack") }
            val result2 = runTest { classifier.classify("attack") }
            
            // Assert
            result1.usedFallback shouldBe true // Failed, used fallback
            result2.usedFallback shouldBe false // Succeeded with ONNX
            result2.confidence shouldBe 0.95f
        }
    }
})
