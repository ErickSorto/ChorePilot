package com.sortoapps.chorepilot.features.chores.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.features.chores.presentation.viewmodel.ChoreLifecycleConfirmationUiState

@Composable
fun ChoreLifecycleConfirmationDialog(
    confirmation: ChoreLifecycleConfirmationUiState?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onTextChange: (String) -> Unit,
) {
    if (confirmation == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(confirmation.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(confirmation.body)
                if (confirmation.requiresTextConfirmation) {
                    OutlinedTextField(
                        value = confirmation.confirmationText,
                        onValueChange = onTextChange,
                        label = { Text("Type ${confirmation.requiredConfirmationText}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmation.confirmEnabled,
            ) {
                Text(confirmation.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
