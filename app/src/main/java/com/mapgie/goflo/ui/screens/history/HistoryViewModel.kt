package com.mapgie.goflo.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PeriodWithSymptoms(
    val period: PeriodEntry,
    val symptoms: List<SymptomEntry>
)

/**
 * How often a symptom appeared across logged periods.
 *
 * @param displayName  Human-readable label (e.g. "Cramps", "nausea").
 * @param count        Number of distinct periods in which the symptom was logged.
 * @param percentage   count / totalPeriods × 100, clamped to 100.
 */
data class SymptomTrend(
    val displayName: String,
    val count: Int,
    val percentage: Int,
)

class HistoryViewModel(private val repository: PeriodRepository) : ViewModel() {

    val periods: StateFlow<List<PeriodEntry>> = repository.getAllPeriods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Top 5 symptoms ranked by the number of periods they appeared in.
     * Only populated when there are 3+ periods (fewer data points would skew percentages).
     */
    val symptomTrends: StateFlow<List<SymptomTrend>> = combine(
        repository.getAllPeriods(),
        repository.getAllSymptomsFlow(),
    ) { periods, symptoms ->
        if (periods.size < 3) return@combine emptyList()
        symptoms
            .groupBy { it.symptomType }
            .map { (type, entries) ->
                val uniquePeriods = entries.map { it.periodId }.toSet().size
                val displayName = runCatching { SymptomType.valueOf(type) }
                    .getOrNull()?.displayName
                    ?: type.replaceFirstChar { it.titlecase() }
                SymptomTrend(
                    displayName = displayName,
                    count       = uniquePeriods,
                    percentage  = (uniquePeriods * 100 / periods.size).coerceAtMost(100),
                )
            }
            .sortedByDescending { it.count }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePeriod(period: PeriodEntry) = viewModelScope.launch {
        repository.deletePeriod(period)
    }

    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
    }
}
