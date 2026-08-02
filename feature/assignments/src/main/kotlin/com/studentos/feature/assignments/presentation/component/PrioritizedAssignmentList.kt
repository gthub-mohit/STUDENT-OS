package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.PrioritizedAssignmentGroup
import com.studentos.feature.assignments.domain.model.UrgencyCategory

@Composable
fun PrioritizedAssignmentList(
    groups: List<PrioritizedAssignmentGroup>,
    subjectsMap: Map<Long, String>,
    onAssignmentClick: (Long) -> Unit,
    onDeleteClick: (AssignmentEntity) -> Unit,
    onStatusClick: (AssignmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(groups, key = { it.category.name }) { group ->
            Column(modifier = Modifier.fillMaxWidth()) {
                CategoryHeader(category = group.category, count = group.assignments.size)

                Spacer(modifier = Modifier.height(8.dp))

                group.assignments.forEach { assignment ->
                    val subjectName = subjectsMap[assignment.subjectId] ?: "Subject"
                    val isUrgent = group.category == UrgencyCategory.OVERDUE || group.category == UrgencyCategory.DUE_TODAY

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(
                                if (isUrgent) {
                                    Modifier.background(
                                        color = if (group.category == UrgencyCategory.OVERDUE) Color(0x1AFF0000) else Color(0x1AFF9800),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        AssignmentCard(
                            assignment = assignment,
                            subjectName = subjectName,
                            onClick = { onAssignmentClick(assignment.id) },
                            onDeleteClick = { onDeleteClick(assignment) },
                            onStatusClick = { onStatusClick(assignment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: UrgencyCategory,
    count: Int
) {
    val headerColor = when (category) {
        UrgencyCategory.OVERDUE -> Color(0xFFD32F2F)
        UrgencyCategory.DUE_TODAY -> Color(0xFFE65100)
        UrgencyCategory.DUE_TOMORROW -> Color(0xFFF57C00)
        UrgencyCategory.DUE_THIS_WEEK -> MaterialTheme.colorScheme.primary
        UrgencyCategory.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.displayName.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = headerColor
        )

        Text(
            text = "$count assignments",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
