package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ActivityLogEntity — Persisted record of confirmed daily activity execution.
 *
 * Foreign Keys:
 *  - goal_id -> goals(id) ON DELETE SET NULL
 *  - commitment_id -> commitments(id) ON DELETE SET NULL
 *
 * Indices:
 *  - idx_activity_goal on (goal_id)
 *  - idx_activity_commitment on (commitment_id)
 *  - idx_activity_timestamp on (timestamp)
 */
@Entity(
    tableName = "activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitment_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["goal_id"], name = "idx_activity_goal"),
        Index(value = ["commitment_id"], name = "idx_activity_commitment"),
        Index(value = ["timestamp"], name = "idx_activity_timestamp")
    ]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,

    @ColumnInfo(name = "commitment_id")
    val commitmentId: Long? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // When activity occurred (epoch ms)

    @ColumnInfo(name = "metric_delta")
    val metricDelta: Double, // Increment (+5.0, -2.0, etc.)

    @ColumnInfo(name = "activity_type")
    val activityType: String, // DSA, PROJECT, ASSIGNMENT, CLUB, LEARNING, GENERAL

    @ColumnInfo(name = "summary")
    val summary: String,

    @ColumnInfo(name = "raw_input")
    val rawInput: String? = null,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null, // null = unknown, 0 = instant

    @ColumnInfo(name = "dedup_hash")
    val dedupHash: String? = null,

    @ColumnInfo(name = "confirmed_at")
    val confirmedAt: Long // When confirmed and persisted (epoch ms)
)
