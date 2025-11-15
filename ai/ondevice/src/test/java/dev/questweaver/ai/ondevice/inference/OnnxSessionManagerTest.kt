package dev.questweaver.ai.ondevice.inference

import android.content.Context
import android.content.res.AssetManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * Unit tests for OnnxSessionManager.
 *
 * Note: These tests use mocks for ONNX Runtime components since we cannot
 * easily create real ONNX sessions in unit tests. Integration tests with
 * real models should be done separately.
 */
class OnnxSessionManagerTest : FunSpec({
    
    context("initialization") {
        test("should handle missing model file gracefully") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws FileNotFoundException("Model not found")
            
            val manager = OnnxSessionManager(context, "models/missing.onnx")
            
            // Act
            manager.initialize()
            
            // Assert
            manager.isReady() shouldBe false
            
            // Verify error was logged (model loading attempted)
            verify { assetManager.open("models/missing.onnx") }
        }
        
        test("should not reinitialize if already initialized") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            val modelBytes = ByteArray(100) { 0 }
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } returns ByteArrayInputStream(modelBytes)
            
            val manager = OnnxSessionManager(context)
            
            // Act - initialize twice
            manager.initialize()
            manager.initialize()
            
            // Assert - should only load model once
            verify(exactly = 1) { assetManager.open(any()) }
        }
    }
    
    context("inference") {
        test("should throw exception when not initialized") {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val manager = OnnxSessionManager(context)
            val tokens = IntArray(128) { 0 }
            
            // Act & Assert
            runTest {
                shouldThrow<IllegalStateException> {
                    manager.infer(tokens)
                }
            }
        }
        
        test("should throw exception when initialization failed") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws FileNotFoundException("Model not found")
            
            val manager = OnnxSessionManager(context)
            manager.initialize()
            
            val tokens = IntArray(128) { 0 }
            
            // Act & Assert
            runTest {
                shouldThrow<IllegalStateException> {
                    manager.infer(tokens)
                }
            }
        }
        
        test("should validate token array length") {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val manager = OnnxSessionManager(context)
            val invalidTokens = IntArray(64) { 0 } // Wrong length
            
            // Act & Assert
            runTest {
                shouldThrow<IllegalArgumentException> {
                    manager.infer(invalidTokens)
                }
            }
        }
    }
    
    context("resource management") {
        test("should allow closing before initialization") {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val manager = OnnxSessionManager(context)
            
            // Act & Assert - should not throw
            manager.close()
            manager.isReady() shouldBe false
        }
        
        test("should mark as not ready after closing") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            val modelBytes = ByteArray(100) { 0 }
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } returns ByteArrayInputStream(modelBytes)
            
            val manager = OnnxSessionManager(context)
            
            // Note: This test would require mocking OrtEnvironment and OrtSession
            // which is complex. In practice, we'd test this with integration tests.
            
            // Act
            manager.close()
            
            // Assert
            manager.isReady() shouldBe false
        }
    }
    
    context("isReady") {
        test("should return false before initialization") {
            // Arrange
            val context = mockk<Context>(relaxed = true)
            val manager = OnnxSessionManager(context)
            
            // Assert
            manager.isReady() shouldBe false
        }
        
        test("should return false after initialization failure") {
            // Arrange
            val context = mockk<Context>()
            val assetManager = mockk<AssetManager>()
            
            every { context.assets } returns assetManager
            every { assetManager.open(any()) } throws FileNotFoundException("Model not found")
            
            val manager = OnnxSessionManager(context)
            
            // Act
            manager.initialize()
            
            // Assert
            manager.isReady() shouldBe false
        }
    }
})
