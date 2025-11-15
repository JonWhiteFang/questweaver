package dev.questweaver.ai.ondevice.classifier

import dev.questweaver.ai.ondevice.fallback.KeywordFallback
import dev.questweaver.ai.ondevice.inference.OnnxSessionManager
import dev.questweaver.ai.ondevice.model.IntentResult
import dev.questweaver.ai.ondevice.tokenizer.Tokenizer
import dev.questweaver.core.domain.intent.IntentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for OnnxIntentClassifier.
 */
class OnnxIntentClassifierTest : FunSpec({
    
    context("ONNX classification") {
        test("should classify with high confidence") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Mock ONNX inference returning high confidence for ATTACK (index 0)
            val probabilities = FloatArray(12) { 0.0f }
            probabilities[0] = 0.95f // ATTACK with 95% confidence
            coEvery { sessionManager.infer(any()) } returns probabilities
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            // Act
            val result = runTest {
                classifier.classify("attack the goblin")
            }
            
            // Assert
            result.intent shouldBe IntentType.ATTACK
            result.confidence shouldBe 0.95f
            result.usedFallback shouldBe false
            
            coVerify { sessionManager.infer(any()) }
            coVerify(exactly = 0) { keywordFallback.classify(any()) }
        }
        
        test("should fall back to keywords when confidence low") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Mock ONNX inference returning low confidence
            val probabilities = FloatArray(12) { 0.08f } // All low confidence
            coEvery { sessionManager.infer(any()) } returns probabilities
            
            // Mock keyword fallback
            coEvery { keywordFallback.classify(any()) } returns IntentResult(
                intent = IntentType.ATTACK,
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
                classifier.classify("hit the orc")
            }
            
            // Assert
            result.intent shouldBe IntentType.ATTACK
            result.usedFallback shouldBe true
            
            coVerify { sessionManager.infer(any()) }
            coVerify { keywordFallback.classify("hit the orc") }
        }
        
        test("should fall back on ONNX exception") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            // Mock ONNX inference throwing exception
            coEvery { sessionManager.infer(any()) } throws RuntimeException("ONNX error")
            
            // Mock keyword fallback
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
            
            // Assert
            result.intent shouldBe IntentType.MOVE
            result.usedFallback shouldBe true
            
            coVerify { keywordFallback.classify("move to E5") }
        }
        
        test("should use fallback when session not ready") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns false
            
            // Mock keyword fallback
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
            
            // Assert
            result.intent shouldBe IntentType.CAST_SPELL
            result.usedFallback shouldBe true
            
            coVerify(exactly = 0) { sessionManager.infer(any()) }
            coVerify { keywordFallback.classify("cast fireball") }
        }
    }
    
    context("intent mapping") {
        test("should map all intent indices correctly") {
            // Arrange
            val sessionManager = mockk<OnnxSessionManager>()
            val tokenizer = mockk<Tokenizer>()
            val keywordFallback = mockk<KeywordFallback>()
            
            every { sessionManager.isReady() } returns true
            every { tokenizer.tokenize(any()) } returns IntArray(128) { 0 }
            
            val classifier = OnnxIntentClassifier(
                sessionManager = sessionManager,
                tokenizer = tokenizer,
                keywordFallback = keywordFallback,
                confidenceThreshold = 0.6f
            )
            
            // Test each intent type
            val intentTests = listOf(
                0 to IntentType.ATTACK,
                1 to IntentType.MOVE,
                2 to IntentType.CAST_SPELL,
                3 to IntentType.USE_ITEM,
                4 to IntentType.DASH,
                5 to IntentType.DODGE,
                6 to IntentType.HELP,
                7 to IntentType.HIDE,
                8 to IntentType.DISENGAGE,
                9 to IntentType.READY,
                10 to IntentType.SEARCH,
                11 to IntentType.UNKNOWN
            )
            
            intentTests.forEach { (index, expectedIntent) ->
                // Arrange
                val probabilities = FloatArray(12) { 0.0f }
                probabilities[index] = 0.95f
                coEvery { sessionManager.infer(any()) } returns probabilities
                
                // Act
                val result = runTest {
                    classifier.classify("test input")
                }
                
                // Assert
                result.intent shouldBe expectedIntent
            }
        }
    }
})
