package com.studentos.feature.projects.domain.model

data class MilestoneDomain(
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String? = null,
    val targetDate: Long? = null,
    val status: String = STATUS_PENDING
) {
    val isDone: Boolean
        get() = status == STATUS_DONE

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_DONE = "DONE"
    }
}
