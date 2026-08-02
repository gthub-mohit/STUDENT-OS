package com.studentos.feature.coding.domain.model

data class CpProfile(
    val id: Long = 0,
    val platform: String,
    val handle: String,
    val currentRating: Int? = null,
    val highestRating: Int? = null,
    val rank: String? = null,
    val problemsSolved: Int? = null,
    val contestCount: Int? = null,
    val lastSyncedAt: Long? = null
)
