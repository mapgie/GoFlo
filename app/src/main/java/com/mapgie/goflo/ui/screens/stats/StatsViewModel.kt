package com.mapgie.goflo.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    DUAL_TIME_SERIES
}

// ── Chart data models ─────────────────────────────────────────────────────────

data class PieSlice(val label: String, val count: Int, val fraction: Float)
data class TimeBucket(val label: String, val count: Int)
data class ComboBar(val label: String, val count: Int)
data class DualBucket(val label: String, val count1: Int, val count2: Int)

sealed class StatsChartData {
    data object Empty : StatsChartData()
    data object Loading : StatsChartData()
    data class PieData(val slices: List<PieSlice>) : StatsChartData()
    data class TimeSeriesData(val buckets: List<TimeBucket>, val categoryName: String) : StatsChartData()
    data class ComboData(val bars: List<ComboBar>) : StatsChartData()
    data class DualTimeSeriesData(
        val buckets: List<DualBucket>,
        val categoryName1: String,
        val categoryName2: String
    ) : StatsChartData()
}

// ── UI State ──────────────────────────────────────────────────────────────────

data class StatsUiState(
    val categories: List<TrackingCategory> = emptyList(),
    val selectedCategory1: TrackingCategory? = null,
    val selectedCategory2: TrackingCategory? = null,
    val timeRange: TimeRange = TimeRange.AllTime,
    val chartType: ChartType = ChartType.PIE,
    val chartData: StatsChartData = StatsChartData.Empty,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class StatsViewModel(private val repository: TrackingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    init {
        viewModelScope.launch {
            repository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    // ── Category selection ────────────────────────────────────────────────────

    fun selectCategory(category: TrackingCategory) {
        _uiState.update { state ->
            when {
                state.selectedCategory1?.id == category.id -> {
                    // Deselect cat1 — promote cat2 if present
                    val newCat1 = state.selectedCategory2
                    val newType = if (newCat1 == null) ChartType.PIE else state.chartType
                    state.copy(
                        selectedCategory1 = newCat1,
                        selectedCategory2 = null,
                        chartType = newType,
                        chartData = StatsChartData.Empty
                    )
                }
                state.selectedCategory2?.id == category.id -> {
                    // Deselect cat2 — reset chart type to single-cat option if needed
                    val newType = if (state.chartType == ChartType.COMBO ||
                        state.chartType == ChartType.DUAL_TIME_SERIES) ChartType.PIE
                    else state.chartType
                    state.copy(
                        selectedCategory2 = null,
                        chartType = newType,
                        chartData = StatsChartData.Empty
                    )
                }
                state.selectedCategory1 == null -> {
                    state.copy(selectedCategory1 = category, chartData = StatsChartData.Empty)
                }
                state.selectedCategory2 == null -> {
                    state.copy(selectedCategory2 = category, chartData = StatsChartData.Empty)
                }
                else -> state // Both slots full — ignore
            }
        }
        reloadChart()
    }

    fun clearSelections() {
        _uiState.update {
            it.copy(
                selectedCategory1 = null,
                selectedCategory2 = null,
                chartType = ChartType.PIE,
                chartData = StatsChartData.Empty
            )
        }
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        reloadChart()
    }

    fun setChartType(type: ChartType) {
        _uiState.update { it.copy(chartType = type) }
        reloadChart()
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
                        StatsChartData.PieData(
                            counts.map { PieSlice(it.valueLabel, it.count, it.count.toFloat() / total) }
                        )
                    }
                }

                ChartType.TIME_SERIES -> {
                    val logs = repository.getLogsForCategoryInRange(cat1.id, start, end)
                    val buckets = groupByTimeBucket(logs, start, end)
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
                        val buckets = buildDualBuckets(logs1, logs2, start, end)
                        if (buckets.isEmpty()) StatsChartData.Empty
                        else StatsChartData.DualTimeSeriesData(buckets, cat1.name, cat2.name)
                    }
                }
            }

            _uiState.update { it.copy(chartData = newData) }
        }
    }

    // ── Date range resolution ─────────────────────────────────────────────────

    private fun resolveRange(range: TimeRange): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (range) {
            TimeRange.AllTime -> LocalDate.of(2000, 1, 1) to today
            TimeRange.YearToDate -> LocalDate.of(today.year, 1, 1) to today
            is TimeRange.CalendarYear ->
                LocalDate.of(range.year, 1, 1) to LocalDate.of(range.year, 12, 31)
            is TimeRange.SpecificMonth ->
                range.yearMonth.atDay(1) to range.yearMonth.atEndOfMonth()
        }
    }

    // ── Time bucket helpers ───────────────────────────────────────────────────

    /**
     * Groups [logs] into time buckets. Uses weekly buckets for ranges ≤ 90 days,
     * monthly buckets otherwise.
     */
    private fun groupByTimeBucket(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<TimeBucket> {
        if (logs.isEmpty()) return emptyList()

        val daysBetween = ChronoUnit.DAYS.between(start, end)
        return if (daysBetween <= 90) {
            groupByWeek(logs, start, end)
        } else {
            groupByMonth(logs, start, end)
        }
    }

    private fun groupByMonth(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<TimeBucket> {
        // Build all months in the range
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
            TimeBucket(
                label = ym.format(shortFmt),
                count = logsByMonth[ym]?.size ?: 0
            )
        }.filter { bucket ->
            // Only keep trailing empty buckets if there's data elsewhere, trim leading blanks
            true // keep all — chart will show zeros as empty bars
        }
    }

    private fun groupByWeek(
        logs: List<TrackingLog>,
        start: LocalDate,
        end: LocalDate
    ): List<TimeBucket> {
        val weekFields = WeekFields.of(Locale.getDefault())

        // Enumerate weeks
        val weeks = mutableListOf<LocalDate>() // Monday of each week
        var weekStart = start.with(weekFields.dayOfWeek(), 1)
        val lastWeekStart = end.with(weekFields.dayOfWeek(), 1)
        while (!weekStart.isAfter(lastWeekStart)) {
            weeks.add(weekStart)
            weekStart = weekStart.plusWeeks(1)
        }

        val logsByWeek = logs.groupBy { log ->
            val date = LocalDate.parse(log.date)
            date.with(weekFields.dayOfWeek(), 1) // normalise to Monday
        }

        return weeks.map { ws ->
            val label = "W${ws.get(weekFields.weekOfWeekBasedYear())} '${ws.format(DateTimeFormatter.ofPattern("yy"))}"
            TimeBucket(label = label, count = logsByWeek[ws]?.size ?: 0)
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
        end: LocalDate
    ): List<DualBucket> {
        val daysBetween = ChronoUnit.DAYS.between(start, end)

        return if (daysBetween <= 90) {
            val combined = (logs1 + logs2).distinctBy { it.date + it.categoryId }
            val weekFields = WeekFields.of(Locale.getDefault())

            val weeks = mutableListOf<LocalDate>()
            var ws = start.with(weekFields.dayOfWeek(), 1)
            val lastWs = end.with(weekFields.dayOfWeek(), 1)
            while (!ws.isAfter(lastWs)) { weeks.add(ws); ws = ws.plusWeeks(1) }

            val logs1ByWeek = logs1.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }
            val logs2ByWeek = logs2.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }

            weeks.map { weekStart ->
                val label = "W${weekStart.get(weekFields.weekOfWeekBasedYear())} '${weekStart.format(DateTimeFormatter.ofPattern("yy"))}"
                DualBucket(
                    label = label,
                    count1 = logs1ByWeek[weekStart]?.size ?: 0,
                    count2 = logs2ByWeek[weekStart]?.size ?: 0
                )
            }
        } else {
            val months = mutableListOf<YearMonth>()
            var m = YearMonth.from(start)
            val endMonth = YearMonth.from(end)
            while (!m.isAfter(endMonth)) { months.add(m); m = m.plusMonths(1) }

            val logs1ByMonth = logs1.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
            val logs2ByMonth = logs2.groupBy { YearMonth.from(LocalDate.parse(it.date)) }

            val shortFmt = DateTimeFormatter.ofPattern("MMM yy")
            months.map { ym ->
                DualBucket(
                    label = ym.format(shortFmt),
                    count1 = logs1ByMonth[ym]?.size ?: 0,
                    count2 = logs2ByMonth[ym]?.size ?: 0
                )
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val repository: TrackingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
    }
}
