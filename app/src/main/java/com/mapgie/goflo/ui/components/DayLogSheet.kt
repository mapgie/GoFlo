package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.ui.util.decodeScaleLabels
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val headerFormat = DateTimeFormatter.ofPattern("EEEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayLogSheet(
    date: LocalDate,
    period: PeriodEntry?,
    trackingLogs: List<TrackingLogWithValues>,
    onDismiss: () -> Unit,
    onEditPeriod: (Long) -> Unit,
    onEditTrackingLog: (categoryId: Long, logId: Long) -> Unit,
    onLogMore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    // Group logs by category — preserving first-appearance order
    val logsByCategory = trackingLogs.groupBy { it.category?.id ?: -1L }
    val categoryOrder = trackingLogs.map { it.category?.id ?: -1L }.distinct()

    // "Display against time" toggle — only relevant if any category tracks time
    val anyTrackAgainstTime = trackingLogs.any {
        it.category?.trackAgainstTime == true && it.log.loggedAt.isNotEmpty()
    }
    var showAgainstTime by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.format(headerFormat),
                    style = MaterialTheme.typography.titleLarge
                )
                if (anyTrackAgainstTime) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Display logs against time") },
                                onClick = { showAgainstTime = !showAgainstTime; showMenu = false },
                                leadingIcon = {
                                    Checkbox(
                                        checked = showAgainstTime,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Period section ────────────────────────────────────────────────

            if (period != null) {
                val periodColor = MaterialTheme.colorScheme.primary

                LogEntryRow(
                    icon        = Icons.Outlined.WaterDrop,
                    iconColor   = periodColor,
                    iconOnColor = MaterialTheme.colorScheme.onPrimary,
                    label       = "Period",
                    onEdit      = { onEditPeriod(period.id) }
                ) {
                    if (period.notes.isNotEmpty()) {
                        Text(
                            text      = "\"${period.notes}\"",
                            style     = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()
            }

            // ── Tracking logs (grouped by category) ───────────────────────────

            if (trackingLogs.isNotEmpty()) {
                Text(
                    "Tracked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                categoryOrder.forEach { catId ->
                    val entries = logsByCategory[catId] ?: return@forEach
                    val first = entries.first()
                    val category = first.category
                    val bubbleColor = category?.colorToken?.toCategoryColor()
                        ?: MaterialTheme.colorScheme.secondary
                    val onBubble = category?.colorToken?.toCategoryOnColor()
                        ?: MaterialTheme.colorScheme.onSecondary
                    val icon = category?.iconName?.toCategoryIcon()?.vector

                    val hasTimedEntries = showAgainstTime &&
                        category?.trackAgainstTime == true &&
                        entries.any { it.log.loggedAt.isNotEmpty() }

                    LogEntryRow(
                        icon        = icon,
                        iconColor   = bubbleColor,
                        iconOnColor = onBubble,
                        label       = category?.name ?: "Unknown",
                        onEdit      = { onEditTrackingLog(first.log.categoryId, entries.last().log.id) }
                    ) {
                        if (hasTimedEntries) {
                            // Show each entry with its timestamp on its own line
                            entries.forEach { entry ->
                                val timePrefix = entry.log.loggedAt.ifEmpty { null }
                                val displayValues = if (category != null) {
                                    entry.values.map { enrichDisplayValue(it, category) }
                                } else {
                                    entry.values
                                }
                                val valueText = displayValues.joinToString(", ")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (timePrefix != null && valueText.isNotEmpty()) "$timePrefix $valueText"
                                               else timePrefix ?: valueText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { onEditTrackingLog(entry.log.categoryId, entry.log.id) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = "edit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Collect all values from all entries and display inline
                            val allDisplayValues = entries.flatMap { entry ->
                                if (category != null) {
                                    entry.values.map { enrichDisplayValue(it, category) }
                                } else {
                                    entry.values
                                }
                            }

                            if (allDisplayValues.isNotEmpty()) {
                                if (allDisplayValues.size == 1) {
                                    Text(
                                        text  = allDisplayValues[0],
                                        style = MaterialTheme.typography.titleMedium,
                                        color = bubbleColor
                                    )
                                } else {
                                    Text(
                                        text  = allDisplayValues.joinToString(" · "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Show notes from the last entry that has them
                            val notesEntry = entries.lastOrNull { it.log.notes.isNotEmpty() }
                            if (notesEntry != null) {
                                Text(
                                    text      = "\"${notesEntry.log.notes}\"",
                                    style     = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
            }

            // ── Log more ──────────────────────────────────────────────────────

            OutlinedButton(
                onClick  = { onDismiss(); onLogMore() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log more for this day…")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Converts a raw stored value string into a display string for the day sheet.
 */
private fun enrichDisplayValue(value: String, category: TrackingCategory): String {
    if (category.categoryType == "numeric_slider" && category.scaleLabels.isNotBlank()) {
        val label = value.toFloatOrNull()?.toInt()?.let { category.scaleLabels.decodeScaleLabels()[it] }
        if (label != null) return label
    }
    return if (category.numericUnit.isNotBlank()) "$value ${category.numericUnit}" else value
}

@Composable
private fun LogEntryRow(
    icon: ImageVector?,
    iconColor: Color,
    iconOnColor: Color,
    label: String,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconOnColor,
                    modifier           = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }

        TextButton(
            onClick        = onEdit,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text  = "edit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AttributeValueLine(
    attribute: String,
    value: String,
    valueColor: Color,
    valueLarge: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = attribute,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = value,
            style = if (valueLarge) MaterialTheme.typography.titleMedium
                    else           MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}
