package com.mapgie.goflo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

class HomeViewModel(
    private val repository: PeriodRepository,
    private val preferencesStore: AppPreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllPeriods(),
        preferencesStore.preferences,
    ) { periods, prefs ->
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

        val predictedDays = buildSet<LocalDate> {
            nextStart?.let { start ->
                val predictedEnd = start.plusDays(4)
                var d = start
                while (!d.isAfter(predictedEnd)) {
                    if (d !in periodDays) add(d)
                    d = d.plusDays(1)
                }
            }
        }

        HomeUiState(
            periods            = periods,
            activePeriod       = active,
            cycleDay           = cycleDay,
            avgCycleLength     = avg,
            cycleOverrideActive = customCycle != null,
            predictedNextPeriod = nextStart,
            ovulationDay       = ovulationDay,
            ovulationWindow    = ovulationWindow,
            periodDays         = periodDays,
            predictedDays      = predictedDays,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    class Factory(
        private val repository: PeriodRepository,
        private val preferencesStore: AppPreferencesStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, preferencesStore) as T
        }
    }
}
