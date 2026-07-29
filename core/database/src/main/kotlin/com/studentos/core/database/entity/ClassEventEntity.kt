package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ClassEventEntity — Individual occurrences of classes for attendance tracking.
 *
 * Foreign Keys:
 *  - timetable_slot_id -> timetable_slots(id) ON DELETE RESTRICT (nullable)
 *  - subject_id        -> subjects(id)        ON DELETE RESTRICT
 *  - linked_slot_id    -> timetable_slots(id) ON DELETE RESTRICT (nullable)
 *
 * Status values: UNMARKED, PRESENT, ABSENT, CANCELLED, HOLIDAY, EXTRA_CLASS
 */
@Entity(
    tableName = "class_events",
    foreignKeys = [
        ForeignKey(
            entity = TimetableSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetable_slot_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TimetableSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["linked_slot_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["timetable_slot_id"], name = "idx_events_slot"),
        Index(value = ["linked_slot_id"], name = "idx_events_linked_slot"),
        Index(value = ["subject_id", "scheduled_at"], name = "idx_events_subject_date"),
        Index(value = ["scheduled_at"], name = "idx_events_date"),
        Index(value = ["subject_id", "status"], name = "idx_events_status")
    ]
)
data class ClassEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timetable_slot_id")
    val timetableSlotId: Long? = null,

    @ColumnInfo(name = "subject_id")
    val subjectId: Long,

    @ColumnInfo(name = "linked_slot_id")
    val linkedSlotId: Long? = null,

    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Long,

    @ColumnInfo(name = "end_at")
    val endAt: Long,

    @ColumnInfo(name = "status", defaultValue = "'UNMARKED'")
    val status: String = STATUS_UNMARKED,

    @ColumnInfo(name = "is_extra", defaultValue = "0")
    val isExtra: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val STATUS_UNMARKED = "UNMARKED"
        const val STATUS_PRESENT = "PRESENT"
        const val STATUS_ABSENT = "ABSENT"
        const val STATUS_CANCELLED = "CANCELLED"
        const val STATUS_HOLIDAY = "HOLIDAY"
        const val STATUS_EXTRA_CLASS = "EXTRA_CLASS"
    }
}
