package com.studentos.core.intelligence.limiter

import com.studentos.core.database.dao.AiCallLogDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AiCallLogEntity
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimiter @Inject constructor(
    private val aiCallLogDao: AiCallLogDao,
    private val settingsDao: SettingsDao,
    private val clock: Clock
) {
    companion object {
        const val KEY_AI_DAILY_LIMIT = "ai_daily_limit"
        const val DEFAULT_DAILY_LIMIT = 10
    }

    suspend fun getDailyLimit(): Int {
        val valStr = settingsDao.get(KEY_AI_DAILY_LIMIT)
        return valStr?.toIntOrNull() ?: DEFAULT_DAILY_LIMIT
    }

    private fun getStartOfDayEpochMs(): Long {
        val today = LocalDate.now(clock)
        return today.atStartOfDay(clock.zone).toInstant().toEpochMilli()
    }

    suspend fun callsToday(): Int {
        return aiCallLogDao.countTodaysCalls(getStartOfDayEpochMs())
    }

    suspend fun canCall(): Boolean {
        val limit = getDailyLimit()
        val count = callsToday()
        return count < limit
    }

    suspend fun remainingCalls(): Int {
        val limit = getDailyLimit()
        val count = callsToday()
        return (limit - count).coerceAtLeast(0)
    }

    suspend fun recordCall(
        triggeredBy: String = "ORCHESTRATOR",
        snapshotHash: String = "",
        wasCacheHit: Boolean = false,
        wasDelta: Boolean = false,
        latencyMs: Long? = null,
        tokenCount: Int = 0,
        success: Boolean = true,
        errorMessage: String? = null
    ): Long {
        val now = clock.millis()
        val log = AiCallLogEntity(
            triggeredBy = triggeredBy,
            snapshotHash = snapshotHash,
            wasCacheHit = wasCacheHit,
            wasDelta = wasDelta,
            latencyMs = latencyMs,
            tokenCount = tokenCount,
            success = success,
            errorMessage = errorMessage,
            createdAt = now
        )
        return aiCallLogDao.insert(log)
    }

    suspend fun resetIfNewDay() {
        // Day rollover is automatically handled via getStartOfDayEpochMs()
    }
}
