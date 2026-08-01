package com.studentos.core.intelligence.cache

import com.studentos.core.database.dao.RecommendationCacheDao
import com.studentos.core.database.entity.RecommendationCacheEntity
import com.studentos.core.intelligence.fallback.GuidanceItem
import com.studentos.core.intelligence.fallback.GuidanceResult
import com.studentos.core.intelligence.fallback.GuidanceSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class RecommendationCacheTest {

    private val dao: RecommendationCacheDao = mockk(relaxed = true)

    private val fixedInstant = Instant.parse("2026-08-01T12:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var cache: RecommendationCache

    @Before
    fun setUp() {
        cache = RecommendationCache(dao, clock)
    }

    @Test
    fun get_cacheHit_returnsCachedRecommendation() = runTest {
        val entity = RecommendationCacheEntity(
            id = 1,
            snapshotHash = "hash123",
            llmResponse = "Guidance output",
            provider = "DEEPSEEK",
            createdAt = fixedInstant.toEpochMilli() - 3600000L, // 1 hour ago
            tokenCount = 120
        )
        coEvery { dao.getByHash("hash123") } returns entity

        val result = cache.get("hash123")

        assertNotNull(result)
        assertEquals("hash123", result?.snapshotHash)
        assertEquals("Guidance output", result?.llmResponse)
        assertEquals("DEEPSEEK", result?.provider)
        assertEquals(120, result?.tokenCount)
    }

    @Test
    fun get_cacheMiss_returnsNull() = runTest {
        coEvery { dao.getByHash("unknown_hash") } returns null

        val result = cache.get("unknown_hash")

        assertNull(result)
    }

    @Test
    fun get_expiredCacheEntry_deletesAndReturnsNull() = runTest {
        val expiredInstant = fixedInstant.toEpochMilli() - (25 * 60 * 60 * 1000L) // 25 hours ago (> 24h TTL)
        val entity = RecommendationCacheEntity(
            id = 1,
            snapshotHash = "expired_hash",
            llmResponse = "Old output",
            provider = "DEEPSEEK",
            createdAt = expiredInstant,
            tokenCount = 50
        )
        coEvery { dao.getByHash("expired_hash") } returns entity

        val result = cache.get("expired_hash")

        assertNull(result)
        coVerify { dao.deleteByHash("expired_hash") }
    }

    @Test
    fun put_duplicateHash_overwritesResponseAndRefreshesTimestamp() = runTest {
        val existing = RecommendationCacheEntity(
            id = 5,
            snapshotHash = "hash_dup",
            llmResponse = "Old response",
            provider = "DEEPSEEK",
            createdAt = 1000L,
            tokenCount = 10
        )
        coEvery { dao.getByHash("hash_dup") } returns existing

        cache.put("hash_dup", "New response", "DEEPSEEK", 40)

        val expectedEntity = RecommendationCacheEntity(
            id = 5,
            snapshotHash = "hash_dup",
            llmResponse = "New response",
            provider = "DEEPSEEK",
            createdAt = fixedInstant.toEpochMilli(),
            tokenCount = 40
        )
        coVerify { dao.upsert(expectedEntity) }
        coVerify { dao.deleteOldestBeyondLimit(7) }
    }

    @Test
    fun put_retentionLimitExceeded_deletesOldestEntriesBeyond7() = runTest {
        coEvery { dao.getByHash(any()) } returns null

        cache.put("hash_new", "Fresh recommendation", "DEEPSEEK", 30)

        coVerify { dao.deleteOldestBeyondLimit(7) }
    }

    @Test
    fun clearExpired_deletesEntriesOlderThan24Hours() = runTest {
        val expectedThreshold = fixedInstant.toEpochMilli() - (24 * 60 * 60 * 1000L)
        coEvery { dao.deleteExpired(expectedThreshold) } returns 3

        val deletedCount = cache.clearExpired()

        assertEquals(3, deletedCount)
        coVerify { dao.deleteExpired(expectedThreshold) }
    }

    @Test
    fun putGuidance_serializesAndCachesGuidanceResult() = runTest {
        coEvery { dao.getByHash("hash_guidance") } returns null

        val guidance = GuidanceResult(
            summary = "Summary",
            recommendations = listOf(
                GuidanceItem(priority = 1, category = "ATTENDANCE", title = "Title", description = "Desc")
            ),
            source = GuidanceSource.OFFLINE
        )

        cache.putGuidance("hash_guidance", guidance)

        coVerify { dao.upsert(match { it.snapshotHash == "hash_guidance" && it.provider == "OFFLINE" && it.llmResponse.contains("Summary") }) }
    }

    @Test
    fun size_returnsDaoCount() = runTest {
        coEvery { dao.getCount() } returns 5

        val size = cache.size()

        assertEquals(5, size)
    }
}
