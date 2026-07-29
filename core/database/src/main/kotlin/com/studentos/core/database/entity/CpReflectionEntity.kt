package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CpReflectionEntity — Post-contest self-reflection.
 *
 * Foreign Key: contest_id -> cp_contests(id) ON DELETE CASCADE
 * Constraint: UNIQUE(contest_id), self_rating BETWEEN 1 AND 5
 */
@Entity(
    tableName = "cp_reflections",
    foreignKeys = [
        ForeignKey(
            entity = CpContestEntity::class,
            parentColumns = ["id"],
            childColumns = ["contest_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contest_id"], unique = true, name = "idx_reflections_contest")
    ]
)
data class CpReflectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "contest_id")
    val contestId: Long,

    @ColumnInfo(name = "went_wrong")
    val wentWrong: String? = null,

    @ColumnInfo(name = "to_revise")
    val toRevise: String? = null,

    @ColumnInfo(name = "self_rating")
    val selfRating: Int
)
