package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity

@Composable
fun StatusChip(
    status: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, labelColor) = when (status) {
        AssignmentEntity.STATUS_PENDING -> Triple("Pending", Color(0xFFFFF3E0), Color(0xFFE65100))
        AssignmentEntity.STATUS_IN_PROGRESS -> Triple("In Progress", Color(0xFFE3F2FD), Color(0xFF1565C0))
        AssignmentEntity.STATUS_SUBMITTED -> Triple("Submitted", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        AssignmentEntity.STATUS_COMPLETED -> Triple("Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple(status, Color.LightGray, Color.DarkGray)
    }

    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = containerColor,
                labelColor = labelColor
            ),
            modifier = modifier
        )
    } else {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
