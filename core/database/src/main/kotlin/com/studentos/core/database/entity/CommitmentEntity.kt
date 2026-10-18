package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CommitmentEntity — Actionable student commitment or deadline (academic, project, club, learning).
 *
 * Distinguishes unknown duration (null) from zero duration (0).
 *
 * Indices:
 *  - idx_commitments_deadline on (status, deadline)
 */
@Entity(
    tableName = "commitments",
    indices = [
        Index(value = ["status", "deadline"], name = "idx_commitments_deadline")
    ]
)
data class CommitmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String, // ACADEMIC, PROJECT, CLUB, LEARNING, PERSONAL

    @ColumnInfo(name = "deadline")
    val deadline: Long? = null, // Epoch ms (null = open-ended)

    @ColumnInfo(name = "priority", defaultValue = "'MEDIUM'")
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW

    @ColumnInfo(name = "status", defaultValue = "'PENDING'")
    val status: String = "PENDING", // PENDING, COMPLETED, CANCELLED

    @ColumnInfo(name = "estimated_minutes")
    val estimatedMinutes: Int? = null, // null = unknown, 0 = instant

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null // Epoch ms when marked completed
)
