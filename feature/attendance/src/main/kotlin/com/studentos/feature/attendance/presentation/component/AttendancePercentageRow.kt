package com.studentos.feature.attendance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AttendancePercentageRow(
    percentage: Double,
    totalHeld: Int = 1,
    isBelowThreshold: Boolean,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val hasData = totalHeld > 0
    val containerColor = if (hasData && isBelowThreshold) {
        Color(0xFFF8D7DA) // Red tint highlight
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (hasData && isBelowThreshold) {
        Color(0xFF721C24) // Red text
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Attendance",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (hasData) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor
                    )
                } else {
                    Text(
                        text = "No attendance recorded yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor
                    )
                }
            }

            Text(
                text = "Target: $threshold%",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}
