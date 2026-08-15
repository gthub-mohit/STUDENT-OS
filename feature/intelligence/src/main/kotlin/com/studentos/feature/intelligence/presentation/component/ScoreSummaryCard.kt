package com.studentos.feature.intelligence.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentos.feature.intelligence.domain.model.DailyBrief
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeroPurple = Color(0xFF7C3AED)
private val OnHero = Color.White

@Composable
fun ScoreSummaryCard(
    date: String,
    scoreTarget: Int,
    scoreActual: Int,
    guidanceSource: String,
    modifier: Modifier = Modifier
) {
    val formattedDate = try {
        LocalDate.parse(date).format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
        )
    } catch (_: Exception) {
        date
    }

    val rawProgress = if (scoreTarget > 0) {
        (scoreActual.toFloat() / scoreTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 600),
        label = "score_summary_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Formatted Date / Title + Single AI/Offline Engine Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnHero
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnHero.copy(alpha = 0.85f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OnHero.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (guidanceSource == DailyBrief.GUIDANCE_SOURCE_LLM) "AI Engine" else "Offline Engine",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnHero,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score / Completion Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progressText = if (scoreTarget > 0) {
                    "$scoreActual / $scoreTarget points earned"
                } else {
                    "No priorities scheduled for today."
                }

                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnHero.copy(alpha = 0.9f)
                )

                if (scoreTarget > 0) {
                    val percentInt = (rawProgress * 100).toInt()
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnHero
                    )
                }
            }

            // Progress Bar (rendered only if scoreTarget > 0)
            if (scoreTarget > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = OnHero,
                    trackColor = OnHero.copy(alpha = 0.25f)
                )
            }
        }
    }
}
