package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.model.FlowLevel
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val headerFormat = DateTimeFormatter.ofPattern("EEEE, MMM d")

/**
 * A bottom sheet that summarises everything logged on [date]:
 * - Period data (if [date] falls within [period])
 * - Tracking category entries ([trackingLogs])
 *
 * Provides "Edit" shortcuts and a "Log more…" button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayLogSheet(
    date: LocalDate,
    period: PeriodEntry?,
    periodSymptoms: List<String>,      // Human-readable symptom labels for the period
    trackingLogs: List<TrackingLogWithValues>,
    onDismiss: () -> Unit,
    onEditPeriod: (Long) -> Unit,
    onEditTrackingLog: (categoryId: Long, logId: Long) -> Unit,
    onLogMore: () -> Unit              // Opens the FAB long-press action menu
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────

            Text(
                text = date.format(headerFormat),
                style = MaterialTheme.typography.titleLarge
            )

            HorizontalDivider()

            // ── Period section ────────────────────────────────────────────────

            if (period != null) {
                SectionHeader(title = "Period") {
                    OutlinedButton(onClick = { onEditPeriod(period.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                val flow = runCatching { FlowLevel.valueOf(period.flowLevel) }
                    .getOrNull()?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                    ?: period.flowLevel

                InfoChipRow(label = "Flow", values = listOf(flow))

                if (periodSymptoms.isNotEmpty()) {
                    InfoChipRow(label = "Symptoms", values = periodSymptoms)
                }

                if (period.notes.isNotEmpty()) {
                    Text(
                        text = "\"${period.notes}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()
            }

            // ── Tracking logs ─────────────────────────────────────────────────

            if (trackingLogs.isNotEmpty()) {
                Text(
                    "Tracked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                trackingLogs.forEach { entry ->
                    SectionHeader(title = entry.category?.name ?: "Unknown") {
                        TextButton(onClick = {
                            onEditTrackingLog(entry.log.categoryId, entry.log.id)
                        }) { Text("Edit") }
                    }

                    if (entry.values.isNotEmpty()) {
                        InfoChipRow(label = null, values = entry.values)
                    }

                    if (entry.log.notes.isNotEmpty()) {
                        Text(
                            text = "\"${entry.log.notes}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }

                HorizontalDivider()
            }

            // ── Log more ──────────────────────────────────────────────────────

            OutlinedButton(
                onClick = { onDismiss(); onLogMore() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log more for this day…")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        action()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoChipRow(label: String?, values: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            values.forEach { v ->
                SelectableChip(label = v, selected = true, onClick = {})
            }
        }
    }
}
