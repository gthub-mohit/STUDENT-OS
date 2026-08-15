package com.studentos.core.intelligence.provider

import com.studentos.core.intelligence.model.LLMResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockProvider @Inject constructor() : LLMProvider {

    override val name: String = "MockProvider"

    override suspend fun generateBrief(prompt: String): LLMResult {
        return LLMResult.Success(
            text = "Prioritize upcoming deadlines and complete scheduled classes.",
            tokenCount = 40
        )
    }

    override suspend fun updateGuidance(prompt: String): LLMResult {
        return LLMResult.Success(
            text = "Progress recorded. Continue focusing on high priority tasks.",
            tokenCount = 25
        )
    }

    override fun isAvailable(): Boolean {
        return true
    }
}
