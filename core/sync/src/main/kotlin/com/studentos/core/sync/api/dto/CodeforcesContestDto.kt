package com.studentos.core.sync.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesRatingResponseDto(
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("result") val result: List<CodeforcesContestDto>? = null
)

@Serializable
data class CodeforcesContestDto(
    @SerialName("contestId") val contestId: Long? = null,
    @SerialName("contestName") val contestName: String? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("ratingUpdateTimeSeconds") val ratingUpdateTimeSeconds: Long? = null,
    @SerialName("oldRating") val oldRating: Int? = null,
    @SerialName("newRating") val newRating: Int? = null
)
