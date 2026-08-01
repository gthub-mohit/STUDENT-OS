package com.studentos.feature.intelligence.data.repository

import com.studentos.core.database.dao.DailyBriefDao
import com.studentos.core.database.entity.DailyBriefEntity
import com.studentos.core.database.relation.DailyBriefSummary
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.DailyBriefSummaryDomain
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DailyBriefRepositoryImpl @Inject constructor(
    private val dailyBriefDao: DailyBriefDao
) : DailyBriefRepository {

    override fun getBriefForDate(date: String): Flow<DailyBrief?> {
        return dailyBriefDao.getBriefForDate(date).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getBriefById(id: Long): Flow<DailyBrief?> {
        return dailyBriefDao.getAllBriefs().map { list ->
            list.firstOrNull { it.id == id }?.toDomain()
        }
    }

    override fun getAllBriefs(): Flow<List<DailyBrief>> {
        return dailyBriefDao.getAllBriefs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBriefSummaries(): Flow<List<DailyBriefSummaryDomain>> {
        return dailyBriefDao.getBriefSummaries().map { summaries ->
            summaries.map { it.toDomain() }
        }
    }

    override suspend fun saveBrief(brief: DailyBrief): Long {
        return dailyBriefDao.insert(brief.toEntity())
    }

    override suspend fun updateGuidance(
        date: String,
        guidance: String,
        source: String,
        updatedAt: Long
    ) {
        dailyBriefDao.updateGuidance(date, guidance, source, updatedAt)
    }

    override suspend fun updateScoreActual(date: String, scoreActual: Int) {
        dailyBriefDao.updateScoreActual(date, scoreActual)
    }

    override suspend fun getBriefByHash(hash: String): DailyBrief? {
        return dailyBriefDao.getBriefByHash(hash)?.toDomain()
    }

    private fun DailyBriefEntity.toDomain() = DailyBrief(
        id = id,
        date = date,
        jsonSnapshot = jsonSnapshot,
        snapshotHash = snapshotHash,
        briefJson = briefJson,
        llmGuidance = llmGuidance,
        guidanceSource = guidanceSource,
        scoreTarget = scoreTarget,
        scoreActual = scoreActual,
        generatedAt = generatedAt,
        guidanceUpdatedAt = guidanceUpdatedAt
    )

    private fun DailyBriefSummary.toDomain() = DailyBriefSummaryDomain(
        id = id,
        date = date,
        scoreTarget = scoreTarget,
        scoreActual = scoreActual,
        guidanceSource = guidanceSource,
        generatedAt = generatedAt
    )

    private fun DailyBrief.toEntity() = DailyBriefEntity(
        id = id,
        date = date,
        jsonSnapshot = jsonSnapshot,
        snapshotHash = snapshotHash,
        briefJson = briefJson,
        llmGuidance = llmGuidance,
        guidanceSource = guidanceSource,
        scoreTarget = scoreTarget,
        scoreActual = scoreActual,
        generatedAt = generatedAt,
        guidanceUpdatedAt = guidanceUpdatedAt
    )
}
