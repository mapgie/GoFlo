package com.mapgie.goflo.ui.screens.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.widget.GoFloWidget
import com.mapgie.goflo.data.model.FlowLevel
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogPeriodUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val existingId: Long? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val flowLevel: FlowLevel = FlowLevel.MEDIUM,
    val symptoms: Set<SymptomType> = emptySet(),
    /** Custom symptom names (lowercase) selected for this period entry. */
    val customSymptoms: Set<String> = emptySet(),
    val notes: String = "",
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class LogPeriodViewModel(
    private val repository: PeriodRepository,
    private val periodId: Long,
    private val prefilledDate: LocalDate? = null,
    private val trackingRepository: com.mapgie.goflo.data.repository.TrackingRepository? = null,
    private val application: Application? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogPeriodUiState(startDate = prefilledDate ?: LocalDate.now()))
    val uiState: StateFlow<LogPeriodUiState> = _uiState.asStateFlow()

    /** All custom symptoms saved to the user's library (alphabetical). */
    val librarySymptoms: StateFlow<List<String>> = repository.getAllCustomSymptoms()
        .map { entries -> entries.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (periodId > 0) {
            viewModelScope.launch {
                val period = repository.getPeriodById(periodId).first()
                val (builtIn, custom) = repository.getSymptomsParsed(periodId)
                if (period != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = true,
                            existingId = period.id,
                            startDate = LocalDate.parse(period.startDate),
                            endDate = period.endDate?.let { d -> LocalDate.parse(d) },
                            flowLevel = runCatching { FlowLevel.valueOf(period.flowLevel) }.getOrDefault(FlowLevel.MEDIUM),
                            symptoms = builtIn,
                            customSymptoms = custom,
                            notes = period.notes
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setStartDate(date: LocalDate) = _uiState.update { state ->
        val end = if (state.endDate != null && date.isAfter(state.endDate)) null else state.endDate
        state.copy(startDate = date, endDate = end)
    }

    fun setEndDate(date: LocalDate?) = _uiState.update {
        it.copy(endDate = date)
    }

    fun setFlowLevel(flow: FlowLevel) = _uiState.update { it.copy(flowLevel = flow) }

    fun toggleSymptom(symptom: SymptomType) = _uiState.update { state ->
        val updated = if (symptom in state.symptoms) state.symptoms - symptom else state.symptoms + symptom
        state.copy(symptoms = updated)
    }

    /** Toggle a custom symptom in/out of the current period selection. */
    fun toggleCustomSymptom(name: String) = _uiState.update { state ->
        val lower = name.lowercase()
        val updated = if (lower in state.customSymptoms) state.customSymptoms - lower else state.customSymptoms + lower
        state.copy(customSymptoms = updated)
    }

    /**
     * Saves [name] to the user's permanent library and selects it for the current period.
     * The library insert is fire-and-forget; the selection is immediate.
     */
    fun addAndSelectCustomSymptom(name: String) {
        val lower = name.lowercase()
        viewModelScope.launch { repository.addCustomSymptom(lower) }
        _uiState.update { state -> state.copy(customSymptoms = state.customSymptoms + lower) }
    }

    fun setNotes(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val entry = PeriodEntry(
                    id = state.existingId ?: 0,
                    startDate = state.startDate.toString(),
                    endDate = state.endDate?.toString(),
                    flowLevel = state.flowLevel.name,
                    notes = state.notes
                )
                if (state.isEditing && state.existingId != null) {
                    repository.updatePeriod(entry, state.symptoms.toList(), state.customSymptoms.toList())
                } else {
                    repository.insertPeriod(entry, state.symptoms.toList(), state.customSymptoms.toList())
                }
                // Dual-write: sync flow data to TrackingLog so Stats can query uniformly
                syncFlowToTrackingLog(state)
                application?.let { GoFloWidget.updateAllWidgets(it) }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not save entry. Please try again.") }
            }
        }
    }

    /**
     * Mirrors the saved period's flow level into the TrackingLog system.
     * This ensures new/edited periods appear in the Stats screen under the Flow category.
     * No-op if [trackingRepository] was not provided (e.g. in tests or legacy callers).
     */
    private suspend fun syncFlowToTrackingLog(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val flowCategory = tr.getSystemCategoryByName("Flow") ?: return
        val flowLabel = state.flowLevel.displayName
        val end = state.endDate ?: state.startDate
        val dates = generateSequence(state.startDate) { d -> if (d < end) d.plusDays(1) else null }.toList()
        tr.syncFlowLogsForPeriod(flowCategory.id, dates, flowLabel)
    }

    fun delete() {
        val state = _uiState.value
        val id = state.existingId ?: return
        viewModelScope.launch {
            try {
                val period = repository.getPeriodById(id).first() ?: return@launch
                repository.deletePeriod(period)
                application?.let { GoFloWidget.updateAllWidgets(it) }
                _uiState.update { it.copy(deleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not delete entry. Please try again.") }
            }
        }
    }

    class Factory(
        private val repository: PeriodRepository,
        private val periodId: Long,
        private val prefilledDate: LocalDate? = null,
        private val trackingRepository: com.mapgie.goflo.data.repository.TrackingRepository? = null,
        private val application: Application? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LogPeriodViewModel(repository, periodId, prefilledDate, trackingRepository, application) as T
        }
    }
}
