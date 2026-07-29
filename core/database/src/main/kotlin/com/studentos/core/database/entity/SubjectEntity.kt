package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SubjectEntity — Canonical registry of all academic subjects in Student OS.
 *
 * Serves as the central foreign key target for `timetable_slots`, `class_events`, and `assignments`.
 */
@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["archived_at"], name = "idx_subjects_active")
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null
)
