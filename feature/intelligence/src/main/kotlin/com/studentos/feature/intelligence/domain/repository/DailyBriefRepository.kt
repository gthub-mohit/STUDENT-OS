package com.studentos.feature.intelligence.domain.repository

import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.DailyBriefSummaryDomain
import kotlinx.coroutines.flow.Flow

interface DailyBriefRepository {
    fun getBriefForDate(date: String): Flow<DailyBrief?>
    fun getBriefById(id: Long): Flow<DailyBrief?>
    fun getAllBriefs(): Flow<List<DailyBrief>>
    fun getBriefSummaries(): Flow<List<DailyBriefSummaryDomain>>
    suspend fun saveBrief(brief: DailyBrief): Long
    suspend fun updateGuidance(date: String, guidance: String, source: String, updatedAt: Long)
    suspend fun updateScoreActual(date: String, scoreActual: Int)
    suspend fun getBriefByHash(hash: String): DailyBrief?
}
