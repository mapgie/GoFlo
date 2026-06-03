package com.mapgie.goflo.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.util.decodeScaleLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

// ── Time range ────────────────────────────────────────────────────────────────

sealed class TimeRange {
    data object AllTime : TimeRange()
    data object YearToDate : TimeRange()
    data class CalendarYear(val year: Int) : TimeRange()
    data class SpecificMonth(val yearMonth: YearMonth) : TimeRange()
}

// ── Chart type ────────────────────────────────────────────────────────────────

enum class ChartType {
    PIE,
    TIME_SERIES,
    COMBO,
    DUAL_TIME_SERIES,
    /** Average of numeric values per time bucket. Only valid when cat1 is numeric. */
    NUMERIC_AVERAGE,
    /** Frequency histogram of each discrete numeric value. Only valid when cat1 is numeric. */
    NUMERIC_DISTRIBUTION,
    /** Scatter plot of cat1 (X) vs cat2 (Y). Only valid when both categories are numeric. */
    SCATTER,
    /** Scatter plot of cat1 value (Y) vs time (X). Only valid when cat1 is numeric. */
    TIME_SCATTER,
    /** Trend bars showing most common values for a category. */
    TRENDS,
    /** Per-phase breakdown of logged values across the cycle. */
    PHASE_SUMMARY,
}

// ── Chart data models ─────────────────────────────────────────────────────────

data class PhaseSummaryRow(
    val phase: String,
    val logCount: Int,
    val topValues: List<String>,
)

data class PieSlice(val label: String, val count: Int, val fraction: Float)
data class TimeBucket(val label: String, val count: Int)
data class ComboBar(val label: String, val count: Int)
data class DualBucket(val label: String, val count1: Int, val count2: Int)

/** One time bucket for a numeric average chart: holds the average value and log count. */
data class NumericBucket(val label: String, val average: Float, val count: Int)

/** One bar in a numeric distribution histogram. */
data class NumericHistBar(val label: String, val count: Int)

data class ScatterPoint(val x: Float, val y: Float)

data class TimeScatterPoint(val dayOffset: Int, val dateLabel: String, val value: Float)

data class TrendsBar(val label: String, val count: Int, val percentage: Int)

data class PinnedStat(
    val id: String,
    val label: String,
    val categoryId1: Long,
    val categoryId2: Long?,
    val timeRangeType: String,  // "ALL_TIME", "YTD", "YEAR:2025", "MONTH:2025-01"
    val chartType: String,
)

sealed class StatsChartData {
    data object Empty : StatsChartData()
    data object Loading : StatsChartData()
    data class PieData(
        val slices: List<PieSlice>,
        /** Category color token used to shade slices by displayOrder. Empty = cycle colours. */
        val colorToken: String = "",
        /** Maps each value label to its displayOrder for ordinal shade computation. */
        val valueOrders: Map<String, Int> = emptyMap(),
    ) : StatsChartData()
    data class TimeSeriesData(val buckets: List<TimeBucket>, val categoryName: String) : StatsChartData()
    data class ComboData(val bars: List<ComboBar>) : StatsChartData()
    data class DualTimeSeriesData(
        val buckets: List<DualBucket>,
        val categoryName1: String,
        val categoryName2: String,
        val colorToken1: String = "",
        val colorToken2: String = "",
    ) : StatsChartData()
    data class NumericAverageData(
        val buckets: List<NumericBucket>,
        val categoryName: String,
        /** Smallest average value across all buckets — used to scale the y-axis. */
        val globalMin: Float,
        /** Largest average value across all buckets — used to scale the y-axis. */
        val globalMax: Float,
    ) : StatsChartData()
    data class NumericDistributionData(
        val bars: List<NumericHistBar>,
        val categoryName: String,
    ) : StatsChartData()
    data class ScatterData(
        val points: List<ScatterPoint>,
        val xAxisName: String,
        val yAxisName: String,
        val xMin: Float,
        val xMax: Float,
        val yMin: Float,
        val yMax: Float,
    ) : StatsChartData()
    data class TimeScatterData(
        val points: List<TimeScatterPoint>,
        val yAxisName: String,
        val yMin: Float,
        val yMax: Float,
    ) : StatsChartData()
    data class TrendsData(
        val bars: List<TrendsBar>,
        val categoryName: String,
        /** Category color token used to shade bars by displayOrder. Empty = use primary. */
        val colorToken: String = "",
        /** Maps each value label to its displayOrder for ordinal shade computation. */
        val valueOrders: Map<String, Int> = emptyMap(),
    ) : StatsChartData()
    data class PhaseSummaryData(
        val rows: List<PhaseSummaryRow>,
        val categoryName: String,
    ) : StatsChartData()
}

// ── Pin result ────────────────────────────────────────────────────────────────

enum class PinResult { ADDED, DUPLICATE }

// ── UI State ──────────────────────────────────────────────────────────────────

data class StatsUiState(
    val categories: List<TrackingCategory> = emptyList(),
    val selectedCategory1: TrackingCategory? = null,
    val selectedCategory2: TrackingCategory? = null,
    val timeRange: TimeRange = TimeRange.YearToDate,
    val chartType: ChartType = ChartType.PIE,
    val chartData: StatsChartData = StatsChartData.Empty,
    val rememberedChartType: ChartType? = null,
    val dashboardEnabled: Boolean = false,
    val activeSlot: Int = 1,
    val zoomLevel: Int = 1,
    val pinResult: PinResult? = null,
    val isCurrentViewPinned: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StatsViewModel(
    private val repository: TrackingRepository,
    private val preferencesStore: AppPreferencesStore? = null,
    private val periodRepository: com.mapgie.goflo.data.repository.PeriodRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private var savedCat1Id: Long = -1L
    private var savedCat2Id: Long = -1L

    init {
        // Restore persisted stats state once, before categories load.
        preferencesStore?.let { store ->
            viewModelScope.launch {
                val prefs = store.preferences.first()
                savedCat1Id = prefs.statsCategory1Id
                savedCat2Id = prefs.statsCategory2Id
                _uiState.update {
                    it.copy(
                        timeRange = parseTimeRangeString(prefs.statsTimeRange),
                        chartType = parseChartTypeString(prefs.statsChartType),
                        zoomLevel = prefs.statsZoomLevel,
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                val alreadySelected = _uiState.value.selectedCategory1 != null
                _uiState.update { it.copy(categories = cats.filter { !it.isArchived }) }
                // Restore saved category selections after categories are first loaded.
                if (!alreadySelected && savedCat1Id != -1L) {
                    val cat1 = cats.firstOrNull { it.id == savedCat1Id }
                    val cat2 = if (savedCat2Id != -1L) cats.firstOrNull { it.id == savedCat2Id } else null
                    if (cat1 != null) {
                        _uiState.update { it.copy(selectedCategory1 = cat1, selectedCategory2 = cat2) }
                        reloadChart()
                    }
                }
                refreshPinnedState()
            }
        }

        preferencesStore?.let { store ->
            viewModelScope.launch {
                store.preferences.collect { prefs ->
                    _uiState.update { it.copy(dashboardEnabled = prefs.dashboardEnabled) }
                    refreshPinnedState()
                }
            }
        }
    }

    // ── Category selection ────────────────────────────────────────────────────

    fun selectCategory(category: TrackingCategory) {
        _uiState.update { state ->
            when (state.activeSlot) {
                1 -> when {
                    state.selectedCategory1?.id == category.id -> {
                        // Deselect slot 1 - promote slot 2 to slot 1
                        val newCat1 = state.selectedCategory2
                        val newType = when {
                            newCat1 == null -> ChartType.TRENDS
                            newCat1.isNumeric -> ChartType.TIME_SCATTER
                            else -> ChartType.TRENDS
                        }
                        state.copy(
                            selectedCategory1 = newCat1,
                            selectedCategory2 = null,
                            chartType = newType,
                            chartData = StatsChartData.Empty
                        )
                    }
                    else -> {
                        // Fill/replace slot 1 with this category
                        val cat2 = state.selectedCategory2
                        val newType = when {
                            cat2 != null -> recalcChartTypeForPair(category, cat2, state.chartType)
                            state.rememberedChartType != null &&
                            isValidChartType(state.rememberedChartType, category, null) ->
                                state.rememberedChartType
                            category.isNumeric -> ChartType.TIME_SCATTER
                            else -> ChartType.TRENDS
                        }
                        state.copy(
                            selectedCategory1 = category,
                            chartType = newType,
                            chartData = StatsChartData.Empty
                        )
                    }
                }
                2 -> when {
                    state.selectedCategory1 == null -> {
                        // Can't have slot 2 without slot 1 - fill slot 1 instead
                        val remembered = state.rememberedChartType
                        val defaultType = if (category.isNumeric) ChartType.TIME_SCATTER else ChartType.TRENDS
                        val newType = if (remembered != null && isValidChartType(remembered, category, null)) remembered else defaultType
                        state.copy(
                            selectedCategory1 = category,
                            chartType = newType,
                            chartData = StatsChartData.Empty
                        )
                    }
                    state.selectedCategory2?.id == category.id -> {
                        // Deselect slot 2
                        val cat1 = state.selectedCategory1!!
                        val newType = when {
                            state.chartType == ChartType.COMBO ||
                            state.chartType == ChartType.DUAL_TIME_SERIES ||
                            state.chartType == ChartType.SCATTER ->
                                if (cat1.isNumeric) ChartType.TIME_SCATTER else ChartType.TRENDS
                            else -> state.chartType
                        }
                        state.copy(
                            selectedCategory2 = null,
                            chartType = newType,
                            chartData = StatsChartData.Empty
                        )
                    }
                    else -> {
                        // Fill/replace slot 2 with this category
                        val cat1 = state.selectedCategory1!!
                        val newType = recalcChartTypeForPair(cat1, category, state.chartType)
                        state.copy(
                            selectedCategory2 = category,
                            chartType = newType,
                            chartData = StatsChartData.Empty
                        )
                    }
                }
                else -> state
            }
        }
        persistCategorySelections()
        reloadChart()
        refreshPinnedState()
    }

    private fun persistCategorySelections() {
        preferencesStore?.let { store ->
            val state = _uiState.value
            viewModelScope.launch {
                store.setStatsCategory1Id(state.selectedCategory1?.id ?: -1L)
                store.setStatsCategory2Id(state.selectedCategory2?.id ?: -1L)
            }
        }
    }

    fun clearSelections() {
        _uiState.update {
            it.copy(
                selectedCategory1 = null,
                selectedCategory2 = null,
                chartType = ChartType.TRENDS,
                chartData = StatsChartData.Empty,
                activeSlot = 1,
            )
        }
    }

    fun setActiveSlot(slot: Int) {
        _uiState.update { it.copy(activeSlot = slot) }
    }

    fun swapCategories() {
        _uiState.update { state ->
            val cat1 = state.selectedCategory1 ?: return@update state
            val cat2 = state.selectedCategory2 ?: return@update state
            val newType = recalcChartTypeForPair(cat2, cat1, state.chartType)
            state.copy(
                selectedCategory1 = cat2,
                selectedCategory2 = cat1,
                chartType = newType,
            )
        }
        reloadChart()
    }

  private fun recalcChartTypeForPair(
        cat1: TrackingCategory,
        cat2: TrackingCategory?,
        currentType: ChartType
    ): ChartType {
        val hiddenForTwoCats = cat2 != null &&
            (currentType == ChartType.TRENDS || currentType == ChartType.TIME_SERIES)
            
        if (!hiddenForTwoCats && isValidChartType(currentType, cat1, cat2)) return currentType
        
        val newChartType = when {
            cat1.isNumeric && cat2?.isNumeric == true -> ChartType.SCATTER
            cat2 != null -> ChartType.DUAL_TIME_SERIES
            cat1.isNumeric -> ChartType.TIME_SCATTER
            else -> ChartType.TRENDS
        } // <-- The missing brace!
        
        preferencesStore?.let { store ->
            viewModelScope.launch {
                store.setStatsCategory1Id(-1L)
                store.setStatsCategory2Id(-1L)
                store.setStatsChartType("")
            }
        }
        
        return newChartType
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setStatsTimeRange(range.toPrefsString()) }
        }
        reloadChart()
        refreshPinnedState()
    }

    fun setChartType(type: ChartType) {
        _uiState.update { it.copy(chartType = type, rememberedChartType = type) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setStatsChartType(type.name) }
        }
        reloadChart()
        refreshPinnedState()
    }

    fun setZoomLevel(level: Int) {
        _uiState.update { it.copy(zoomLevel = level.coerceIn(0, 2)) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setStatsZoomLevel(level.coerceIn(0, 2)) }
        }
    }

    fun toggleDashboard() {
        viewModelScope.launch {
            preferencesStore?.setDashboardEnabled(!_uiState.value.dashboardEnabled)
        }
    }

    private fun computePinHash(
        cat1Id: Long,
        cat2Id: Long?,
        chartType: ChartType,
        timeRangeStr: String,
    ): String = "${cat1Id}_${cat2Id ?: ""}_${chartType.name}_$timeRangeStr"

    private fun currentTimeRangeStr(timeRange: TimeRange): String = when (val tr = timeRange) {
        is TimeRange.AllTime -> "ALL_TIME"
        is TimeRange.YearToDate -> "YTD"
        is TimeRange.CalendarYear -> "YEAR:${tr.year}"
        is TimeRange.SpecificMonth -> "MONTH"
    }

    private fun refreshPinnedState() {
        viewModelScope.launch {
            val state = _uiState.value
            val cat1 = state.selectedCategory1
            if (cat1 == null) {
                _uiState.update { it.copy(isCurrentViewPinned = false) }
                return@launch
            }
            val cat2Id = state.selectedCategory2?.id
            val timeRangeStr = currentTimeRangeStr(state.timeRange)
            val hash = computePinHash(cat1.id, cat2Id, state.chartType, timeRangeStr)
            val existing = loadPins()
            val isPinned = existing.any { it.id == hash }
            _uiState.update { it.copy(isCurrentViewPinned = isPinned) }
        }
    }

    fun pinCurrentView() {
        val state = _uiState.value
        val cat1 = state.selectedCategory1 ?: return
        viewModelScope.launch {
            val cat2Id = state.selectedCategory2?.id
            val timeRangeStr = currentTimeRangeStr(state.timeRange)
            val existing = loadPins()
            val isDuplicate = existing.any { pin ->
                pin.categoryId1 == cat1.id &&
                pin.categoryId2 == cat2Id &&
                pin.chartType == state.chartType.name &&
                pin.timeRangeType == timeRangeStr
            }
            if (isDuplicate) {
                _uiState.update { it.copy(pinResult = PinResult.DUPLICATE, isCurrentViewPinned = true) }
                return@launch
            }
            val label = buildString {
                append(cat1.name)
                state.selectedCategory2?.let { append(" vs ${it.name}") }
            }
            val comboHash = computePinHash(cat1.id, cat2Id, state.chartType, timeRangeStr)
            val pin = PinnedStat(
                id = comboHash,
                label = label,
                categoryId1 = cat1.id,
                categoryId2 = cat2Id,
                timeRangeType = timeRangeStr,
                chartType = state.chartType.name
            )
            savePins(existing + pin)
            _uiState.update { it.copy(pinResult = PinResult.ADDED, isCurrentViewPinned = true) }
        }
    }

    fun unpinCurrentView() {
        val state = _uiState.value
        val cat1 = state.selectedCategory1 ?: return
        viewModelScope.launch {
            val cat2Id = state.selectedCategory2?.id
            val timeRangeStr = currentTimeRangeStr(state.timeRange)
            val hash = computePinHash(cat1.id, cat2Id, state.chartType, timeRangeStr)
            val existing = loadPins()
            val updated = existing.filter { it.id != hash }
            savePins(updated)
            _uiState.update { it.copy(isCurrentViewPinned = false) }
        }
    }

    fun clearPinResult() {
        _uiState.update { it.copy(pinResult = null) }
    }

    private suspend fun loadPins(): List<PinnedStat> {
        val json = preferencesStore?.preferences?.let { flow ->
            flow.first().pinnedStats
        } ?: return emptyList()
        return parsePins(json)
    }

    private suspend fun savePins(pins: List<PinnedStat>) {
        preferencesStore?.setPinnedStats(encodePins(pins))
    }

    private fun isValidChartType(type: ChartType, cat1: TrackingCategory, cat2: TrackingCategory?): Boolean {
        return when (type) {
            ChartType.SCATTER -> cat1.isNumeric && cat2?.isNumeric == true
            ChartType.DUAL_TIME_SERIES -> cat2 != null
            ChartType.COMBO -> cat2 != null && !cat1.isNumeric && cat2.let { !it.isNumeric }
            ChartType.NUMERIC_AVERAGE, ChartType.NUMERIC_DISTRIBUTION, ChartType.TIME_SCATTER -> cat1.isNumeric
            ChartType.TRENDS -> !cat1.isNumeric
            ChartType.PHASE_SUMMARY -> !cat1.isNumeric && cat2 == null
            ChartType.PIE, ChartType.TIME_SERIES -> true
        }
    }

    // ── Chart data computation ────────────────────────────────────────────────

    fun reloadChart() {
        val state = _uiState.value
        val cat1 = state.selectedCategory1 ?: run {
            _uiState.update { it.copy(chartData = StatsChartData.Empty) }
            return
        }
        _uiState.update { it.copy(chartData = StatsChartData.Loading) }

        viewModelScope.launch {
            val (start, end) = resolveRange(state.timeRange)
            val cat2 = state.selectedCategory2

            val newData: StatsChartData = when (state.chartType) {

                ChartType.PIE -> {
                    val counts = repository.getValueCountsForCategory(cat1.id, start, end)
                    if (counts.isEmpty()) {
                        StatsChartData.Empty
                    } else {
                        val total = counts.sumOf { it.count }.coerceAtLeast(1)
                        val valueOrders = if (cat1.categoryType == "default") {
                            repository.getValuesForCategoryOnce(cat1.id)
                                .associate { it.label to it.displayOrder }
                        } else emptyMap()
                        StatsChartData.PieData(
                            slices = counts.map { PieSlice(it.valueLabel, it.count, it.count.toFloat() / total) },
                            colorToken = if (cat1.categoryType == "default") cat1.colorToken else "",
                            valueOrders = valueOrders,
                        )
                    }
                }

                ChartType.TIME_SERIES -> {
                    val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)
                    val countMap: Map<Long, Int> = if (cat1.categoryType == "increment") {
                        logs.associate { log ->
                            log.id to (repository.getValuesForLog(log.id).firstOrNull()?.toIntOrNull() ?: 1)
                        }
                    } else emptyMap()
                    val buckets = groupByTimeBucket(logs, start, end, state.timeRange, countMap)
                    if (buckets.isEmpty()) StatsChartData.Empty
                    else StatsChartData.TimeSeriesData(buckets, cat1.name)
                }

                ChartType.COMBO -> {
                    if (cat2 == null) {
                        StatsChartData.Empty
                    } else {
                        val logs1 = repository.getLogsForCategoryInRange(cat1.id, start, end)
                        val logs2 = repository.getLogsForCategoryInRange(cat2.id, start, end)
                        val combos = buildComboCounts(logs1, logs2)
                        if (combos.isEmpty()) StatsChartData.Empty
                        else StatsChartData.ComboData(combos)
                    }
                }

                ChartType.DUAL_TIME_SERIES -> {
                    if (cat2 == null) {
                        StatsChartData.Empty
                    } else {
                        val logs1 = repository.getLogsForCategoryInRange(cat1.id, start, end)
                        val logs2 = repository.getLogsForCategoryInRange(cat2.id, start, end)
                        val buckets = buildDualBuckets(logs1, logs2, start, end, state.timeRange)
                        if (buckets.isEmpty()) StatsChartData.Empty
                        else StatsChartData.DualTimeSeriesData(buckets, cat1.name, cat2.name, cat1.colorToken, cat2.colorToken)
                    }
                }

                ChartType.NUMERIC_AVERAGE -> {
                    val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)
                    // Pair each log with its parsed float value
                    val logValues = logs.mapNotNull { log ->
                        repository.getValuesForLog(log.id).firstOrNull()?.toFloatOrNull()
                            ?.let { log to it }
                    }
                    if (logValues.isEmpty()) {
                        StatsChartData.Empty
                    } else {
                        val buckets = buildNumericAverageBuckets(logValues, start, end, state.timeRange)
                        val allVals = logValues.map { it.second }
                        StatsChartData.NumericAverageData(
                            buckets      = buckets,
                            categoryName = cat1.name,
                            globalMin    = allVals.min(),
                            globalMax    = allVals.max(),
                        )
                    }
                }

                ChartType.NUMERIC_DISTRIBUTION -> {
                    val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)
                    val counts = mutableMapOf<String, Int>()
                    for (log in logs) {
                        val label = repository.getValuesForLog(log.id).firstOrNull() ?: continue
                        counts[label] = (counts[label] ?: 0) + 1
                    }
                    if (counts.isEmpty()) {
                        StatsChartData.Empty
                    } else {
                        // Map whole-number values to their optional scale label for readability.
                        val scaleMap = cat1.scaleLabels.decodeScaleLabels()
                        val bars = counts.entries
                            .sortedBy { it.key.toFloatOrNull() ?: Float.MAX_VALUE }
                            .map { entry ->
                                val label = entry.key.toIntOrNull()?.let { scaleMap[it] }
                                NumericHistBar(label ?: entry.key, entry.value)
                            }
                        StatsChartData.NumericDistributionData(bars, cat1.name)
                    }
                }

                ChartType.SCATTER -> {
                    if (cat2 == null || !cat1.isNumeric || !cat2.isNumeric) {
                        StatsChartData.Empty
                    } else {
                        val logs1 = repository.getLogsForCategoryInRange(cat1.id, start, end)
                        val logs2 = repository.getLogsForCategoryInRange(cat2.id, start, end)
                        val byDate1 = logs1.associateBy { it.date }
                        val byDate2 = logs2.associateBy { it.date }
                        val points = mutableListOf<ScatterPoint>()
                        for (date in (byDate1.keys intersect byDate2.keys)) {
                            val x = repository.getValuesForLog(byDate1[date]!!.id).firstOrNull()?.toFloatOrNull() ?: continue
                            val y = repository.getValuesForLog(byDate2[date]!!.id).firstOrNull()?.toFloatOrNull() ?: continue
                            points.add(ScatterPoint(x, y))
                        }
                        if (points.isEmpty()) StatsChartData.Empty
                        else StatsChartData.ScatterData(
                            points    = points,
                            xAxisName = cat1.name,
                            yAxisName = cat2.name,
                            xMin = 0f,
                            xMax = points.maxOf { it.x },
                            yMin = 0f,
                            yMax = points.maxOf { it.y },
                        )
                    }
                }

                ChartType.TIME_SCATTER -> {
                    val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)
                    val points = mutableListOf<TimeScatterPoint>()
                    for (log in logs) {
                        val date = LocalDate.parse(log.date)
                        val value = repository.getValuesForLog(log.id).firstOrNull()?.toFloatOrNull() ?: continue
                        val offset = ChronoUnit.DAYS.between(start, date).toInt()
                        val label = date.format(DateTimeFormatter.ofPattern("d MMM"))
                        points.add(TimeScatterPoint(dayOffset = offset, dateLabel = label, value = value))
                    }
                    if (points.isEmpty()) StatsChartData.Empty
                    else StatsChartData.TimeScatterData(
                        points = points,
                        yAxisName = cat1.name,
                        yMin = 0f,
                        yMax = points.maxOf { it.value },
                    )
                }

                ChartType.TRENDS -> {
                    val counts = repository.getValueCountsForCategory(cat1.id, start, end)
                    if (counts.isEmpty()) StatsChartData.Empty
                    else {
                        val total = counts.sumOf { it.count }.coerceAtLeast(1)
                        val valueOrders = if (cat1.categoryType == "default") {
                            repository.getValuesForCategoryOnce(cat1.id)
                                .associate { it.label to it.displayOrder }
                        } else emptyMap()
                        val bars = counts.sortedByDescending { it.count }.take(10).map { vc ->
                            TrendsBar(
                                label = vc.valueLabel,
                                count = vc.count,
                                percentage = (vc.count * 100 / total).coerceAtMost(100)
                            )
                        }
                        StatsChartData.TrendsData(
                            bars = bars,
                            categoryName = cat1.name,
                            colorToken = if (cat1.categoryType == "default") cat1.colorToken else "",
                            valueOrders = valueOrders,
                        )
                    }
                }

                ChartType.PHASE_SUMMARY -> {
                    val repo = periodRepository
                    if (repo == null) {
                        StatsChartData.Empty
                    } else {
                        val periods = repo.getAllPeriods().first()
                        val avgCycle = com.mapgie.goflo.data.repository.PeriodRepository
                            .calculateAvgCycleLength(periods)
                        val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)

                        val phaseAccum = mutableMapOf<String, MutableList<String>>()
                        for (log in logs) {
                            val logDate = LocalDate.parse(log.date)
                            val lastStart = periods
                                .filter { !LocalDate.parse(it.startDate).isAfter(logDate) }
                                .maxByOrNull { it.startDate }
                                ?.startDate ?: continue
                            val cycleDay = (ChronoUnit.DAYS.between(
                                LocalDate.parse(lastStart), logDate
                            ).toInt() + 1).takeIf { it >= 1 } ?: continue
                            val phase = com.mapgie.goflo.data.repository.PeriodRepository
                                .cyclePhaseLabel(cycleDay, avgCycle)
                            val values = repository.getValuesForLog(log.id)
                                .ifEmpty { listOf("(logged)") }
                            phaseAccum.getOrPut(phase) { mutableListOf() }.addAll(values)
                        }

                        val phaseOrder = listOf("Menstrual", "Follicular", "Ovulatory", "Luteal")
                        val rows = phaseOrder.mapNotNull { phase ->
                            val vals = phaseAccum[phase] ?: return@mapNotNull null
                            val top = vals.groupingBy { it }.eachCount()
                                .entries.sortedByDescending { it.value }
                                .take(3).map { it.key }
                            PhaseSummaryRow(phase = phase, logCount = vals.size, topValues = top)
                        }

                        if (rows.isEmpty()) StatsChartData.Empty
                        else StatsChartData.PhaseSummaryData(rows, cat1.name)
                    }
                }
            }

            _uiState.update { it.copy(chartData = newData) }
        }
    }

    // ── Date range resolution ─────────────────────────────────────────────────

    private suspend fun resolveRange(range: TimeRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (range) {
            TimeRange.AllTime -> {
                val earliest = repository.getEarliestLogDate() ?: today
                earliest to today
            }
            TimeRange.YearToDate -> LocalDate.of(today.year, 1, 1) to today
            is TimeRange.CalendarYear ->
                LocalDate.of(range.year, 1, 1) to LocalDate.of(range.year, 12, 31)
            is TimeRange.SpecificMonth ->
                range.yearMonth.atDay(1) to range.yearMonth.atEndOfMonth()
        }
    }

    // ── Time bucket helpers ───────────────────────────────────────────────────

    /**
     * Groups [logs] into time buckets. Uses daily buckets for SpecificMonth,
     * weekly buckets for ranges <= 90 days, monthly buckets otherwise.
     */
    private fun groupByTimeBucket(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate,
        timeRange: TimeRange = TimeRange.AllTime,
        incrementCountMap: Map<Long, Int> = emptyMap(),
    ): List<TimeBucket> {
        if (logs.isEmpty()) return emptyList()
        return when {
            timeRange is TimeRange.SpecificMonth -> groupByDay(logs, start, end, incrementCountMap)
            ChronoUnit.DAYS.between(start, end) <= 90 -> groupByWeek(logs, start, end, incrementCountMap)
            else -> groupByMonth(logs, start, end, incrementCountMap)
        }
    }

    private fun groupByDay(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate,
        incrementCountMap: Map<Long, Int> = emptyMap(),
    ): List<TimeBucket> {
        val days = mutableListOf<LocalDate>()
        var d = start
        while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
        val logsByDay = logs.groupBy { LocalDate.parse(it.date) }
        val shortFmt = DateTimeFormatter.ofPattern("d MMM")
        return days.map { day ->
            val dayLogs = logsByDay[day] ?: emptyList()
            val count = if (incrementCountMap.isNotEmpty())
                dayLogs.sumOf { incrementCountMap[it.id] ?: 1 }
            else dayLogs.size
            TimeBucket(label = day.format(shortFmt), count = count)
        }
    }

    private fun groupByMonth(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate,
        incrementCountMap: Map<Long, Int> = emptyMap(),
    ): List<TimeBucket> {
        val months = mutableListOf<YearMonth>()
        var current = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!current.isAfter(endMonth)) {
            months.add(current)
            current = current.plusMonths(1)
        }

        val logsByMonth = logs.groupBy { log ->
            YearMonth.from(LocalDate.parse(log.date))
        }

        val shortFmt = DateTimeFormatter.ofPattern("MMM yy")
        return months.map { ym ->
            val monthLogs = logsByMonth[ym] ?: emptyList()
            val count = if (incrementCountMap.isNotEmpty())
                monthLogs.sumOf { incrementCountMap[it.id] ?: 1 }
            else monthLogs.size
            TimeBucket(label = ym.format(shortFmt), count = count)
        }
    }

    private fun groupByWeek(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate,
        incrementCountMap: Map<Long, Int> = emptyMap(),
    ): List<TimeBucket> {
        val weekFields = WeekFields.of(Locale.getDefault())

        val weeks = mutableListOf<LocalDate>()
        var weekStart = start.with(weekFields.dayOfWeek(), 1)
        val lastWeekStart = end.with(weekFields.dayOfWeek(), 1)
        while (!weekStart.isAfter(lastWeekStart)) {
            weeks.add(weekStart)
            weekStart = weekStart.plusWeeks(1)
        }

        val logsByWeek = logs.groupBy { log ->
            val date = LocalDate.parse(log.date)
            date.with(weekFields.dayOfWeek(), 1)
        }

        return weeks.map { ws ->
            val label = "W${ws.get(weekFields.weekOfWeekBasedYear())} '${ws.format(DateTimeFormatter.ofPattern("yy"))}"
            val weekLogs = logsByWeek[ws] ?: emptyList()
            val count = if (incrementCountMap.isNotEmpty())
                weekLogs.sumOf { incrementCountMap[it.id] ?: 1 }
            else weekLogs.size
            TimeBucket(label = label, count = count)
        }
    }

    // ── Numeric average bucket helpers ───────────────────────────────────────

    /**
     * Groups [logValues] (log + parsed float) into time buckets and computes the
     * average numeric value per bucket.
     */
    private fun buildNumericAverageBuckets(
        logValues: List<Pair<TrackingLog, Float>>,
        start: LocalDate,
        end: LocalDate,
        timeRange: TimeRange = TimeRange.AllTime
    ): List<NumericBucket> {
        if (logValues.isEmpty()) return emptyList()
        return when {
            timeRange is TimeRange.SpecificMonth -> buildNumericDayBuckets(logValues, start, end)
            ChronoUnit.DAYS.between(start, end) <= 90 -> buildNumericWeekBuckets(logValues, start, end)
            else -> buildNumericMonthBuckets(logValues, start, end)
        }
    }

    private fun buildNumericDayBuckets(
        logValues: List<Pair<TrackingLog, Float>>,
        start: LocalDate,
        end: LocalDate
    ): List<NumericBucket> {
        val days = mutableListOf<LocalDate>()
        var d = start
        while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
        val byDay = logValues.groupBy { (log, _) -> LocalDate.parse(log.date) }
        val shortFmt = DateTimeFormatter.ofPattern("d MMM")
        return days.mapNotNull { day ->
            val entries = byDay[day] ?: return@mapNotNull null
            val avg = entries.map { it.second }.average().toFloat()
            NumericBucket(day.format(shortFmt), avg, entries.size)
        }
    }

    private fun buildNumericWeekBuckets(
        logValues: List<Pair<TrackingLog, Float>>,
        start: LocalDate,
        end: LocalDate
    ): List<NumericBucket> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val weeks = mutableListOf<LocalDate>()
        var ws = start.with(weekFields.dayOfWeek(), 1)
        val lastWs = end.with(weekFields.dayOfWeek(), 1)
        while (!ws.isAfter(lastWs)) { weeks.add(ws); ws = ws.plusWeeks(1) }

        val byWeek = logValues.groupBy { (log, _) ->
            LocalDate.parse(log.date).with(weekFields.dayOfWeek(), 1)
        }
        return weeks.mapNotNull { weekStart ->
            val entries = byWeek[weekStart] ?: return@mapNotNull null
            val avg = entries.map { it.second }.average().toFloat()
            val label = "W${weekStart.get(weekFields.weekOfWeekBasedYear())} '${weekStart.format(DateTimeFormatter.ofPattern("yy"))}"
            NumericBucket(label, avg, entries.size)
        }
    }

    private fun buildNumericMonthBuckets(
        logValues: List<Pair<TrackingLog, Float>>,
        start: LocalDate,
        end: LocalDate
    ): List<NumericBucket> {
        val months = mutableListOf<YearMonth>()
        var m = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!m.isAfter(endMonth)) { months.add(m); m = m.plusMonths(1) }

        val byMonth = logValues.groupBy { (log, _) ->
            YearMonth.from(LocalDate.parse(log.date))
        }
        val fmt = DateTimeFormatter.ofPattern("MMM yy")
        return months.mapNotNull { ym ->
            val entries = byMonth[ym] ?: return@mapNotNull null
            val avg = entries.map { it.second }.average().toFloat()
            NumericBucket(ym.format(fmt), avg, entries.size)
        }
    }

    // ── Combo count helpers ───────────────────────────────────────────────────

    private suspend fun buildComboCounts(
        logs1: List<TrackingLog>,
        logs2: List<TrackingLog>
    ): List<ComboBar> {
        val byDate1 = logs1.associateBy { it.date }
        val byDate2 = logs2.associateBy { it.date }

        val sharedDates = byDate1.keys intersect byDate2.keys
        val comboCounts = mutableMapOf<String, Int>()

        for (date in sharedDates) {
            val log1 = byDate1[date] ?: continue
            val log2 = byDate2[date] ?: continue
            val values1 = repository.getValuesForLog(log1.id).ifEmpty { listOf("(no value)") }
            val values2 = repository.getValuesForLog(log2.id).ifEmpty { listOf("(no value)") }
            for (v1 in values1) {
                for (v2 in values2) {
                    val key = "$v1 + $v2"
                    comboCounts[key] = (comboCounts[key] ?: 0) + 1
                }
            }
        }

        return comboCounts.entries
            .sortedByDescending { it.value }
            .map { ComboBar(it.key, it.value) }
    }

    // ── Dual time series helpers ──────────────────────────────────────────────

    private fun buildDualBuckets(
        logs1: List<TrackingLog>,
        logs2: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate,
        timeRange: TimeRange = TimeRange.AllTime
    ): List<DualBucket> {
        return when {
            timeRange is TimeRange.SpecificMonth -> buildDualDayBuckets(logs1, logs2, start, end)
            ChronoUnit.DAYS.between(start, end) <= 90 -> buildDualWeekBuckets(logs1, logs2, start, end)
            else -> buildDualMonthBuckets(logs1, logs2, start, end)
        }
    }

    private fun buildDualDayBuckets(
        logs1: List<TrackingLog>,
        logs2: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<DualBucket> {
        val days = mutableListOf<LocalDate>()
        var d = start
        while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
        val logs1ByDay = logs1.groupBy { LocalDate.parse(it.date) }
        val logs2ByDay = logs2.groupBy { LocalDate.parse(it.date) }
        val shortFmt = DateTimeFormatter.ofPattern("d MMM")
        return days.map { day ->
            DualBucket(
                label = day.format(shortFmt),
                count1 = logs1ByDay[day]?.size ?: 0,
                count2 = logs2ByDay[day]?.size ?: 0
            )
        }
    }

    private fun buildDualWeekBuckets(
        logs1: List<TrackingLog>,
        logs2: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<DualBucket> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val weeks = mutableListOf<LocalDate>()
        var ws = start.with(weekFields.dayOfWeek(), 1)
        val lastWs = end.with(weekFields.dayOfWeek(), 1)
        while (!ws.isAfter(lastWs)) { weeks.add(ws); ws = ws.plusWeeks(1) }

        val logs1ByWeek = logs1.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }
        val logs2ByWeek = logs2.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }

        return weeks.map { weekStart ->
            val label = "W${weekStart.get(weekFields.weekOfWeekBasedYear())} '${weekStart.format(DateTimeFormatter.ofPattern("yy"))}"
            DualBucket(
                label = label,
                count1 = logs1ByWeek[weekStart]?.size ?: 0,
                count2 = logs2ByWeek[weekStart]?.size ?: 0
            )
        }
    }

    private fun buildDualMonthBuckets(
        logs1: List<TrackingLog>,
        logs2: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<DualBucket> {
        val months = mutableListOf<YearMonth>()
        var m = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!m.isAfter(endMonth)) { months.add(m); m = m.plusMonths(1) }

        val logs1ByMonth = logs1.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
        val logs2ByMonth = logs2.groupBy { YearMonth.from(LocalDate.parse(it.date)) }

        val shortFmt = DateTimeFormatter.ofPattern("MMM yy")
        return months.map { ym ->
            DualBucket(
                label = ym.format(shortFmt),
                count1 = logs1ByMonth[ym]?.size ?: 0,
                count2 = logs2ByMonth[ym]?.size ?: 0
            )
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val repository: TrackingRepository,
        private val preferencesStore: AppPreferencesStore? = null,
        private val periodRepository: com.mapgie.goflo.data.repository.PeriodRepository? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, preferencesStore, periodRepository) as T
        }
    }
}

// ── Prefs serialisation helpers (package-level) ───────────────────────────────

fun TimeRange.toPrefsString(): String = when (this) {
    is TimeRange.AllTime       -> "ALL_TIME"
    is TimeRange.YearToDate    -> "YTD"
    is TimeRange.CalendarYear  -> "YEAR:${year}"
    is TimeRange.SpecificMonth -> "MONTH:${yearMonth}"
}

fun parseTimeRangeString(s: String): TimeRange = runCatching {
    when {
        s == "ALL_TIME" -> TimeRange.AllTime
        s == "YTD"      -> TimeRange.YearToDate
        s == "MONTH"    -> TimeRange.SpecificMonth(YearMonth.now())
        s.startsWith("YEAR:")  -> TimeRange.CalendarYear(s.removePrefix("YEAR:").toInt())
        s.startsWith("MONTH:") -> TimeRange.SpecificMonth(YearMonth.parse(s.removePrefix("MONTH:")))
        else -> TimeRange.YearToDate
    }
}.getOrDefault(TimeRange.YearToDate)

fun parseChartTypeString(s: String): ChartType =
    runCatching { ChartType.valueOf(s) }.getOrDefault(ChartType.PIE)

// ── Pin helpers (package-level) ───────────────────────────────────────────────

fun parsePins(json: String): List<PinnedStat> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            PinnedStat(
                id = obj.getString("id"),
                label = obj.getString("label"),
                categoryId1 = obj.getLong("categoryId1"),
                categoryId2 = obj.optLong("categoryId2", -1L).takeIf { it != -1L },
                timeRangeType = obj.getString("timeRangeType"),
                chartType = obj.getString("chartType"),
            )
        }
    }.getOrDefault(emptyList())
}

fun encodePins(pins: List<PinnedStat>): String {
    val arr = org.json.JSONArray()
    pins.forEach { pin ->
        val obj = org.json.JSONObject()
        obj.put("id", pin.id)
        obj.put("label", pin.label)
        obj.put("categoryId1", pin.categoryId1)
        pin.categoryId2?.let { obj.put("categoryId2", it) }
        obj.put("timeRangeType", pin.timeRangeType)
        obj.put("chartType", pin.chartType)
        arr.put(obj)
    }
    return arr.toString()
}
