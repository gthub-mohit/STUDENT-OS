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
import com.studentos.feature.projects.domain.model.BugDomain

@Composable
fun CreateBugDialog(
    bugToEdit: BugDomain?,
    onDismiss: () -> Unit,
    onConfirm: (description: String, severity: String) -> Unit
) {
    var description by remember(bugToEdit) { mutableStateOf(bugToEdit?.description ?: "") }
    var severity by remember(bugToEdit) { mutableStateOf(bugToEdit?.severity ?: BugDomain.SEVERITY_MEDIUM) }
    var descriptionError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (bugToEdit == null) "Report Bug" else "Edit Bug")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = it.isBlank()
                    },
                    label = { Text("Bug Description") },
                    isError = descriptionError,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (descriptionError) {
                    Text(
                        text = "Description cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Severity",
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
                        selected = severity.equals(BugDomain.SEVERITY_LOW, ignoreCase = true),
                        onClick = { severity = BugDomain.SEVERITY_LOW },
                        label = { Text("Low") }
                    )
                    FilterChip(
                        selected = severity.equals(BugDomain.SEVERITY_MEDIUM, ignoreCase = true),
                        onClick = { severity = BugDomain.SEVERITY_MEDIUM },
                        label = { Text("Medium") }
                    )
                    FilterChip(
                        selected = severity.equals(BugDomain.SEVERITY_HIGH, ignoreCase = true),
                        onClick = { severity = BugDomain.SEVERITY_HIGH },
                        label = { Text("High") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) {
                        descriptionError = true
                    } else {
                        onConfirm(description, severity)
                    }
                }
            ) {
                Text(text = if (bugToEdit == null) "Report" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
