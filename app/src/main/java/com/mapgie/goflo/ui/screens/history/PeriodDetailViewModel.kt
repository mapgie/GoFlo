package com.mapgie.goflo.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.Group
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.screens.log.PeriodDaySync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One logged day of a period episode, resolved for display.
 *
 * [flowLabel] is always a word ("Medium"), never a number: numeric_slider flow
 * values are mapped back through [PeriodDaySync.flowLabelForSliderValue], and a
 * raw label that does not parse as a number (a legacy chip-mode value) is shown
 * as-is. Null when the day has no flow log.
 */
data class PeriodDayDetail(
    val date: LocalDate,
    /** 1-based day number within the episode (from its start date). */
    val dayNumber: Int,
    val flowLabel: String?,
    val symptoms: List<String>,
    /** Distinct categories (beyond flow and symptoms) logged on this day. */
    val otherLoggedCount: Int,
)

data class PeriodDetailUiState(
    val isLoading: Boolean = true,
    /** The episode no longer exists; the screen should pop back. */
    val notFound: Boolean = false,
    val period: PeriodEntry? = null,
    val startDate: LocalDate? = null,
    /** Explicit episode end; null while the episode is ongoing. */
    val endDate: LocalDate? = null,
    /** Whole-episode length in days (through today while ongoing). */
    val lengthDays: Int = 0,
    /** Days from this episode's start to the next episode's start, when plausible. */
    val cycleLengthDays: Int? = null,
    val days: List<PeriodDayDetail> = emptyList(),
    /** The Flow system category, for resolving the day rows' role colour. */
    val flowCategory: TrackingCategory? = null,
    val groups: List<Group> = emptyList(),
)

/**
 * ViewModel for [PeriodDetailScreen]: loads one episode, its period_days rows,
 * and the range's tracking logs (flow word, symptoms, and a count of other
 * logged categories per day) in a fixed number of queries.
 *
 * The episode row is observed reactively; day-level tracking logs are one-shot
 * reads, so the screen calls [refresh] when it re-enters composition after a
 * day was edited through the unified day screen.
 */
class PeriodDetailViewModel(
    private val periodId: Long,
    private val repository: PeriodRepository,
    private val trackingRepository: TrackingRepository,
    private val preferencesStore: AppPreferencesStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeriodDetailUiState())
    val uiState: StateFlow<PeriodDetailUiState> = _uiState.asStateFlow()

    /** Bumped by [refresh] to re-run the day-level loads. */
    private val refreshTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(repository.getPeriodById(periodId), refreshTick) { period, _ -> period }
                .collect { period ->
                    if (period == null) {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                    } else {
                        runCatching { loadDetails(period) }
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    /** Reloads the day list; called when the screen returns from a day or the editor. */
    fun refresh() = refreshTick.update { it + 1 }

    private suspend fun loadDetails(period: PeriodEntry) {
        val tolerance = preferencesStore?.preferences?.first()?.periodGapToleranceDays
            ?: PeriodRepository.DEFAULT_GAP_TOLERANCE_DAYS
        val start = LocalDate.parse(period.startDate)
        val end = period.endDate?.let { LocalDate.parse(it) }

        // The episode's logged days (period_days is the per-day source of
        // truth). Defensive: an episode should always have at least its start
        // day logged, but fall back to it rather than rendering nothing.
        val dayDates = repository.getDaysForEpisode(period, tolerance)
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sorted()
            .ifEmpty { listOf(start) }

        // One range query for the logs, one for their values (exportTrackingLogs
        // batches both), then group by date in memory.
        val categories = trackingRepository.getAllCategoriesOnce()
        val flowCat = categories.firstOrNull { it.systemKey == "flow" }
        val symptomsCat = categories.firstOrNull { it.systemKey == "symptoms" }
        val logsByDate = trackingRepository
            .exportTrackingLogs(categories.map { it.id }, start, dayDates.last())
            .groupBy { it.log.date }
        val groups = trackingRepository.getAllGroupsOnce()

        val days = dayDates.map { date ->
            val dayLogs = logsByDate[date.toString()].orEmpty()
            val flowRaw = dayLogs
                .firstOrNull { it.log.categoryId == flowCat?.id }
                ?.values?.firstOrNull()
            val flowLabel = flowRaw?.let { raw ->
                if (flowCat?.categoryType == "numeric_slider") {
                    // Slider mode stores the numeric step; a value that does not
                    // parse is a legacy chip-mode label and stands on its own.
                    raw.toFloatOrNull()
                        ?.let { PeriodDaySync.flowLabelForSliderValue(it.toInt()) }
                        ?: raw
                } else raw
            }
            val symptoms = dayLogs
                .firstOrNull { it.log.categoryId == symptomsCat?.id }
                ?.values.orEmpty()
            val otherCount = dayLogs
                .filter { it.log.categoryId != flowCat?.id && it.log.categoryId != symptomsCat?.id }
                .distinctBy { it.log.categoryId }
                .count()
            PeriodDayDetail(
                date = date,
                dayNumber = ChronoUnit.DAYS.between(start, date).toInt() + 1,
                flowLabel = flowLabel,
                symptoms = symptoms,
                otherLoggedCount = otherCount,
            )
        }

        // Cycle context: days to the next episode's start, same plausibility
        // bounds as the History list.
        val allPeriods = repository.getAllPeriodsOnce().sortedBy { it.startDate }
        val nextStart = allPeriods
            .firstOrNull { LocalDate.parse(it.startDate).isAfter(start) }
            ?.let { LocalDate.parse(it.startDate) }
        val cycleLength = nextStart
            ?.let { ChronoUnit.DAYS.between(start, it).toInt() }
            ?.takeIf { it in 15..60 }

        val lengthEnd = end ?: maxOf(LocalDate.now(), dayDates.last())
        val lengthDays = (ChronoUnit.DAYS.between(start, lengthEnd).toInt() + 1)
            .coerceAtLeast(1)

        _uiState.update {
            it.copy(
                isLoading = false,
                notFound = false,
                period = period,
                startDate = start,
                endDate = end,
                lengthDays = lengthDays,
                cycleLengthDays = cycleLength,
                days = days,
                flowCategory = flowCat,
                groups = groups,
            )
        }
    }

    class Factory(
        private val periodId: Long,
        private val repository: PeriodRepository,
        private val trackingRepository: TrackingRepository,
        private val preferencesStore: AppPreferencesStore? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PeriodDetailViewModel(periodId, repository, trackingRepository, preferencesStore) as T
        }
    }
}
