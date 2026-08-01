package com.studentos.core.intelligence.limiter

import com.studentos.core.database.dao.AiCallLogDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AiCallLogEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RateLimiterTest {

    private val aiCallLogDao: AiCallLogDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)

    private val day1Instant = Instant.parse("2026-08-01T10:00:00Z")
    private val day2Instant = Instant.parse("2026-08-02T10:00:00Z")

    private var currentInstant: Instant = day1Instant
    private val clock: Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = currentInstant
    }

    private lateinit var rateLimiter: RateLimiter

    @Before
    fun setUp() {
        currentInstant = day1Instant
        rateLimiter = RateLimiter(aiCallLogDao, settingsDao, clock)
    }

    @Test
    fun getDailyLimit_defaultLimit_returns10() = runTest {
        coEvery { settingsDao.get("ai_daily_limit") } returns null

        val limit = rateLimiter.getDailyLimit()

        assertEquals(10, limit)
    }

    @Test
    fun getDailyLimit_customLimit_returnsConfiguredValue() = runTest {
        coEvery { settingsDao.get("ai_daily_limit") } returns "5"

        val limit = rateLimiter.getDailyLimit()

        assertEquals(5, limit)
    }

    @Test
    fun recordCall_incrementsCallLogAndUsesClockTimestamp() = runTest {
        coEvery { aiCallLogDao.insert(any()) } returns 1L

        rateLimiter.recordCall(triggeredBy = "TEST", snapshotHash = "hash123")

        val expectedEntity = AiCallLogEntity(
            triggeredBy = "TEST",
            snapshotHash = "hash123",
            wasCacheHit = false,
            wasDelta = false,
            latencyMs = null,
            tokenCount = 0,
            success = true,
            errorMessage = null,
            createdAt = day1Instant.toEpochMilli()
        )
        coVerify { aiCallLogDao.insert(expectedEntity) }
    }

    @Test
    fun canCall_belowLimit_returnsTrue() = runTest {
        coEvery { settingsDao.get("ai_daily_limit") } returns "5"
        coEvery { aiCallLogDao.countTodaysCalls(any()) } returns 3

        val canCall = rateLimiter.canCall()

        assertTrue(canCall)
    }

    @Test
    fun canCall_atLimit_returnsFalse() = runTest {
        coEvery { settingsDao.get("ai_daily_limit") } returns "5"
        coEvery { aiCallLogDao.countTodaysCalls(any()) } returns 5

        val canCall = rateLimiter.canCall()

        assertFalse(canCall)
    }

    @Test
    fun remainingCalls_calculatesCorrectDifference_neverNegative() = runTest {
        coEvery { settingsDao.get("ai_daily_limit") } returns "5"
        coEvery { aiCallLogDao.countTodaysCalls(any()) } returns 7 // Over limit

        val remaining = rateLimiter.remainingCalls()

        assertEquals(0, remaining)
    }

    @Test
    fun automaticReset_onNextDay_returnsZeroCallsToday() = runTest {
        val day1StartEpoch = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()
        val day2StartEpoch = Instant.parse("2026-08-02T00:00:00Z").toEpochMilli()

        coEvery { aiCallLogDao.countTodaysCalls(day1StartEpoch) } returns 5
        coEvery { aiCallLogDao.countTodaysCalls(day2StartEpoch) } returns 0

        currentInstant = day1Instant
        assertEquals(5, rateLimiter.callsToday())

        currentInstant = day2Instant
        assertEquals(0, rateLimiter.callsToday())
        assertTrue(rateLimiter.canCall())
    }

    @Test
    fun concurrentCalls_threadSafeExecution() = runTest {
        coEvery { aiCallLogDao.insert(any()) } returns 1L

        val jobs = List(10) { index ->
            async {
                rateLimiter.recordCall(triggeredBy = "THREAD_$index")
            }
        }
        jobs.awaitAll()

        coVerify(exactly = 10) { aiCallLogDao.insert(any()) }
    }
}
