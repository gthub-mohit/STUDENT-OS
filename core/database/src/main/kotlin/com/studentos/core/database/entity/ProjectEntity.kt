package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProjectEntity — Project records, serving as the root entity for milestones, bugs, tasks, and resources.
 *
 * Index: idx_projects_active on (archived_at)
 * Constraint: CHECK(inactivity_threshold_days > 0)
 */
@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["archived_at"], name = "idx_projects_active")
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,

    @ColumnInfo(name = "inactivity_threshold_days", defaultValue = "7")
    val inactivityThresholdDays: Int = 7,

    @ColumnInfo(name = "last_activity_at")
    val lastActivityAt: Long
)
