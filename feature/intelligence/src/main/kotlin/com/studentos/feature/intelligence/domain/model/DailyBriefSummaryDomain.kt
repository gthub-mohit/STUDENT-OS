package com.studentos.feature.intelligence.domain.model

data class DailyBriefSummaryDomain(
    val id: Long,
    val date: String,
    val scoreTarget: Int,
    val scoreActual: Int,
    val guidanceSource: String,
    val generatedAt: Long
)
