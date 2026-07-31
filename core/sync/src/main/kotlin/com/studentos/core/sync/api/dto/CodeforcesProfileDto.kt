package com.studentos.core.sync.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesUserResponseDto(
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("result") val result: List<CodeforcesProfileDto>? = null
)

@Serializable
data class CodeforcesProfileDto(
    @SerialName("handle") val handle: String? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("maxRating") val maxRating: Int? = null,
    @SerialName("rank") val rank: String? = null
)
