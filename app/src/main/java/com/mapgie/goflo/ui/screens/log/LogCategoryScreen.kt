package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.util.decodeScaleLabels
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Slider input for numeric tracking categories.
 *
 * Shows the category's configured range, a Material3 [Slider] constrained to
 * [category.numericMin]..[category.numericMax], and the current value in large text.
 * When [value] is null (no value set yet) the slider defaults to [category.numericMin]
 * and shows a gentle hint.
 */
@Composable
private fun NumericSliderSection(
    category: TrackingCategory,
    value: Float?,
    onValueChange: (Float) -> Unit,
) {
    val min = category.numericMin
    val max = category.numericMax
    val sliderValue = value ?: min

    // Steps: 0 = continuous (for decimals), otherwise whole-number steps
    val steps = if (category.allowDecimals) 0 else {
        val range = (max - min).toInt()
        if (range > 1) range - 1 else 0
    }

    val displayValue = if (category.allowDecimals)
        "%.1f".format(sliderValue)
    else
        sliderValue.toInt().toString()

    val minLabel = if (category.allowDecimals) "%.1f".format(min) else min.toInt().toString()
    val maxLabel = if (category.allowDecimals) "%.1f".format(max) else max.toInt().toString()

    // Optional label for the current whole-number value (e.g. 3 → "Neutral")
    val scaleLabel = if (!category.allowDecimals)
        category.scaleLabels.decodeScaleLabels()[sliderValue.toInt()]
    else null

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        displayValue,
                        style = MaterialTheme.typography.headlineMedium,
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
            }

            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
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
                if (value == null) {
                    Text(
                        "Drag to set a value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(maxLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NumericFreeInputSection(
    category: TrackingCategory,
    value: String,
    onValueChange: (String) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value           = value,
                onValueChange   = onValueChange,
                label           = {
                    val label = if (category.numericUnit.isNotBlank()) category.numericUnit else "Value"
                    Text(label)
                },
                placeholder     = { Text("Enter a number") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * One-tap counter input for "increment" (Plus One) categories.
 *
 * Shows the running count for the day with a prominent "Add one" button and a
 * smaller decrement control to correct mistakes. The count is held in the
 * view-model's numericValue and persisted as a whole number on save.
 */
@Composable
private fun IncrementSection(
    category: TrackingCategory,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (category.numericUnit.isNotBlank()) {
                    Text(
                        category.numericUnit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDecrement, enabled = count > 0) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Button(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add one")
                }
            }
        }
    }
}

/**
 * Timed increment section for "Plus One" categories with trackAgainstTime enabled.
 * Each tap immediately records a new log entry with the current time.
 */
@Composable
private fun TimedIncrementSection(
    category: TrackingCategory,
    entries: List<com.mapgie.goflo.data.repository.TrackingLogWithValues>,
    onAddOne: () -> Unit,
    onDeleteEntry: (com.mapgie.goflo.data.database.entities.TrackingLog) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val total = entries.size
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    total.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (category.numericUnit.isNotBlank()) {
                    Text(
                        category.numericUnit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            Button(onClick = onAddOne) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log +1 now")
            }

            if (entries.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Logged today:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.log.loggedAt.ifEmpty { "–" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = if (entry.log.loggedAt.isEmpty()) FontStyle.Italic else FontStyle.Normal
                        )
                        IconButton(
                            onClick = { onDeleteEntry(entry.log) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogCategoryScreen(
    viewModel: LogCategoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Pop back on save or delete
    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onNavigateBack()
    }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("This log entry for ${state.category?.name} on ${state.date.format(displayFormat)} will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.delete() }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.category?.name ?: "Log")
                        Text(
                            state.date.format(displayFormat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Input area — slider / free input / chips ──────────────────────

            val cat = state.category
            when {
                cat?.categoryType == "numeric_slider" -> {
                    NumericSliderSection(
                        category = cat,
                        value = state.numericValue,
                        onValueChange = viewModel::setNumericValue
                    )
                }
                cat?.categoryType == "numeric_free" -> {
                    NumericFreeInputSection(
                        category = cat,
                        value = state.numericFreeText,
                        onValueChange = viewModel::setNumericFreeText
                    )
                }
                cat?.categoryType == "increment" && cat.trackAgainstTime -> {
                    // Timed increment: each +1 saves immediately with timestamp
                    TimedIncrementSection(
                        category = cat,
                        entries = state.timedEntriesToday,
                        onAddOne = viewModel::addTimedIncrement,
                        onDeleteEntry = viewModel::deleteTimedEntry
                    )
                }
                cat?.categoryType == "increment" -> {
                    IncrementSection(
                        category = cat,
                        count = state.numericValue?.toInt() ?: 0,
                        onIncrement = { viewModel.setNumericValue((state.numericValue ?: 0f) + 1f) },
                        onDecrement = {
                            viewModel.setNumericValue(((state.numericValue ?: 0f) - 1f).coerceAtLeast(0f))
                        }
                    )
                }
                else -> {
                // Text value chips
                if (state.availableValues.isNotEmpty()) {
                    Text(
                        "Select all that apply:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.availableValues.forEach { value ->
                            SelectableChip(
                                label = value.label,
                                selected = value.label in state.selectedValues,
                                onClick = { viewModel.toggleValue(value.label) }
                            )
                        }
                    }

                    // Show removed values (in historical record but no longer in catalog)
                    val removedValues = state.selectedValues.filter { label ->
                        state.availableValues.none { it.label == label }
                    }
                    if (removedValues.isNotEmpty()) {
                        Text(
                            "Previously recorded (removed from options):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            removedValues.forEach { label ->
                                SelectableChip(
                                    label = "$label (removed)",
                                    selected = true,
                                    onClick = { viewModel.toggleValue(label) }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "No values defined for this category yet. You can add values in Settings → Tracking Categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }
            }

            // Timed increment entries are saved immediately — no notes/save button needed
            val isTimedIncrement = cat?.categoryType == "increment" && cat.trackAgainstTime

            if (!isTimedIncrement) {
                // ── Track against time checkbox ───────────────────────────────────

                if (cat?.trackAgainstTime == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = state.trackTime,
                            onCheckedChange = viewModel::setTrackTime
                        )
                        Text(
                            text = "Track against time",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ── Notes ──────────────────────────────────────────────────────

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { if (it.length <= 500) viewModel.setNotes(it) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        if (state.notes.isNotEmpty()) {
                            Text("${state.notes.length}/500")
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isEditing) "Update" else "Save")
                }
            }
        }
    }
}
