package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProjectResourceEntity — Reference links attached to a project.
 *
 * Foreign Key: project_id -> projects(id) ON DELETE CASCADE
 * Index: idx_resources_project on (project_id)
 */
@Entity(
    tableName = "project_resources",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id"], name = "idx_resources_project")
    ]
)
data class ProjectResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "project_id")
    val projectId: Long,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "label")
    val label: String? = null
)
