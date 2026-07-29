package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AssignmentEntity — Represents a student assignment in Room database.
 *
 * Foreign Key: subject_id -> subjects(id) ON DELETE RESTRICT
 * Indexes:
 *  - idx_assignments_deadline on (deadline, status)
 *  - idx_assignments_subject on (subject_id)
 */
@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subject_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["deadline", "status"], name = "idx_assignments_deadline"),
        Index(value = ["subject_id"], name = "idx_assignments_subject")
    ]
)
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "subject_id")
    val subjectId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "deadline")
    val deadline: Long,

    @ColumnInfo(name = "priority", defaultValue = "'MEDIUM'")
    val priority: String = PRIORITY_MEDIUM,

    @ColumnInfo(name = "status", defaultValue = "'PENDING'")
    val status: String = STATUS_PENDING,

    @ColumnInfo(name = "attachment_uri")
    val attachmentUri: String? = null,

    @ColumnInfo(name = "reminder_lead_ms")
    val reminderLeadMs: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0
) {
    companion object {
        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_LOW = "LOW"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_SUBMITTED = "SUBMITTED"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
