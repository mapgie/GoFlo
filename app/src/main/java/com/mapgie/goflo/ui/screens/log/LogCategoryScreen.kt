package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.components.MetricConfig
import com.mapgie.goflo.ui.components.MetricInput
import com.mapgie.goflo.ui.components.MetricValue
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.components.Timeline
import com.mapgie.goflo.ui.components.TimelineEntryData
import com.mapgie.goflo.ui.components.usesStepScale
import com.mapgie.goflo.ui.util.CategoryType
import com.mapgie.goflo.ui.util.decodeScaleLabels
import com.mapgie.goflo.ui.util.toCategoryType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Tappable card showing the date this entry will be logged against. Opens a date
 * picker so any category can be logged for a past day, not only today.
 */
@Composable
private fun DateSelectorCard(
    date: LocalDate,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    date.format(displayFormat),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Change date",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis ?: return@TextButton
                onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState)
    }
}

/** Builds the [MetricConfig] the [MetricInput] facade renders from a category row. */
private fun metricConfigFor(
    category: TrackingCategory,
    availableValues: List<String>,
): MetricConfig = MetricConfig(
    name = category.name,
    options = availableValues,
    min = category.numericMin.toInt(),
    max = category.numericMax.toInt(),
    stepLabels = category.scaleLabels.decodeScaleLabels(),
    unit = category.numericUnit.takeIf { it.isNotBlank() },
    allowDecimals = category.allowDecimals,
)

/**
 * Maps the screen state onto the [MetricValue] variant [MetricInput] expects
 * for [type]. Yes/No and Time reuse [LogCategoryUiState.selectedValues] as a
 * single-label set, matching how their readings are stored ("Yes"/"No",
 * "HH:mm" value labels).
 */
private fun metricValueFor(
    type: CategoryType,
    config: MetricConfig,
    state: LogCategoryUiState,
): MetricValue = when (type) {
    CategoryType.DEFAULT -> MetricValue.Choice(state.selectedValues)
    CategoryType.NUMERIC_SLIDER ->
        if (config.usesStepScale()) MetricValue.Scale(state.numericValue?.toInt())
        else MetricValue.Continuous(state.numericValue)
    CategoryType.NUMERIC_FREE -> MetricValue.FreeNumber(state.numericFreeText)
    CategoryType.INCREMENT -> MetricValue.Count(state.numericValue?.toInt() ?: 0)
    CategoryType.YES_NO -> MetricValue.YesNo(
        when {
            "Yes" in state.selectedValues -> true
            "No" in state.selectedValues -> false
            else -> null
        }
    )
    CategoryType.TIME -> MetricValue.TimeOfDay(state.selectedValues.firstOrNull())
}

/**
 * Timed increment ("Plus One" + track against time): each append saves a new
 * timestamped log immediately, so the day renders as a running total plus a
 * [Timeline] of today's entries with per-entry delete. There is deliberately
 * no notes field or Save button on this path.
 */
@Composable
private fun TimedIncrementTimeline(
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    entries.size.toString(),
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
        }
    }
    Timeline(
        entries = entries.map { entry ->
            TimelineEntryData(
                id = entry.log.id,
                time = entry.log.loggedAt.ifEmpty { "No time" },
                value = "+1",
            )
        },
        role = MaterialTheme.colorScheme.primary,
        onAppend = onAddOne,
        appendLabel = "Log +1 now",
        onDeleteEntry = { data ->
            entries.firstOrNull { it.log.id == data.id }?.let { onDeleteEntry(it.log) }
        },
    )
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
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialogWrapper(
            initial = state.date,
            onConfirm = { viewModel.setDate(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }

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
            LogEntryTopBar(
                title = state.category?.name ?: "Log",
                subtitle = state.date.format(displayFormat),
                onBack = onNavigateBack,
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
                }
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
            // ── Date ──────────────────────────────────────────────────────────
            // Editable for new entries so any category can be logged for a past
            // day; fixed when editing one specific existing entry.

            if (state.canEditDate) {
                DateSelectorCard(
                    date = state.date,
                    onClick = { showDatePicker = true }
                )
            }

            // ── Input area — everything renders through the MetricInput facade;
            //    the timed-increment timeline is the one screen-level flow (it
            //    saves per tap rather than collect-then-save). ────────────────

            val cat = state.category
            val type = cat?.categoryType?.toCategoryType() ?: CategoryType.DEFAULT
            val isTimedIncrement =
                cat != null && type == CategoryType.INCREMENT && cat.trackAgainstTime

            if (cat != null) {
                if (isTimedIncrement) {
                    TimedIncrementTimeline(
                        category = cat,
                        entries = state.timedEntriesToday,
                        onAddOne = viewModel::addTimedIncrement,
                        onDeleteEntry = viewModel::deleteTimedEntry
                    )
                } else {
                    val config = metricConfigFor(cat, state.availableValues.map { it.label })
                    val metricValue = metricValueFor(type, config, state)
                    val onMetricChange: (MetricValue) -> Unit = { v ->
                        when (v) {
                            is MetricValue.Choice -> viewModel.setSelectedValues(v.selected)
                            is MetricValue.Scale -> v.step?.let { viewModel.setNumericValue(it.toFloat()) }
                            is MetricValue.Continuous -> v.value?.let { viewModel.setNumericValue(it) }
                            is MetricValue.FreeNumber -> viewModel.setNumericFreeText(v.text)
                            is MetricValue.Count -> viewModel.setNumericValue(v.count.toFloat())
                            is MetricValue.YesNo -> v.value?.let {
                                viewModel.setSelectedValues(setOf(if (it) "Yes" else "No"))
                            }
                            is MetricValue.TimeOfDay -> v.time?.let {
                                viewModel.setSelectedValues(setOf(it))
                            }
                        }
                    }

                    if (type == CategoryType.DEFAULT) {
                        // Chips render bare, with the catalog empty-state and the
                        // "previously recorded" chips for labels no longer offered.
                        if (state.availableValues.isNotEmpty()) {
                            Text(
                                "Select all that apply:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MetricInput(
                                type = type,
                                config = config,
                                value = metricValue,
                                role = MaterialTheme.colorScheme.primary,
                                onRole = MaterialTheme.colorScheme.onPrimary,
                                onChange = onMetricChange
                            )

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
                    } else {
                        // Every other input renders in the same framed card the
                        // per-type sections used: category name label + control.
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment =
                                    if (type == CategoryType.INCREMENT) Alignment.CenterHorizontally
                                    else Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MetricInput(
                                    type = type,
                                    config = config,
                                    value = metricValue,
                                    role = MaterialTheme.colorScheme.primary,
                                    onRole = MaterialTheme.colorScheme.onPrimary,
                                    onChange = onMetricChange
                                )
                            }
                        }
                    }
                }
            }

            // Timed increment entries are saved immediately — no notes/save button needed
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
