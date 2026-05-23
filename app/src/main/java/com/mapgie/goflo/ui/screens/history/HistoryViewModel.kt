package com.mapgie.goflo.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    // ── Pending-delete state ──────────────────────────────────────────────────
    // IDs staged for deletion. Excluded from the visible list immediately so the
    // card disappears when the user swipes, but not yet removed from the DB.
    // The DB write is deferred until the snackbar times out (SnackbarResult.Dismissed).
    // If the user taps Undo (SnackbarResult.ActionPerformed), the ID is cleared
    // from this set and the period reappears in the list — no DB write at all.
    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

    /** Visible period list — periods pending a snackbar-undo delete are hidden. */
    val periods: StateFlow<List<PeriodEntry>> = combine(
        repository.getAllPeriods(),
        _pendingDeleteIds,
    ) { all, pending ->
        all.filter { it.id !in pending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Top 5 symptoms ranked by the number of periods they appeared in.
     * Uses the unfiltered repository list so trends reflect full logged history,
     * not the transient visible list. Only populated when ≥3 periods are logged.
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

    // ── Delete lifecycle ──────────────────────────────────────────────────────

    /** Hides [period] from the visible list without touching the DB. */
    fun stageDeletion(period: PeriodEntry) {
        _pendingDeleteIds.update { it + period.id }
    }

    /**
     * Cancels a staged deletion (Undo).
     * The period reappears in the visible list; the DB is never written.
     */
    fun undoDeletion(period: PeriodEntry) {
        _pendingDeleteIds.update { it - period.id }
    }

    /**
     * Commits a staged deletion to the DB (called after the Undo snackbar times
     * out without the user tapping Undo).
     */
    fun commitDeletion(period: PeriodEntry) = viewModelScope.launch {
        repository.deletePeriod(period)
        _pendingDeleteIds.update { it - period.id }
    }

    class Factory(private val repository: PeriodRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
    }
}
