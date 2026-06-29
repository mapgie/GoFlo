package com.mapgie.goflo.ui.screens.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.util.decodeScaleLabels
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

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showAddSymptomDialog by rememberSaveable { mutableStateOf(false) }
    var showOngoingConfirm by rememberSaveable { mutableStateOf(false) }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (state.hasChanges) showUnsavedChangesDialog = true else onBack()
    }

    BackHandler(enabled = state.hasChanges) { showUnsavedChangesDialog = true }

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
                // Dismiss the dialog AND immediately open the end-date picker so
                // the button label matches the action ("Set end date" → date picker opens).
                TextButton(onClick = { showOngoingConfirm = false; showEndPicker = true }) {
                    Text("Set end date")
                }
            }
        )
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Do you want to save this entry before going back?") },
            confirmButton = {
                Button(onClick = { showUnsavedChangesDialog = false; viewModel.save() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsavedChangesDialog = false; onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            }
        )
    }

    if (showAddSymptomDialog) {
        AddSymptomDialog(
            existingLabels = state.symptomOptions.map { it.label },
            selectedLabels = state.symptoms,
            onAdd = { name ->
                viewModel.addNewSymptomToLibrary(name)
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
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded        = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                    ) {
                        DropdownMenuItem(
                            text    = { Text("Disable period logging") },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.disablePeriodTracking()
                                onBack()
                            }
                        )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date section
                SectionLabel("Dates")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val softBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f), border = softBorder) {
                        Text("Start: ${state.startDate.format(displayFormat)}")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f), border = softBorder) {
                        Text("End: ${state.endDate?.format(displayFormat) ?: "Ongoing"}")
                    }
                }
                if (state.endDate != null) {
                    TextButton(onClick = { viewModel.setEndDate(null) }) {
                        Text("Clear end date (mark as ongoing)")
                    }
                }

                // Flow section — shown whenever the category exists and is not archived
                val flowCat = state.flowCategory
                if (flowCat != null && !flowCat.isArchived) {
                SectionLabel(state.flowCategoryName)
                if (flowCat.categoryType == "numeric_slider") {
                    val sliderValue = state.flowSliderValue ?: flowCat.numericMin
                    val scaleMap = flowCat.scaleLabels.decodeScaleLabels()
                    val scaleLabel = scaleMap[sliderValue.toInt()] ?: sliderValue.toInt().toString()
                    val steps = (flowCat.numericMax - flowCat.numericMin).toInt() - 1
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = scaleLabel,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = sliderValue,
                                onValueChange = { viewModel.setFlowSliderValue(it) },
                                valueRange = flowCat.numericMin..flowCat.numericMax,
                                steps = steps,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    scaleMap[flowCat.numericMin.toInt()] ?: flowCat.numericMin.toInt().toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    scaleMap[flowCat.numericMax.toInt()] ?: flowCat.numericMax.toInt().toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    if (state.flowOptions.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.flowOptions.forEach { option ->
                                SelectableChip(
                                    label = option.label,
                                    selected = state.selectedFlowLabel == option.label,
                                    onClick = { viewModel.setFlowLevel(option.label) }
                                )
                            }
                        }
                    } else {
                        Text(
                            "No flow levels configured. Add levels in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                } // end flow showInLogPeriod guard

                // Symptoms section — shown whenever the category exists and is not archived
                val symptomsCat = state.symptomsCategory
                if (symptomsCat != null && !symptomsCat.isArchived) {
                SectionLabel(state.symptomsCategoryName)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.symptomOptions.forEach { option ->
                        SelectableChip(
                            label = option.label,
                            selected = option.label in state.symptoms,
                            onClick = { viewModel.toggleSymptom(option.label) }
                        )
                    }

                    // "+" chip — opens the Add Symptom dialog to create a new option
                    AssistChip(
                        onClick = { showAddSymptomDialog = true },
                        label = { Text("Add") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add symptom",
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
                } // end symptoms showInLogPeriod guard

                // Pinned tracking categories
                state.pinnedCategories.forEach { category ->
                    SectionLabel(category.name)
                    PinnedCategoryInput(
                        category       = category,
                        availableValues = state.pinnedCategoryValues[category.id] ?: emptyList(),
                        selectedValues  = state.pinnedCategorySelections[category.id] ?: emptySet(),
                        numericValue    = state.pinnedNumericValues[category.id],
                        freeText        = state.pinnedFreeTextValues[category.id] ?: "",
                        onToggleValue   = { viewModel.togglePinnedValue(category.id, it) },
                        onNumericChange = { viewModel.setPinnedNumericValue(category.id, it) },
                        onFreeTextChange = { viewModel.setPinnedFreeText(category.id, it) },
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
                    supportingText = { Text("${state.notes.length}/500") },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
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

// ── Pinned category input ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedCategoryInput(
    category: TrackingCategory,
    availableValues: List<String>,
    selectedValues: Set<String>,
    numericValue: Float?,
    freeText: String,
    onToggleValue: (String) -> Unit,
    onNumericChange: (Float) -> Unit,
    onFreeTextChange: (String) -> Unit,
) {
    when (category.categoryType) {
        "numeric_slider" -> {
            val min = category.numericMin
            val max = category.numericMax
            val sliderValue = numericValue ?: min
            val steps = if (category.allowDecimals) 0 else {
                val range = (max - min).toInt()
                if (range > 1) range - 1 else 0
            }
            val displayValue = if (category.allowDecimals) "%.1f".format(sliderValue)
                               else sliderValue.toInt().toString()
            val minLabel = if (category.allowDecimals) "%.1f".format(min) else min.toInt().toString()
            val maxLabel = if (category.allowDecimals) "%.1f".format(max) else max.toInt().toString()
            val scaleLabel = if (!category.allowDecimals)
                category.scaleLabels.decodeScaleLabels()[sliderValue.toInt()]
            else null

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (category.numericUnit.isNotBlank()) "$displayValue ${category.numericUnit}" else displayValue,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (scaleLabel != null) {
                            Text(
                                scaleLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = onNumericChange,
                        valueRange = min..max,
                        steps = steps,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(minLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (numericValue == null) {
                            Text("Drag to set a value", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(maxLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        "numeric_free" -> {
            OutlinedTextField(
                value           = freeText,
                onValueChange   = onFreeTextChange,
                label           = { Text(if (category.numericUnit.isNotBlank()) category.numericUnit else "Value") },
                placeholder     = { Text("Enter a number") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.fillMaxWidth(),
                colors          = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        "increment" -> {
            val count = numericValue?.toInt() ?: 0
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onNumericChange((count - 1).toFloat()) },
                            enabled = count > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Button(onClick = { onNumericChange((count + 1).toFloat()) }) {
                            Text("+1")
                        }
                    }
                }
            }
        }

        else -> {
            if (availableValues.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableValues.forEach { label ->
                        SelectableChip(
                            label    = label,
                            selected = label in selectedValues,
                            onClick  = { onToggleValue(label) }
                        )
                    }
                }
            } else {
                Text(
                    "No values configured. Add values in Settings → Tracking Categories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
