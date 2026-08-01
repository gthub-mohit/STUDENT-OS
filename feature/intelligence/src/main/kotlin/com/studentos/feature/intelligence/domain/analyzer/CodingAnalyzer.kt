package com.studentos.feature.intelligence.domain.analyzer

import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.feature.intelligence.domain.model.fact.CodingFact
import com.studentos.feature.intelligence.domain.model.fact.ContestItemFact
import com.studentos.feature.intelligence.domain.model.fact.DsaTopicFact
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

class CodingAnalyzer @Inject constructor(
    private val cpProfileDao: CpProfileDao,
    private val cpContestDao: CpContestDao,
    private val dsaTopicDao: DsaTopicDao,
    private val clock: Clock
) : IntelligenceAnalyzer {

    override val key: String = KEY

    override suspend fun analyze(todayDate: String): CodingFact {
        val nowMs = clock.millis()
        val profiles = cpProfileDao.getAllProfiles().first()
        val lastSyncedAt = profiles.mapNotNull { it.lastSyncedAt }.maxOrNull()

        val lookaheadMs = nowMs + (24 * 3600 * 1000L)
        val upcomingContests = cpContestDao.getUpcomingContests(nowMs, lookaheadMs)
            .map { contest ->
                val hoursUntil = maxOf(0L, (contest.contestDate - nowMs) / (3600 * 1000L))
                ContestItemFact(
                    id = contest.id,
                    name = contest.contestName,
                    dateEpochMs = contest.contestDate,
                    startsInHours = hoursUntil
                )
            }

        val suggestedTopicEntity = dsaTopicDao.getSuggestedTopic()
        val suggestedDsaTopic = suggestedTopicEntity?.let { topic ->
            DsaTopicFact(
                id = topic.id,
                categoryId = topic.categoryId,
                name = topic.name,
                confidenceLevel = topic.confidenceLevel
            )
        }

        return CodingFact(
            lastSyncedAtEpochMs = lastSyncedAt,
            upcomingContests = upcomingContests,
            suggestedDsaTopic = suggestedDsaTopic
        )
    }

    companion object {
        const val KEY = "coding"
    }
}
