package com.studentos.feature.assignments.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun DeadlineCountdown(
    deadline: Long,
    nowEpoch: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    val deadlineDateTime = remember(deadline) {
        Instant.ofEpochMilli(deadline).atZone(zoneId)
    }
    val deadlineDate = deadlineDateTime.toLocalDate()
    val todayDate = remember(nowEpoch) {
        Instant.ofEpochMilli(nowEpoch).atZone(zoneId).toLocalDate()
    }
    val tomorrowDate = todayDate.plusDays(1)
    val timeStr = deadlineDateTime.format(timeFormatter)

    val diff = deadline - nowEpoch
    val (text, color, isBold) = when {
        diff < 0 -> {
            val overdueDays = TimeUnit.MILLISECONDS.toDays(-diff)
            val overdueText = if (overdueDays == 0L) {
                "Overdue · today ($timeStr)"
            } else {
                "Overdue · $overdueDays ${if (overdueDays == 1L) "day" else "days"}"
            }
            Triple(overdueText, MaterialTheme.colorScheme.error, true)
        }
        deadlineDate.isEqual(todayDate) -> {
            Triple("Due today · $timeStr", MaterialTheme.colorScheme.tertiary, true)
        }
        deadlineDate.isEqual(tomorrowDate) -> {
            Triple("Due tomorrow · $timeStr", MaterialTheme.colorScheme.secondary, true)
        }
        else -> {
            val dateStr = deadlineDate.format(dateFormatter)
            Triple("Due $dateStr · $timeStr", MaterialTheme.colorScheme.onSurfaceVariant, false)
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        modifier = modifier
    )
}

