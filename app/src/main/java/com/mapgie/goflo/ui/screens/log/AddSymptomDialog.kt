package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.model.SymptomType

/**
 * Dialog for adding a custom symptom to the current period entry.
 *
 * - Shows the user's existing custom symptom library (filtered by search query) as tappable chips.
 * - When the typed query doesn't match any existing library entry or built-in symptom, an
 *   "Add '[name]'" chip is shown so the user can create a new one.
 * - Selecting any chip immediately closes the dialog.
 *
 * @param librarySymptoms      All names in the user's custom symptom library (lowercase).
 * @param selectedCustomSymptoms  Names already selected for the current period (excluded from suggestions).
 * @param onSelectExisting     Called when the user picks an already-saved symptom.
 * @param onAddNew             Called when the user confirms a brand-new symptom name.
 * @param onDismiss            Called on cancel / outside tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSymptomDialog(
    librarySymptoms: List<String>,
    selectedCustomSymptoms: Set<String>,
    onSelectExisting: (String) -> Unit,
    onAddNew: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val queryNorm = query.trim().lowercase()

    // Library entries not yet selected for this period
    val unselected = librarySymptoms.filter { it !in selectedCustomSymptoms }

    // Filter unselected library entries by the current query
    val suggestions = if (queryNorm.isBlank()) unselected
    else unselected.filter { it.contains(queryNorm, ignoreCase = true) }

    // A "new" chip is shown when the query is non-empty and doesn't exactly match anything
    // already in the library, already selected, or a built-in symptom name.
    val alreadyInLibrary = librarySymptoms.any { it.equals(queryNorm, ignoreCase = true) }
    val alreadySelected = selectedCustomSymptoms.any { it.equals(queryNorm, ignoreCase = true) }
    val matchesBuiltIn = SymptomType.entries.any { it.displayName.equals(queryNorm, ignoreCase = true) }
    val canAddNew = queryNorm.isNotBlank() && !alreadyInLibrary && !alreadySelected && !matchesBuiltIn

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add symptom") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search or type new symptom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                when {
                    suggestions.isNotEmpty() -> {
                        Text(
                            "Your symptoms",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            suggestions.forEach { name ->
                                SuggestionChip(
                                    onClick = { onSelectExisting(name) },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                    queryNorm.isBlank() && unselected.isEmpty() -> {
                        Text(
                            "Type a name below to create your first custom symptom.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    queryNorm.isNotBlank() && !canAddNew -> {
                        Text(
                            "\"$queryNorm\" is already added.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canAddNew) {
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = { onAddNew(queryNorm) },
                        label = { Text("+ Add \"$queryNorm\"") }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
