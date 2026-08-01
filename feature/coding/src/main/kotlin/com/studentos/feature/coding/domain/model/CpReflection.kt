package com.studentos.feature.coding.domain.model

data class CpReflection(
    val id: Long = 0,
    val contestId: Long,
    val wentWrong: String? = null,
    val toRevise: String? = null,
    val selfRating: Int
)
