package com.studentos.feature.intelligence.domain.analyzer

interface IntelligenceAnalyzer {
    val key: String
    suspend fun analyze(todayDate: String): Any
}
