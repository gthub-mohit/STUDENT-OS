package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * GoalEntity — Long-term measurable goal (e.g. solve 200 DSA questions, log 100 project hours).
 *
 * Indices:
 *  - idx_goals_active on (is_archived, target_date)
 */
@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["is_archived", "target_date"], name = "idx_goals_active")
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String, // DSA, EMBEDDED, ML, ACADEMIC, GENERAL

    @ColumnInfo(name = "metric_type")
    val metricType: String, // COUNT, HOURS

    @ColumnInfo(name = "unit")
    val unit: String, // "questions", "hours", "commits"

    @ColumnInfo(name = "start_value")
    val startValue: Double,

    @ColumnInfo(name = "target_value")
    val targetValue: Double,

    @ColumnInfo(name = "start_date")
    val startDate: Long, // Epoch ms when tracking starts

    @ColumnInfo(name = "target_date")
    val targetDate: Long, // Target completion deadline (epoch ms)

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
