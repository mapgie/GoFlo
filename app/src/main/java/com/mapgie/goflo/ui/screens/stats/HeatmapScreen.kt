package com.mapgie.goflo.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.goflo.ui.util.continuousShade
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import kotlin.math.roundToInt

private val LABEL_COLUMN_WIDTH = 108.dp
private val HEADER_HEIGHT = 26.dp

/** Cell side length in dp for each zoom level (0 compact, 1 normal, 2 wide). */
private fun cellSideFor(zoom: Int) = when (zoom) {
    0 -> 16.dp
    2 -> 34.dp
    else -> 24.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    viewModel: HeatmapViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grid") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CategoryMultiSelect(
                    state = state,
                    onToggle = viewModel::toggleCategory,
                    onClear = viewModel::clearSelection,
                )
            }

            if (state.selectedCategoryIds.isNotEmpty()) {
                item {
                    HeatmapControls(
                        state = state,
                        onSetWindow = viewModel::setWindow,
                        onSetAggregation = viewModel::setAggregation,
                        onSetZoom = viewModel::setZoomLevel,
                    )
                }
            }

            item {
                GridArea(state = state)
            }
        }
    }
}

// ── Category multi-select ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMultiSelect(
    state: HeatmapUiState,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Categories", style = MaterialTheme.typography.titleMedium)
                if (state.selectedCategoryIds.isNotEmpty()) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            }
            Text(
                "Pick categories to stack as rows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.categories.forEach { category ->
                    val isSelected = category.id in state.selectedCategoryIds
                    val bubbleColor = category.colorToken.toCategoryColor()
                    val onBubbleColor = category.colorToken.toCategoryOnColor()
                    val icon = category.iconName.toCategoryIcon()
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(bubbleColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon.vector,
                                    contentDescription = null,
                                    tint = onBubbleColor,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                        ),
                    )
                }
            }
        }
    }
}

// ── Controls ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeatmapControls(
    state: HeatmapUiState,
    onSetWindow: (HeatmapWindow) -> Unit,
    onSetAggregation: (HeatmapAggregation) -> Unit,
    onSetZoom: (Int) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Window
            Column {
                Text("Window", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                val windows = HeatmapWindow.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    windows.forEachIndexed { index, window ->
                        SegmentedButton(
                            selected = state.window == window,
                            onClick = { onSetWindow(window) },
                            shape = SegmentedButtonDefaults.itemShape(index, windows.size),
                        ) {
                            Text(window.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Aggregation
            Column {
                Text("Aggregation", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                val modes = HeatmapAggregation.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.aggregation == mode,
                            onClick = { onSetAggregation(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        ) {
                            Text(
                                if (mode == HeatmapAggregation.SUM) "Sum" else "Average",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            // Zoom
            Column {
                Text("Cell size", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                val labels = listOf("Compact", "Normal", "Wide")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = state.zoomLevel == index,
                            onClick = { onSetZoom(index) },
                            shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Grid area (handles empty / loading / grid) ─────────────────────────────────

@Composable
private fun GridArea(state: HeatmapUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (val grid = state.grid) {
                is HeatmapData.Empty -> {
                    Text(
                        "Select one or more categories to build the grid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is HeatmapData.Loading -> {
                    Text(
                        "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is HeatmapData.Grid -> {
                    if (grid.rows.all { row -> row.magnitudes.all { it == null } }) {
                        Text(
                            "No entries logged in this window.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        HeatmapGrid(grid = grid, zoomLevel = state.zoomLevel)
                        Spacer(Modifier.height(12.dp))
                        HeatmapLegend()
                    }
                }
            }
        }
    }
}

// ── The grid itself ────────────────────────────────────────────────────────────

@Composable
private fun HeatmapGrid(grid: HeatmapData.Grid, zoomLevel: Int) {
    val cellSide = cellSideFor(zoomLevel)
    // Rows share one height on both the pinned label column and the scrolling cells so
    // they line up; never shorter than 20dp so compact cells stay readable.
    val rowHeight = cellSide.coerceAtLeast(20.dp)
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val scrollState = rememberScrollState()

    Row(modifier = Modifier.fillMaxWidth()) {
        // Pinned left label column
        Column(modifier = Modifier.width(LABEL_COLUMN_WIDTH)) {
            Spacer(Modifier.height(HEADER_HEIGHT))
            grid.rows.forEach { row ->
                RowLabel(row = row, height = rowHeight)
            }
        }

        // Scrollable date header + cells
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // Date header
            Row(modifier = Modifier.height(HEADER_HEIGHT)) {
                grid.columns.forEach { col ->
                    Box(
                        modifier = Modifier
                            .width(cellSide)
                            .height(HEADER_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = col.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // One row of cells per category
            grid.rows.forEach { row ->
                val base = row.colorToken.toCategoryColor()
                Row(
                    modifier = Modifier.height(rowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.magnitudes.forEachIndexed { i, magnitude ->
                        HeatmapCell(
                            magnitude = magnitude,
                            row = row,
                            base = base,
                            surface = surface,
                            cellSide = cellSide,
                            columnAccessibilityLabel = grid.columns[i].accessibilityLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowLabel(row: HeatmapRow, height: androidx.compose.ui.unit.Dp) {
    val bubbleColor = row.colorToken.toCategoryColor()
    val onBubbleColor = row.colorToken.toCategoryOnColor()
    val icon = row.iconName.toCategoryIcon()
    Row(
        modifier = Modifier
            .height(height)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(bubbleColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon.vector,
                contentDescription = null,
                tint = onBubbleColor,
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeatmapCell(
    magnitude: Float?,
    row: HeatmapRow,
    base: Color,
    surface: Color,
    cellSide: androidx.compose.ui.unit.Dp,
    columnAccessibilityLabel: String,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val fraction = if (magnitude == null) 0f else {
        if (row.rowMax <= row.rowMin) 1f
        else ((magnitude - row.rowMin) / (row.rowMax - row.rowMin)).coerceIn(0f, 1f)
    }
    val cellColor = if (magnitude == null) Color.Transparent else continuousShade(base, surface, fraction)
    val description = buildString {
        append(row.name)
        append(", ")
        append(columnAccessibilityLabel)
        append(": ")
        append(if (magnitude == null) "no entry" else row.describeMagnitude(magnitude))
    }

    Box(
        modifier = Modifier
            .size(cellSide)
            .padding(1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(cellColor)
            .then(
                if (magnitude == null)
                    Modifier.border(0.5.dp, outline.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                else Modifier
            )
            .clearAndSetSemantics { contentDescription = description },
    )
}

// ── Legend ─────────────────────────────────────────────────────────────────────

@Composable
private fun HeatmapLegend() {
    val base = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val steps = 6
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Lower",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Row {
                repeat(steps) { i ->
                    val fraction = i.toFloat() / (steps - 1)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(continuousShade(base, surface, fraction))
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "Higher",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Intensity is scaled within each row. Numeric rows show the value, " +
                "ordinal rows show the level, and other rows count entries (these ignore Sum and Average).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Magnitude formatting ───────────────────────────────────────────────────────

/** Human-readable text for a cell's [magnitude], used in accessibility descriptions. */
private fun HeatmapRow.describeMagnitude(magnitude: Float): String = when (kind) {
    MagnitudeKind.COUNT -> {
        val n = magnitude.roundToInt()
        if (n == 1) "1 entry" else "$n entries"
    }
    MagnitudeKind.ORDINAL -> {
        val order = magnitude.roundToInt()
        ordinalLabels[order] ?: "level $order"
    }
    MagnitudeKind.NUMERIC -> {
        val rounded = magnitude.roundToInt()
        val scaleLabel = numericScaleLabels[rounded]
        if (scaleLabel != null && magnitude == rounded.toFloat()) {
            scaleLabel
        } else {
            val number = if (magnitude == rounded.toFloat()) rounded.toString()
            else "%.1f".format(magnitude)
            if (unit.isNotBlank()) "$number $unit" else number
        }
    }
}
