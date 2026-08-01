package com.studentos.feature.coding.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.presentation.state.CategoryWithTopics
import com.studentos.feature.coding.presentation.state.DsaKnowledgeUiState
import com.studentos.feature.coding.presentation.viewmodel.KnowledgeTreeViewModel

@Composable
fun KnowledgeTreeRoute(
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeTreeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    KnowledgeTreeScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleCategoryExpansion = viewModel::toggleCategoryExpansion,
        onAddCategoryClicked = viewModel::onAddCategoryClicked,
        onDismissAddCategoryDialog = viewModel::onDismissAddCategoryDialog,
        onConfirmAddCategory = viewModel::onConfirmAddCategory,
        onDeleteCategoryClicked = viewModel::onDeleteCategoryClicked,
        onDismissDeleteCategoryDialog = viewModel::onDismissDeleteCategoryDialog,
        onConfirmDeleteCategory = viewModel::onConfirmDeleteCategory,
        onTopicConfidenceChanged = viewModel::onTopicConfidenceChanged,
        onToggleTopicSolved = viewModel::onToggleTopicSolved,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTreeScreen(
    uiState: DsaKnowledgeUiState,
    onNavigateBack: () -> Unit,
    onToggleCategoryExpansion: (Long) -> Unit,
    onAddCategoryClicked: () -> Unit,
    onDismissAddCategoryDialog: () -> Unit,
    onConfirmAddCategory: (String) -> Unit,
    onDeleteCategoryClicked: (DsaCategory) -> Unit,
    onDismissDeleteCategoryDialog: () -> Unit,
    onConfirmDeleteCategory: (Long) -> Unit,
    onTopicConfidenceChanged: (DsaTopic, Int) -> Unit,
    onToggleTopicSolved: (DsaTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCategoryName by remember { mutableStateOf("") }

    if (uiState.showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = onDismissAddCategoryDialog,
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmAddCategory(newCategoryName)
                        newCategoryName = ""
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAddCategoryDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteCategoryDialog,
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete category '${uiState.categoryToDelete.name}'?") },
            confirmButton = {
                TextButton(onClick = { onConfirmDeleteCategory(uiState.categoryToDelete.id) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteCategoryDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DSA Knowledge Tree",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddCategoryClicked) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category"
                        )
                    }
                }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.isEmpty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No DSA categories found. Tap '+' to add a category.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Overall Progress Header
                        item {
                            OverallProgressHeader(progressPercentage = uiState.overallTreeProgress)
                        }

                        // Categories List
                        items(
                            items = uiState.categories,
                            key = { "category_${it.category.id}" }
                        ) { categoryWithTopics ->
                            CategoryCard(
                                categoryWithTopics = categoryWithTopics,
                                onToggleExpansion = { onToggleCategoryExpansion(categoryWithTopics.category.id) },
                                onDeleteCategory = { onDeleteCategoryClicked(categoryWithTopics.category) },
                                onTopicConfidenceChanged = onTopicConfidenceChanged,
                                onToggleTopicSolved = onToggleTopicSolved
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallProgressHeader(progressPercentage: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Tree Mastery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (progressPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CategoryCard(
    categoryWithTopics: CategoryWithTopics,
    onToggleExpansion: () -> Unit,
    onDeleteCategory: () -> Unit,
    onTopicConfidenceChanged: (DsaTopic, Int) -> Unit,
    onToggleTopicSolved: (DsaTopic) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryWithTopics.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${categoryWithTopics.topics.size} Topics • ${categoryWithTopics.completionPercentage.toInt()}% Revised",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                IconButton(onClick = onDeleteCategory) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Category",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Icon(
                    imageVector = if (categoryWithTopics.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (categoryWithTopics.isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = categoryWithTopics.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (categoryWithTopics.topics.isEmpty()) {
                        Text(
                            text = "No topics in this category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        categoryWithTopics.topics.forEach { topic ->
                            TopicRow(
                                topic = topic,
                                onConfidenceChanged = { newConfidence -> onTopicConfidenceChanged(topic, newConfidence) },
                                onToggleSolved = { onToggleTopicSolved(topic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(
    topic: DsaTopic,
    onConfidenceChanged: (Int) -> Unit,
    onToggleSolved: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { star ->
                        val isSelected = star <= topic.confidenceLevel
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = "Confidence $star",
                            tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onConfidenceChanged(star) }
                                .padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val isRevised = topic.revisionStatus == DsaTopic.STATUS_REVISED
            FilterChip(
                selected = isRevised,
                onClick = onToggleSolved,
                label = {
                    Text(
                        text = if (isRevised) "Revised" else "Not Revised",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}
