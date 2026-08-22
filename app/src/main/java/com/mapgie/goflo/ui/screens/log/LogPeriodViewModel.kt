package com.mapgie.goflo.ui.screens.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.notifications.ReminderScheduler
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
import java.time.temporal.ChronoUnit

/**
 * UI state for per-day period logging.
 *
 * The unit of logging is a single day: [date] is the day being logged or
 * edited, and every day-specific value (flow, symptoms, pinned categories)
 * applies to that day only. Episode-level context (which period the day
 * belongs to, its start, its explicit end, its notes) is shown and editable
 * alongside, but the period itself is derived from the logged days.
 */
data class LogPeriodUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val existingId: Long? = null,
    /** The day being logged or edited. All per-day values below apply to it. */
    val date: LocalDate = LocalDate.now(),
    /** Episode start (editing only — moving it re-keys the episode's days). */
    val startDate: LocalDate = LocalDate.now(),
    /** Explicit episode end ("until"), or null to leave the period open. */
    val endDate: LocalDate? = null,
    /**
     * When creating: the start date of the existing period that [date] would
     * continue (within gap tolerance), or null if it starts a new period.
     */
    val continuesEpisodeStart: LocalDate? = null,
    /** 1-based day number of [date] within its episode, when known. */
    val episodeDayNumber: Int? = null,
    /** Gap tolerance (days) loaded from preferences. */
    val toleranceDays: Int = PeriodRepository.DEFAULT_GAP_TOLERANCE_DAYS,
    /** Currently selected flow level label for [date] (e.g. "Medium"). */
    val selectedFlowLabel: String = "Medium",
    /** All symptom labels selected for [date]. */
    val symptoms: Set<String> = emptySet(),
    /** Episode-level notes. */
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
    /** Full Flow system category entity — used to know its current categoryType and showInLogPeriod. */
    val flowCategory: TrackingCategory? = null,
    /** Full Symptoms system category entity — used to check showInLogPeriod. */
    val symptomsCategory: TrackingCategory? = null,
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

    private val _uiState = MutableStateFlow(
        LogPeriodUiState(
            date = prefilledDate ?: LocalDate.now(),
            startDate = prefilledDate ?: LocalDate.now(),
        )
    )
    val uiState: StateFlow<LogPeriodUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tolerance = preferencesStore?.preferences?.first()?.periodGapToleranceDays
                ?: PeriodRepository.DEFAULT_GAP_TOLERANCE_DAYS
            _uiState.update { it.copy(toleranceDays = tolerance) }

            if (periodId > 0) {
                val period = repository.getPeriodById(periodId).first()
                if (period != null) {
                    val start = LocalDate.parse(period.startDate)
                    val storedEnd = period.endDate?.let { d -> LocalDate.parse(d) }
                    val day = prefilledDate ?: start
                    // If this screen was opened for a day that continues the
                    // period (a day past its stored end, within tolerance),
                    // extend the end to that day so saving naturally continues
                    // the period instead of trimming the new day away.
                    val effectiveEnd = if (storedEnd != null && day.isAfter(storedEnd)) day else storedEnd
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = true,
                            existingId = period.id,
                            date = day,
                            startDate = start,
                            endDate = effectiveEnd,
                            episodeDayNumber = dayNumber(start, day),
                            notes = period.notes,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                resolveContinuationContext(_uiState.value.date, tolerance)
                _uiState.update { it.copy(isLoading = false) }
            }
            loadSystemCategoryNames()
            loadPinnedCategories()
        }
    }

    /**
     * Looks up whether logging [date] would continue an existing period, and
     * prefills the episode context (day number, notes) from it if so.
     */
    private suspend fun resolveContinuationContext(date: LocalDate, tolerance: Int) {
        val periods = repository.getAllPeriodsOnce()
        val episode = PeriodRepository.periodForDate(periods, date, tolerance)
        _uiState.update { state ->
            if (episode != null) {
                val start = LocalDate.parse(episode.startDate)
                state.copy(
                    continuesEpisodeStart = start,
                    episodeDayNumber = dayNumber(minOf(start, date), date),
                    notes = if (state.notes.isBlank()) episode.notes else state.notes,
                )
            } else {
                state.copy(continuesEpisodeStart = null, episodeDayNumber = null)
            }
        }
    }

    private suspend fun loadSystemCategoryNames() {
        val tr = trackingRepository ?: return
        val flowCat = tr.getSystemCategoryByKey("flow")
        val symptomsCat = tr.getSystemCategoryByKey("symptoms")

        // Load current flow and symptoms from TrackingLog for the day being logged.
        val date = _uiState.value.date
        var editFlowLabel: String? = null
        var editFlowSlider: Float? = null
        var editSymptoms: Set<String>? = null
        if (flowCat != null) {
            val raw = tr.getExistingLog(date, flowCat.id)?.values?.firstOrNull()
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
            editSymptoms = tr.getExistingLog(date, symptomsCat.id)?.values?.toSet()
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
                symptomsCategory     = symptomsCat,
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
        // Exclude system categories (flow, symptoms) — they have dedicated UI sections above.
        val categories = tr.getShowInLogPeriodCategories().filter { !it.isSystem }
        if (categories.isEmpty()) return

        val valuesMap = mutableMapOf<Long, List<String>>()
        val selectionsMap = mutableMapOf<Long, Set<String>>()
        val numericMap = mutableMapOf<Long, Float?>()
        val freeTextMap = mutableMapOf<Long, String>()

        val date = _uiState.value.date
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

    /**
     * Changes the day being logged (new entries only). Re-resolves the
     * continuation context and reloads that day's existing values so the
     * form always reflects the selected day.
     */
    fun setDate(date: LocalDate) {
        _uiState.update { state ->
            val end = if (state.endDate != null && date.isAfter(state.endDate)) null else state.endDate
            state.copy(date = date, startDate = date, endDate = end, hasChanges = true)
        }
        viewModelScope.launch {
            resolveContinuationContext(date, _uiState.value.toleranceDays)
            loadSystemCategoryNames()
            loadPinnedCategories()
        }
    }

    /** Moves the episode start (editing only). */
    fun setStartDate(date: LocalDate) = _uiState.update { state ->
        val end = if (state.endDate != null && date.isAfter(state.endDate)) null else state.endDate
        state.copy(
            startDate = date,
            endDate = end,
            episodeDayNumber = dayNumber(date, state.date),
            hasChanges = true,
        )
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
     * Adds [name] as a new option in the symptoms catalog and selects it for this day.
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

    /**
     * Saves the day: marks [LogPeriodUiState.date] as a period day (which
     * starts, continues, or bridges an episode as needed), applies any
     * episode-boundary edits, and writes this day's flow/symptoms/pinned
     * values to the per-day tracking logs.
     */
    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val tolerance = state.toleranceDays
                val episode: PeriodEntry? = if (state.isEditing && state.existingId != null) {
                    repository.logPeriodDay(state.date, tolerance)
                    repository.updateEpisode(
                        id = state.existingId,
                        start = state.startDate,
                        end = state.endDate,
                        notes = state.notes,
                        toleranceDays = tolerance,
                    )
                } else if (state.endDate != null && !state.endDate.isBefore(state.date)) {
                    repository.logPeriodRange(state.date, state.endDate, tolerance)
                } else {
                    repository.logPeriodDay(state.date, tolerance)
                }

                if (episode != null) {
                    repository.updateEpisodeMeta(
                        id = episode.id,
                        notes = state.notes,
                        flowLevel = state.selectedFlowLabel,
                    )
                }

                syncFlowToTrackingLog(state)
                syncSymptomsToTrackingLog(state)
                syncPinnedCategoryLogs(state)
                application?.let { GoFloWidget.updateAllWidgets(it) }
                // Saving a period day changes the cycle predictions, so the pre-period,
                // ovulation, and daily reminders must be re-armed against the new dates.
                // Failure must not report the (already successful) save as failed.
                application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not save entry. Please try again.") }
            }
        }
    }

    /**
     * Removes the edited day from the period without touching the day's own
     * tracking logs — a flow or symptom logged on a day that turns out not to
     * be a period day is still a valid, dated record.
     */
    fun removeDay() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                repository.unlogPeriodDay(state.date, state.toleranceDays)
                application?.let { GoFloWidget.updateAllWidgets(it) }
                application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
                _uiState.update { it.copy(deleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not remove this day. Please try again.") }
            }
        }
    }

    /**
     * Mirrors this day's flow level into the TrackingLog system.
     * This ensures logged days appear in the Stats screen under the Flow category.
     * No-op if [trackingRepository] was not provided (e.g. in tests or legacy callers).
     */
    private suspend fun syncFlowToTrackingLog(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val flowCategory = tr.getSystemCategoryByKey("flow") ?: return
        if (flowCategory.isArchived) return
        val flowLabel = if (flowCategory.categoryType == "numeric_slider") {
            val v = state.flowSliderValue ?: flowLabelToSliderValue(state.selectedFlowLabel)
            v.toInt().toString()
        } else {
            state.selectedFlowLabel
        }
        tr.saveLog(
            date           = state.date,
            categoryId     = flowCategory.id,
            selectedValues = setOf(flowLabel),
            notes          = "",
            allowMultiple  = false,
        )
    }

    private suspend fun syncSymptomsToTrackingLog(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val symptomsCategory = tr.getSystemCategoryByKey("symptoms") ?: return
        if (symptomsCategory.isArchived) return
        if (state.symptoms.isEmpty()) {
            val existing = tr.getExistingLog(state.date, symptomsCategory.id) ?: return
            tr.deleteLog(existing.log)
        } else {
            val loggedAt = if (symptomsCategory.trackAgainstTime) {
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else ""
            tr.saveLog(
                date           = state.date,
                categoryId     = symptomsCategory.id,
                selectedValues = state.symptoms,
                notes          = "",
                allowMultiple  = false,
                loggedAt       = loggedAt,
            )
        }
    }

    /** Saves each pinned category's current selection as a tracking log for the day being logged. */
    private suspend fun syncPinnedCategoryLogs(state: LogPeriodUiState) {
        val tr = trackingRepository ?: return
        val date = state.date
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
                // Fall back to numericMin so the slider's displayed position is always saved.
                val v = state.pinnedNumericValues[cat.id] ?: cat.numericMin
                setOf(if (cat.allowDecimals) "%.1f".format(v) else v.toInt().toString())
            }
            "numeric_free" -> {
                val text = (state.pinnedFreeTextValues[cat.id] ?: "").trim()
                if (text.isEmpty()) null else setOf(text)
            }
            "increment" -> {
                // Always save, including 0 — a zero count is meaningful data for a
                // category the user chose to track alongside periods.
                val count = state.pinnedNumericValues[cat.id]?.toInt() ?: 0
                setOf(count.toString())
            }
            else -> {
                val selected = state.pinnedCategorySelections[cat.id] ?: emptySet()
                if (selected.isEmpty()) null else selected
            }
        }

    fun disablePeriodTracking() {
        viewModelScope.launch { preferencesStore?.setPeriodTrackingEnabled(false) }
    }

    /** Deletes the entire episode: its days, its row, and its per-day logs. */
    fun delete() {
        val state = _uiState.value
        val id = state.existingId ?: return
        viewModelScope.launch {
            try {
                val period = repository.getPeriodById(id).first() ?: return@launch
                val days = repository.getDaysForEpisode(period, state.toleranceDays)
                trackingRepository?.deleteLogsForPeriod(
                    LocalDate.parse(period.startDate),
                    period.endDate?.let { LocalDate.parse(it) }
                        ?: days.lastOrNull()?.let { LocalDate.parse(it) }
                )
                repository.deletePeriod(period, state.toleranceDays)
                application?.let { GoFloWidget.updateAllWidgets(it) }
                application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
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

        /** 1-based day number of [date] within an episode starting at [start], or null when before it. */
        private fun dayNumber(start: LocalDate, date: LocalDate): Int? {
            val n = ChronoUnit.DAYS.between(start, date).toInt() + 1
            return if (n >= 1) n else null
        }
    }
}
