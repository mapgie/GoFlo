package com.mapgie.goflo.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.components.StatsWarningBanner
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var bannerExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatsWarningBanner(
                    isExpanded = bannerExpanded,
                    onToggle = { bannerExpanded = !bannerExpanded }
                )
            }

            item {
                TimeRangePicker(
                    selectedRange = state.timeRange,
                    onSelect = viewModel::setTimeRange
                )
            }

            item {
                CategoryPickerSection(
                    categories = state.categories,
                    selectedCat1 = state.selectedCategory1,
                    selectedCat2 = state.selectedCategory2,
                    onSelect = viewModel::selectCategory,
                    onClear = viewModel::clearSelections
                )
            }

            if (state.selectedCategory1 != null) {
                item {
                    ChartTypeSelector(
                        cat1 = state.selectedCategory1!!,
                        cat2 = state.selectedCategory2,
                        selectedType = state.chartType,
                        onSelect = viewModel::setChartType
                    )
                }
            }

            item {
                ChartArea(
                    chartData = state.chartData,
                    hasCategorySelected = state.selectedCategory1 != null
                )
            }
        }
    }
}

// ── Time Range Picker ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangePicker(
    selectedRange: TimeRange,
    onSelect: (TimeRange) -> Unit
) {
    var showYearDialog by rememberSaveable { mutableStateOf(false) }
    var showMonthDialog by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    val options = listOf("All Time", "YTD", "Year", "Month")

    val selectedIndex = when (selectedRange) {
        is TimeRange.AllTime       -> 0
        is TimeRange.YearToDate    -> 1
        is TimeRange.CalendarYear  -> 2
        is TimeRange.SpecificMonth -> 3
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Time range",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedIndex == index,
                        onClick = {
                            when (index) {
                                0 -> onSelect(TimeRange.AllTime)
                                1 -> onSelect(TimeRange.YearToDate)
                                2 -> showYearDialog = true
                                3 -> showMonthDialog = true
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            val rangeLabel = when (selectedRange) {
                is TimeRange.AllTime -> null
                is TimeRange.YearToDate -> "January 1 – Today"
                is TimeRange.CalendarYear -> "Full year ${selectedRange.year}"
                is TimeRange.SpecificMonth -> selectedRange.yearMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy")
                )
            }
            if (rangeLabel != null) {
                Spacer(modifier = Modifier.height(2.dp))
                // Tapping the range label reopens the picker so the user can jump to
                // a different month or year without having to re-tap the chip first.
                TextButton(
                    onClick = {
                        when (selectedRange) {
                            is TimeRange.CalendarYear  -> showYearDialog  = true
                            is TimeRange.SpecificMonth -> showMonthDialog = true
                            else -> {}
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = rangeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showYearDialog) {
        val years = (today.year downTo maxOf(2020, today.year - 10)).toList()
        AlertDialog(
            onDismissRequest = { showYearDialog = false },
            title = { Text("Select year") },
            text = {
                LazyColumn {
                    items(years) { year ->
                        Text(
                            text = year.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(TimeRange.CalendarYear(year))
                                    showYearDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMonthDialog) {
        val months = (0 until 36).map { YearMonth.now().minusMonths(it.toLong()) }
        val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
        AlertDialog(
            onDismissRequest = { showMonthDialog = false },
            title = { Text("Select month") },
            text = {
                LazyColumn {
                    items(months) { ym ->
                        Text(
                            text = ym.format(monthFmt),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(TimeRange.SpecificMonth(ym))
                                    showMonthDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Category Picker ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSection(
    categories: List<TrackingCategory>,
    selectedCat1: TrackingCategory?,
    selectedCat2: TrackingCategory?,
    onSelect: (TrackingCategory) -> Unit,
    onClear: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pick up to 2 categories",
                    style = MaterialTheme.typography.titleSmall
                )
                if (selectedCat1 != null) {
                    TextButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            if (selectedCat1 != null || selectedCat2 != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCat1?.let {
                        Text(
                            text = "X: ${it.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    selectedCat2?.let {
                        Text(
                            text = "Y: ${it.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { category ->
                    val isCat1 = selectedCat1?.id == category.id
                    val isCat2 = selectedCat2?.id == category.id
                    val isSelected = isCat1 || isCat2

                    val icon = category.iconName.toCategoryIcon()
                    val bubbleColor = category.colorToken.toCategoryColor()
                    val onBubbleColor = category.colorToken.toCategoryOnColor()

                    val borderColor = when {
                        isCat1 -> MaterialTheme.colorScheme.primary
                        isCat2 -> MaterialTheme.colorScheme.secondary
                        else   -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(category) },
                        label = {
                            val prefix = when {
                                isCat1 -> "X  "
                                isCat2 -> "Y  "
                                else   -> ""
                            }
                            Text("$prefix${category.name}")
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(bubbleColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon.vector,
                                    contentDescription = null,
                                    tint = onBubbleColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = borderColor,
                            selectedBorderColor = borderColor,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
            }
        }
    }
}

// ── Chart Type Selector ───────────────────────────────────────────────────────

@Composable
private fun ChartTypeSelector(
    cat1: TrackingCategory,
    cat2: TrackingCategory?,
    selectedType: ChartType,
    onSelect: (ChartType) -> Unit
) {
    data class ChartOption(val type: ChartType, val icon: ImageVector, val label: String)

    val eitherNumeric = cat1.isNumeric || cat2?.isNumeric == true

    val options = buildList {
        when {
            cat1.isNumeric && cat2?.isNumeric == true -> {
                add(ChartOption(ChartType.SCATTER,          Icons.Default.BubbleChart, "Scatter"))
                add(ChartOption(ChartType.NUMERIC_AVERAGE,  Icons.Default.ShowChart,   "Average"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.BarChart,    "Compare"))
            }
            cat1.isNumeric && cat2 == null -> {
                add(ChartOption(ChartType.NUMERIC_AVERAGE,      Icons.Default.ShowChart, "Average"))
                add(ChartOption(ChartType.NUMERIC_DISTRIBUTION, Icons.Default.BarChart,  "Distribution"))
            }
            eitherNumeric -> {
                add(ChartOption(ChartType.TIME_SERIES,      Icons.Default.BarChart,  "Over Time"))
                if (cat2 != null)
                    add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.ShowChart, "Compare"))
            }
            cat2 != null -> {
                add(ChartOption(ChartType.PIE,              Icons.Default.DonutLarge,  "Distribution"))
                add(ChartOption(ChartType.TIME_SERIES,      Icons.Default.BarChart,    "Over Time"))
                add(ChartOption(ChartType.COMBO,            Icons.Default.TableChart,  "Combinations"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.ShowChart,   "Compare"))
            }
            else -> {
                add(ChartOption(ChartType.PIE,         Icons.Default.DonutLarge, "Distribution"))
                add(ChartOption(ChartType.TIME_SERIES, Icons.Default.BarChart,   "Over Time"))
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Chart type",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(options) { option ->
                    val isSelected = selectedType == option.type
                    Card(
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onSelect(option.type) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Chart Area ────────────────────────────────────────────────────────────────

@Composable
private fun ChartArea(
    chartData: StatsChartData,
    hasCategorySelected: Boolean
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (chartData) {
                is StatsChartData.Empty -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = if (!hasCategorySelected)
                                "Select a category above to get started"
                            else
                                "No data found for the selected range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is StatsChartData.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                }

                is StatsChartData.PieData -> {
                    PieChart(
                        data = chartData,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                is StatsChartData.TimeSeriesData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            text = chartData.categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        BarChart(data = chartData)
                    }
                }

                is StatsChartData.ComboData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            text = "Most common combinations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        ComboBarChart(data = chartData)
                    }
                }

                is StatsChartData.DualTimeSeriesData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        DualBarChart(data = chartData)
                    }
                }

                is StatsChartData.NumericAverageData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        NumericAverageChart(data = chartData)
                    }
                }

                is StatsChartData.NumericDistributionData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        NumericDistributionChart(data = chartData)
                    }
                }

                is StatsChartData.ScatterData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${chartData.yAxisName} vs ${chartData.xAxisName}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        ScatterPlot(data = chartData)
                    }
                }
            }
        }
    }
}
