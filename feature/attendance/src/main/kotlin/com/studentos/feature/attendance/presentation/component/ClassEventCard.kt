package com.studentos.feature.attendance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.ClassEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClassEventCard(
    event: ClassEventEntity,
    subjectName: String,
    onStatusSelected: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingStatusToConfirm by remember { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()
    val isFutureEvent = event.scheduledAt > now

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusChip(
                    currentStatus = event.status,
                    onStatusClick = { nextStatus ->
                        if (isFutureEvent) {
                            pendingStatusToConfirm = nextStatus
                        } else {
                            onStatusSelected(event.id, nextStatus)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = timeFormat.format(Date(event.scheduledAt))
            val endTime = timeFormat.format(Date(event.endAt))

            Text(
                text = "$startTime - $endTime",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    pendingStatusToConfirm?.let { nextStatus ->
        AlertDialog(
            onDismissRequest = { pendingStatusToConfirm = null },
            title = { Text("Confirm Attendance Marking") },
            text = { Text("This is a future event. Are you sure you want to mark it as $nextStatus?") },
            confirmButton = {
                Button(
                    onClick = {
                        onStatusSelected(event.id, nextStatus)
                        pendingStatusToConfirm = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatusToConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusChip(
    currentStatus: String,
    onStatusClick: (String) -> Unit
) {
    val nextStatus = when (currentStatus) {
        ClassEventEntity.STATUS_UNMARKED -> ClassEventEntity.STATUS_PRESENT
        ClassEventEntity.STATUS_PRESENT -> ClassEventEntity.STATUS_ABSENT
        ClassEventEntity.STATUS_ABSENT -> ClassEventEntity.STATUS_CANCELLED
        ClassEventEntity.STATUS_CANCELLED -> ClassEventEntity.STATUS_HOLIDAY
        ClassEventEntity.STATUS_HOLIDAY -> ClassEventEntity.STATUS_PRESENT
        else -> ClassEventEntity.STATUS_PRESENT
    }

    val (chipColor, textColor) = when (currentStatus) {
        ClassEventEntity.STATUS_PRESENT, ClassEventEntity.STATUS_EXTRA_CLASS -> Color(0xFFD4EDDA) to Color(0xFF155724)
        ClassEventEntity.STATUS_ABSENT -> Color(0xFFF8D7DA) to Color(0xFF721C24)
        ClassEventEntity.STATUS_CANCELLED -> Color(0xFFFFF3CD) to Color(0xFF856404)
        ClassEventEntity.STATUS_HOLIDAY -> Color(0xFFE2E3E5) to Color(0xFF383D41)
        else -> Color(0xFFE0E0E0) to Color(0xFF424242)
    }

    FilterChip(
        selected = true,
        onClick = { onStatusClick(nextStatus) },
        label = { Text(text = currentStatus, color = textColor) }
    )
}
