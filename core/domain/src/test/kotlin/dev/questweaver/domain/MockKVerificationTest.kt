package dev.questweaver.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Verification test to ensure MockK works correctly with kotest.
 * This test validates the integration between the testing frameworks.
 */
class MockKVerificationTest : FunSpec({
    
    test("MockK should work with kotest for basic mocking") {
        // Arrange
        val mockList = mockk<MutableList<String>>()
        every { mockList.size } returns 5
        every { mockList[0] } returns "test"
        
        // Act
        val size = mockList.size
        val firstElement = mockList[0]
        
        // Assert
        size shouldBe 5
        firstElement shouldBe "test"
        verify { mockList.size }
        verify { mockList[0] }
    }
    
    test("MockK should support relaxed mocks") {
        // Arrange
        val relaxedMock = mockk<MutableList<String>>(relaxed = true)
        
        // Act
        relaxedMock.add("item")
        
        // Assert - relaxed mocks return default values
        verify { relaxedMock.add("item") }
    }
    
    test("MockK should support argument matchers") {
        // Arrange
        val mockMap = mockk<MutableMap<String, Int>>()
        every { mockMap.put(any(), any()) } returns null
        every { mockMap.get("key") } returns 42
        
        // Act
        mockMap.put("key", 42)
        val value = mockMap.get("key")
        
        // Assert
        value shouldBe 42
        verify { mockMap.put("key", 42) }
        verify { mockMap.get("key") }
    }
})
