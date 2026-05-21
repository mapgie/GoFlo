package com.mapgie.goflo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val periods: List<PeriodEntry> = emptyList(),
    val activePeriod: PeriodEntry? = null,
    val cycleDay: Int? = null,
    val avgCycleLength: Int = 28,
    val predictedNextPeriod: LocalDate? = null,
    val ovulationDay: LocalDate? = null,
    val periodDays: Set<LocalDate> = emptySet(),
    val predictedDays: Set<LocalDate> = emptySet()
)

class HomeViewModel(private val repository: PeriodRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.getAllPeriods()
        .map { periods ->
            val avg = PeriodRepository.calculateAvgCycleLength(periods)
            val nextStart = PeriodRepository.predictNextStart(periods, avg)
            val ovulation = PeriodRepository.ovulationDate(periods, avg)
            val active = PeriodRepository.activePeriod(periods)
            val cycleDay = PeriodRepository.cycleDay(periods)

            val periodDays = buildSet {
                periods.forEach { entry ->
                    var d = LocalDate.parse(entry.startDate)
                    val end = entry.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                    while (!d.isAfter(end)) {
                        add(d)
                        d = d.plusDays(1)
                    }
                }
            }

            val predictedDays = buildSet {
                if (nextStart != null) {
                    val predictedEnd = nextStart.plusDays(4)
                    var d = nextStart
                    while (!d.isAfter(predictedEnd)) {
                        if (d !in periodDays) add(d)
                        d = d.plusDays(1)
                    }
                }
            }

            HomeUiState(
                periods = periods,
                activePeriod = active,
                cycleDay = cycleDay,
                avgCycleLength = avg,
                predictedNextPeriod = nextStart,
                ovulationDay = ovulation,
                periodDays = periodDays,
                predictedDays = predictedDays
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
    }
}
