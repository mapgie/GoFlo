package com.mapgie.goflo.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.screens.stats.ChartType
import com.mapgie.goflo.ui.screens.stats.PinnedStat
import com.mapgie.goflo.ui.screens.stats.StatsChartData
import com.mapgie.goflo.ui.screens.stats.TimeRange
import com.mapgie.goflo.ui.screens.stats.computeChartData
import com.mapgie.goflo.ui.screens.stats.encodePins
import com.mapgie.goflo.ui.screens.stats.parsePins
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class PinnedChartItem(
    val pin: PinnedStat,
    val chartData: StatsChartData?,  // null while loading or on error
)

class DashboardViewModel(
    private val preferencesStore: AppPreferencesStore,
    private val repository: TrackingRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<PinnedChartItem>>(emptyList())
    val items: StateFlow<List<PinnedChartItem>> = _items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            preferencesStore.preferences
                .map { it.pinnedStats }
                .collect { json ->
                    val pins = parsePins(json)
                    // Initialise each item as loading (null chartData)
                    _items.value = pins.map { PinnedChartItem(pin = it, chartData = null) }
                    // Load chart data for each pin concurrently
                    pins.forEachIndexed { index, pin ->
                        launch {
                            val chartData = loadChartDataForPin(pin)
                            _items.value = _items.value.toMutableList().also { list ->
                                if (index < list.size && list[index].pin.id == pin.id) {
                                    list[index] = list[index].copy(chartData = chartData)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun removePin(pin: PinnedStat) {
        viewModelScope.launch {
            val json = preferencesStore.preferences.first().pinnedStats
            val updated = parsePins(json).filter { it.id != pin.id }
            preferencesStore.setPinnedStats(encodePins(updated))
        }
    }

    private suspend fun loadChartDataForPin(pin: PinnedStat): StatsChartData {
        return runCatching {
            val category1 = repository.getCategoryByIdOnce(pin.categoryId1)
                ?: return StatsChartData.Empty
            val category2 = pin.categoryId2?.let { repository.getCategoryByIdOnce(it) }

            val chartType = runCatching { ChartType.valueOf(pin.chartType) }.getOrNull()
                ?: return StatsChartData.Empty

            val (start, end) = resolveTimeRange(pin.timeRangeType)

            val timeRange = parseTimeRange(pin.timeRangeType)

            computeChartData(
                category1 = category1,
                category2 = category2,
                chartType = chartType,
                repository = repository,
                start = start,
                end = end,
                timeRange = timeRange,
            )
        }.getOrDefault(StatsChartData.Empty)
    }

    private suspend fun resolveTimeRange(timeRangeType: String): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when {
            timeRangeType == "ALL_TIME" -> (repository.getEarliestLogDate() ?: today) to today
            timeRangeType == "YTD" -> LocalDate.of(today.year, 1, 1) to today
            timeRangeType == "MONTH" -> {
                val ym = YearMonth.now()
                ym.atDay(1) to ym.atEndOfMonth()
            }
            timeRangeType.startsWith("YEAR:") -> {
                val year = timeRangeType.removePrefix("YEAR:").toIntOrNull() ?: today.year
                LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)
            }
            timeRangeType.startsWith("MONTH:") -> {
                val ymStr = timeRangeType.removePrefix("MONTH:")
                val ym = runCatching { YearMonth.parse(ymStr) }.getOrNull()
                    ?: YearMonth.now()
                ym.atDay(1) to ym.atEndOfMonth()
            }
            else -> LocalDate.of(2000, 1, 1) to today
        }
    }

    private fun parseTimeRange(timeRangeType: String): TimeRange {
        return when {
            timeRangeType == "ALL_TIME" -> TimeRange.AllTime
            timeRangeType == "YTD" -> TimeRange.YearToDate
            timeRangeType == "MONTH" -> TimeRange.SpecificMonth(YearMonth.now())
            timeRangeType.startsWith("YEAR:") -> {
                val year = timeRangeType.removePrefix("YEAR:").toIntOrNull()
                    ?: LocalDate.now().year
                TimeRange.CalendarYear(year)
            }
            timeRangeType.startsWith("MONTH:") -> {
                val ymStr = timeRangeType.removePrefix("MONTH:")
                val ym = runCatching { YearMonth.parse(ymStr) }.getOrNull()
                    ?: YearMonth.now()
                TimeRange.SpecificMonth(ym)
            }
            else -> TimeRange.AllTime
        }
    }

    class Factory(
        private val preferencesStore: AppPreferencesStore,
        private val repository: TrackingRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(preferencesStore, repository) as T
        }
    }
}
