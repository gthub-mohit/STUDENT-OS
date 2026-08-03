package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentos.feature.intelligence.domain.model.RecommendationCard
import com.studentos.feature.intelligence.presentation.state.HomeUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Accent palette ────────────────────────────────────────────────────────────
private val HeroPurple = Color(0xFF7C3AED)
private val HeroPurpleLight = Color(0xFF9F67FF)
private val HeroPurpleDark = Color(0xFF5B21B6)
private val OnHero = Color.White

/**
 * HomeScreen — the redesigned Student OS home screen.
 *
 * Three sections:
 * 1. Hero Card (purple accent, fully clickable → Daily Brief)
 * 2. Today's Focus (top 3 actionable recommendations)
 * 3. Quick Access (4 compact navigation cards)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onHeroClick: () -> Unit,
    onRecommendationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onQuickNavClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Header: Greeting + Date + Settings ──────────────────────────
            HeaderSection(onSettingsClick = onSettingsClick)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 1: Hero Card ────────────────────────────────────────
            HeroCard(
                uiState = uiState,
                onClick = onHeroClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Today's Focus ────────────────────────────────────
            if (uiState.topRecommendations.isNotEmpty()) {
                TodaysFocusSection(
                    recommendations = uiState.topRecommendations,
                    onRecommendationClick = onRecommendationClick
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Section 3: Quick Access ─────────────────────────────────────
            QuickAccessSection(onQuickNavClick = onQuickNavClick)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Header Section
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(onSettingsClick: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val today = LocalDate.now()
    val dateFormatted = today.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 1 — Hero Card
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    uiState: HomeUiState,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressBarValue,
        animationSpec = tween(durationMillis = 800),
        label = "hero_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Score row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Today's Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnHero.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = OnHero,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "${uiState.currentScore}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = OnHero
                        )
                    }
                }
                if (!uiState.isLoading && uiState.targetScore > 0) {
                    Text(
                        text = "/ ${uiState.targetScore}",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnHero.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = OnHero,
                trackColor = OnHero.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Goal summary + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.todayGoalSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnHero.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.hasBrief) "View brief" else "Start your day",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnHero.copy(alpha = 0.7f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Daily Brief",
                        tint = OnHero.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 2 — Today's Focus
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodaysFocusSection(
    recommendations: List<RecommendationCard>,
    onRecommendationClick: (String) -> Unit
) {
    Text(
        text = "Today's Focus",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recommendations.take(3).forEach { card ->
            FocusItem(
                card = card,
                onClick = {
                    if (!card.actionRoute.isNullOrEmpty()) {
                        onRecommendationClick(card.actionRoute)
                    }
                }
            )
        }
    }
}

@Composable
private fun FocusItem(
    card: RecommendationCard,
    onClick: () -> Unit
) {
    val emoji = when (card.category.lowercase()) {
        "attendance" -> "🏫"
        "assignments", "assignment" -> "📚"
        "coding", "cp", "dsa" -> "💻"
        "projects", "project" -> "📋"
        else -> "✅"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 3 — Quick Access
// ────────────────────────────────────────────────────────────────────────────────

private data class QuickAccessItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val emoji: String
)

private val quickAccessItems = listOf(
    QuickAccessItem("Attendance", Icons.Default.DateRange, "weekly", "🏫"),
    QuickAccessItem("Assignments", Icons.Default.Edit, "assignments/list", "📚"),
    QuickAccessItem("Coding", Icons.Default.Build, "coding/cp-dashboard", "💻"),
    QuickAccessItem("Projects", Icons.Default.List, "projects/list", "📋")
)

@Composable
private fun QuickAccessSection(onQuickNavClick: (String) -> Unit) {
    Text(
        text = "Quick Access",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 2×2 grid
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickAccessItems.take(2).forEach { item ->
                QuickAccessCard(
                    item = item,
                    onClick = { onQuickNavClick(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickAccessItems.drop(2).forEach { item ->
                QuickAccessCard(
                    item = item,
                    onClick = { onQuickNavClick(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = item.emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
