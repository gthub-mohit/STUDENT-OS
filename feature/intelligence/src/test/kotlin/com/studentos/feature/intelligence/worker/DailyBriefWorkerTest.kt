package com.studentos.feature.intelligence.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.orchestrator.IntelligenceOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailyBriefWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val orchestrator: IntelligenceOrchestrator = mockk()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var worker: DailyBriefWorker

    @Before
    fun setUp() {
        coEvery { workerParams.runAttemptCount } returns 0
        worker = DailyBriefWorker(
            context = context,
            params = workerParams,
            orchestrator = orchestrator,
            clock = clock
        )
    }

    @Test
    fun doWork_callsOrchestratorExactlyOnce_andReturnsSuccess() = runTest {
        val today = LocalDate.of(2026, 8, 1)
        val dummyBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "{}"
        )
        coEvery { orchestrator.generateMorningBrief(today) } returns dummyBrief

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { orchestrator.generateMorningBrief(today) }
    }

    @Test
    fun doWork_returnsRetry_onExceptionWhenRetriesRemain() = runTest {
        val today = LocalDate.of(2026, 8, 1)
        coEvery { workerParams.runAttemptCount } returns 2
        coEvery { orchestrator.generateMorningBrief(today) } throws RuntimeException("Network timeout")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun doWork_returnsFailure_onExceptionWhenRetriesExceeded() = runTest {
        val today = LocalDate.of(2026, 8, 1)
        coEvery { workerParams.runAttemptCount } returns 3
        coEvery { orchestrator.generateMorningBrief(today) } throws RuntimeException("Fatal error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
