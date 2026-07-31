package com.studentos.feature.coding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun RatingBadge(
    profile: CpProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.platform.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile.handle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getRatingBadgeColor(profile.platform, profile.currentRating)
            ) {
                Text(
                    text = if (profile.currentRating != null) "${profile.currentRating}" else "Unrated",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
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
