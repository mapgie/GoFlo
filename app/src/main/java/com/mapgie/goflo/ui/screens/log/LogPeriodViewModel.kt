package com.mapgie.goflo.ui.screens.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.widget.GoFloWidget
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class LogPeriodUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val existingId: Long? = null,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    /** Currently selected flow level label (e.g. "Medium", or a user-defined label). */
    val selectedFlowLabel: String = "Medium",
    /** All symptom labels selected for this period. */
    val symptoms: Set<String> = emptySet(),
    val notes: String = "",
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
    /** Non-system categories the user has marked "Log with period". */
    val pinnedCategories: List<TrackingCategory> = emptyList(),
    /** Available value labels for each pinned default category. */
    val pinnedCategoryValues: Map<Long, List<String>> = emptyMap(),
    /** User-selected value labels for each pinned default category. */
    val pinnedCategorySelections: Map<Long, Set<String>> = emptyMap(),
    /** Current slider position for each pinned numeric_slider category. */
    val pinnedNumericValues: Map<Long, Float?> = emptyMap(),
    /** Current text entry for each pinned numeric_free category. */
    val pinnedFreeTextValues: Map<Long, String> = emptyMap(),
    /** User-chosen display name for the Flow system category. */
    val flowCategoryName: String = "Flow",
    /** User-chosen display name for the Symptoms system category. */
    val symptomsCategoryName: String = "Symptoms",
    /** Full Flow system category entity — used to know its current categoryType. */
    val flowCategory: TrackingCategory? = null,
    /** Current slider position when the Flow category is in slider mode (1-4). */
    val flowSliderValue: Float? = null,
    /** Ordered list of selectable flow level options (from TrackingValues). */
    val flowOptions: List<TrackingValue> = emptyList(),
    /** Ordered list of all symptom options (from TrackingValues). */
    val symptomOptions: List<TrackingValue> = emptyList(),
    /** True once the user has made at least one edit — enables the save-on-back prompt. */
    val hasChanges: Boolean = false,
)

class LogPeriodViewModel(
    private val repository: PeriodRepository,
    private val periodId: Long,
    private val prefilledDate: LocalDate? = null,
    private val trackingRepository: com.mapgie.goflo.data.repository.TrackingRepository? = null,
    private val application: Application? = null,
    private val preferencesStore: AppPreferencesStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogPeriodUiState(startDate = prefilledDate ?: LocalDate.now()))
    val uiState: StateFlow<LogPeriodUiState> = _uiState.asStateFlow()

    init {
        if (periodId > 0) {
            viewModelScope.launch {
                val period = repository.getPeriodById(periodId).first()
                if (period != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = true,
                            existingId = period.id,
                            startDate = LocalDate.parse(period.startDate),
                            endDate = period.endDate?.let { d -> LocalDate.parse(d) },
                            notes = period.notes
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
                loadSystemCategoryNames()
                loadPinnedCategories()
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            viewModelScope.launch {
                loadSystemCategoryNames()
                loadPinnedCategories()
            }
        }
    }

    private suspend fun loadSystemCategoryNames() {
        val tr = trackingRepository ?: return
        val flowCat = tr.getSystemCategoryByKey("flow")
        val symptomsCat = tr.getSystemCategoryByKey("symptoms")

        // When editing, load current flow and symptoms from TrackingLog for the start date.
        val currentState = _uiState.value
        var editFlowLabel: String? = null
        var editFlowSlider: Float? = null
        var editSymptoms: Set<String>? = null
        if (currentState.isEditing) {
            val startDate = currentState.startDate
            if (flowCat != null) {
                val raw = tr.getExistingLog(startDate, flowCat.id)?.values?.firstOrNull()
                if (raw != null) {
                    if (flowCat.categoryType == "numeric_slider") {
                        editFlowSlider = raw.toFloatOrNull()
                        editFlowLabel = when (editFlowSlider?.toInt()) {
                            1 -> "Spotting"; 2 -> "Light"; 4 -> "Heavy"; else -> "Medium"
                        }
                    } else {
                        editFlowLabel = raw
                    }
                }
            }
            if (symptomsCat != null) {
                editSymptoms = tr.getExistingLog(startDate, symptomsCat.id)?.values?.toSet() ?: emptySet()
            }
        }

        _uiState.update { state ->
            val selectedFlow = editFlowLabel ?: state.selectedFlowLabel
            val sliderValue = editFlowSlider ?: if (flowCat?.categoryType == "numeric_slider" && state.flowSliderValue == null) {
                flowLabelToSliderValue(selectedFlow)
            } else {
                state.flowSliderValue
            }
            state.copy(
                flowCategoryName     = flowCat?.name ?: state.flowCategoryName,
                symptomsCategoryName = symptomsCat?.name ?: state.symptomsCategoryName,
                flowCategory         = flowCat,
                flowSliderValue      = sliderValue,
                selectedFlowLabel    = selectedFlow,
                symptoms             = editSymptoms ?: state.symptoms,
            )
        }

        // Subscribe to value lists in separate coroutines so chips update live after edits.
        if (flowCat != null) {
            viewModelScope.launch {
                tr.getValuesForCategory(flowCat.id).collect { values ->
                    _uiState.update { it.copy(flowOptions = values) }
                }
            }
        }
        if (symptomsCat != null) {
            viewModelScope.launch {
                tr.getValuesForCategory(symptomsCat.id).collect { values ->
                    _uiState.update { it.copy(symptomOptions = values) }
                }
            }
        }
    }

    private suspend fun loadPinnedCategories() {
        val tr = trackingRepository ?: return
        val categories = tr.getShowInLogPeriodCategories()
        if (categories.isEmpty()) return

        val valuesMap = mutableMapOf<Long, List<String>>()
        val selectionsMap = mutableMapOf<Long, Set<String>>()
        val numericMap = mutableMapOf<Long, Float?>()
        val freeTextMap = mutableMapOf<Long, String>()

        val date = _uiState.value.startDate
        for (cat in categories) {
            valuesMap[cat.id] = tr.getValuesForCategory(cat.id).first().map { it.label }
            val existing = tr.getExistingLog(date, cat.id)
            when (cat.categoryType) {
                "numeric_slider",
                "increment"      -> numericMap[cat.id] = existing?.values?.firstOrNull()?.toFloatOrNull()
                "numeric_free"   -> freeTextMap[cat.id] = existing?.values?.firstOrNull() ?: ""
                else             -> selectionsMap[cat.id] = existing?.values?.toSet() ?: emptySet()
            }
        }

        _uiState.update {
            it.copy(
                pinnedCategories       = categories,
                pinnedCategoryValues   = valuesMap,
                pinnedCategorySelections = selectionsMap,
                pinnedNumericValues    = numericMap,
                pinnedFreeTextValues   = freeTextMap,
            )
        }
    }

    fun setStartDate(date: LocalDate) = _uiState.update { state ->
        val end = if (state.endDate != null && date.isAfter(state.endDate)) null else state.endDate
        state.copy(startDate = date, endDate = end, hasChanges = true)
    }

    fun setEndDate(date: LocalDate?) = _uiState.update {
        it.copy(endDate = date, hasChanges = true)
    }

    fun setFlowLevel(label: String) = _uiState.update { it.copy(selectedFlowLabel = label, hasChanges = true) }

    fun setFlowSliderValue(value: Float) = _uiState.update { state ->
        // Map slider position to the nearest built-in label for storage.
        val label = when (value.toInt()) {
            1    -> "Spotting"
            2    -> "Light"
            4    -> "Heavy"
            else -> "Medium"
        }
        state.copy(flowSliderValue = value, selectedFlowLabel = label, hasChanges = true)
    }

    /** Toggles [label] in/out of the selected symptoms set. */
    fun toggleSymptom(label: String) = _uiState.update { state ->
        val updated = if (label in state.symptoms) state.symptoms - label else state.symptoms + label
        state.copy(symptoms = updated, hasChanges = true)
    }

    /**
     * Adds [name] as a new option in the symptoms catalog and selects it for this period.
     * The catalog insert is fire-and-forget; the selection is immediate.
     */
    fun addNewSymptomToLibrary(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val tr = trackingRepository ?: return@launch
            val sympCat = tr.getSystemCategoryByKey("symptoms") ?: return@launch
            tr.addValueToCategory(sympCat.id, trimmed)
        }
        _uiState.update { state -> state.copy(symptoms = state.symptoms + trimmed, hasChanges = true) }
    }

    fun setNotes(notes: String) = _uiState.update { it.copy(notes = notes, hasChanges = true) }

    fun togglePinnedValue(categoryId: Long, label: String) = _uiState.update { state ->
        val current = state.pinnedCategorySelections[categoryId] ?: emptySet()
        val updated = if (label in current) current - label else current + label
        state.copy(pinnedCategorySelections = state.pinnedCategorySelections + (categoryId to updated), hasChanges = true)
    }

    fun setPinnedNumericValue(categoryId: Long, value: Float) = _uiState.update { state ->
        state.copy(pinnedNumericValues = state.pinnedNumericValues + (categoryId to value), hasChanges = true)
    }

    fun setPinnedFreeText(categoryId: Long, text: String) = _uiState.update { state ->
        state.copy(pinnedFreeTextValues = state.pinnedFreeTextValues + (categoryId to text), hasChanges = true)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val entry = PeriodEntry(
                    id = state.existingId ?: 0,
                    startDate = state.startDate.toString(),
                    endDate = state.endDate?.toString(),
                    flowLevel = "",
                    notes = state.notes
                )
                if (state.isEditing && state.existingId != null) {
                    repository.updatePeriod(entry, emptyList())
                } else {
                    repository.insertPeriod(entry, emptyList())
                }
                syncFlowToTrackingLog(state)
                syncSymptomsToTrackingLog(state)
                syncPinnedCategoryLogs(state)
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
        val flowCategory = tr.getSystemCategoryByKey("flow") ?: return
        val flowLabel = if (flowCategory.categoryType == "numeric_slider") {
            val v = state.flowSliderValue ?: flowLabelToSliderValue(state.selectedFlowLabel)
            v.toInt().toString()
        } else {
            state.selectedFlowLabel
        }
        tr.syncFlowLogsForPeriod(flowCategory.id, listOf(state.startDate), flowLabel)
    }

    private suspend fun syncSymptomsToTrackingLog(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val symptomsCategory = tr.getSystemCategoryByKey("symptoms") ?: return
        if (state.symptoms.isEmpty()) {
            val existing = tr.getExistingLog(state.startDate, symptomsCategory.id) ?: return
            tr.deleteLog(existing.log)
        } else {
            val loggedAt = if (symptomsCategory.trackAgainstTime) {
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else ""
            tr.saveLog(
                date           = state.startDate,
                categoryId     = symptomsCategory.id,
                selectedValues = state.symptoms,
                notes          = "",
                allowMultiple  = false,
                loggedAt       = loggedAt,
            )
        }
    }

    /** Saves each pinned category's current selection as a tracking log for the period start date. */
    private suspend fun syncPinnedCategoryLogs(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val date = state.startDate
        for (cat in state.pinnedCategories) {
            val valuesToSave = computePinnedValues(cat, state) ?: continue
            tr.saveLog(
                date           = date,
                categoryId     = cat.id,
                selectedValues = valuesToSave,
                notes          = "",
                allowMultiple  = false,
            )
        }
    }

    private fun computePinnedValues(cat: TrackingCategory, state: LogPeriodUiState): Set<String>? =
        when (cat.categoryType) {
            "numeric_slider" -> {
                val v = state.pinnedNumericValues[cat.id] ?: return null
                setOf(if (cat.allowDecimals) "%.1f".format(v) else v.toInt().toString())
            }
            "numeric_free" -> {
                val text = (state.pinnedFreeTextValues[cat.id] ?: "").trim()
                if (text.isEmpty()) null else setOf(text)
            }
            "increment" -> {
                val count = state.pinnedNumericValues[cat.id]?.toInt() ?: 0
                if (count <= 0) null else setOf(count.toString())
            }
            else -> {
                val selected = state.pinnedCategorySelections[cat.id] ?: emptySet()
                if (selected.isEmpty()) null else selected
            }
        }

    fun disablePeriodTracking() {
        viewModelScope.launch { preferencesStore?.setPeriodTrackingEnabled(false) }
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
        private val application: Application? = null,
        private val preferencesStore: AppPreferencesStore? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LogPeriodViewModel(repository, periodId, prefilledDate, trackingRepository, application, preferencesStore) as T
        }
    }

    companion object {
        private fun flowLabelToSliderValue(label: String): Float = when (label) {
            "Spotting" -> 1f
            "Light"    -> 2f
            "Heavy"    -> 4f
            else       -> 3f  // "Medium" and any custom label default to the middle
        }
    }
}
