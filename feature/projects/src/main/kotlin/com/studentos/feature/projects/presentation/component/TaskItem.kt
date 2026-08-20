package com.studentos.feature.projects.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskEngine
import com.studentos.feature.projects.domain.model.ProjectTaskPriority
import com.studentos.feature.projects.domain.model.ProjectTaskState

@Composable
fun TaskItem(
    task: ProjectTaskDomain,
    taskState: ProjectTaskState,
    blockerTask: ProjectTaskDomain?,
    onToggleCompletion: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMs: Long = System.currentTimeMillis()
) {
    val isBlocked = taskState == ProjectTaskState.BLOCKED
    val isCompleted = task.isCompleted

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> MaterialTheme.colorScheme.surfaceContainerLowest
                isBlocked -> MaterialTheme.colorScheme.surfaceContainerLow
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBlocked) {
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Task Blocked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleCompletion() }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (taskState == ProjectTaskState.AVAILABLE) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        isBlocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Blocker Subtitle (for blocked tasks)
                if (isBlocked) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val blockerName = blockerTask?.title ?: "Prerequisite task"
                    Text(
                        text = "Waiting for: \"$blockerName\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Badges Row (Priority + Deadline)
                val deadlineContext = ProjectTaskEngine.formatDeadlineContext(task.deadline, nowMs)
                val hasPriorityBadge = !isCompleted && task.priority != ProjectTaskPriority.MEDIUM
                val hasDeadlineBadge = !isCompleted && deadlineContext != null

                if (hasPriorityBadge || hasDeadlineBadge) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasPriorityBadge) {
                            val (bgColor, textColor, label) = when (task.priority) {
                                ProjectTaskPriority.HIGH -> Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer,
                                    "High Priority"
                                )
                                ProjectTaskPriority.LOW -> Triple(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    "Low Priority"
                                )
                                else -> Triple(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    "Medium"
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = bgColor
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (deadlineContext != null) {
                            val isOverdue = deadlineContext == "Overdue"
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = deadlineContext,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
