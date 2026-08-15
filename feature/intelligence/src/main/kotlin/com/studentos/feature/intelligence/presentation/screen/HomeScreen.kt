package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.presentation.state.HomeUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Accent palette ────────────────────────────────────────────────────────────
private val HeroPurple = Color(0xFF7C3AED)
private val OnHero = Color.White

/**
 * Clock/History Material vector icon.
 */
private val HistoryClockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "HistoryClock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black)
    ) {
        moveTo(13.0f, 3.0f)
        curveTo(8.03f, 3.0f, 4.0f, 7.03f, 4.0f, 12.0f)
        horizontalLineTo(1.0f)
        lineToRelative(4.0f, 4.0f)
        lineToRelative(4.0f, -4.0f)
        horizontalLineTo(6.0f)
        curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
        curveToRelative(3.87f, 0.0f, 7.0f, 3.13f, 7.0f, 7.0f)
        reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
        curveToRelative(-1.93f, 0.0f, -3.68f, -0.79f, -4.94f, -2.06f)
        lineToRelative(-1.42f, 1.42f)
        curveTo(8.27f, 19.99f, 10.51f, 21.0f, 13.0f, 21.0f)
        curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
        reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f)
        close()
        moveTo(12.0f, 8.0f)
        verticalLineToRelative(5.0f)
        lineToRelative(4.25f, 2.52f)
        lineToRelative(0.77f, -1.28f)
        lineToRelative(-3.52f, -2.09f)
        verticalLineTo(8.0f)
        horizontalLineTo(12.0f)
        close()
    }.build()
}

/**
 * HomeScreen — Redesigned Student OS home screen based on finalized UX decisions.
 *
 * Major Sections:
 * 1. Header (Greeting + Date + History & Settings action buttons)
 * 2. Today's Progress Hero Card (Dominant purple card, single-source-of-truth progress)
 * 3. Today's Focus (Up to 3 actionable priorities with interactive checkboxes)
 * 4. Coming Up (Up to 3 nearest upcoming deadlines/classes/contests/milestones)
 *
 * Quick Access grid is completely removed to optimize vertical hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onHeroClick: () -> Unit,
    onToggleFocusItem: (TodayFocusItem) -> Unit,
    onFocusItemClick: (String) -> Unit,
    onComingUpItemClick: (String) -> Unit,
    onViewAllComingUpClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
            // ── Section 1: Header ───────────────────────────────────────────
            HeaderSection(
                onHistoryClick = onHistoryClick,
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 2: Today's Progress Hero Card ───────────────────────
            TodaysProgressHeroCard(
                uiState = uiState,
                onClick = onHeroClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 3: Today's Focus ────────────────────────────────────
            TodaysFocusSection(
                focusItems = uiState.focusItems,
                onToggleItem = onToggleFocusItem,
                onItemClick = onFocusItemClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 4: Coming Up ────────────────────────────────────────
            ComingUpSection(
                comingUpItems = uiState.comingUpItems,
                onItemClick = onComingUpItemClick,
                onViewAllClick = onViewAllComingUpClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 1 — Header
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val today = LocalDate.now()
    val dateFormatted = today.format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = HistoryClockIcon,
                    contentDescription = "Brief History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 2 — Today's Progress Hero Card
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodaysProgressHeroCard(
    uiState: HomeUiState,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progressBarValue,
        animationSpec = tween(durationMillis = 600),
        label = "hero_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnHero
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnHero,
                            strokeWidth = 2.dp
                        )
                    } else {
                        val progressText = if (uiState.totalPrioritiesCount > 0) {
                            "${uiState.completedPrioritiesCount} / ${uiState.totalPrioritiesCount} priorities completed"
                        } else {
                            "You're all caught up for today 🎉"
                        }
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnHero.copy(alpha = 0.9f)
                        )
                    }
                }
                if (!uiState.isLoading && uiState.totalPrioritiesCount > 0) {
                    val percentInt = (uiState.progressBarValue * 100).toInt()
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnHero
                    )
                }
            }

            // Progress Bar (Accurate representation: only shown when priorities > 0)
            if (!uiState.isLoading && uiState.totalPrioritiesCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

            // Supporting Footer Text + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.todayGoalSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnHero.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View daily plan",
                    tint = OnHero.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 3 — Today's Focus
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodaysFocusSection(
    focusItems: List<TodayFocusItem>,
    onToggleItem: (TodayFocusItem) -> Unit,
    onItemClick: (String) -> Unit
) {
    Text(
        text = "Today's Focus",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (focusItems.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "No priorities for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            focusItems.take(3).forEach { item ->
                FocusItemRow(
                    item = item,
                    onToggle = { onToggleItem(item) },
                    onClick = {
                        if (!item.actionRoute.isNullOrEmpty()) {
                            onItemClick(item.actionRoute)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FocusItemRow(
    item: TodayFocusItem,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with proper tap target
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    color = if (item.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!item.actionRoute.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open detail",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Section 4 — Coming Up
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun ComingUpSection(
    comingUpItems: List<ComingUpItem>,
    onItemClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Coming Up",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (comingUpItems.isNotEmpty()) {
            Text(
                text = "View all →",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onViewAllClick)
                    .padding(4.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (comingUpItems.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "No upcoming deadlines or scheduled classes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            comingUpItems.take(3).forEach { item ->
                ComingUpItemRow(
                    item = item,
                    onClick = {
                        if (!item.actionRoute.isNullOrEmpty()) {
                            onItemClick(item.actionRoute)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ComingUpItemRow(
    item: ComingUpItem,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (item.category.uppercase()) {
        "ASSIGNMENT" -> Icons.Default.Edit
        "CLASS", "ATTENDANCE" -> Icons.Default.DateRange
        "CONTEST", "CODING", "DSA" -> Icons.Default.Build
        "PROJECT" -> Icons.AutoMirrored.Filled.List
        else -> HistoryClockIcon
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.category,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
