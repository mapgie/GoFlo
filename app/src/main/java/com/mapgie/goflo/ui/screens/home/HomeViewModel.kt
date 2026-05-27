package com.mapgie.goflo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.theme.BannerStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val periods: List<PeriodEntry> = emptyList(),
    val activePeriod: PeriodEntry? = null,
    val cycleDay: Int? = null,
    val avgCycleLength: Int = 28,
    /** True when the user has manually overridden the cycle length. */
    val cycleOverrideActive: Boolean = false,
    val predictedNextPeriod: LocalDate? = null,
    val ovulationDay: LocalDate? = null,
    /** ±2-day window around the estimated ovulation day (3–5 dates). */
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
    /** Decorative shape style for the home-screen banner. */
    val bannerStyle: BannerStyle = BannerStyle.PLAIN,
)

/** Data loaded for the Day Log bottom sheet. */
data class DayLogData(
    val date: LocalDate,
    val period: PeriodEntry?,
    val periodSymptomLabels: List<String>,
    val trackingLogs: List<TrackingLogWithValues>
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

        val periodDays = buildSet {
            periods.forEach { entry ->
                var d = LocalDate.parse(entry.startDate)
                val end = entry.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                while (!d.isAfter(end)) { add(d); d = d.plusDays(1) }
            }
        }

        val predictedDays = if (prefs.showPeriodPrediction) buildSet<LocalDate> {
            nextStart?.let { start ->
                val predictedEnd = start.plusDays(4)
                var d = start
                while (!d.isAfter(predictedEnd)) {
                    if (d !in periodDays) add(d)
                    d = d.plusDays(1)
                }
            }
        } else emptySet()

        HomeUiState(
            periods              = periods,
            activePeriod         = active,
            cycleDay             = cycleDay,
            avgCycleLength       = avg,
            cycleOverrideActive  = customCycle != null,
            predictedNextPeriod  = if (prefs.showPeriodPrediction) nextStart else null,
            ovulationDay         = if (prefs.showOvulationMarkers) ovulationDay else null,
            ovulationWindow      = if (prefs.showOvulationMarkers) ovulationWindow else emptySet(),
            periodDays           = periodDays,
            predictedDays        = predictedDays,
            trackingLogDates     = trackingDates,
            daysWithAnyData      = periodDays + trackingDates,
            trackingCategories   = categories,
            quickLogCategoryId   = prefs.quickLogCategoryId,
            bannerStyle          = runCatching { BannerStyle.valueOf(prefs.bannerStyle) }
                                       .getOrDefault(BannerStyle.PLAIN),
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
    val dayLogData: StateFlow<DayLogData?> = _selectedDay
        .flatMapLatest { date ->
            if (date == null) return@flatMapLatest flowOf(null)

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
                    val labels = symptomEntries.map { entry ->
                        runCatching { SymptomType.valueOf(entry.symptomType) }
                            .getOrNull()
                            ?.name
                            ?.lowercase()
                            ?.split('_')
                            ?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                            ?: entry.symptomType
                    }
                    DayLogData(
                        date = date,
                        period = period,
                        periodSymptomLabels = labels,
                        trackingLogs = trackingLogs
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDay(date: LocalDate) { _selectedDay.value = date }
    fun clearSelectedDay() { _selectedDay.value = null }

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
