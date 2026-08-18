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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import com.studentos.feature.attendance.domain.calculator.BunkCalculator
import java.util.Locale

@Composable
fun AttendancePredictionCard(
    title: String,
    present: Int,
    absent: Int,
    cancelled: Int,
    holiday: Int,
    extraPresent: Int,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    var futureAttended by remember { mutableIntStateOf(0) }
    var futureBunked by remember { mutableIntStateOf(0) }

    val currentPct = AttendanceCalculator.calculatePercentage(present, absent, cancelled, holiday, extraPresent)
    val canSkip = BunkCalculator.canSkip(present, absent, cancelled, holiday, extraPresent, threshold)
    val mustAttend = BunkCalculator.mustAttend(present, absent, cancelled, holiday, extraPresent, threshold)

    val predictedPct = BunkCalculator.predictAttendance(
        present = present,
        absent = absent,
        cancelled = cancelled,
        holiday = holiday,
        extraPresent = extraPresent,
        futureAttended = futureAttended,
        futureBunked = futureBunked
    )

    val isPredictedSafe = predictedPct >= threshold

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$title — Bunk & Prediction Tool",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "Current: %.1f%%", currentPct),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Threshold: $threshold%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalHeld = present + absent + extraPresent
                if (totalHeld == 0) {
                    Text(
                        text = "No attendance recorded yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (canSkip > 0) {
                    Text(
                        text = "Can Skip: $canSkip classes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF155724)
                    )
                } else if (mustAttend > 0) {
                    Text(
                        text = "Must Attend: $mustAttend classes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Status: On Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF155724)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Simulate Future Classes:",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Attend +$futureAttended")
                Row {
                    IconButton(onClick = { if (futureAttended > 0) futureAttended-- }) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { futureAttended++ }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Bunk +$futureBunked")
                Row {
                    IconButton(onClick = { if (futureBunked > 0) futureBunked-- }) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { futureBunked++ }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "Predicted: %.1f%%", predictedPct),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPredictedSafe) Color(0xFF155724) else MaterialTheme.colorScheme.error
                )

                Text(
                    text = if (isPredictedSafe) "SAFE" else "BELOW TARGET",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPredictedSafe) Color(0xFF155724) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
