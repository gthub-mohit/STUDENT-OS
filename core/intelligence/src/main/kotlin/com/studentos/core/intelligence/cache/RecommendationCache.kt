package com.studentos.core.intelligence.cache

import com.studentos.core.database.dao.RecommendationCacheDao
import com.studentos.core.database.entity.RecommendationCacheEntity
import com.studentos.core.intelligence.fallback.GuidanceResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationCache @Inject constructor(
    private val recommendationCacheDao: RecommendationCacheDao,
    private val clock: Clock
) {
    companion object {
        const val TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val MAX_RETENTION_LIMIT = 7
    }

    suspend fun get(snapshotHash: String): CachedRecommendation? {
        val entity = recommendationCacheDao.getByHash(snapshotHash) ?: return null
        val now = clock.millis()
        val age = now - entity.createdAt

        val isInvalid = age < 0 ||
                age > TTL_MS ||
                entity.provider.equals("MockProvider", ignoreCase = true) ||
                entity.provider.equals("MOCK", ignoreCase = true) ||
                entity.llmResponse.contains("Mock Brief Guidance", ignoreCase = true) ||
                entity.llmResponse.contains("Prompt length", ignoreCase = true) ||
                entity.llmResponse.contains("Delta length", ignoreCase = true)

        if (isInvalid) {
            recommendationCacheDao.deleteByHash(snapshotHash)
            return null
        }

        return CachedRecommendation(
            snapshotHash = entity.snapshotHash,
            llmResponse = entity.llmResponse,
            provider = entity.provider,
            createdAt = entity.createdAt,
            tokenCount = entity.tokenCount
        )
    }

    suspend fun put(
        snapshotHash: String,
        response: String,
        provider: String = "DEEPSEEK",
        tokenCount: Int = 0
    ) {
        val now = clock.millis()
        val existing = recommendationCacheDao.getByHash(snapshotHash)

        val entity = RecommendationCacheEntity(
            id = existing?.id ?: 0,
            snapshotHash = snapshotHash,
            llmResponse = response,
            provider = provider,
            createdAt = now,
            tokenCount = tokenCount
        )

        recommendationCacheDao.upsert(entity)
        recommendationCacheDao.deleteOldestBeyondLimit(MAX_RETENTION_LIMIT)
    }

    suspend fun putGuidance(
        snapshotHash: String,
        guidance: GuidanceResult,
        provider: String = "OFFLINE",
        tokenCount: Int = 0
    ) {
        val serializedResponse = Json.encodeToString(guidance)
        put(
            snapshotHash = snapshotHash,
            response = serializedResponse,
            provider = provider,
            tokenCount = tokenCount
        )
    }

    suspend fun clearExpired(): Int {
        val threshold = clock.millis() - TTL_MS
        return recommendationCacheDao.deleteExpired(threshold)
    }

    suspend fun size(): Int {
        return recommendationCacheDao.getCount()
    }
}
