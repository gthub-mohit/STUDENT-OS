package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * MilestoneEntity — Time-bounded goals within a project.
 *
 * Foreign Key: project_id -> projects(id) ON DELETE CASCADE
 * Index: idx_milestones_project on (project_id, status)
 * Constraint: CHECK(status IN ('PENDING','IN_PROGRESS','DONE'))
 */
@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id", "status"], name = "idx_milestones_project")
    ]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "status", defaultValue = "'PENDING'")
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_DONE = "DONE"
    }
}
