package com.studentos.feature.projects.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.domain.model.ProjectResourceDomain

@Composable
fun CreateResourceDialog(
    resourceToEdit: ProjectResourceDomain?,
    onDismiss: () -> Unit,
    onConfirm: (url: String, label: String?, type: String) -> Unit
) {
    var label by remember(resourceToEdit) { mutableStateOf(resourceToEdit?.label ?: "") }
    var url by remember(resourceToEdit) { mutableStateOf(resourceToEdit?.url ?: "") }
    var type by remember(resourceToEdit) { mutableStateOf(resourceToEdit?.type ?: ProjectResourceDomain.TYPE_LINK) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (resourceToEdit == null) "Add Resource" else "Edit Resource")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Title / Label (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = it.isBlank()
                    },
                    label = { Text("URL / Note Path") },
                    isError = urlError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (urlError) {
                    Text(
                        text = "URL / Note Path cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Resource Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = type.equals(ProjectResourceDomain.TYPE_LINK, ignoreCase = true),
                        onClick = { type = ProjectResourceDomain.TYPE_LINK },
                        label = { Text("Link") }
                    )
                    FilterChip(
                        selected = type.equals(ProjectResourceDomain.TYPE_NOTE, ignoreCase = true),
                        onClick = { type = ProjectResourceDomain.TYPE_NOTE },
                        label = { Text("Note") }
                    )
                    FilterChip(
                        selected = type.equals(ProjectResourceDomain.TYPE_DOCUMENTATION, ignoreCase = true),
                        onClick = { type = ProjectResourceDomain.TYPE_DOCUMENTATION },
                        label = { Text("Docs") }
                    )
                    FilterChip(
                        selected = type.equals(ProjectResourceDomain.TYPE_FILE, ignoreCase = true),
                        onClick = { type = ProjectResourceDomain.TYPE_FILE },
                        label = { Text("File") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank()) {
                        urlError = true
                    } else {
                        onConfirm(url, label.ifBlank { null }, type)
                    }
                }
            ) {
                Text(text = if (resourceToEdit == null) "Add Resource" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
