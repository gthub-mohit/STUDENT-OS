package com.studentos.feature.assignments.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.concurrent.TimeUnit

@Composable
fun DeadlineCountdown(
    deadline: Long,
    nowEpoch: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val diff = deadline - nowEpoch
    val (text, color) = when {
        diff < 0 -> {
            val overdueDays = TimeUnit.MILLISECONDS.toDays(-diff)
            if (overdueDays == 0L) {
                "Overdue today" to Color(0xFFC62828)
            } else {
                "Overdue by $overdueDays ${if (overdueDays == 1L) "day" else "days"}" to Color(0xFFC62828)
            }
        }
        diff < TimeUnit.HOURS.toMillis(24) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            if (hours == 0L) {
                "Due in less than an hour" to Color(0xFFE65100)
            } else {
                "Due in $hours ${if (hours == 1L) "hour" else "hours"}" to Color(0xFFE65100)
            }
        }
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "Due in $days ${if (days == 1L) "day" else "days"}" to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier
    )
}
