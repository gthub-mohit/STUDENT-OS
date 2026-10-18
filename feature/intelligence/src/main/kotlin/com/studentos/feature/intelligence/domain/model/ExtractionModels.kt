package com.studentos.feature.intelligence.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Raw structured extraction payload returned by OmniRoute.
 */
@Serializable
data class ExtractionResponse(
    @SerialName("summary")
    val summary: String? = null,

    @SerialName("activities")
    val activities: List<ExtractedActivityDto> = emptyList(),

    @SerialName("commitments")
    val commitments: List<ExtractedCommitmentDto> = emptyList(),

    @SerialName("clarifications")
    val clarifications: List<String> = emptyList()
)

/**
 * AI-extracted activity item before user verification.
 */
@Serializable
data class ExtractedActivityDto(
    @SerialName("title")
    val title: String,

    @SerialName("activityType")
    val activityType: String = "GENERAL", // DSA, PROJECT, ASSIGNMENT, CLUB, LEARNING, GENERAL

    @SerialName("metricDelta")
    val metricDelta: Double? = null,

    @SerialName("durationMinutes")
    val durationMinutes: Int? = null, // null if unknown, 0 if instant

    @SerialName("matchedGoalId")
    val matchedGoalId: Long? = null,

    @SerialName("confidence")
    val confidence: Float = 1.0f
)

/**
 * AI-extracted commitment or deadline before user verification.
 */
@Serializable
data class ExtractedCommitmentDto(
    @SerialName("title")
    val title: String,

    @SerialName("category")
    val category: String = "ACADEMIC", // ACADEMIC, PROJECT, CLUB, LEARNING, PERSONAL

    @SerialName("targetDate")
    val targetDate: String? = null, // ISO-8601 "YYYY-MM-DD" or null if unspecified

    @SerialName("targetTime")
    val targetTime: String? = null, // "HH:mm" or null if unspecified

    @SerialName("priority")
    val priority: String? = "MEDIUM", // HIGH, MEDIUM, LOW

    @SerialName("estimatedMinutes")
    val estimatedMinutes: Int? = null // null if unknown
)

/**
 * Editable representation of an extracted activity shown in preview before confirmation.
 */
data class EditableActivity(
    val tempId: String = UUID.randomUUID().toString(),
    var title: String,
    var activityType: String = "GENERAL",
    var metricDelta: Double? = null,
    var durationMinutes: Int? = null,
    var matchedGoalId: Long? = null,
    val confidence: Float = 1.0f
)

/**
 * Editable representation of an extracted commitment shown in preview before confirmation.
 */
data class EditableCommitment(
    val tempId: String = UUID.randomUUID().toString(),
    var title: String,
    var category: String = "ACADEMIC",
    var targetDate: String? = null,
    var targetTime: String? = null,
    var priority: String = "MEDIUM",
    var estimatedMinutes: Int? = null
)

/**
 * Result of the extraction process.
 */
sealed class ExtractionResult {
    data class Success(
        val response: ExtractionResponse,
        val journalEntryId: Long
    ) : ExtractionResult()

    data class Error(
        val message: String,
        val journalEntryId: Long? = null,
        val isOffline: Boolean = false,
        val cause: Throwable? = null
    ) : ExtractionResult()
}
