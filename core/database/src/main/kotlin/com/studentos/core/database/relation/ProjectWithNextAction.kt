package com.studentos.core.database.relation

import androidx.room.Embedded
import com.studentos.core.database.entity.ProjectEntity

/**
 * ProjectWithNextAction — Projection data class combining a project entity with its next immediate action task.
 */
data class ProjectWithNextAction(
    @Embedded
    val project: ProjectEntity,

    val nextActionId: Long? = null,
    val nextActionTitle: String? = null,
    val isNextAction: Boolean? = null,
    val isParallel: Boolean? = null,
    val completedAt: Long? = null,
    val sortOrder: Int? = null
)
