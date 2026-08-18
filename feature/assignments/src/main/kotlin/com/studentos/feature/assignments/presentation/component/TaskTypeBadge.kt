package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.feature.assignments.domain.model.TaskType

@Composable
fun TaskTypeBadge(
    taskType: TaskType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (taskType) {
        TaskType.ASSIGNMENT -> Color(0xFFEDE7F6) to Color(0xFF5E35B1) // Purple
        TaskType.QUIZ -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Orange
        TaskType.LAB_RECORD -> Color(0xFFE0F7FA) to Color(0xFF00838F) // Cyan
        TaskType.PRACTICAL -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Green
        TaskType.OTHER -> Color(0xFFECEFF1) to Color(0xFF455A64) // Blue Grey
    }

    Text(
        text = taskType.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
