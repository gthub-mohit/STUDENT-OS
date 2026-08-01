package com.studentos.feature.intelligence.repository

import app.cash.turbine.test
import com.studentos.core.database.dao.DailyBriefDao
import com.studentos.core.database.entity.DailyBriefEntity
import com.studentos.core.database.relation.DailyBriefSummary
import com.studentos.feature.intelligence.data.repository.DailyBriefRepositoryImpl
import com.studentos.feature.intelligence.domain.model.DailyBrief
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DailyBriefRepositoryTest {

    private class FakeDailyBriefDao : DailyBriefDao {
        val briefs = mutableListOf<DailyBriefEntity>()
        private val briefsFlow = MutableStateFlow<List<DailyBriefEntity>>(emptyList())

        override suspend fun insert(brief: DailyBriefEntity): Long {
            val index = briefs.indexOfFirst { it.date == brief.date }
            val id = if (index >= 0) briefs[index].id else (briefs.size + 1).toLong()
            val created = brief.copy(id = id)
            if (index >= 0) {
                briefs[index] = created
            } else {
                briefs.add(created)
            }
            briefsFlow.value = briefs.toList()
            return id
        }

        override suspend fun updateGuidance(
            date: String,
            guidance: String,
            source: String,
            updatedAt: Long
        ) {
            val index = briefs.indexOfFirst { it.date == date }
            if (index >= 0) {
                val current = briefs[index]
                briefs[index] = current.copy(
                    llmGuidance = guidance,
                    guidanceSource = source,
                    guidanceUpdatedAt = updatedAt
                )
                briefsFlow.value = briefs.toList()
            }
        }

        override suspend fun updateScoreActual(date: String, scoreActual: Int) {
            val index = briefs.indexOfFirst { it.date == date }
            if (index >= 0) {
                val current = briefs[index]
                briefs[index] = current.copy(scoreActual = scoreActual)
                briefsFlow.value = briefs.toList()
            }
        }

        override fun getBriefForDate(date: String): Flow<DailyBriefEntity?> =
            briefsFlow.map { list -> list.firstOrNull { it.date == date } }

        override fun getAllBriefs(): Flow<List<DailyBriefEntity>> = briefsFlow

        override fun getBriefSummaries(): Flow<List<DailyBriefSummary>> =
            briefsFlow.map { list ->
                list.map {
                    DailyBriefSummary(
                        id = it.id,
                        date = it.date,
                        scoreTarget = it.scoreTarget,
                        scoreActual = it.scoreActual,
                        guidanceSource = it.guidanceSource,
                        generatedAt = it.generatedAt
                    )
                }
            }

        override suspend fun getBriefByHash(hash: String): DailyBriefEntity? =
            briefs.firstOrNull { it.snapshotHash == hash }

        override suspend fun getScoreHistory(limit: Int): List<DailyBriefEntity> =
            briefs.take(limit)
    }

    private lateinit var dao: FakeDailyBriefDao
    private lateinit var repository: DailyBriefRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeDailyBriefDao()
        repository = DailyBriefRepositoryImpl(dao)
    }

    @Test
    fun saveBrief_and_getBriefForDate_mapsCorrectly() = runTest {
        val brief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 80
        )

        repository.getBriefForDate("2026-08-01").test {
            assertNull(awaitItem())

            repository.saveBrief(brief)

            val item = awaitItem()
            assertNotNull(item)
            assertEquals("2026-08-01", item?.date)
            assertEquals("hash123", item?.snapshotHash)
            assertEquals(100, item?.scoreTarget)
            assertEquals(80, item?.scoreActual)
        }
    }

    @Test
    fun getBriefForDate_notFound_returnsNull() = runTest {
        repository.getBriefForDate("2099-01-01").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun updateGuidance_updatesEntityInDao() = runTest {
        val brief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            guidanceSource = DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC
        )
        repository.saveBrief(brief)

        repository.updateGuidance("2026-08-01", "Focus on DSA today", DailyBrief.GUIDANCE_SOURCE_LLM, 1000L)

        val updated = repository.getBriefByHash("hash123")
        assertNotNull(updated)
        assertEquals("Focus on DSA today", updated?.llmGuidance)
        assertEquals(DailyBrief.GUIDANCE_SOURCE_LLM, updated?.guidanceSource)
    }
}
