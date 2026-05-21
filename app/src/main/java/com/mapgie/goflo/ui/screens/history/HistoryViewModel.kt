package com.mapgie.goflo.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PeriodWithSymptoms(
    val period: PeriodEntry,
    val symptoms: List<SymptomEntry>
)

class HistoryViewModel(private val repository: PeriodRepository) : ViewModel() {

    val periods: StateFlow<List<PeriodEntry>> = repository.getAllPeriods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
    }
}
