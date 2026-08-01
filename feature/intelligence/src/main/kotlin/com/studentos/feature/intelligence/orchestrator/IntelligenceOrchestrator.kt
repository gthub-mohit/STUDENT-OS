package com.studentos.feature.intelligence.orchestrator

import com.studentos.core.events.AppEventBus
import com.studentos.core.intelligence.cache.RecommendationCache
import com.studentos.core.intelligence.fallback.DeterministicFallback
import com.studentos.core.intelligence.limiter.RateLimiter
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.prompt.PromptBuilder
import com.studentos.core.intelligence.provider.LLMProviderFactory
import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import com.studentos.core.intelligence.snapshot.SnapshotBuilder
import com.studentos.core.intelligence.snapshot.SnapshotDiffer
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class IntelligenceOrchestrator @Inject constructor(
    private val snapshotBuilder: SnapshotBuilder,
    private val promptBuilder: PromptBuilder,
    private val llmProviderFactory: LLMProviderFactory,
    private val recommendationCache: RecommendationCache,
    private val rateLimiter: RateLimiter,
    private val fallbackEngine: DeterministicFallback,
    private val repository: DailyBriefRepository,
    private val snapshotDiffer: SnapshotDiffer,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) {
    private val jsonFormatter = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var previousSnapshot: IntelligenceSnapshot? = null

    private var eventSubscriptionJob: Job? = null

    /**
     * Start listening for AppEventBus events and trigger debounced intra-day refreshes.
     */
    @Synchronized
    fun startEventSubscription(scope: CoroutineScope, debounceMillis: Long = 30_000L): Job {
        eventSubscriptionJob?.cancel()
        val job = appEventBus.events
            .debounce(debounceMillis)
            .onEach {
                processIntraDayRefresh()
            }
            .launchIn(scope)
        eventSubscriptionJob = job
        return job
    }

    fun stopEventSubscription() {
        eventSubscriptionJob?.cancel()
        eventSubscriptionJob = null
    }

    fun setPreviousSnapshotForTesting(snapshot: IntelligenceSnapshot?) {
        previousSnapshot = snapshot
    }

    fun getPreviousSnapshot(): IntelligenceSnapshot? = previousSnapshot

    suspend fun generateMorningBrief(today: LocalDate = LocalDate.now(clock)): DailyBrief {
        val dateStr = today.toString()
        val nowMs = clock.millis()

        // 1. Build Snapshot
        val snapshot: IntelligenceSnapshot = snapshotBuilder.build(dateStr)

        // Store snapshot for future intra-day diffing
        previousSnapshot = snapshot

        // 2. Compute Snapshot Hash
        val jsonSnapshot = jsonFormatter.encodeToString(snapshot)
        val snapshotHash = sha256(jsonSnapshot)

        // 3. Check Cache
        val cached = recommendationCache.get(snapshotHash)
        if (cached != null) {
            val cachedBrief = DailyBrief(
                date = dateStr,
                jsonSnapshot = jsonSnapshot,
                snapshotHash = snapshotHash,
                briefJson = cached.llmResponse,
                llmGuidance = cached.llmResponse,
                guidanceSource = if (cached.provider == "OFFLINE") DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC else DailyBrief.GUIDANCE_SOURCE_LLM,
                scoreTarget = snapshot.score.target,
                scoreActual = snapshot.score.actual,
                generatedAt = nowMs,
                guidanceUpdatedAt = cached.createdAt
            )
            repository.saveBrief(cachedBrief)
            return cachedBrief
        }

        // 4. Rate Limit Check
        val canCallAI = rateLimiter.canCall()
        if (canCallAI) {
            val provider = llmProviderFactory.getProvider()
            val prompt = promptBuilder.buildMorningPrompt(snapshot)
            val result = provider.generateBrief(prompt)

            if (result is LLMResult.Success) {
                recommendationCache.put(
                    snapshotHash = snapshotHash,
                    response = result.text,
                    provider = provider.name,
                    tokenCount = result.tokenCount
                )
                rateLimiter.recordCall(
                    triggeredBy = "MORNING_BRIEF",
                    snapshotHash = snapshotHash,
                    wasCacheHit = false,
                    wasDelta = false,
                    tokenCount = result.tokenCount,
                    success = true
                )

                val aiBrief = DailyBrief(
                    date = dateStr,
                    jsonSnapshot = jsonSnapshot,
                    snapshotHash = snapshotHash,
                    briefJson = result.text,
                    llmGuidance = result.text,
                    guidanceSource = DailyBrief.GUIDANCE_SOURCE_LLM,
                    scoreTarget = snapshot.score.target,
                    scoreActual = snapshot.score.actual,
                    generatedAt = nowMs,
                    guidanceUpdatedAt = nowMs
                )
                repository.saveBrief(aiBrief)
                return aiBrief
            } else if (result is LLMResult.Failure) {
                rateLimiter.recordCall(
                    triggeredBy = "MORNING_BRIEF",
                    snapshotHash = snapshotHash,
                    wasCacheHit = false,
                    wasDelta = false,
                    success = false,
                    errorMessage = result.message
                )
            }
        }

        // 5. Fallback (Offline Guidance)
        val fallbackResult = fallbackEngine.generateGuidance(snapshot)
        val fallbackJson = jsonFormatter.encodeToString(fallbackResult)

        val fallbackBrief = DailyBrief(
            date = dateStr,
            jsonSnapshot = jsonSnapshot,
            snapshotHash = snapshotHash,
            briefJson = fallbackJson,
            llmGuidance = fallbackResult.summary,
            guidanceSource = DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC,
            scoreTarget = snapshot.score.target,
            scoreActual = snapshot.score.actual,
            generatedAt = nowMs,
            guidanceUpdatedAt = nowMs
        )

        repository.saveBrief(fallbackBrief)
        return fallbackBrief
    }

    /**
     * Process intra-day refresh driven by live AppEventBus events.
     */
    suspend fun processIntraDayRefresh(today: LocalDate = LocalDate.now(clock)): DailyBrief? {
        val dateStr = today.toString()
        val nowMs = clock.millis()

        // 1. Build current snapshot
        val currentSnapshot = snapshotBuilder.build(dateStr)

        val prev = previousSnapshot
        if (prev == null) {
            previousSnapshot = currentSnapshot
            return null
        }

        // 2. Evaluate delta
        val delta = snapshotDiffer.diff(prev, currentSnapshot)
        if (delta.isEmpty) {
            return null
        }

        // 3. Compute new snapshot hash
        val jsonSnapshot = jsonFormatter.encodeToString(currentSnapshot)
        val snapshotHash = sha256(jsonSnapshot)

        // 4. Check RecommendationCache
        val cached = recommendationCache.get(snapshotHash)
        if (cached != null) {
            val source = if (cached.provider == "OFFLINE") DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC else DailyBrief.GUIDANCE_SOURCE_LLM
            repository.updateGuidance(
                date = dateStr,
                guidance = cached.llmResponse,
                source = source,
                updatedAt = cached.createdAt
            )
            previousSnapshot = currentSnapshot
            return repository.getBriefByHash(snapshotHash)
        }

        // 5. Rate Limit Check
        val canCallAI = rateLimiter.canCall()
        if (canCallAI) {
            val provider = llmProviderFactory.getProvider()
            val deltaPrompt = promptBuilder.buildDeltaPrompt(prev, currentSnapshot, delta)
            val result = provider.generateBrief(deltaPrompt)

            if (result is LLMResult.Success) {
                recommendationCache.put(
                    snapshotHash = snapshotHash,
                    response = result.text,
                    provider = provider.name,
                    tokenCount = result.tokenCount
                )
                rateLimiter.recordCall(
                    triggeredBy = "EVENT_REFRESH",
                    snapshotHash = snapshotHash,
                    wasCacheHit = false,
                    wasDelta = true,
                    tokenCount = result.tokenCount,
                    success = true
                )
                repository.updateGuidance(
                    date = dateStr,
                    guidance = result.text,
                    source = DailyBrief.GUIDANCE_SOURCE_LLM,
                    updatedAt = nowMs
                )
                previousSnapshot = currentSnapshot
                return repository.getBriefByHash(snapshotHash)
            } else if (result is LLMResult.Failure) {
                rateLimiter.recordCall(
                    triggeredBy = "EVENT_REFRESH",
                    snapshotHash = snapshotHash,
                    wasCacheHit = false,
                    wasDelta = true,
                    success = false,
                    errorMessage = result.message
                )
            }
        }

        // 6. Fallback (Offline Guidance)
        val fallbackResult = fallbackEngine.generateGuidance(currentSnapshot)
        repository.updateGuidance(
            date = dateStr,
            guidance = fallbackResult.summary,
            source = DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC,
            updatedAt = nowMs
        )
        previousSnapshot = currentSnapshot
        return repository.getBriefByHash(snapshotHash)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
