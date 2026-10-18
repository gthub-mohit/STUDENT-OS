package com.studentos.feature.intelligence.domain.usecase

import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.model.GoalProgress
import com.studentos.feature.intelligence.domain.model.GoalStatus
import com.studentos.feature.intelligence.domain.model.ProgressDataPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class CalculateGoalProgressUseCase @Inject constructor() {

    operator fun invoke(
        goal: GoalEntity,
        logs: List<ActivityLogEntity>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): GoalProgress {
        // 1. Filter and sort confirmed logs linked to this goal
        val goalLogs = logs.filter { it.goalId == goal.id }
            .sortedBy { it.timestamp }

        // 2. Derive actual progress: start_value + sum(metric_delta)
        val sumDelta = goalLogs.sumOf { it.metricDelta }
        val actualProgress = goal.startValue + sumDelta

        // 3. Compute remaining value and days
        val remainingValue = max(0.0, goal.targetValue - actualProgress)
        val oneDayMs = 24L * 60L * 60L * 1000L
        val daysRemaining = if (goal.targetDate > currentTimeMs) {
            (goal.targetDate - currentTimeMs) / oneDayMs
        } else {
            0L
        }

        // 4. Progress percentage
        val span = goal.targetValue - goal.startValue
        val progressPercentage = if (span <= 0.0) {
            if (actualProgress >= goal.targetValue) 100.0 else 0.0
        } else {
            ((actualProgress - goal.startValue) / span) * 100.0
        }

        // 5. Evaluate date range edge cases
        val isInvalidDateRange = goal.targetDate <= goal.startDate
        val isBeforeStart = currentTimeMs < goal.startDate
        val isAfterTarget = currentTimeMs >= goal.targetDate

        val expectedProgressToday: Double? = if (isInvalidDateRange || isBeforeStart) {
            null
        } else if (isAfterTarget) {
            goal.targetValue
        } else {
            val totalTimeSpan = (goal.targetDate - goal.startDate).toDouble()
            val elapsedTime = (currentTimeMs - goal.startDate).toDouble()
            val ratio = (elapsedTime / totalTimeSpan).coerceIn(0.0, 1.0)
            goal.startValue + (span * ratio)
        }

        // 6. Determine Goal Status
        val (status, statusDesc) = when {
            actualProgress >= goal.targetValue -> {
                GoalStatus.COMPLETED to "Goal target reached!"
            }
            isInvalidDateRange -> {
                GoalStatus.INSUFFICIENT_DATA to "Target date must be after start date."
            }
            isBeforeStart -> {
                val daysToStart = max(1L, (goal.startDate - currentTimeMs) / oneDayMs)
                GoalStatus.NOT_STARTED to "Tracking begins in $daysToStart days."
            }
            expectedProgressToday != null -> {
                val tolerance = max(1.0, abs(span) * 0.05)
                val diff = actualProgress - expectedProgressToday
                when {
                    isAfterTarget && actualProgress < goal.targetValue -> {
                        GoalStatus.BEHIND to "Target deadline passed."
                    }
                    diff > tolerance -> {
                        GoalStatus.AHEAD to "Ahead of expected pace."
                    }
                    diff < -tolerance -> {
                        GoalStatus.BEHIND to "Behind expected pace."
                    }
                    else -> {
                        GoalStatus.ON_TRACK to "On track with expected trajectory."
                    }
                }
            }
            else -> {
                GoalStatus.INSUFFICIENT_DATA to "Not enough data to calculate trajectory."
            }
        }

        // 7. Reconstruct actual historical progress data points from stored confirmed logs
        val historyPoints = mutableListOf<ProgressDataPoint>()
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        // Initial baseline point at start date
        historyPoints.add(
            ProgressDataPoint(
                timestamp = goal.startDate,
                actualProgress = goal.startValue,
                expectedProgress = goal.startValue,
                dateLabel = dateFormat.format(Date(goal.startDate))
            )
        )

        var runningActual = goal.startValue
        goalLogs.forEach { log ->
            runningActual += log.metricDelta
            val expectedAtLogTime = calculateExpectedAt(log.timestamp, goal)
            historyPoints.add(
                ProgressDataPoint(
                    timestamp = log.timestamp,
                    actualProgress = runningActual,
                    expectedProgress = expectedAtLogTime,
                    dateLabel = dateFormat.format(Date(log.timestamp))
                )
            )
        }

        // Ensure current time point is represented if after start date and after last log
        if (currentTimeMs > goal.startDate) {
            val lastTimestamp = historyPoints.last().timestamp
            if (currentTimeMs > lastTimestamp) {
                historyPoints.add(
                    ProgressDataPoint(
                        timestamp = currentTimeMs,
                        actualProgress = actualProgress,
                        expectedProgress = expectedProgressToday,
                        dateLabel = "Today"
                    )
                )
            }
        }

        return GoalProgress(
            goal = goal,
            actualProgress = actualProgress,
            expectedProgressToday = expectedProgressToday,
            progressPercentage = progressPercentage,
            status = status,
            statusDescription = statusDesc,
            historyPoints = historyPoints,
            totalLogsCount = goalLogs.size,
            remainingValue = remainingValue,
            daysRemaining = daysRemaining
        )
    }

    private fun calculateExpectedAt(timestamp: Long, goal: GoalEntity): Double? {
        if (goal.targetDate <= goal.startDate) return null
        if (timestamp < goal.startDate) return goal.startValue
        if (timestamp >= goal.targetDate) return goal.targetValue

        val totalTime = (goal.targetDate - goal.startDate).toDouble()
        val elapsed = (timestamp - goal.startDate).toDouble()
        val ratio = (elapsed / totalTime).coerceIn(0.0, 1.0)
        return goal.startValue + ((goal.targetValue - goal.startValue) * ratio)
    }
}
