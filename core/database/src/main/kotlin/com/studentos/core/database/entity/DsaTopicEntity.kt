package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DsaTopicEntity — Individual DSA topics within a category, tracking mastery status and revision queue.
 *
 * Foreign Key: category_id -> dsa_categories(id) ON DELETE RESTRICT
 * Constraints:
 *  - UNIQUE(category_id, name)
 *  - confidence_level BETWEEN 1 AND 5
 *  - revision_status IN ('NOT_STARTED', 'IN_PROGRESS', 'REVISED')
 */
@Entity(
    tableName = "dsa_topics",
    foreignKeys = [
        ForeignKey(
            entity = DsaCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["category_id"], name = "idx_topics_category"),
        Index(value = ["revision_status", "confidence_level"], name = "idx_topics_suggestion"),
        Index(value = ["category_id", "name"], unique = true, name = "idx_topics_category_name")
    ]
)
data class DsaTopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "difficulty", defaultValue = "'MEDIUM'")
    val difficulty: String = DIFFICULTY_MEDIUM,

    @ColumnInfo(name = "confidence_level", defaultValue = "1")
    val confidenceLevel: Int = 1,

    @ColumnInfo(name = "revision_status", defaultValue = "'NOT_STARTED'")
    val revisionStatus: String = STATUS_NOT_STARTED,

    @ColumnInfo(name = "next_revision_date")
    val nextRevisionDate: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = 0
) {
    companion object {
        const val STATUS_NOT_STARTED = "NOT_STARTED"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_REVISED = "REVISED"

        const val DIFFICULTY_EASY = "EASY"
        const val DIFFICULTY_MEDIUM = "MEDIUM"
        const val DIFFICULTY_HARD = "HARD"
    }
}
