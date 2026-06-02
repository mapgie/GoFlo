package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog for adding a brand-new symptom to the catalog and selecting it for
 * the current period in one step.
 *
 * All existing symptoms are already shown as chips on the log screen, so this
 * dialog is only needed to introduce a symptom that doesn't exist yet.
 *
 * @param existingLabels  All labels already in the symptom catalog (case-insensitive
 *                        duplicate check).
 * @param selectedLabels  Labels already selected for this period (shown as "already added").
 * @param onAdd           Called with the trimmed name when the user confirms.
 * @param onDismiss       Called on cancel or outside tap.
 */
@Composable
fun AddSymptomDialog(
    existingLabels: List<String>,
    selectedLabels: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()

    val alreadyInCatalog = existingLabels.any { it.equals(trimmed, ignoreCase = true) }
    val alreadySelected  = selectedLabels.any { it.equals(trimmed, ignoreCase = true) }
    val canAdd = trimmed.isNotBlank() && !alreadyInCatalog

    val hint = when {
        trimmed.isBlank()     -> null
        alreadySelected       -> "\"$trimmed\" is already selected."
        alreadyInCatalog      -> "\"$trimmed\" is already in your symptom list."
        else                  -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New symptom") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Symptom name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (hint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canAdd) onAdd(trimmed) }, enabled = canAdd) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
