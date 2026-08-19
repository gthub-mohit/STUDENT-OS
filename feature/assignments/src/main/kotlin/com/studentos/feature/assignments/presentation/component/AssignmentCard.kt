package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.TaskType

@Composable
fun AssignmentCard(
    assignment: AssignmentEntity,
    subjectName: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val taskType = TaskType.fromString(assignment.taskType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Subject, Priority, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                PriorityBadge(priority = assignment.priority)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = assignment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val desc = assignment.description
            if (!desc.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges Row: TaskTypeBadge & StatusChip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskTypeBadge(taskType = taskType)
                StatusChip(
                    status = assignment.status,
                    onClick = onStatusClick
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Deadline Row
            DeadlineCountdown(
                deadline = assignment.deadline,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

