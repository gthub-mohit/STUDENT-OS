package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TimetableSlotEntity — Defines recurring weekly timetable slot templates.
 *
 * Foreign Key: subject_id -> subjects(id) ON DELETE RESTRICT
 * Compound Uniqueness: UNIQUE(subject_id, day_of_week, start_time, week_parity, valid_from)
 */
@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["subject_id"], name = "idx_slots_subject"),
        Index(value = ["day_of_week", "week_parity"], name = "idx_slots_day"),
        Index(
            value = ["subject_id", "day_of_week", "start_time", "week_parity", "valid_from"],
            unique = true,
            name = "idx_slots_unique"
        )
    ]
)
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "subject_id")
    val subjectId: Long,

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,

    @ColumnInfo(name = "start_time")
    val startTime: String,

    @ColumnInfo(name = "end_time")
    val endTime: String,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "week_parity")
    val weekParity: String? = null,

    @ColumnInfo(name = "valid_from")
    val validFrom: Long,

    @ColumnInfo(name = "valid_until")
    val validUntil: Long? = null
)
