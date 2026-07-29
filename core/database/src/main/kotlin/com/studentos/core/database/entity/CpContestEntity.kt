package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CpContestEntity — Competitive programming contest history.
 *
 * Foreign Key: profile_id -> cp_profiles(id) ON DELETE CASCADE
 * Compound Uniqueness: UNIQUE(profile_id, contest_name, contest_date)
 */
@Entity(
    tableName = "cp_contests",
    foreignKeys = [
        ForeignKey(
            entity = CpProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id", "contest_date"], name = "idx_contests_profile_date"),
        Index(
            value = ["profile_id", "contest_name", "contest_date"],
            unique = true,
            name = "idx_contests_unique"
        )
    ]
)
data class CpContestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: Long,

    @ColumnInfo(name = "contest_name")
    val contestName: String,

    @ColumnInfo(name = "contest_date")
    val contestDate: Long,

    @ColumnInfo(name = "rank")
    val rank: Int? = null,

    @ColumnInfo(name = "rating_change")
    val ratingChange: Int? = null,

    @ColumnInfo(name = "problems_solved")
    val problemsSolved: Int? = null
)
