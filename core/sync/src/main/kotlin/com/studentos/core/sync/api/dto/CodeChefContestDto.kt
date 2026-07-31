package com.studentos.core.sync.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodeChefContestDto(
    @SerialName("code") val code: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("ratingChange") val ratingChange: Int? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("getratedDate") val getRatedDate: Long? = null,
    @SerialName("problemsSolved") val problemsSolved: Int? = null
)
