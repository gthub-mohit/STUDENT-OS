package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun AttachmentRow(
    attachmentUri: String?,
    onAddAttachment: () -> Unit = {},
    onReplaceAttachment: () -> Unit = {},
    onRemoveAttachment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!attachmentUri.isNullOrBlank()) {
            val fileName = File(attachmentUri).name
            AssistChip(
                onClick = onReplaceAttachment,
                label = { Text(fileName) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Attachment File"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onRemoveAttachment) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Attachment"
                        )
                    }
                }
            )
        } else {
            OutlinedButton(onClick = onAddAttachment) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Attachment"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Attachment")
            }
        }
    }
}
