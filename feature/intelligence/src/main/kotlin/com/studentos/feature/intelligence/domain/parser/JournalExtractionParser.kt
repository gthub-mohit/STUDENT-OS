package com.studentos.feature.intelligence.domain.parser

import com.studentos.feature.intelligence.domain.model.ExtractionResponse
import kotlinx.serialization.json.Json

object JournalExtractionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parses raw model text into [ExtractionResponse].
     * Handles markdown code fences, trailing text, and malformed JSON safely.
     */
    fun parse(rawText: String): Result<ExtractionResponse> {
        if (rawText.isBlank()) {
            return Result.failure(IllegalArgumentException("AI response is empty"))
        }

        val cleanedJson = extractJsonPayload(rawText)
            ?: return Result.failure(IllegalArgumentException("No valid JSON payload found in AI response"))

        return try {
            val parsed = json.decodeFromString<ExtractionResponse>(cleanedJson)

            // Sanitize and validate extracted items
            val sanitizedActivities = parsed.activities.filter { it.title.isNotBlank() }
            val sanitizedCommitments = parsed.commitments.filter { it.title.isNotBlank() }

            Result.success(
                parsed.copy(
                    activities = sanitizedActivities,
                    commitments = sanitizedCommitments
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts pure JSON block from possibly markdown-wrapped response.
     */
    fun extractJsonPayload(raw: String): String? {
        val trimmed = raw.trim()

        // 1. Check for standard markdown code blocks: ```json ... ``` or ``` ... ```
        val codeBlockRegex = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(trimmed)
        if (match != null) {
            val content = match.groupValues[1].trim()
            val firstBrace = content.indexOf('{')
            val lastBrace = content.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace > firstBrace) {
                return content.substring(firstBrace, lastBrace + 1)
            }
        }

        // 2. Fallback: find outermost curly braces in raw text
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }

        return null
    }
}
