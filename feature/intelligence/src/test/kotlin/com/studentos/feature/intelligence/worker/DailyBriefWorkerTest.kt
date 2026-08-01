package com.studentos.feature.intelligence.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.domain.usecase.GenerateDailyBriefUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DailyBriefWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val generateDailyBriefUseCase: GenerateDailyBriefUseCase = mockk()
    private val repository: DailyBriefRepository = mockk()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var worker: DailyBriefWorker

    @Before
    fun setUp() {
        coEvery { workerParams.runAttemptCount } returns 0
        worker = DailyBriefWorker(
            context = context,
            params = workerParams,
            generateDailyBriefUseCase = generateDailyBriefUseCase,
            repository = repository,
            clock = clock
        )
    }

    @Test
    fun doWork_skipsGeneration_whenTodayBriefAlreadyExists() = runTest {
        val existing = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash1",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 90
        )
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(existing)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { generateDailyBriefUseCase(any()) }
    }

    @Test
    fun doWork_generatesBrief_whenTodayBriefDoesNotExist() = runTest {
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(null)
        val newBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash1",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { generateDailyBriefUseCase("2026-08-01") } returns newBrief

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { generateDailyBriefUseCase("2026-08-01") }
    }

    @Test
    fun doWork_returnsRetry_onExceptionWhenRetriesRemain() = runTest {
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(null)
        coEvery { generateDailyBriefUseCase("2026-08-01") } throws RuntimeException("Database error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
