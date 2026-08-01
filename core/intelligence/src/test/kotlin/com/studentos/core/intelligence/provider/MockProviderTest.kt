package com.studentos.core.intelligence.provider

import com.studentos.core.intelligence.model.LLMResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockProviderTest {

    private lateinit var mockProvider: MockProvider

    @Before
    fun setUp() {
        mockProvider = MockProvider()
    }

    @Test
    fun isAvailable_returnsTrue() {
        assertTrue(mockProvider.isAvailable())
        assertEquals("MockProvider", mockProvider.name)
    }

    @Test
    fun generateBrief_returnsDeterministicSuccessResult() = runTest {
        val prompt = "Sample Prompt for Brief"
        val result1 = mockProvider.generateBrief(prompt)
        val result2 = mockProvider.generateBrief(prompt)

        assertTrue(result1 is LLMResult.Success)
        assertEquals(result1, result2)

        val success = result1 as LLMResult.Success
        assertTrue(success.text.contains("Mock Brief Guidance"))
        assertTrue(success.tokenCount > 0)
    }

    @Test
    fun updateGuidance_returnsDeterministicSuccessResult() = runTest {
        val deltaPrompt = "Delta Prompt for Guidance"
        val result1 = mockProvider.updateGuidance(deltaPrompt)
        val result2 = mockProvider.updateGuidance(deltaPrompt)

        assertTrue(result1 is LLMResult.Success)
        assertEquals(result1, result2)

        val success = result1 as LLMResult.Success
        assertTrue(success.text.contains("Mock Intra-Day Update"))
        assertTrue(success.tokenCount > 0)
    }
}
