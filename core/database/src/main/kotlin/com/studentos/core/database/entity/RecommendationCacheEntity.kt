package com.studentos.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RecommendationCacheEntity — Caches LLM responses keyed by snapshot hash.
 *
 * Index: idx_rec_cache_hash (UNIQUE) on (snapshot_hash)
 * Constraints: UNIQUE(snapshot_hash), CHECK(token_count >= 0)
 */
@Entity(
    tableName = "recommendation_cache",
    indices = [
        Index(value = ["snapshot_hash"], unique = true, name = "idx_rec_cache_hash")
    ]
)
data class RecommendationCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "snapshot_hash")
    val snapshotHash: String,

    @ColumnInfo(name = "llm_response")
    val llmResponse: String,

    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "token_count")
    val tokenCount: Int
)
