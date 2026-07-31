package com.studentos.feature.coding.domain.model

data class CpContest(
    val id: Long = 0,
    val profileId: Long,
    val contestName: String,
    val contestDate: Long,
    val rank: Int? = null,
    val ratingChange: Int? = null,
    val problemsSolved: Int? = null
)
