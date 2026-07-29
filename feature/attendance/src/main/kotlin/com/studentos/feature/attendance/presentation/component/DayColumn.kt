package com.studentos.feature.attendance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DayColumn(
    selectedDayOfWeek: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { (dayInt, label) ->
            FilterChip(
                selected = (selectedDayOfWeek == dayInt),
                onClick = { onDaySelected(dayInt) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
