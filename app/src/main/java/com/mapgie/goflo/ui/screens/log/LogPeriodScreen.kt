package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.model.FlowLevel
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.ui.components.SelectableChip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogPeriodScreen(
    viewModel: LogPeriodViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val librarySymptoms by viewModel.librarySymptoms.collectAsState()

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showAddSymptomDialog by rememberSaveable { mutableStateOf(false) }
    var showOngoingConfirm by rememberSaveable { mutableStateOf(false) }

    if (showStartPicker && !state.isLoading) {
        DatePickerDialogWrapper(
            initial = state.startDate,
            onConfirm = { viewModel.setStartDate(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker && !state.isLoading) {
        DatePickerDialogWrapper(
            initial = state.endDate ?: LocalDate.now(),
            minDate = state.startDate,
            onConfirm = { viewModel.setEndDate(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    if (showDeleteConfirm && !state.isLoading) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete period?") },
            text = { Text("This will permanently remove this period log.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showOngoingConfirm) {
        AlertDialog(
            onDismissRequest = { showOngoingConfirm = false },
            title = { Text("No end date set") },
            text  = { Text(
                "This period will be saved as ongoing. You can add the end date later " +
                "from History once your period ends. Ongoing entries are excluded from " +
                "average cycle calculations."
            ) },
            confirmButton = {
                TextButton(onClick = { showOngoingConfirm = false; viewModel.save() }) {
                    Text("Save as ongoing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOngoingConfirm = false }) { Text("Set end date") }
            }
        )
    }

    if (showAddSymptomDialog) {
        AddSymptomDialog(
            librarySymptoms = librarySymptoms,
            selectedCustomSymptoms = state.customSymptoms,
            onSelectExisting = { name ->
                viewModel.toggleCustomSymptom(name)
                showAddSymptomDialog = false
            },
            onAddNew = { name ->
                viewModel.addAndSelectCustomSymptom(name)
                showAddSymptomDialog = false
            },
            onDismiss = { showAddSymptomDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Period" else "Log Period") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Date section
                SectionLabel("Dates")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                        Text("Start: ${state.startDate.format(displayFormat)}")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                        Text("End: ${state.endDate?.format(displayFormat) ?: "Ongoing"}")
                    }
                }
                if (state.endDate != null) {
                    TextButton(onClick = { viewModel.setEndDate(null) }) {
                        Text("Clear end date (mark as ongoing)")
                    }
                }

                // Flow section
                SectionLabel("Flow")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowLevel.entries.forEach { level ->
                        SelectableChip(
                            label = level.displayName,
                            selected = state.flowLevel == level,
                            onClick = { viewModel.setFlowLevel(level) }
                        )
                    }
                }

                // Symptoms section
                SectionLabel("Symptoms")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Built-in symptoms — displayed in lowercase for visual consistency
                    SymptomType.entries.forEach { symptom ->
                        SelectableChip(
                            label = symptom.displayName.lowercase(),
                            selected = symptom in state.symptoms,
                            onClick = { viewModel.toggleSymptom(symptom) }
                        )
                    }

                    // Custom symptoms selected for this period (shown so the user can deselect)
                    state.customSymptoms.sorted().forEach { name ->
                        SelectableChip(
                            label = name,
                            selected = true,
                            onClick = { viewModel.toggleCustomSymptom(name) }
                        )
                    }

                    // "+" chip — opens the Add Symptom dialog
                    AssistChip(
                        onClick = { showAddSymptomDialog = true },
                        label = { Text("Add") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom symptom",
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }

                // Notes
                SectionLabel("Notes")
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { if (it.length <= 500) viewModel.setNotes(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("How are you feeling? Any other details…") },
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${state.notes.length}/500") }
                )

                Spacer(Modifier.height(8.dp))

                // Save — warn when no end date so the user doesn't accidentally create
                // an "ongoing" entry that would corrupt cycle-length averages.
                Button(
                    onClick = { if (state.endDate == null) showOngoingConfirm = true else viewModel.save() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }

                // Delete (only when editing)
                if (state.isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Entry")
                    }
                }

                state.error?.let {
                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initial: LocalDate,
    minDate: LocalDate? = null,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis ?: return@TextButton
                val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                if (minDate == null || !picked.isBefore(minDate)) {
                    onConfirm(picked)
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState)
    }
}
