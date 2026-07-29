package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * BugEntity — Issue tracker scoped to a project.
 *
 * Foreign Key: project_id -> projects(id) ON DELETE CASCADE
 * Index: idx_bugs_project_status on (project_id, status)
 * Constraints:
 *  - CHECK(severity IN ('LOW','MEDIUM','HIGH'))
 *  - CHECK(status IN ('OPEN','RESOLVED'))
 */
@Entity(
    tableName = "bugs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id", "status"], name = "idx_bugs_project_status")
    ]
)
data class BugEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "severity", defaultValue = "'MEDIUM'")
    val severity: String = SEVERITY_MEDIUM,

    @ColumnInfo(name = "status", defaultValue = "'OPEN'")
    val status: String = STATUS_OPEN
) {
    companion object {
        const val SEVERITY_LOW = "LOW"
        const val SEVERITY_MEDIUM = "MEDIUM"
        const val SEVERITY_HIGH = "HIGH"

        const val STATUS_OPEN = "OPEN"
        const val STATUS_RESOLVED = "RESOLVED"
    }
}
