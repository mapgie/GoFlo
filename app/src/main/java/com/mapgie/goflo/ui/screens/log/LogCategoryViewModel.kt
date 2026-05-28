package com.mapgie.goflo.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.database.entities.TrackingValue
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogCategoryUiState(
    val isLoading: Boolean = true,
    val category: TrackingCategory? = null,
    val availableValues: List<TrackingValue> = emptyList(),
    /** Labels of values currently selected (may include labels not in availableValues if removed). */
    val selectedValues: Set<String> = emptySet(),
    /**
     * Current slider position for numeric_slider categories.
     * Null until the user interacts (or an existing value is loaded).
     * Ignored for text categories.
     */
    val numericValue: Float? = null,
    /** Current text entry for numeric_free categories. */
    val numericFreeText: String = "",
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isEditing: Boolean = false,
    val existingLog: TrackingLog? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class LogCategoryViewModel(
    private val categoryId: Long,
    private val prefilledDate: LocalDate?,
    private val existingLogId: Long?,
    private val repository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LogCategoryUiState(date = prefilledDate ?: LocalDate.now())
    )
    val uiState: StateFlow<LogCategoryUiState> = _uiState.asStateFlow()

    init {
        // Load initial state once (suspend: gets first emission of category + values + existing log)
        viewModelScope.launch { loadInitialData() }

        // Keep category and value list in sync with DB after initial load
        viewModelScope.launch {
            combine(
                repository.getCategoryById(categoryId),
                repository.getValuesForCategory(categoryId)
            ) { cat, vals -> cat to vals }
                .collect { (cat, vals) ->
                    if (!_uiState.value.isLoading) {
                        _uiState.update { it.copy(category = cat, availableValues = vals) }
                    }
                }
        }
    }

    private suspend fun loadInitialData() {
        val date = prefilledDate ?: LocalDate.now()

        // Get first emission of category + values (suspend, cancels collection after first)
        val (category, values) = combine(
            repository.getCategoryById(categoryId),
            repository.getValuesForCategory(categoryId)
        ) { cat, vals -> cat to vals }
            .first()

        // Load existing log for this date+category (or by logId for edit mode)
        val existingEntry = when {
            existingLogId != null -> repository.getLogById(existingLogId)
            else -> repository.getExistingLog(date, categoryId)
        }

        // For numeric categories, parse the first stored label back to Float
        val existingNumeric: Float? = if (category?.categoryType == "numeric_slider")
            existingEntry?.values?.firstOrNull()?.toFloatOrNull()
        else null

        val existingFreeText: String = if (category?.categoryType == "numeric_free")
            existingEntry?.values?.firstOrNull() ?: ""
        else ""


        _uiState.update {
            it.copy(
                isLoading = false,
                category = category,
                availableValues = values,
                selectedValues = existingEntry?.values?.toSet() ?: emptySet(),
                numericValue = existingNumeric,
                numericFreeText = existingFreeText,
                date = existingEntry?.log?.date?.let { d ->
                    runCatching { LocalDate.parse(d) }.getOrElse { date }
                } ?: date,
                notes = existingEntry?.log?.notes ?: "",
                isEditing = existingEntry != null,
                existingLog = existingEntry?.log
            )
        }
    }

    fun toggleValue(label: String) {
        _uiState.update { state ->
            val selected = state.selectedValues.toMutableSet()
            if (label in selected) selected.remove(label) else selected.add(label)
            state.copy(selectedValues = selected)
        }
    }

    fun setNumericValue(v: Float) = _uiState.update { it.copy(numericValue = v) }

    fun setNumericFreeText(text: String) = _uiState.update { it.copy(numericFreeText = text) }


    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading) return
        val cat = state.category

        // Determine the values to persist
        val valuesToSave: Set<String> = when (cat?.categoryType) {
            "numeric_slider" -> {
                val v = state.numericValue ?: return   // nothing to save yet
                setOf(formatNumericValue(v, cat.allowDecimals))
            }
            "numeric_free" -> {
                val text = state.numericFreeText.trim()
                if (text.isEmpty()) return
                setOf(text)
            }
            else -> state.selectedValues
        }

        viewModelScope.launch {
            runCatching {
                repository.saveLog(
                    date = state.date,
                    categoryId = categoryId,
                    selectedValues = valuesToSave,
                    notes = state.notes
                )
                _uiState.update { it.copy(saved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun formatNumericValue(v: Float, allowDecimals: Boolean): String =
        if (allowDecimals) "%.1f".format(v) else v.toInt().toString()

    fun delete() {
        val log = _uiState.value.existingLog ?: return
        viewModelScope.launch {
            runCatching {
                repository.deleteLog(log)
                _uiState.update { it.copy(deleted = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    class Factory(
        private val categoryId: Long,
        private val prefilledDate: LocalDate?,
        private val existingLogId: Long?,
        private val repository: TrackingRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LogCategoryViewModel(categoryId, prefilledDate, existingLogId, repository) as T
        }
    }
}
