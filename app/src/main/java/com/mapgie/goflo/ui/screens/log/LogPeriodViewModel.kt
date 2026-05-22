package com.mapgie.goflo.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.model.FlowLevel
import com.mapgie.goflo.data.model.SymptomType
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val notes: String = "",
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class LogPeriodViewModel(
    private val repository: PeriodRepository,
    private val periodId: Long,
    private val prefilledDate: LocalDate? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogPeriodUiState(startDate = prefilledDate ?: LocalDate.now()))
    val uiState: StateFlow<LogPeriodUiState> = _uiState.asStateFlow()

    init {
        if (periodId > 0) {
            viewModelScope.launch {
                val period = repository.getPeriodById(periodId).first()
                val symptoms = repository.getSymptomsOnce(periodId)
                    .mapNotNull { runCatching { SymptomType.valueOf(it.symptomType) }.getOrNull() }
                    .toSet()
                if (period != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = true,
                            existingId = period.id,
                            startDate = LocalDate.parse(period.startDate),
                            endDate = period.endDate?.let { d -> LocalDate.parse(d) },
                            flowLevel = runCatching { FlowLevel.valueOf(period.flowLevel) }.getOrDefault(FlowLevel.MEDIUM),
                            symptoms = symptoms,
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
                    repository.updatePeriod(entry, state.symptoms.toList())
                } else {
                    repository.insertPeriod(entry, state.symptoms.toList())
                }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not save entry. Please try again.") }
            }
        }
    }

    fun delete() {
        val state = _uiState.value
        val id = state.existingId ?: return
        viewModelScope.launch {
            try {
                val period = repository.getPeriodById(id).first() ?: return@launch
                repository.deletePeriod(period)
                _uiState.update { it.copy(deleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not delete entry. Please try again.") }
            }
        }
    }

    class Factory(
        private val repository: PeriodRepository,
        private val periodId: Long,
        private val prefilledDate: LocalDate? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LogPeriodViewModel(repository, periodId, prefilledDate) as T
        }
    }
}
