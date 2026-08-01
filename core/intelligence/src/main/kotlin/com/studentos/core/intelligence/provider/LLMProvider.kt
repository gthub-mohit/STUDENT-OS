package com.studentos.core.intelligence.provider

import com.studentos.core.intelligence.model.LLMResult

interface LLMProvider {
    val name: String
    suspend fun generateBrief(prompt: String): LLMResult
    suspend fun updateGuidance(prompt: String): LLMResult
    fun isAvailable(): Boolean
}
