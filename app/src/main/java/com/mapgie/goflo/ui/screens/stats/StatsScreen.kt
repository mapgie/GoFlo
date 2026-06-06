package com.mapgie.goflo.ui.screens.stats

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Dashboard as DashboardOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    onNavigateToGrid: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    var bannerExpanded by rememberSaveable { mutableStateOf(false) }
    var categoriesExpanded by rememberSaveable { mutableStateOf(true) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val scrollBehavior = if (isLandscape) TopAppBarDefaults.enterAlwaysScrollBehavior() else null
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.pinResult) {
        val result = state.pinResult ?: return@LaunchedEffect
        val message = when (result) {
            PinResult.ADDED -> "Pinned to dashboard"
            PinResult.DUPLICATE -> "Already pinned to dashboard"
        }
        snackbarHostState.showSnackbar(message)
        viewModel.clearPinResult()
    }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        StatsHelpDialog(onDismiss = { showHelpDialog = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onToggleDashboard) {
                        Icon(
                            imageVector = if (dashboardEnabled) Icons.Filled.Dashboard else Icons.Outlined.DashboardOutline,
                            contentDescription = if (dashboardEnabled) "Dashboard enabled. Tap to disable." else "Dashboard disabled. Tap to enable.",
                            tint = if (dashboardEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    if (dashboardEnabled && state.selectedCategory1 != null) {
                        if (state.isCurrentViewPinned) {
                            IconButton(onClick = { viewModel.unpinCurrentView() }) {
                                Icon(
                                    Icons.Outlined.BookmarkRemove,
                                    contentDescription = "Unpin from Dashboard",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            IconButton(onClick = onPinStat) {
                                Icon(
                                    Icons.Outlined.BookmarkAdd,
                                    contentDescription = "Pin this view to Dashboard",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Help") },
                                onClick = {
                                    showOverflowMenu = false
                                    showHelpDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        modifier = if (scrollBehavior != null)
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        else Modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (!isLandscape) {
                BetaFeedbackBanner()
            }
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

                // Chart card — the headline of the screen, kept at the top.
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChartArea(
                                chartData = state.chartData,
                                hasCategorySelected = state.selectedCategory1 != null,
                                timeRange = state.timeRange,
                                onSelectRange = viewModel::setTimeRange,
                                zoomLevel = state.zoomLevel,
                                showZoom = state.timeRange is TimeRange.SpecificMonth,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            TimeRangePicker(
                                selectedRange = state.timeRange,
                                onSelect = viewModel::setTimeRange,
                                zoomLevel = state.zoomLevel,
                                onZoom = viewModel::setZoomLevel,
                            )
                        }
                    }
                }

                // Category picker — sits right under the chart, collapsible so it
                // can step out of the way and free space for the controls below.
                item {
                    CategoryPickerSection(
                        categories = state.categories,
                        selectedCat1 = state.selectedCategory1,
                        selectedCat2 = state.selectedCategory2,
                        chartType = state.chartType,
                        activeSlot = state.activeSlot,
                        expanded = categoriesExpanded,
                        onToggleExpanded = { categoriesExpanded = !categoriesExpanded },
                        onSelect = viewModel::selectCategory,
                        onSetActiveSlot = viewModel::setActiveSlot,
                        onSwap = viewModel::swapCategories,
                        onClear = viewModel::clearSelections,
                    )
                }

                // Chart configuration — only meaningful once a category is chosen.
                if (state.selectedCategory1 != null) {
                    item {
                        ChartTypeSelector(
                            cat1 = state.selectedCategory1!!,
                            cat2 = state.selectedCategory2,
                            selectedType = state.chartType,
                            onSelect = viewModel::setChartType
                        )
                    }
                    item {
                        ChartGridModeSwitcher(onNavigateToGrid = onNavigateToGrid)
                    }
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
    onSelect: (TimeRange) -> Unit,
    zoomLevel: Int = 1,
    onZoom: (Int) -> Unit = {},
) {
    var showYearDialog by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    // Order: All Time, Year, YTD, Month
    val options = listOf("All Time", "Year", "YTD", "Month")

    val selectedIndex = when (selectedRange) {
        is TimeRange.AllTime -> 0
        is TimeRange.CalendarYear -> 1
        is TimeRange.YearToDate -> 2
        is TimeRange.SpecificMonth -> 3
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                                1 -> showYearDialog = true
                                2 -> onSelect(TimeRange.YearToDate)
                                3 -> {
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
                                .semantics { role = Role.Button }
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
    chartType: ChartType,
    activeSlot: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelect: (TrackingCategory) -> Unit,
    onSetActiveSlot: (Int) -> Unit,
    onSwap: () -> Unit,
    onClear: () -> Unit,
) {
    val useAxisLabels = chartType == ChartType.SCATTER

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row — tapping anywhere collapses or expands the panel.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .semantics {
                        role = Role.Button
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }
                    .clickable { onToggleExpanded() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Categories", style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        val summary = listOfNotNull(selectedCat1?.name, selectedCat2?.name)
                            .joinToString(" + ")
                            .ifEmpty { "No categories selected" }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (expanded && selectedCat1 != null) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    // Slot selector row
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Slot 1
                        val slot1Label = if (useAxisLabels) "X" else "1"
                        CategorySlotChip(
                            slotLabel = slot1Label,
                            category = selectedCat1,
                            isActive = activeSlot == 1,
                            placeholder = "Pick a category",
                            onClick = { onSetActiveSlot(1) },
                        )

                        // Slot 2 (only when slot 1 is filled)
                        if (selectedCat1 != null) {
                            val slot2Label = if (useAxisLabels) "Y" else "2"
                            CategorySlotChip(
                                slotLabel = slot2Label,
                                category = selectedCat2,
                                isActive = activeSlot == 2,
                                placeholder = "Add second...",
                                onClick = { onSetActiveSlot(2) },
                            )
                        }

                        // Swap button when both slots filled
                        if (selectedCat1 != null && selectedCat2 != null) {
                            IconButton(onClick = onSwap, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = "Swap categories",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category chip list — clicking fills the active slot
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isCat1 = selectedCat1?.id == category.id
                            val isCat2 = selectedCat2?.id == category.id
                            val isSelected = isCat1 || isCat2
                            val isActiveSlotSelected =
                                (activeSlot == 1 && isCat1) || (activeSlot == 2 && isCat2)

                            val icon = category.iconName.toCategoryIcon()
                            val bubbleColor = category.colorToken.toCategoryColor()
                            val onBubbleColor = category.colorToken.toCategoryOnColor()

                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelect(category) },
                                label = { Text(category.name) },
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
                                    selectedContainerColor = if (isActiveSlotSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = if (isActiveSlotSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySlotChip(
    slotLabel: String,
    category: TrackingCategory?,
    isActive: Boolean,
    placeholder: String,
    onClick: () -> Unit,
) {
    val bubbleColor = category?.colorToken?.toCategoryColor()
    val onBubbleColor = category?.colorToken?.toCategoryOnColor()
    val icon = category?.iconName?.toCategoryIcon()

    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = {
            Text(
                text = if (category != null) "$slotLabel: ${category.name}" else "$slotLabel: $placeholder",
                maxLines = 1,
            )
        },
        leadingIcon = if (category != null && bubbleColor != null && onBubbleColor != null && icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(bubbleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon.vector,
                        contentDescription = null,
                        tint = onBubbleColor,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isActive,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp,
        )
    )
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

    val options = buildList {
        when {
            cat1.isNumeric && cat2?.isNumeric == true -> {
                add(ChartOption(ChartType.SCATTER, Icons.Default.BubbleChart, "Scatter"))
                add(ChartOption(ChartType.NUMERIC_AVERAGE, Icons.Default.BarChart, "Average"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.BarChart, "Compare"))
                if (cat1.trackAgainstTime && cat2.trackAgainstTime)
                    add(ChartOption(ChartType.TIME_CORRELATION, Icons.Default.ScatterPlot, "Timing"))
            }

            cat1.isNumeric && cat2 == null -> {
                add(ChartOption(ChartType.TIME_SCATTER, Icons.Default.ScatterPlot, "Over Time"))
                add(ChartOption(ChartType.NUMERIC_AVERAGE, Icons.Default.BarChart, "Average"))
                add(ChartOption(ChartType.TIME_SERIES, Icons.Default.BarChart, "Timeline"))
                add(ChartOption(ChartType.NUMERIC_DISTRIBUTION, Icons.Default.DonutLarge, "Spread"))
                add(ChartOption(ChartType.WEEKDAY, Icons.Default.DateRange, "By Day"))
                if (cat1.trackAgainstTime)
                    add(ChartOption(ChartType.TIME_OF_DAY, Icons.Default.Schedule, "By Hour"))
            }

            cat1.isNumeric || cat2?.isNumeric == true -> {
                if (cat2 != null)
                    add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.BarChart, "Compare"))
                else
                    add(ChartOption(ChartType.TIME_SERIES, Icons.Default.BarChart, "Over Time"))
                if (cat1.trackAgainstTime && cat2?.trackAgainstTime == true)
                    add(ChartOption(ChartType.TIME_CORRELATION, Icons.Default.ScatterPlot, "Timing"))
            }

            cat2 != null -> {
                add(ChartOption(ChartType.PIE, Icons.Default.DonutLarge, "Breakdown"))
                add(ChartOption(ChartType.COMBO, Icons.Default.TableChart, "Combos"))
                add(ChartOption(ChartType.DUAL_TIME_SERIES, Icons.Default.BarChart, "Compare"))
                if (cat1.trackAgainstTime && cat2.trackAgainstTime)
                    add(ChartOption(ChartType.TIME_CORRELATION, Icons.Default.ScatterPlot, "Timing"))
            }

            else -> {
                add(ChartOption(ChartType.TRENDS, Icons.Default.BarChart, "Trends"))
                add(ChartOption(ChartType.PIE, Icons.Default.DonutLarge, "Breakdown"))
                add(ChartOption(ChartType.TIME_SERIES, Icons.Default.BarChart, "Over Time"))
                add(ChartOption(ChartType.PHASE_SUMMARY, Icons.Default.TableChart, "By Phase"))
                add(ChartOption(ChartType.WEEKDAY, Icons.Default.DateRange, "By Day"))
                if (cat1.trackAgainstTime)
                    add(ChartOption(ChartType.TIME_OF_DAY, Icons.Default.Schedule, "By Hour"))
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
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = selectedType == option.type,
                            onClick = { onSelect(option.type) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            icon = { SegmentedButtonDefaults.Icon(active = selectedType == option.type) }
                        ) {
                            Text(option.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ── Chart / Grid mode switcher ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartGridModeSwitcher(onNavigateToGrid: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("View", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = true,
                    onClick = {},
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Chart", style = MaterialTheme.typography.labelSmall)
                }
                SegmentedButton(
                    selected = false,
                    onClick = onNavigateToGrid,
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Grid", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Stats Help Dialog ─────────────────────────────────────────────────────────

@Composable
private fun StatsHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stats help") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsHelpSection(
                    title = "Time ranges",
                    body = "All Time: every entry you have logged.\n" +
                        "Year to date (YTD): from 1 January of this year to today.\n" +
                        "Year: all entries in a specific calendar year.\n" +
                        "Month: entries in a single month, with left/right arrows to step through months."
                )
                StatsHelpSection(
                    title = "Chart types",
                    body = "Trends: the most common logged values, ranked by frequency.\n" +
                        "Breakdown: how often each value was logged as a proportion of the total.\n" +
                        "Over Time: how many times each value was logged per time period.\n" +
                        "Compare: two categories side by side over time.\n" +
                        "By Phase: how values are distributed across cycle phases.\n" +
                        "Scatter: plot one numeric value against another.\n" +
                        "Average: the mean numeric value per time period.\n" +
                        "Spread: how often each numeric value was recorded."
                )
                StatsHelpSection(
                    title = "Grid view",
                    body = "The Grid view stacks several categories as rows and shows intensity-shaded cells day by day, so you can spot patterns across categories at a glance."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
private fun StatsHelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Chart Area ────────────────────────────────────────────────────────────────

@Composable
private fun ChartArea(
    chartData: StatsChartData,
    hasCategorySelected: Boolean,
    timeRange: TimeRange,
    onSelectRange: (TimeRange) -> Unit,
    zoomLevel: Int = 1,
    showZoom: Boolean = false,
) {
    val currentMonth = (timeRange as? TimeRange.SpecificMonth)?.yearMonth
    val today = LocalDate.now()
    val currentYear = when (timeRange) {
        is TimeRange.CalendarYear -> timeRange.year
        is TimeRange.YearToDate -> today.year
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Navigation row: month arrows for SpecificMonth, year arrows for CalendarYear/YTD
            when {
                currentMonth != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onSelectRange(TimeRange.SpecificMonth(currentMonth.minusMonths(1)))
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous month"
                            )
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                if (currentMonth < YearMonth.now()) {
                                    onSelectRange(TimeRange.SpecificMonth(currentMonth.plusMonths(1)))
                                }
                            },
                            enabled = currentMonth < YearMonth.now()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next month"
                            )
                        }
                    }
                }
                currentYear != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onSelectRange(TimeRange.CalendarYear(currentYear - 1))
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous year"
                            )
                        }
                        Text(
                            text = if (timeRange is TimeRange.YearToDate) "YTD $currentYear" else "$currentYear",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                if (currentYear < today.year) {
                                    onSelectRange(
                                        if (currentYear + 1 == today.year) TimeRange.YearToDate
                                        else TimeRange.CalendarYear(currentYear + 1)
                                    )
                                }
                            },
                            enabled = currentYear < today.year
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next year"
                            )
                        }
                    }
                }
            }

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
                                    "Select a category below to get started"
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
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)) {
                            Text(
                                text = chartData.categoryName,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            BarChart(data = chartData, zoomLevel = if (showZoom) zoomLevel else 1)
                        }
                    }

                    is StatsChartData.ComboData -> {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            Text(
                                text = "Most common combinations",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            ComboBarChart(data = chartData)
                        }
                    }

                    is StatsChartData.DualTimeSeriesData -> {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            DualBarChart(
                                data = chartData,
                                zoomLevel = if (showZoom) zoomLevel else 1
                            )
                        }
                    }

                    is StatsChartData.NumericAverageData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            NumericAverageChart(
                                data = chartData,
                                zoomLevel = if (showZoom) zoomLevel else 1
                            )
                        }
                    }

                    is StatsChartData.NumericDistributionData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            NumericDistributionChart(data = chartData)
                        }
                    }

                    is StatsChartData.ScatterData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${chartData.yAxisName} vs ${chartData.xAxisName}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            ScatterPlot(data = chartData)
                        }
                    }

                    is StatsChartData.TimeScatterData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${chartData.yAxisName} over time",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            TimeScatterChart(data = chartData)
                        }
                    }

                    is StatsChartData.TrendsData -> {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            TrendsChart(data = chartData)
                        }
                    }

                    is StatsChartData.PhaseSummaryData -> {
                        PhaseSummaryChart(data = chartData)
                    }

                    is StatsChartData.WeekdayData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (chartData.isNumeric) "Average ${chartData.categoryName} by weekday"
                                       else "${chartData.categoryName} by weekday",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            WeekdayChart(data = chartData)
                        }
                    }

                    is StatsChartData.TimeOfDayData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (chartData.isNumeric) "Average ${chartData.categoryName} by time of day"
                                       else "${chartData.categoryName}: time of day",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            TimeOfDayChart(data = chartData)
                        }
                    }

                    is StatsChartData.TimeCorrelationData -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            TimeCorrelationChart(data = chartData)
                        }
                    }
                }
            }
    }
}

// ── Phase Summary Chart ───────────────────────────────────────────────────────

@Composable
private fun PhaseSummaryChart(data: StatsChartData.PhaseSummaryData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "${data.categoryName} by cycle phase",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        data.rows.forEach { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.phase, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${row.logCount} logs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.topValues.isNotEmpty()) {
                    Text(
                        row.topValues.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
