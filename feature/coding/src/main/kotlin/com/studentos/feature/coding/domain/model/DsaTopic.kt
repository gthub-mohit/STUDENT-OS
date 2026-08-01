package com.studentos.feature.coding.domain.model

data class DsaTopic(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val confidenceLevel: Int = 1,
    val revisionStatus: String = STATUS_NOT_STARTED,
    val notes: String? = null,
    val updatedAt: Long = 0
) {
    companion object {
        const val STATUS_NOT_STARTED = "NOT_STARTED"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_REVISED = "REVISED"
    }
}
