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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Switch
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import com.mapgie.goflo.ui.components.BetaFeedbackBanner
import com.mapgie.goflo.ui.components.StatsWarningBanner
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    dashboardEnabled: Boolean = false,
    onToggleDashboard: () -> Unit = {},
    onPinStat: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var bannerExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (dashboardEnabled && state.selectedCategory1 != null) {
                        IconButton(onClick = onPinStat) {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                contentDescription = "Pin this view",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            BetaFeedbackBanner()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = 12.dp,
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

            // Dashboard toggle card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Dashboard", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (dashboardEnabled) "Dashboard enabled — pin favourite views" else "Enable to create a quick-access dashboard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = dashboardEnabled, onCheckedChange = { onToggleDashboard() })
                    }
                }
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

            // Pin this view button (when dashboard enabled and a category is selected)
            if (dashboardEnabled && state.selectedCategory1 != null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onPinStat,
                            modifier = Modifier.fillMaxWidth().padding(4.dp)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pin this view to Dashboard")
                        }
                    }
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
}

// ── Time Range Picker ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangePicker(
    selectedRange: TimeRange,
    onSelect: (TimeRange) -> Unit
) {
    var showYearDialog by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    val options = listOf("All Time", "YTD", "Year", "Month")

    val selectedIndex = when (selectedRange) {
        is TimeRange.AllTime       -> 0
        is TimeRange.YearToDate    -> 1
        is TimeRange.CalendarYear  -> 2
        is TimeRange.SpecificMonth -> 3
    }

    // Current month for inline navigation
    val currentMonth = when (selectedRange) {
        is TimeRange.SpecificMonth -> selectedRange.yearMonth
        else -> YearMonth.now()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Time range",
                style = MaterialTheme.typography.titleMedium,
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
                                3 -> {
                                    // When Month tab tapped and not already SpecificMonth, default to now
                                    if (selectedRange !is TimeRange.SpecificMonth) {
                                        onSelect(TimeRange.SpecificMonth(YearMonth.now()))
                                    }
                                }
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Inline month navigation row for SpecificMonth
            if (selectedRange is TimeRange.SpecificMonth) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        onSelect(TimeRange.SpecificMonth(currentMonth.minusMonths(1)))
                    }) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = "Previous month")
                    }
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            if (currentMonth < YearMonth.now()) {
                                onSelect(TimeRange.SpecificMonth(currentMonth.plusMonths(1)))
                            }
                        },
                        enabled = currentMonth < YearMonth.now()
                    ) {
                        Icon(Icons.Default.NavigateNext, contentDescription = "Next month")
                    }
                }
            } else {
                // Show range label for YTD and CalendarYear
                val rangeLabel = when (selectedRange) {
                    is TimeRange.AllTime -> null
                    is TimeRange.YearToDate -> "January 1 – Today"
                    is TimeRange.CalendarYear -> "Full year ${selectedRange.year}"
                    is TimeRange.SpecificMonth -> null // handled above
                }
                if (rangeLabel != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    TextButton(
                        onClick = {
                            when (selectedRange) {
                                is TimeRange.CalendarYear -> showYearDialog = true
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
                    style = MaterialTheme.typography.titleMedium
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedCat1?.let {
                        Text(
                            text = "X: ${it.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    selectedCat2?.let {
                        if (selectedCat1 != null) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Y: ${it.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isCat1 = selectedCat1?.id == category.id
                    val isCat2 = selectedCat2?.id == category.id
                    val isSelected = isCat1 || isCat2

                    val icon = category.iconName.toCategoryIcon()
                    val bubbleColor = category.colorToken.toCategoryColor()
                    val onBubbleColor = category.colorToken.toCategoryOnColor()

                    val chipSelectedContainerColor = when {
                        isCat1 -> MaterialTheme.colorScheme.primaryContainer
                        isCat2 -> MaterialTheme.colorScheme.secondaryContainer
                        else   -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val chipSelectedLabelColor = when {
                        isCat1 -> MaterialTheme.colorScheme.onPrimaryContainer
                        isCat2 -> MaterialTheme.colorScheme.onSecondaryContainer
                        else   -> MaterialTheme.colorScheme.onSurfaceVariant
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
                            selectedContainerColor = chipSelectedContainerColor,
                            selectedLabelColor = chipSelectedLabelColor,
                            selectedLeadingIconColor = chipSelectedLabelColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
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
                add(ChartOption(ChartType.TIME_SERIES,      Icons.Default.BarChart,    "Over Time"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.BarChart,    "Compare"))
            }
            cat1.isNumeric && cat2 == null -> {
                add(ChartOption(ChartType.TIME_SCATTER,         Icons.Default.ScatterPlot,  "Time"))
                add(ChartOption(ChartType.NUMERIC_AVERAGE,      Icons.Default.ShowChart,    "Average"))
                add(ChartOption(ChartType.TIME_SERIES,          Icons.Default.BarChart,     "Over Time"))
                add(ChartOption(ChartType.NUMERIC_DISTRIBUTION, Icons.Default.DonutLarge,   "Distribution"))
            }
            eitherNumeric -> {
                add(ChartOption(ChartType.TIME_SERIES,      Icons.Default.BarChart,  "Over Time"))
                if (cat2 != null)
                    add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.ShowChart, "Compare"))
            }
            cat2 != null -> {
                add(ChartOption(ChartType.TRENDS,           Icons.Default.BarChart,    "Trends"))
                add(ChartOption(ChartType.PIE,              Icons.Default.DonutLarge,  "Distribution"))
                add(ChartOption(ChartType.TIME_SERIES,      Icons.Default.BarChart,    "Over Time"))
                add(ChartOption(ChartType.COMBO,            Icons.Default.TableChart,  "Combinations"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.ShowChart,   "Compare"))
            }
            else -> {
                add(ChartOption(ChartType.TRENDS,      Icons.Default.BarChart,   "Trends"))
                add(ChartOption(ChartType.PIE,         Icons.Default.DonutLarge, "Distribution"))
                add(ChartOption(ChartType.TIME_SERIES, Icons.Default.BarChart,   "Over Time"))
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Chart type",
                style = MaterialTheme.typography.titleMedium
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
                            .width(80.dp)
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
                                .padding(8.dp),
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

                is StatsChartData.TimeScatterData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${chartData.yAxisName} over time",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        TimeScatterChart(data = chartData)
                    }
                }

                is StatsChartData.TrendsData -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        TrendsChart(data = chartData)
                    }
                }
            }
        }
    }
}
