package com.studentos.feature.projects.domain.model

data class ProjectResourceDomain(
    val id: Long = 0,
    val projectId: Long,
    val url: String,
    val label: String? = null,
    val type: String = TYPE_LINK
) {
    val displayTitle: String
        get() = if (!label.isNullOrBlank()) label else url

    companion object {
        const val TYPE_LINK = "LINK"
        const val TYPE_NOTE = "NOTE"
        const val TYPE_DOCUMENTATION = "DOCUMENTATION"
        const val TYPE_FILE = "FILE"
    }
}
