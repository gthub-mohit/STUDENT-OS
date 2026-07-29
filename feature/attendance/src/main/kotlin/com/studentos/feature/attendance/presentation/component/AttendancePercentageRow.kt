package com.studentos.feature.attendance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    isBelowThreshold: Boolean,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isBelowThreshold) {
        Color(0xFFF8D7DA) // Red tint highlight
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (isBelowThreshold) {
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f%% (Target: %d%%)", percentage, threshold),
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
        }
    }
}
