package com.studentos.feature.assignments.domain.model

/**
 * TaskType — Enum representing different categories of academic tasks.
 */
enum class TaskType {
    ASSIGNMENT,
    QUIZ,
    LAB_RECORD,
    OTHER;

    val displayName: String
        get() = when (this) {
            ASSIGNMENT -> "Assignment"
            QUIZ -> "Quiz"
            LAB_RECORD -> "Lab Record"
            OTHER -> "Other"
        }

    companion object {
        fun fromString(value: String?): TaskType {
            return when (value?.trim()?.uppercase()) {
                "QUIZ" -> QUIZ
                "LAB_RECORD", "LAB", "LABRECORD", "LAB RECORD", "PRACTICAL" -> LAB_RECORD
                "OTHER" -> OTHER
                else -> ASSIGNMENT
            }
        }
    }
}
