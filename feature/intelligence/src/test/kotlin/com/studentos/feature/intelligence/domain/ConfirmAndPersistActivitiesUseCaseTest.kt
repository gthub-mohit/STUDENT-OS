package com.studentos.feature.intelligence.domain

import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.feature.intelligence.domain.model.EditableActivity
import com.studentos.feature.intelligence.domain.model.EditableCommitment
import com.studentos.feature.intelligence.domain.usecase.ConfirmAndPersistActivitiesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConfirmAndPersistActivitiesUseCaseTest {

    private val activityLogDao: ActivityLogDao = mockk()
    private val commitmentDao: CommitmentDao = mockk()
    private val journalDao: JournalDao = mockk()

    private lateinit var useCase: ConfirmAndPersistActivitiesUseCase

    @Before
    fun setup() {
        useCase = ConfirmAndPersistActivitiesUseCase(
            activityLogDao = activityLogDao,
            commitmentDao = commitmentDao,
            journalDao = journalDao
        )
    }

    @Test
    fun `persists confirmed activities and commitments through their DAOs`() = runTest {
        val activitySlot = slot<ActivityLogEntity>()
        val commitmentSlot = slot<CommitmentEntity>()

        coEvery { activityLogDao.findByDedupHash(any()) } returns null
        coEvery { activityLogDao.insert(capture(activitySlot)) } returns 1L
        coEvery { commitmentDao.insert(capture(commitmentSlot)) } returns 2L
        coEvery { journalDao.updateSummary(any(), any(), any()) } returns Unit

        val activities = listOf(
            EditableActivity(
                title = "Worked on Android MVP",
                activityType = "PROJECT",
                metricDelta = 2.0,
                durationMinutes = 120,
                matchedGoalId = 7L
            )
        )
        val commitments = listOf(
            EditableCommitment(
                title = "Submit project report",
                category = "PROJECT",
                targetDate = "2026-10-25",
                targetTime = "23:59",
                priority = "HIGH"
            )
        )

        val result = useCase(
            journalEntryId = 55L,
            confirmedActivities = activities,
            confirmedCommitments = commitments,
            summary = "Made solid project progress."
        )

        assertEquals(1, result.persistedActivitiesCount)
        assertEquals(1, result.persistedCommitmentsCount)
        assertEquals(0, result.skippedDuplicatesCount)

        // Verify activity fields
        val savedActivity = activitySlot.captured
        assertEquals("Worked on Android MVP", savedActivity.summary)
        assertEquals("PROJECT", savedActivity.activityType)
        assertEquals(2.0, savedActivity.metricDelta, 0.001)
        assertEquals(120, savedActivity.durationMinutes)
        assertEquals(7L, savedActivity.goalId)

        // Verify commitment fields
        val savedCommitment = commitmentSlot.captured
        assertEquals("Submit project report", savedCommitment.title)
        assertEquals("HIGH", savedCommitment.priority)
        assertEquals("PENDING", savedCommitment.status)

        // Verify journal summary update
        coVerify(exactly = 1) { journalDao.updateSummary(55L, "Made solid project progress.", any()) }
    }

    @Test
    fun `safe deduplication prevents double confirmation duplicate inserts`() = runTest {
        // Mock existing activity with the same dedup hash
        val existingEntity = ActivityLogEntity(
            id = 10L,
            timestamp = 1000L,
            metricDelta = 1.0,
            activityType = "DSA",
            summary = "Solved 2 DP problems",
            confirmedAt = 1000L
        )
        coEvery { activityLogDao.findByDedupHash(any()) } returns existingEntity
        coEvery { journalDao.updateSummary(any(), any(), any()) } returns Unit

        val activities = listOf(
            EditableActivity(
                title = "Solved 2 DP problems",
                activityType = "DSA",
                metricDelta = 2.0,
                durationMinutes = 40
            )
        )

        val result = useCase(
            journalEntryId = 12L,
            confirmedActivities = activities,
            confirmedCommitments = emptyList(),
            summary = "Summary"
        )

        assertEquals(0, result.persistedActivitiesCount)
        assertEquals(1, result.skippedDuplicatesCount)
        coVerify(exactly = 0) { activityLogDao.insert(any()) }
    }

    @Test
    fun `unknown duration is preserved as null and not coerced to zero`() = runTest {
        val activitySlot = slot<ActivityLogEntity>()
        coEvery { activityLogDao.findByDedupHash(any()) } returns null
        coEvery { activityLogDao.insert(capture(activitySlot)) } returns 1L
        coEvery { journalDao.updateSummary(any(), any(), any()) } returns Unit

        val activities = listOf(
            EditableActivity(
                title = "Attended club orientation",
                activityType = "CLUB",
                durationMinutes = null
            )
        )

        useCase(
            journalEntryId = 99L,
            confirmedActivities = activities,
            confirmedCommitments = emptyList(),
            summary = null
        )

        val saved = activitySlot.captured
        assertNull("Duration must be stored as null when unknown", saved.durationMinutes)
    }
}
