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
import com.studentos.core.database.entity.AssignmentEntity

@Composable
fun PriorityBadge(
    priority: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (priority.uppercase()) {
        AssignmentEntity.PRIORITY_HIGH -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        AssignmentEntity.PRIORITY_MEDIUM -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        AssignmentEntity.PRIORITY_LOW -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> Color.LightGray to Color.DarkGray
    }

    Text(
        text = priority.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
