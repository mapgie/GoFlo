package com.mapgie.goflo.ui.screens.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.notifications.ReminderScheduler
import com.mapgie.goflo.widget.GoFloWidget
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PeriodWithSymptoms(
    val period: PeriodEntry,
    val symptoms: List<SymptomEntry>
)

class HistoryViewModel(
    private val repository: PeriodRepository,
    private val application: Application? = null,
    private val trackingRepository: TrackingRepository? = null,
) : ViewModel() {

    // ── Pending-delete state ──────────────────────────────────────────────────
    // IDs of periods that have been swiped but whose Undo snackbar is still
    // visible. The period is hidden from the visible list immediately AND deleted
    // from the DB straight away (inside viewModelScope, so the delete survives
    // navigation). If the user taps Undo, the full period + symptoms are
    // re-inserted from the in-memory cache below.
    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

    private data class UndoData(
        val period: PeriodEntry,
        val symptoms: Set<String>,
    )
    private val pendingUndo = mutableMapOf<Long, UndoData>()

    val avgCycleLength: StateFlow<Int> = repository.getAllPeriods()
        .map { PeriodRepository.calculateAvgCycleLength(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 28)

    /** Visible period list — periods being deleted are hidden during the Undo window. */
    val periods: StateFlow<List<PeriodEntry>> = combine(
        repository.getAllPeriods(),
        _pendingDeleteIds,
    ) { all, pending ->
        all.filter { it.id !in pending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Delete lifecycle ──────────────────────────────────────────────────────

    /**
     * Hides [period] from the visible list and immediately deletes it from the DB.
     * Symptoms are read first and stored for a potential Undo re-insertion.
     * Using viewModelScope ensures the delete completes even if the user
     * navigates away before the snackbar times out.
     */
    fun stageDeletion(period: PeriodEntry) {
        viewModelScope.launch {
            // Read symptoms before hiding or deleting so the undo cache is always
            // populated before the snackbar can be tapped.
            val symptoms = repository.getSymptomsParsed(period.id)
            pendingUndo[period.id] = UndoData(period, symptoms)
            _pendingDeleteIds.update { it + period.id }
            trackingRepository?.deleteLogsForPeriod(
                LocalDate.parse(period.startDate),
                period.endDate?.let { LocalDate.parse(it) }
            )
            repository.deletePeriod(period)
            application?.let { GoFloWidget.updateAllWidgets(it) }
            // Deleting a period changes the cycle predictions the reminders are armed on.
            application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
        }
    }

    /**
     * Re-inserts the deleted period and its symptoms (Undo tapped).
     */
    fun undoDeletion(period: PeriodEntry) {
        viewModelScope.launch {
            val undo = pendingUndo.remove(period.id)
            if (undo != null) {
                repository.insertPeriod(undo.period, undo.symptoms.toList())
                application?.let { GoFloWidget.updateAllWidgets(it) }
                application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
            }
            _pendingDeleteIds.update { it - period.id }
        }
    }

    /**
     * Clears the in-memory Undo cache (snackbar timed out without Undo).
     * The DB delete already happened in [stageDeletion].
     */
    fun commitDeletion(period: PeriodEntry) {
        pendingUndo.remove(period.id)
        _pendingDeleteIds.update { it - period.id }
    }

    /**
     * Merges [first] and [second] into a single continuous period, combining their
     * notes and symptoms. The tracking-log entries (flow, symptoms, pinned
     * categories) belonging to whichever period is absorbed are also removed,
     * mirroring the cleanup performed on delete — otherwise its flow/symptom
     * entry would linger, orphaned, under a date no longer tied to any period.
     */
    fun mergePeriods(first: PeriodEntry, second: PeriodEntry) {
        viewModelScope.launch {
            val absorbed = if (first.startDate <= second.startDate) second else first
            repository.mergePeriods(first, second)
            trackingRepository?.deleteLogsForPeriod(
                LocalDate.parse(absorbed.startDate),
                absorbed.endDate?.let { LocalDate.parse(it) }
            )
            application?.let { GoFloWidget.updateAllWidgets(it) }
            application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
        }
    }

    class Factory(
        private val repository: PeriodRepository,
        private val application: Application? = null,
        private val trackingRepository: TrackingRepository? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository, application, trackingRepository) as T
        }
    }
}
