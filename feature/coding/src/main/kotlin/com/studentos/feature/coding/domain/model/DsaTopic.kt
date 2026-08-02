package com.studentos.feature.coding.domain.model

data class DsaTopic(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val difficulty: String = DIFFICULTY_MEDIUM,
    val confidenceLevel: Int = 1,
    val revisionStatus: String = STATUS_NOT_STARTED,
    val nextRevisionDate: Long? = null,
    val notes: String? = null,
    val updatedAt: Long = 0
) {
    companion object {
        const val STATUS_NOT_STARTED = "NOT_STARTED"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_REVISED = "REVISED"

        const val DIFFICULTY_EASY = "EASY"
        const val DIFFICULTY_MEDIUM = "MEDIUM"
        const val DIFFICULTY_HARD = "HARD"
    }
}
