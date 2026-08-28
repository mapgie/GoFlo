package com.mapgie.goflo.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.ui.components.MetricConfig
import com.mapgie.goflo.ui.components.Timeline
import com.mapgie.goflo.ui.components.TimelineEntryData
import com.mapgie.goflo.ui.util.decodeScaleLabels

/**
 * Builds the [MetricConfig] the MetricInput facade renders from a category
 * row. One shared mapping so every surface renders a category identically.
 */
internal fun metricConfigFor(
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
 * Timed increment ("Plus One" + track against time): each append saves a new
 * timestamped log immediately, so the day renders as a running total plus a
 * [Timeline] of the day's entries with per-entry delete. There is deliberately
 * no notes field or Save button on this path.
 */
@Composable
internal fun TimedIncrementTimeline(
    category: TrackingCategory,
    entries: List<TrackingLogWithValues>,
    onAddOne: () -> Unit,
    onDeleteEntry: (TrackingLog) -> Unit,
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
