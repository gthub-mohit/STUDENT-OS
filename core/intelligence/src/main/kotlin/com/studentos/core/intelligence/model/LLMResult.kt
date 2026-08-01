package com.studentos.core.intelligence.model

sealed class LLMResult {
    data class Success(val text: String, val tokenCount: Int) : LLMResult()
    data class Failure(val reason: FailureReason, val message: String) : LLMResult()
}

enum class FailureReason {
    OFFLINE,
    API_ERROR,
    RATE_LIMITED,
    INVALID_KEY,
    TIMEOUT
}
