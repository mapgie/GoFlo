package com.mapgie.goflo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val periods: List<PeriodEntry> = emptyList(),
    val activePeriod: PeriodEntry? = null,
    val cycleDay: Int? = null,
    val avgCycleLength: Int = 28,
    /** True when the user has manually overridden the cycle length. */
    val cycleOverrideActive: Boolean = false,
    val cyclePhaseLabel: String? = null,
    val predictedNextPeriod: LocalDate? = null,
    /** True when today falls within the predicted period window but no period is active. */
    val isInExpectedPeriod: Boolean = false,
    val ovulationDay: LocalDate? = null,
    /** ±2-day window around the estimated ovulation day (3-5 dates). */
    val ovulationWindow: Set<LocalDate> = emptySet(),
    val periodDays: Set<LocalDate> = emptySet(),
    val predictedDays: Set<LocalDate> = emptySet(),
    /** Dates that have at least one tracking category log entry. */
    val trackingLogDates: Set<LocalDate> = emptySet(),
    /** All dates that have any logged data (period or tracking). */
    val daysWithAnyData: Set<LocalDate> = emptySet(),
    /** All tracking categories (for FAB long-press menu). */
    val trackingCategories: List<TrackingCategory> = emptyList(),
    /** The preferred Quick Log category ID (-1L = Log Period). */
    val quickLogCategoryId: Long = -1L,
    /** True once the new-user onboarding banner has been dismissed. */
    val onboardingBannerDismissed: Boolean = false,
)

/** Data loaded for the Day Log bottom sheet. */
data class DayLogData(
    val date: LocalDate,
    val period: PeriodEntry?,
    val periodSymptomLabels: List<String>,
    val trackingLogs: List<TrackingLogWithValues>,
    val flowCategoryName: String = "Flow",
    val symptomsCategoryName: String = "Symptoms",
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: PeriodRepository,
    private val trackingRepository: TrackingRepository,
    private val preferencesStore: AppPreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllPeriods(),
        preferencesStore.preferences,
        trackingRepository.getAllLogDates(),
        trackingRepository.getActiveCategories(),
    ) { periods, prefs, trackingDates, categories ->
        val customCycle = prefs.preferredCycleLength.takeIf { it > 0 }
        val avg = customCycle ?: PeriodRepository.calculateAvgCycleLength(periods)
        val nextStart = PeriodRepository.predictNextStart(periods, avg)
        val ovulationDay = PeriodRepository.ovulationDate(periods, avg)
        val ovulationWindow = ovulationDay?.let {
            setOf(it.minusDays(2), it.minusDays(1), it, it.plusDays(1), it.plusDays(2))
        } ?: emptySet()
        val active = PeriodRepository.activePeriod(periods)
        val cycleDay = PeriodRepository.cycleDay(periods)
        val cyclePhaseLabel = cycleDay?.let { PeriodRepository.cyclePhaseLabel(it, avg) }

        val periodDays = buildSet {
            periods.forEach { entry ->
                var d = LocalDate.parse(entry.startDate)
                val end = entry.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                while (!d.isAfter(end)) { add(d); d = d.plusDays(1) }
            }
        }

        val today = LocalDate.now()
        val nextStartIsFuture = nextStart != null && !nextStart.isBefore(today)
        // Show prediction window as long as any day of the window (start..start+4) is still today or future.
        // This ensures the calendar markers and "expected" status appear even mid-window.
        val predictionWindowActive = nextStart != null && !nextStart.plusDays(4).isBefore(today)

        val predictedDays = if (prefs.showPeriodPrediction && predictionWindowActive) buildSet<LocalDate> {
            nextStart!!.let { start ->
                val predictedEnd = start.plusDays(4)
                var d = start
                while (!d.isAfter(predictedEnd)) {
                    if (d !in periodDays) add(d)
                    d = d.plusDays(1)
                }
            }
        } else emptySet()

        // True when today is inside the predicted window but the period hasn't started yet (not logged).
        val isInExpectedPeriod = prefs.showPeriodPrediction &&
            predictionWindowActive && !nextStartIsFuture && active == null

        HomeUiState(
            periods              = periods,
            activePeriod         = active,
            cycleDay             = cycleDay,
            avgCycleLength       = avg,
            cycleOverrideActive  = customCycle != null,
            cyclePhaseLabel      = cyclePhaseLabel,
            predictedNextPeriod  = if (prefs.showPeriodPrediction && nextStartIsFuture) nextStart else null,
            isInExpectedPeriod   = isInExpectedPeriod,
            ovulationDay         = if (prefs.showOvulationMarkers) ovulationDay else null,
            ovulationWindow      = if (prefs.showOvulationMarkers) ovulationWindow else emptySet(),
            periodDays           = periodDays,
            predictedDays        = predictedDays,
            trackingLogDates     = trackingDates,
            daysWithAnyData      = periodDays + trackingDates,
            trackingCategories          = categories,
            quickLogCategoryId          = prefs.quickLogCategoryId,
            onboardingBannerDismissed   = prefs.onboardingBannerDismissed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    // ── Day Log sheet ─────────────────────────────────────────────────────────

    /** The date for which the DayLogSheet is open; null = sheet closed. */
    private val _selectedDay = MutableStateFlow<LocalDate?>(null)

    /**
     * Reactive stream of data for the DayLogSheet.
     * When [_selectedDay] changes, this flow switches to load that day's data.
     * Null when no day is selected.
     */
    val dayLogData: StateFlow<DayLogData?> = combine(
        _selectedDay,
        trackingRepository.getActiveCategories()
    ) { date, cats -> Pair(date, cats) }
        .flatMapLatest { (date, cats) ->
            if (date == null) return@flatMapLatest flowOf(null)

            val flowCatName = cats.firstOrNull { it.systemKey == "flow" }?.name ?: "Flow"
            val symptomsCatName = cats.firstOrNull { it.systemKey == "symptoms" }?.name ?: "Symptoms"

            // Combine period list + tracking logs for this date; when either
            // updates (e.g. after user saves/deletes) the sheet re-renders.
            combine(
                repository.getAllPeriods(),
                trackingRepository.getLogsForDate(date)
            ) { periods, trackingLogs ->
                // Find the period covering this date
                val period = periods.firstOrNull { p ->
                    val start = LocalDate.parse(p.startDate)
                    val end = p.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                    !date.isBefore(start) && !date.isAfter(end)
                }
                Pair(period, trackingLogs)
            }.flatMapLatest { (period, trackingLogs) ->
                // If there's a period, load its symptoms reactively
                val symptomsFlow = period?.let { repository.getSymptomsForPeriod(it.id) }
                    ?: flowOf(emptyList())

                symptomsFlow.map { symptomEntries ->
                    val labels = symptomEntries.map { it.symptomType }
                    DayLogData(
                        date = date,
                        period = period,
                        periodSymptomLabels = labels,
                        trackingLogs = trackingLogs,
                        flowCategoryName = flowCatName,
                        symptomsCategoryName = symptomsCatName,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDay(date: LocalDate) { _selectedDay.value = date }
    fun clearSelectedDay() { _selectedDay.value = null }

    // ── Quick increment (Plus One categories) ───────────────────────────────────

    /** Transient confirmation message after an instant increment; null when none pending. */
    private val _quickLogMessage = MutableStateFlow<String?>(null)
    val quickLogMessage: StateFlow<String?> = _quickLogMessage.asStateFlow()

    private data class LastIncrementInfo(
        val categoryId: Long,
        val date: LocalDate,
        /** Non-null when the tap created a timed entry; null for counter-style increments. */
        val timedLogId: Long? = null,
    )
    private var lastIncrement: LastIncrementInfo? = null

    /**
     * Instantly adds one to an "increment" category for [date] (today by default)
     * without opening the log screen, then surfaces a brief confirmation message.
     *
     * For categories with [trackAgainstTime] enabled, each tap saves a separate
     * timestamped log entry. For regular increment categories, it upserts a counter.
     */
    fun incrementCategory(categoryId: Long, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val cat = trackingRepository.getCategoryByIdOnce(categoryId) ?: return@launch
            val unit = if (cat.numericUnit.isNotBlank()) " ${cat.numericUnit}" else ""
            if (cat.trackAgainstTime) {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                val logId = trackingRepository.saveLog(
                    date           = date,
                    categoryId     = categoryId,
                    selectedValues = setOf("1"),
                    notes          = "",
                    allowMultiple  = true,
                    loggedAt       = time,
                )
                val count = trackingRepository.getLogsForDateAndCategory(date, categoryId).size
                lastIncrement = LastIncrementInfo(categoryId, date, timedLogId = logId)
                _quickLogMessage.value = "${cat.name}: $count$unit"
            } else {
                val newCount = trackingRepository.incrementLog(date, categoryId)
                lastIncrement = LastIncrementInfo(categoryId, date)
                _quickLogMessage.value = "${cat.name}: $newCount$unit"
            }
        }
    }

    fun undoLastIncrement() {
        val last = lastIncrement ?: return
        lastIncrement = null
        viewModelScope.launch {
            if (last.timedLogId != null) {
                val logWithValues = trackingRepository.getLogById(last.timedLogId)
                if (logWithValues != null) trackingRepository.deleteLog(logWithValues.log)
            } else {
                trackingRepository.incrementLog(last.date, last.categoryId, delta = -1)
            }
        }
    }

    fun clearQuickLogMessage() { _quickLogMessage.value = null }

    fun dismissOnboardingBanner() {
        viewModelScope.launch { preferencesStore.setOnboardingBannerDismissed(true) }
    }

    class Factory(
        private val repository: PeriodRepository,
        private val trackingRepository: TrackingRepository,
        private val preferencesStore: AppPreferencesStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, trackingRepository, preferencesStore) as T
        }
    }
}
