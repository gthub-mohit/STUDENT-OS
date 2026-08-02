package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CpProfileEntity — Competitive programming profile for CodeChef and Codeforces.
 *
 * Unique Constraint: UNIQUE(platform)
 */
@Entity(
    tableName = "cp_profiles",
    indices = [
        Index(value = ["platform"], unique = true, name = "idx_cp_platform")
    ]
)
data class CpProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "platform")
    val platform: String,

    @ColumnInfo(name = "handle")
    val handle: String,

    @ColumnInfo(name = "current_rating")
    val currentRating: Int? = null,

    @ColumnInfo(name = "highest_rating")
    val highestRating: Int? = null,

    @ColumnInfo(name = "rank")
    val rank: String? = null,

    @ColumnInfo(name = "problems_solved")
    val problemsSolved: Int? = null,

    @ColumnInfo(name = "contest_count")
    val contestCount: Int? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null
) {
    companion object {
        const val PLATFORM_CODECHEF = "CODECHEF"
        const val PLATFORM_CODEFORCES = "CODEFORCES"
    }
}
