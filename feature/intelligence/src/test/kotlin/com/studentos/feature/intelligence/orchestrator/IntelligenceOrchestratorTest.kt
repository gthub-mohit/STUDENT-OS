package com.studentos.feature.intelligence.orchestrator

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.intelligence.cache.CachedRecommendation
import com.studentos.core.intelligence.cache.RecommendationCache
import com.studentos.core.intelligence.fallback.DeterministicFallback
import com.studentos.core.intelligence.fallback.GuidanceResult
import com.studentos.core.intelligence.fallback.GuidanceSource
import com.studentos.core.intelligence.limiter.RateLimiter
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.prompt.PromptBuilder
import com.studentos.core.intelligence.provider.LLMProvider
import com.studentos.core.intelligence.provider.LLMProviderFactory
import com.studentos.core.intelligence.snapshot.AttendanceWarningSnapshot
import com.studentos.core.intelligence.snapshot.CpSummarySnapshot
import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import com.studentos.core.intelligence.snapshot.ScoreSnapshot
import com.studentos.core.intelligence.snapshot.SnapshotBuilder
import com.studentos.core.intelligence.snapshot.SnapshotDelta
import com.studentos.core.intelligence.snapshot.SnapshotDiffer
import com.studentos.core.intelligence.snapshot.StudentContextSnapshot
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class IntelligenceOrchestratorTest {

    private val snapshotBuilder: SnapshotBuilder = mockk()
    private val promptBuilder: PromptBuilder = mockk()
    private val llmProviderFactory: LLMProviderFactory = mockk()
    private val llmProvider: LLMProvider = mockk()
    private val recommendationCache: RecommendationCache = mockk(relaxed = true)
    private val rateLimiter: RateLimiter = mockk(relaxed = true)
    private val fallbackEngine: DeterministicFallback = mockk()
    private val repository: DailyBriefRepository = mockk(relaxed = true)
    private val snapshotDiffer: SnapshotDiffer = mockk()
    private val appEventBus: AppEventBus = mockk()

    private val eventsFlow = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)

    private val fixedInstant = Instant.parse("2026-08-01T08:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var orchestrator: IntelligenceOrchestrator

    private val dummySnapshot = IntelligenceSnapshot(
        snapshotVersion = 1,
        date = "2026-08-01",
        studentContext = StudentContextSnapshot(name = "Alex", tonePreference = "motivational"),
        classesToday = emptyList(),
        attendanceWarnings = emptyList(),
        assignmentsUrgent = emptyList(),
        freeSlots = emptyList(),
        suggestedDsaTopic = null,
        suggestedProjectAction = null,
        score = ScoreSnapshot(target = 50, actual = 10),
        cpSummary = CpSummarySnapshot()
    )

    private val updatedSnapshot = dummySnapshot.copy(
        attendanceWarnings = listOf(
            AttendanceWarningSnapshot(subject = "Maths", percentage = 60.0)
        )
    )

    private val dummyFallbackResult = GuidanceResult(
        summary = "Offline guidance summary",
        recommendations = emptyList(),
        source = GuidanceSource.OFFLINE
    )

    @Before
    fun setUp() {
        every { appEventBus.events } returns eventsFlow

        orchestrator = IntelligenceOrchestrator(
            snapshotBuilder = snapshotBuilder,
            promptBuilder = promptBuilder,
            llmProviderFactory = llmProviderFactory,
            recommendationCache = recommendationCache,
            rateLimiter = rateLimiter,
            fallbackEngine = fallbackEngine,
            repository = repository,
            snapshotDiffer = snapshotDiffer,
            appEventBus = appEventBus,
            clock = clock
        )

        coEvery { snapshotBuilder.build("2026-08-01") } returns dummySnapshot
        every { llmProvider.name } returns "DEEPSEEK"
        coEvery { llmProviderFactory.getProvider() } returns llmProvider
        every { promptBuilder.buildMorningPrompt(dummySnapshot) } returns "PROMPT"
        every { fallbackEngine.generateGuidance(any()) } returns dummyFallbackResult
        coEvery { repository.saveBrief(any()) } returns 1L
    }

    @Test
    fun generateMorningBrief_cacheHit_returnsCachedRecommendationAndSkipsLLM() = runTest {
        val cached = CachedRecommendation(
            snapshotHash = "hash123",
            llmResponse = "Cached AI Brief",
            provider = "DEEPSEEK",
            createdAt = fixedInstant.toEpochMilli() - 1000L,
            tokenCount = 150
        )
        coEvery { recommendationCache.get(any()) } returns cached

        val today = LocalDate.of(2026, 8, 1)
        val brief = orchestrator.generateMorningBrief(today)

        assertNotNull(brief)
        assertEquals("Cached AI Brief", brief.briefJson)
        assertEquals(DailyBrief.GUIDANCE_SOURCE_LLM, brief.guidanceSource)
        coVerify(exactly = 0) { llmProvider.generateBrief(any()) }
        coVerify { repository.saveBrief(brief) }
        assertEquals(dummySnapshot, orchestrator.getPreviousSnapshot())
    }

    @Test
    fun generateMorningBrief_cacheMiss_callsLLMAndCachesResponse() = runTest {
        coEvery { recommendationCache.get(any()) } returns null
        coEvery { rateLimiter.canCall() } returns true
        coEvery { llmProvider.generateBrief("PROMPT") } returns LLMResult.Success("Fresh AI Brief", 200)

        val today = LocalDate.of(2026, 8, 1)
        val brief = orchestrator.generateMorningBrief(today)

        assertEquals("Fresh AI Brief", brief.briefJson)
        assertEquals(DailyBrief.GUIDANCE_SOURCE_LLM, brief.guidanceSource)
        coVerify { recommendationCache.put(any(), "Fresh AI Brief", "DEEPSEEK", 200) }
        coVerify { rateLimiter.recordCall(triggeredBy = "MORNING_BRIEF", snapshotHash = any(), wasCacheHit = false, wasDelta = false, tokenCount = 200, success = true) }
        coVerify { repository.saveBrief(brief) }
    }

    @Test
    fun generateMorningBrief_rateLimitExceeded_usesOfflineFallback() = runTest {
        coEvery { recommendationCache.get(any()) } returns null
        coEvery { rateLimiter.canCall() } returns false

        val today = LocalDate.of(2026, 8, 1)
        val brief = orchestrator.generateMorningBrief(today)

        assertEquals(DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC, brief.guidanceSource)
        assertTrue(brief.llmGuidance?.contains("Offline guidance summary") == true)
        coVerify(exactly = 0) { llmProvider.generateBrief(any()) }
        coVerify { repository.saveBrief(brief) }
    }

    @Test
    fun processIntraDayRefresh_emptyDelta_stopsPipelineWithoutLLMOrRepoCalls() = runTest {
        orchestrator.setPreviousSnapshotForTesting(dummySnapshot)
        coEvery { snapshotBuilder.build("2026-08-01") } returns dummySnapshot
        every { snapshotDiffer.diff(dummySnapshot, dummySnapshot) } returns SnapshotDelta() // Empty delta

        val today = LocalDate.of(2026, 8, 1)
        val result = orchestrator.processIntraDayRefresh(today)

        assertNull(result)
        coVerify(exactly = 0) { promptBuilder.buildDeltaPrompt(any(), any(), any()) }
        coVerify(exactly = 0) { llmProvider.generateBrief(any()) }
        coVerify(exactly = 0) { repository.updateGuidance(any(), any(), any(), any()) }
    }

    @Test
    fun processIntraDayRefresh_cacheHit_skipsLLMAndUpdatesRepoFromCache() = runTest {
        orchestrator.setPreviousSnapshotForTesting(dummySnapshot)
        coEvery { snapshotBuilder.build("2026-08-01") } returns updatedSnapshot

        val nonNullDelta = mockk<SnapshotDelta>()
        every { nonNullDelta.isEmpty } returns false
        every { snapshotDiffer.diff(dummySnapshot, updatedSnapshot) } returns nonNullDelta

        val cached = CachedRecommendation(
            snapshotHash = "hash_updated",
            llmResponse = "Cached Delta Guidance",
            provider = "DEEPSEEK",
            createdAt = fixedInstant.toEpochMilli()
        )
        coEvery { recommendationCache.get(any()) } returns cached
        coEvery { repository.getBriefByHash(any()) } returns DailyBrief(date = "2026-08-01", jsonSnapshot = "", snapshotHash = "hash_updated", briefJson = "Cached Delta Guidance")

        val today = LocalDate.of(2026, 8, 1)
        val result = orchestrator.processIntraDayRefresh(today)

        assertNotNull(result)
        coVerify { repository.updateGuidance("2026-08-01", "Cached Delta Guidance", DailyBrief.GUIDANCE_SOURCE_LLM, fixedInstant.toEpochMilli()) }
        coVerify(exactly = 0) { promptBuilder.buildDeltaPrompt(any(), any(), any()) }
        coVerify(exactly = 0) { llmProvider.generateBrief(any()) }
        assertEquals(updatedSnapshot, orchestrator.getPreviousSnapshot())
    }

    @Test
    fun processIntraDayRefresh_rateLimitExceeded_triggersDeterministicFallback() = runTest {
        orchestrator.setPreviousSnapshotForTesting(dummySnapshot)
        coEvery { snapshotBuilder.build("2026-08-01") } returns updatedSnapshot

        val nonNullDelta = mockk<SnapshotDelta>()
        every { nonNullDelta.isEmpty } returns false
        every { snapshotDiffer.diff(dummySnapshot, updatedSnapshot) } returns nonNullDelta

        coEvery { recommendationCache.get(any()) } returns null
        coEvery { rateLimiter.canCall() } returns false
        coEvery { repository.getBriefByHash(any()) } returns DailyBrief(date = "2026-08-01", jsonSnapshot = "", snapshotHash = "hash_updated", briefJson = "Offline guidance summary")

        val today = LocalDate.of(2026, 8, 1)
        val result = orchestrator.processIntraDayRefresh(today)

        assertNotNull(result)
        coVerify { repository.updateGuidance("2026-08-01", "Offline guidance summary", DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC, fixedInstant.toEpochMilli()) }
        coVerify(exactly = 0) { llmProvider.generateBrief(any()) }
        assertEquals(updatedSnapshot, orchestrator.getPreviousSnapshot())
    }

    @Test
    fun processIntraDayRefresh_successfulDeltaRefresh_callsDeltaPromptAndUpdatesRepository() = runTest {
        orchestrator.setPreviousSnapshotForTesting(dummySnapshot)
        coEvery { snapshotBuilder.build("2026-08-01") } returns updatedSnapshot

        val nonNullDelta = mockk<SnapshotDelta>()
        every { nonNullDelta.isEmpty } returns false
        every { snapshotDiffer.diff(dummySnapshot, updatedSnapshot) } returns nonNullDelta

        coEvery { recommendationCache.get(any()) } returns null
        coEvery { rateLimiter.canCall() } returns true
        every { promptBuilder.buildDeltaPrompt(dummySnapshot, updatedSnapshot, nonNullDelta) } returns "DELTA_PROMPT"
        coEvery { llmProvider.generateBrief("DELTA_PROMPT") } returns LLMResult.Success("Delta AI Guidance", 80)
        coEvery { repository.getBriefByHash(any()) } returns DailyBrief(date = "2026-08-01", jsonSnapshot = "", snapshotHash = "hash_updated", briefJson = "Delta AI Guidance")

        val today = LocalDate.of(2026, 8, 1)
        val result = orchestrator.processIntraDayRefresh(today)

        assertNotNull(result)
        coVerify { recommendationCache.put(any(), "Delta AI Guidance", "DEEPSEEK", 80) }
        coVerify { rateLimiter.recordCall(triggeredBy = "EVENT_REFRESH", snapshotHash = any(), wasCacheHit = false, wasDelta = true, tokenCount = 80, success = true) }
        coVerify { repository.updateGuidance("2026-08-01", "Delta AI Guidance", DailyBrief.GUIDANCE_SOURCE_LLM, fixedInstant.toEpochMilli()) }
        assertEquals(updatedSnapshot, orchestrator.getPreviousSnapshot())
    }

    @Test
    fun startEventSubscription_singleSubscriptionActive_cancelsPreviousJob() = runTest {
        val job1 = orchestrator.startEventSubscription(this, debounceMillis = 1000L)
        val job2 = orchestrator.startEventSubscription(this, debounceMillis = 1000L)

        assertTrue(job1.isCancelled)
        assertTrue(job2.isActive)

        job2.cancel()
    }

    @Test
    fun processIntraDayRefresh_verifiesSnapshotUpdatedCorrectly() = runTest {
        orchestrator.setPreviousSnapshotForTesting(dummySnapshot)
        coEvery { snapshotBuilder.build("2026-08-01") } returns updatedSnapshot

        val nonNullDelta = mockk<SnapshotDelta>()
        every { nonNullDelta.isEmpty } returns false
        every { snapshotDiffer.diff(dummySnapshot, updatedSnapshot) } returns nonNullDelta

        coEvery { recommendationCache.get(any()) } returns null
        coEvery { rateLimiter.canCall() } returns false
        coEvery { repository.getBriefByHash(any()) } returns DailyBrief(date = "2026-08-01", jsonSnapshot = "", snapshotHash = "hash_updated", briefJson = "Summary")

        val today = LocalDate.of(2026, 8, 1)
        orchestrator.processIntraDayRefresh(today)

        assertEquals(updatedSnapshot, orchestrator.getPreviousSnapshot())
    }
}
