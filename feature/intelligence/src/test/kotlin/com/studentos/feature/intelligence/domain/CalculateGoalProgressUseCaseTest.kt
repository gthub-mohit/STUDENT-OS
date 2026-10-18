package com.studentos.feature.intelligence.domain

import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.model.GoalStatus
import com.studentos.feature.intelligence.domain.usecase.CalculateGoalProgressUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateGoalProgressUseCaseTest {

    private lateinit var useCase: CalculateGoalProgressUseCase

    @Before
    fun setup() {
        useCase = CalculateGoalProgressUseCase()
    }

    private fun createTestGoal(
        id: Long = 1L,
        metricType: String = "COUNT",
        unit: String = "questions",
        startValue: Double = 0.0,
        targetValue: Double = 100.0,
        startDate: Long = 1000L,
        targetDate: Long = 5000L
    ): GoalEntity {
        return GoalEntity(
            id = id,
            title = "Test Goal",
            category = "DSA",
            metricType = metricType,
            unit = unit,
            startValue = startValue,
            targetValue = targetValue,
            startDate = startDate,
            targetDate = targetDate,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }

    @Test
    fun `baseline is preserved when no activity logs exist`() {
        val goal = createTestGoal(startValue = 25.0, targetValue = 100.0)
        val result = useCase(goal = goal, logs = emptyList(), currentTimeMs = 3000L)

        assertEquals(25.0, result.actualProgress, 0.001)
        assertEquals(75.0, result.remainingValue, 0.001)
        assertEquals(0, result.totalLogsCount)
        assertEquals(0.0, result.progressPercentage, 0.001)
    }

    @Test
    fun `confirmed logs for matching goal sum correctly with baseline`() {
        val goal = createTestGoal(id = 1L, startValue = 10.0, targetValue = 100.0)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 5.0, activityType = "DSA", summary = "A", confirmedAt = 2000L),
            ActivityLogEntity(id = 2, goalId = 1L, timestamp = 3000L, metricDelta = 15.0, activityType = "DSA", summary = "B", confirmedAt = 3000L),
            // Log for different goal — should NOT affect this goal
            ActivityLogEntity(id = 3, goalId = 2L, timestamp = 3500L, metricDelta = 50.0, activityType = "DSA", summary = "C", confirmedAt = 3500L),
            // Unlinked log — should NOT affect this goal
            ActivityLogEntity(id = 4, goalId = null, timestamp = 3600L, metricDelta = 20.0, activityType = "DSA", summary = "D", confirmedAt = 3600L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        // 10 baseline + 5 + 15 = 30
        assertEquals(30.0, result.actualProgress, 0.001)
        assertEquals(2, result.totalLogsCount)
        assertEquals(70.0, result.remainingValue, 0.001)
    }

    @Test
    fun `negative correction logs decrease actual progress safely`() {
        val goal = createTestGoal(id = 1L, startValue = 0.0, targetValue = 50.0)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 10.0, activityType = "DSA", summary = "A", confirmedAt = 2000L),
            // User made a -3 correction
            ActivityLogEntity(id = 2, goalId = 1L, timestamp = 2500L, metricDelta = -3.0, activityType = "DSA", summary = "Correction", confirmedAt = 2500L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        // 0 + 10 - 3 = 7.0
        assertEquals(7.0, result.actualProgress, 0.001)
        assertEquals(43.0, result.remainingValue, 0.001)
    }

    @Test
    fun `hours metric type supports fractional values`() {
        val goal = createTestGoal(id = 1L, metricType = "HOURS", unit = "hours", startValue = 2.5, targetValue = 40.0)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 1.75, activityType = "LEARNING", summary = "OS study", confirmedAt = 2000L),
            ActivityLogEntity(id = 2, goalId = 1L, timestamp = 2500L, metricDelta = 2.25, activityType = "LEARNING", summary = "Compiler study", confirmedAt = 2500L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        // 2.5 + 1.75 + 2.25 = 6.5
        assertEquals(6.5, result.actualProgress, 0.001)
    }

    @Test
    fun `target reached marks goal as COMPLETED and allows unclamped actual values`() {
        val goal = createTestGoal(id = 1L, startValue = 0.0, targetValue = 50.0)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 65.0, activityType = "DSA", summary = "Marathon", confirmedAt = 2000L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        assertEquals(GoalStatus.COMPLETED, result.status)
        assertEquals(65.0, result.actualProgress, 0.001) // Not clamped to 50!
        assertEquals(130.0, result.progressPercentage, 0.001)
        assertEquals(0.0, result.remainingValue, 0.001)
    }

    @Test
    fun `status is AHEAD when progress exceeds expected pace by tolerance`() {
        // Goal from t=1000 to t=5000 (span = 4000). At t=3000 (50% of time), expected = 50.0.
        val goal = createTestGoal(id = 1L, startValue = 0.0, targetValue = 100.0, startDate = 1000L, targetDate = 5000L)
        // Actual is 75.0 (well above 50.0 + 5.0 tolerance)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 75.0, activityType = "DSA", summary = "A", confirmedAt = 2000L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        assertEquals(GoalStatus.AHEAD, result.status)
        assertEquals(50.0, result.expectedProgressToday!!, 0.001)
    }

    @Test
    fun `status is BEHIND when progress falls behind expected pace`() {
        // Goal from t=1000 to t=5000. At t=3000, expected = 50.0. Actual is only 10.0.
        val goal = createTestGoal(id = 1L, startValue = 0.0, targetValue = 100.0, startDate = 1000L, targetDate = 5000L)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 10.0, activityType = "DSA", summary = "A", confirmedAt = 2000L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3000L)

        assertEquals(GoalStatus.BEHIND, result.status)
    }

    @Test
    fun `status is NOT_STARTED when current time is before goal start date`() {
        val goal = createTestGoal(startDate = 5000L, targetDate = 10000L)
        val result = useCase(goal = goal, logs = emptyList(), currentTimeMs = 1000L)

        assertEquals(GoalStatus.NOT_STARTED, result.status)
        assertNull(result.expectedProgressToday)
    }

    @Test
    fun `status is INSUFFICIENT_DATA when target date is equal to or before start date`() {
        val goal = createTestGoal(startDate = 5000L, targetDate = 5000L)
        val result = useCase(goal = goal, logs = emptyList(), currentTimeMs = 6000L)

        assertEquals(GoalStatus.INSUFFICIENT_DATA, result.status)
        assertNull(result.expectedProgressToday)
    }

    @Test
    fun `graph history points reconstruct actual trajectory chronologically`() {
        val goal = createTestGoal(id = 1L, startValue = 5.0, targetValue = 50.0, startDate = 1000L, targetDate = 5000L)
        val logs = listOf(
            ActivityLogEntity(id = 1, goalId = 1L, timestamp = 2000L, metricDelta = 10.0, activityType = "DSA", summary = "A", confirmedAt = 2000L),
            ActivityLogEntity(id = 2, goalId = 1L, timestamp = 3000L, metricDelta = 15.0, activityType = "DSA", summary = "B", confirmedAt = 3000L)
        )

        val result = useCase(goal = goal, logs = logs, currentTimeMs = 3500L)

        // Baseline (t=1000, 5.0) -> Log 1 (t=2000, 15.0) -> Log 2 (t=3000, 30.0) -> Current (t=3500, 30.0)
        assertEquals(4, result.historyPoints.size)
        assertEquals(5.0, result.historyPoints[0].actualProgress, 0.001)
        assertEquals(15.0, result.historyPoints[1].actualProgress, 0.001)
        assertEquals(30.0, result.historyPoints[2].actualProgress, 0.001)
        assertEquals(30.0, result.historyPoints[3].actualProgress, 0.001)
    }
}
