package com.mapgie.goflo.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.export.DateRangePreset
import com.mapgie.goflo.data.export.ExportConfig
import com.mapgie.goflo.data.export.ExportFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateDisplayFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * Dialog that lets the user configure which data to export:
 * - Date range (preset or custom)
 * - Whether to include period cycle data
 * - Which tracking categories to include
 * - Output format (JSON or CSV)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportOptionsDialog(
    categories: List<TrackingCategory>,
    onDismiss: () -> Unit,
    onExport: (ExportConfig) -> Unit
) {
    var format by rememberSaveable { mutableStateOf(ExportFormat.JSON) }
    var datePreset by rememberSaveable { mutableStateOf(DateRangePreset.ALL_TIME) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var includePeriods by rememberSaveable { mutableStateOf(true) }
    var selectedCategoryIds by remember {
        mutableStateOf(categories.map { it.id }.toSet())
    }

    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }

    if (showStartPicker) {
        ExportDatePickerDialog(
            initial = customStart ?: LocalDate.now().minusYears(1),
            onConfirm = { customStart = it; showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        ExportDatePickerDialog(
            initial = customEnd ?: LocalDate.now(),
            minDate = customStart,
            onConfirm = { customEnd = it; showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Date range ───────────────────────────────────────────────
                Text("Date range", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = datePreset == preset,
                            onClick  = { datePreset = preset },
                            label    = { Text(preset.label) }
                        )
                    }
                }
                if (datePreset == DateRangePreset.CUSTOM) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick  = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(customStart?.format(dateDisplayFmt) ?: "From")
                        }
                        Text("–")
                        OutlinedButton(
                            onClick  = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(customEnd?.format(dateDisplayFmt) ?: "To")
                        }
                    }
                }

                HorizontalDivider()

                // ── What to include ──────────────────────────────────────────
                Text("What to include", style = MaterialTheme.typography.labelLarge)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Periods chip
                    FilterChip(
                        selected = includePeriods,
                        onClick  = { includePeriods = !includePeriods },
                        label    = { Text("Periods") }
                    )

                    // One chip per category
                    categories.forEach { cat ->
                        val selected = cat.id in selectedCategoryIds
                        FilterChip(
                            selected = selected,
                            onClick  = {
                                selectedCategoryIds = if (selected)
                                    selectedCategoryIds - cat.id
                                else
                                    selectedCategoryIds + cat.id
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cat.name)
                                    if (cat.isArchived) {
                                        Icon(
                                            Icons.Filled.Archive,
                                            contentDescription = "Archived",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalDivider()

                // ── Format ───────────────────────────────────────────────────
                Text("Format", style = MaterialTheme.typography.labelLarge)
                ExportFormat.entries.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { format = f }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = format == f, onClick = { format = f })
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when (f) {
                                    ExportFormat.JSON -> "Full backup — can be re-imported"
                                    ExportFormat.CSV  -> "Spreadsheet-friendly flat table"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            val customRangeReady = datePreset != DateRangePreset.CUSTOM ||
                (customStart != null && customEnd != null)
            val hasAnything = includePeriods || selectedCategoryIds.isNotEmpty()
            Button(
                onClick = {
                    onExport(
                        ExportConfig(
                            format              = format,
                            includePeriods      = includePeriods,
                            selectedCategoryIds = selectedCategoryIds,
                            dateRangePreset     = datePreset,
                            customStartDate     = customStart,
                            customEndDate       = customEnd
                        )
                    )
                },
                enabled = customRangeReady && hasAnything
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportDatePickerDialog(
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
                if (minDate == null || !picked.isBefore(minDate)) onConfirm(picked)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState)
    }
}
