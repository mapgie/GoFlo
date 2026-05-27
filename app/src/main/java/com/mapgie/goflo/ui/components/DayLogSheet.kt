package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.WaterDrop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.model.FlowLevel
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val headerFormat = DateTimeFormatter.ofPattern("EEEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayLogSheet(
    date: LocalDate,
    period: PeriodEntry?,
    periodSymptoms: List<String>,
    trackingLogs: List<TrackingLogWithValues>,
    onDismiss: () -> Unit,
    onEditPeriod: (Long) -> Unit,
    onEditTrackingLog: (categoryId: Long, logId: Long) -> Unit,
    onLogMore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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
                val periodColor   = MaterialTheme.colorScheme.primary
                val periodOnColor = MaterialTheme.colorScheme.onPrimary

                CategorySectionHeader(
                    icon      = Icons.Outlined.WaterDrop,
                    iconColor = periodColor,
                    iconOnColor = periodOnColor,
                    title     = "Period"
                ) {
                    TextButton(onClick = { onEditPeriod(period.id) }) { Text("Edit") }
                }

                val flow = runCatching { FlowLevel.valueOf(period.flowLevel) }
                    .getOrNull()?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                    ?: period.flowLevel

                ChipRow(
                    label          = "Flow",
                    values         = listOf(flow),
                    containerColor = periodColor,
                    contentColor   = periodOnColor
                )

                if (periodSymptoms.isNotEmpty()) {
                    ChipRow(
                        label          = "Symptoms",
                        values         = periodSymptoms,
                        containerColor = periodColor,
                        contentColor   = periodOnColor
                    )
                }

                if (period.notes.isNotEmpty()) {
                    Text(
                        text  = "\"${period.notes}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 42.dp)
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
                    val category    = entry.category
                    val bubbleColor = category?.colorToken?.toCategoryColor()
                        ?: MaterialTheme.colorScheme.secondary
                    val onBubble    = category?.colorToken?.toCategoryOnColor()
                        ?: MaterialTheme.colorScheme.onSecondary
                    val icon        = category?.iconName?.toCategoryIcon()?.vector

                    CategorySectionHeader(
                        icon        = icon,
                        iconColor   = bubbleColor,
                        iconOnColor = onBubble,
                        title       = category?.name ?: "Unknown"
                    ) {
                        TextButton(onClick = {
                            onEditTrackingLog(entry.log.categoryId, entry.log.id)
                        }) { Text("Edit") }
                    }

                    if (entry.values.isNotEmpty()) {
                        ChipRow(
                            label          = null,
                            values         = entry.values,
                            containerColor = bubbleColor,
                            contentColor   = onBubble,
                            modifier       = Modifier.padding(start = 42.dp)
                        )
                    }

                    if (entry.log.notes.isNotEmpty()) {
                        Text(
                            text  = "\"${entry.log.notes}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 42.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
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

@Composable
private fun CategorySectionHeader(
    icon: ImageVector?,
    iconColor: Color,
    iconOnColor: Color,
    title: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier           = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment   = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = iconOnColor,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        action()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    label: String?,
    values: List<String>,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier              = modifier,
        verticalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(4.dp)
        ) {
            values.forEach { v ->
                SelectableChip(
                    label          = v,
                    selected       = true,
                    containerColor = containerColor,
                    contentColor   = contentColor,
                    onClick        = {}
                )
            }
        }
    }
}
