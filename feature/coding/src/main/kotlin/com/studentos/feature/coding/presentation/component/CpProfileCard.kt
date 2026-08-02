package com.studentos.feature.coding.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentos.feature.coding.domain.model.CpProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CpProfileCard(
    profile: CpProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.platform.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.handle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getRatingBadgeColor(profile.platform, profile.currentRating)
                ) {
                    Text(
                        text = if (profile.currentRating != null) "${profile.currentRating}" else "Unrated",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                profile.highestRating?.let { maxRating ->
                    StatItem(label = "Max Rating", value = "$maxRating")
                }

                profile.rank?.let { rank ->
                    StatItem(label = "Rank", value = rank)
                }

                profile.contestCount?.let { count ->
                    StatItem(label = "Contests", value = "$count")
                }

                profile.problemsSolved?.let { solved ->
                    StatItem(label = "Solved", value = "$solved")
                }
            }

            profile.lastSyncedAt?.let { syncTime ->
                Spacer(modifier = Modifier.height(8.dp))
                val formatted = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(syncTime))
                Text(
                    text = "Last synced: $formatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getRatingBadgeColor(platform: String, rating: Int?): Color {
    if (rating == null) return Color.Gray
    return when (platform.uppercase()) {
        "CODEFORCES" -> when {
            rating >= 2400 -> Color(0xFFFF0000)
            rating >= 2100 -> Color(0xFFFF8C00)
            rating >= 1900 -> Color(0xFFAA00AA)
            rating >= 1600 -> Color(0xFF0000FF)
            rating >= 1400 -> Color(0xFF03A89E)
            rating >= 1200 -> Color(0xFF008000)
            else -> Color(0xFF808080)
        }
        "CODECHEF" -> when {
            rating >= 2500 -> Color(0xFFFF0000)
            rating >= 2200 -> Color(0xFFFF8C00)
            rating >= 2000 -> Color(0xFFFFD700)
            rating >= 1800 -> Color(0xFFAA00AA)
            rating >= 1600 -> Color(0xFF0000FF)
            rating >= 1400 -> Color(0xFF03A89E)
            else -> Color(0xFF008000)
        }
        else -> Color(0xFF6200EE)
    }
}
