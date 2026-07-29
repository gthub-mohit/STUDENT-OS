package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProjectTaskEntity — Task list for a project.
 *
 * Foreign Key: project_id -> projects(id) ON DELETE CASCADE
 * Index: idx_tasks_project_next on (project_id, is_next_action)
 * Partial Unique Index (via DDL): CREATE UNIQUE INDEX idx_one_next_action ON project_tasks(project_id) WHERE is_next_action = 1 AND is_parallel = 0
 * Constraints:
 *  - CHECK(is_next_action IN (0,1))
 *  - CHECK(is_parallel IN (0,1))
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
        Index(value = ["project_id", "is_next_action"], name = "idx_tasks_project_next")
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
    val sortOrder: Int = 0
)
