package com.studentos.core.intelligence.provider

import com.studentos.core.intelligence.model.LLMResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockProvider @Inject constructor() : LLMProvider {

    override val name: String = "MockProvider"

    override suspend fun generateBrief(prompt: String): LLMResult {
        val deterministicContent = "Mock Brief Guidance: Prioritize upcoming deadlines and complete scheduled classes."
        val mockTokenCount = prompt.length / 4 + 30
        return LLMResult.Success(
            text = "$deterministicContent [Prompt length: ${prompt.length}]",
            tokenCount = mockTokenCount
        )
    }

    override suspend fun updateGuidance(prompt: String): LLMResult {
        val deterministicContent = "Mock Intra-Day Update: Progress recorded. Continue focusing on high priority tasks."
        val mockTokenCount = prompt.length / 4 + 15
        return LLMResult.Success(
            text = "$deterministicContent [Delta length: ${prompt.length}]",
            tokenCount = mockTokenCount
        )
    }

    override fun isAvailable(): Boolean {
        return true
    }
}
