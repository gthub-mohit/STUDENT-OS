package com.studentos.core.sync.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeChefProfileResponseDto(
    @SerialName("status") val status: String? = null,
    @SerialName("success") val success: Boolean? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("handle") val handle: String? = null,
    @SerialName("currentRating") val currentRating: Int? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("stars") val stars: String? = null,
    @SerialName("globalRank") val globalRank: Int? = null,
    @SerialName("countryRank") val countryRank: Int? = null,
    @SerialName("ratingData") val ratingData: List<CodeChefContestDto>? = null
)

@Serializable
data class CodeChefProfileDto(
    @SerialName("handle") val handle: String? = null,
    @SerialName("currentRating") val currentRating: Int? = null,
    @SerialName("rating") val rating: Int? = null
)
