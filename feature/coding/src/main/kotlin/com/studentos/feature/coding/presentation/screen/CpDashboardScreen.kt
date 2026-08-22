package com.studentos.feature.coding.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.coding.presentation.component.ContestResultCard
import com.studentos.feature.coding.presentation.component.LastSyncedBanner
import com.studentos.feature.coding.presentation.component.RatingBadge
import com.studentos.feature.coding.presentation.state.CpDashboardUiState
import com.studentos.feature.coding.presentation.viewmodel.CpDashboardViewModel

@Composable
fun CpDashboardRoute(
    onNavigateToKnowledgeTree: () -> Unit = {},
    onNavigateToReflection: (Long) -> Unit = {},
    viewModel: CpDashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CpDashboardScreen(
        uiState = uiState,
        onSync = { viewModel.triggerSync() },
        onNavigateToKnowledgeTree = onNavigateToKnowledgeTree,
        onNavigateToReflection = onNavigateToReflection,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpDashboardScreen(
    uiState: CpDashboardUiState,
    onSync: () -> Unit,
    onNavigateToKnowledgeTree: () -> Unit,
    onNavigateToReflection: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coding Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateToKnowledgeTree) {
                        Text(
                            text = "DSA Tracker",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onSync) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync CP Profiles"
                        )
                    }
                }
            )
        },
        bottomBar = {
            LastSyncedBanner(
                lastSyncedAt = uiState.lastSyncedAt,
                isOffline = uiState.isOffline
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.isEmpty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No CP profile set up.\nAdd your CodeChef or Codeforces handle in Settings → AI & Coding, or tap Sync below.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onSync) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Sync Profiles")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profiles Section
                        item {
                            Text(
                                text = "Profiles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(
                            items = uiState.profiles,
                            key = { "profile_${it.id}" }
                        ) { profile ->
                            RatingBadge(profile = profile)
                        }

                        // Recent Contests Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recent Contests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (uiState.contests.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = "No contests recorded yet. Tap sync above to fetch recent contests.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.contests,
                                key = { "contest_${it.id}" }
                            ) { contest ->
                                ContestResultCard(
                                    contest = contest,
                                    onClick = { onNavigateToReflection(contest.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
