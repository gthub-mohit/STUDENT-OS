package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProjectTaskEntity — Task list for a project with dependency and priority support.
 *
 * Foreign Key: project_id -> projects(id) ON DELETE CASCADE
 * Indices:
 *  - idx_tasks_project_next on (project_id, is_next_action)
 *  - idx_tasks_dependency on (dependency_task_id)
 */
@Entity(
    tableName = "project_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id", "is_next_action"], name = "idx_tasks_project_next"),
        Index(value = ["dependency_task_id"], name = "idx_tasks_dependency")
    ]
)
data class ProjectTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "is_next_action", defaultValue = "0")
    val isNextAction: Boolean = false,

    @ColumnInfo(name = "is_parallel", defaultValue = "0")
    val isParallel: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "dependency_task_id")
    val dependencyTaskId: Long? = null,

    @ColumnInfo(name = "priority", defaultValue = "MEDIUM")
    val priority: String = "MEDIUM",

    @ColumnInfo(name = "deadline")
    val deadline: Long? = null
)
