package com.mapgie.goflo.ui.screens.log

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.Group
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.database.entities.TrackingValue
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingLogWithValues
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.notifications.ReminderScheduler
import com.mapgie.goflo.widget.GoFloWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The per-day state of one tracked category on the unified day screen.
 *
 * Mirrors the fields LogCategoryViewModel keeps for its single category, held
 * once per category here. [touched] records whether the user changed anything
 * this session: untouched entries are skipped on save, so the screen neither
 * fabricates logs for ignored categories nor rewrites stored entries (which
 * would re-stamp or clear their recorded time). Pinned categories are the
 * exception while the day is on-period: they keep the period screen's
 * always-save fan-out semantics.
 */
data class DayMetricEntry(
    val selectedValues: Set<String> = emptySet(),
    /** Slider position or running count, per the category type. */
    val numericValue: Float? = null,
    /** Text entry for numeric_free categories. */
    val freeText: String = "",
    /** Per-log notes (500-char cap enforced by the screen). */
    val notes: String = "",
    /** Whether to stamp the save with the current time (pre-set from trackAgainstTime). */
    val trackTime: Boolean = false,
    val existingLog: TrackingLog? = null,
    /** Timed entries already logged this day (increment + trackAgainstTime only). */
    val timedEntries: List<TrackingLogWithValues> = emptyList(),
    val touched: Boolean = false,
)

/**
 * UI state for the unified day screen: one day, with an active period as a
 * state of that day rather than a separate destination.
 */
data class LogUiState(
    val isLoading: Boolean = true,
    /** The day being logged or edited. */
    val date: LocalDate = LocalDate.now(),
    val toleranceDays: Int = PeriodRepository.DEFAULT_GAP_TOLERANCE_DAYS,
    val periodTrackingEnabled: Boolean = true,

    // ── Period state of the day ───────────────────────────────────────────────
    /** The episode covering (or within tolerance reach of) [date], if any. */
    val episodeId: Long? = null,
    val episodeStart: LocalDate? = null,
    /** Editable explicit episode end ("until"), null = open. */
    val endDate: LocalDate? = null,
    /** The end date as loaded, so the End action can be undone before saving. */
    val loadedEndDate: LocalDate? = null,
    /** True when [date] itself is a logged period day. */
    val isPeriodDay: Boolean = false,
    /** True when [date] falls inside its episode's start..end span. */
    val dayInEpisode: Boolean = false,
    /**
     * Start of the episode [date] would continue (within gap tolerance) when
     * the day is not itself on-period; null when logging would start fresh.
     */
    val continuesEpisodeStart: LocalDate? = null,
    /** 1-based day number of [date] within its episode, when known. */
    val episodeDayNumber: Int? = null,
    /** The user tapped "Period started today"; applied on save. */
    val startPeriodToday: Boolean = false,
    /** Episode-level notes (the period screen's Notes field). */
    val periodNotes: String = "",

    // ── Flow (rendered only while the day is on-period) ──────────────────────
    val flowCategory: TrackingCategory? = null,
    val flowCategoryName: String = "Flow",
    val flowOptions: List<TrackingValue> = emptyList(),
    val selectedFlowLabel: String = "Medium",
    val flowSliderValue: Float? = null,

    // ── Symptoms ─────────────────────────────────────────────────────────────
    val symptomsCategory: TrackingCategory? = null,
    val symptomsCategoryName: String = "Symptoms",
    val symptomOptions: List<TrackingValue> = emptyList(),
    val symptoms: Set<String> = emptySet(),
    val symptomsTouched: Boolean = false,

    // ── Tracked categories (active, non-system) ──────────────────────────────
    val groups: List<Group> = emptyList(),
    val categories: List<TrackingCategory> = emptyList(),
    /** Catalog value labels per category id. */
    val categoryValues: Map<Long, List<String>> = emptyMap(),
    /** Per-category day entries, keyed by category id. */
    val entries: Map<Long, DayMetricEntry> = emptyMap(),

    // ── Screen state ─────────────────────────────────────────────────────────
    /** The category whose input is currently expanded from a grouped card row. */
    val activeCategoryId: Long? = null,
    val hasChanges: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
) {
    /** Whether the day renders in its on-period arrangement. */
    val periodActive: Boolean
        get() = isPeriodDay || dayInEpisode || startPeriodToday
}

/**
 * ViewModel for the unified `LogScreen(date)`.
 *
 * Period behaviour (episode continuation, the flow slider mapping, the save
 * fan-out into the tracking system, widget and reminder refreshes) delegates
 * to [PeriodDaySync] and [PeriodRepository], the same code paths
 * [LogPeriodViewModel] uses, so the two surfaces cannot drift. Generic
 * category behaviour mirrors [LogCategoryViewModel]'s save rules per entry.
 */
class LogViewModel(
    private val repository: PeriodRepository,
    private val trackingRepository: TrackingRepository,
    private val initialDate: LocalDate,
    private val application: Application? = null,
    private val preferencesStore: AppPreferencesStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState(date = initialDate))
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private var optionSubscriptionsStarted = false

    init {
        viewModelScope.launch {
            val prefs = preferencesStore?.preferences?.first()
            _uiState.update {
                it.copy(
                    toleranceDays = prefs?.periodGapToleranceDays
                        ?: PeriodRepository.DEFAULT_GAP_TOLERANCE_DAYS,
                    periodTrackingEnabled = prefs?.periodTrackingEnabled ?: true,
                )
            }
            loadDay(initialDate)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    /**
     * Loads (or reloads) everything the screen shows for [date]: the period
     * context, the day's stored flow/symptom values, and one [DayMetricEntry]
     * per active non-system category. Leaves the form pristine
     * (hasChanges = false) because after a load it matches the stored state.
     */
    private suspend fun loadDay(date: LocalDate) {
        val tolerance = _uiState.value.toleranceDays

        // Period context: episode covering or within tolerance reach of the day.
        val periods = repository.getAllPeriodsOnce()
        val isPeriodDay = repository.isPeriodDay(date)
        val episode = PeriodRepository.periodForDate(periods, date, tolerance)
        val epStart = episode?.let { LocalDate.parse(it.startDate) }
        val epEndStored = episode?.endDate?.let { LocalDate.parse(it) }
        val dayInEpisode = epStart != null &&
            !date.isBefore(epStart) &&
            (epEndStored == null || !date.isAfter(epEndStored))
        // Opening a day just past the stored end (within tolerance) extends the
        // end to that day, so saving continues the period instead of trimming
        // the new day away — same rule as LogPeriodViewModel's init.
        val effectiveEnd =
            if (epEndStored != null && date.isAfter(epEndStored)) date else epEndStored

        // Flow + symptoms stored values for this day.
        val flowCat = trackingRepository.getSystemCategoryByKey("flow")
        val symptomsCat = trackingRepository.getSystemCategoryByKey("symptoms")
        var editFlowLabel: String? = null
        var editFlowSlider: Float? = null
        if (flowCat != null) {
            val raw = trackingRepository.getExistingLog(date, flowCat.id)?.values?.firstOrNull()
            if (raw != null) {
                if (flowCat.categoryType == "numeric_slider") {
                    editFlowSlider = raw.toFloatOrNull()
                    editFlowLabel = PeriodDaySync.flowLabelForSliderValue(editFlowSlider?.toInt() ?: 3)
                } else {
                    editFlowLabel = raw
                }
            }
        }
        val editSymptoms = symptomsCat?.let {
            trackingRepository.getExistingLog(date, it.id)?.values?.toSet()
        }
        val selectedFlow = editFlowLabel ?: "Medium"
        val sliderValue = editFlowSlider ?: if (flowCat?.categoryType == "numeric_slider") {
            PeriodDaySync.flowLabelToSliderValue(selectedFlow)
        } else null

        // Tracked categories, their groups, catalogs, and this day's entries.
        val groups = trackingRepository.getAllGroupsOnce()
        val categories = trackingRepository.getActiveCategories().first().filter { !it.isSystem }
        val valuesMap = mutableMapOf<Long, List<String>>()
        val entriesMap = mutableMapOf<Long, DayMetricEntry>()
        for (cat in categories) {
            valuesMap[cat.id] = trackingRepository.getValuesForCategoryOnce(cat.id).map { it.label }
            entriesMap[cat.id] = loadEntry(cat, date)
        }

        _uiState.update { state ->
            state.copy(
                date = date,
                episodeId = episode?.id,
                episodeStart = epStart,
                endDate = effectiveEnd,
                loadedEndDate = effectiveEnd,
                isPeriodDay = isPeriodDay,
                dayInEpisode = dayInEpisode,
                continuesEpisodeStart =
                    if (epStart != null && !isPeriodDay && !dayInEpisode) epStart else null,
                episodeDayNumber = epStart?.let { PeriodDaySync.dayNumber(minOf(it, date), date) },
                startPeriodToday = false,
                periodNotes = episode?.notes ?: "",
                flowCategory = flowCat,
                flowCategoryName = flowCat?.name ?: state.flowCategoryName,
                selectedFlowLabel = selectedFlow,
                flowSliderValue = sliderValue,
                symptomsCategory = symptomsCat,
                symptomsCategoryName = symptomsCat?.name ?: state.symptomsCategoryName,
                symptoms = editSymptoms ?: emptySet(),
                symptomsTouched = false,
                groups = groups,
                categories = categories,
                categoryValues = valuesMap,
                entries = entriesMap,
                activeCategoryId = null,
                hasChanges = false,
            )
        }

        // Keep the flow/symptom option chips live after catalog edits
        // (e.g. the inline Add Symptom dialog).
        if (!optionSubscriptionsStarted) {
            optionSubscriptionsStarted = true
            if (flowCat != null) {
                viewModelScope.launch {
                    trackingRepository.getValuesForCategory(flowCat.id).collect { values ->
                        _uiState.update { it.copy(flowOptions = values) }
                    }
                }
            }
            if (symptomsCat != null) {
                viewModelScope.launch {
                    trackingRepository.getValuesForCategory(symptomsCat.id).collect { values ->
                        _uiState.update { it.copy(symptomOptions = values) }
                    }
                }
            }
        }
    }

    /** Loads one category's stored entry for [date] into a [DayMetricEntry]. */
    private suspend fun loadEntry(cat: TrackingCategory, date: LocalDate): DayMetricEntry {
        val timed = cat.categoryType == "increment" && cat.trackAgainstTime
        val timedEntries =
            if (timed) trackingRepository.getLogsForDateAndCategory(date, cat.id) else emptyList()
        // allowMultiple categories always start a fresh entry, matching
        // LogCategoryViewModel's new-entry behaviour.
        val existing = if (timed || cat.allowMultiple) null
            else trackingRepository.getExistingLog(date, cat.id)
        val numeric =
            if (cat.categoryType == "numeric_slider" || cat.categoryType == "increment")
                existing?.values?.firstOrNull()?.toFloatOrNull()
            else null
        val freeText = if (cat.categoryType == "numeric_free")
            existing?.values?.firstOrNull() ?: "" else ""
        return DayMetricEntry(
            selectedValues = existing?.values?.toSet() ?: emptySet(),
            numericValue = numeric,
            freeText = freeText,
            notes = existing?.log?.notes ?: "",
            trackTime = cat.trackAgainstTime,
            existingLog = existing?.log,
            timedEntries = timedEntries,
        )
    }

    private suspend fun reloadEntry(categoryId: Long) {
        val state = _uiState.value
        val cat = state.categories.firstOrNull { it.id == categoryId } ?: return
        val fresh = loadEntry(cat, state.date)
        _uiState.update { it.copy(entries = it.entries + (categoryId to fresh)) }
    }

    // ── Day switching ─────────────────────────────────────────────────────────

    /**
     * Changes the day being shown and reloads every section's stored values
     * for it, so the form always reflects the selected day (the same rule as
     * LogCategoryViewModel.setDate). Unsaved edits are guarded by the screen
     * before this is called.
     */
    fun setDate(newDate: LocalDate) {
        if (newDate == _uiState.value.date) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadDay(newDate)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── Period actions ────────────────────────────────────────────────────────

    /** Marks the day to be logged as a period day (starting or continuing one) on save. */
    fun startPeriodToday() = _uiState.update {
        it.copy(startPeriodToday = true, hasChanges = true)
    }

    fun undoStartPeriod() = _uiState.update {
        it.copy(startPeriodToday = false, hasChanges = true)
    }

    /** Moves the episode start (existing episodes only). */
    fun setStartDate(date: LocalDate) = _uiState.update { state ->
        if (state.episodeId == null) return@update state
        val end = if (state.endDate != null && date.isAfter(state.endDate)) null else state.endDate
        state.copy(
            episodeStart = date,
            endDate = end,
            episodeDayNumber = PeriodDaySync.dayNumber(date, state.date),
            hasChanges = true,
        )
    }

    fun setEndDate(date: LocalDate?) = _uiState.update {
        it.copy(endDate = date, hasChanges = true)
    }

    /** The footer's End action: close the period on the day being logged. */
    fun endPeriodOnThisDay() = _uiState.update {
        it.copy(endDate = it.date, hasChanges = true)
    }

    /** Undoes the End action, restoring the end date as loaded. */
    fun undoEndPeriod() = _uiState.update {
        it.copy(endDate = it.loadedEndDate, hasChanges = true)
    }

    fun setFlowLevel(label: String) = _uiState.update {
        it.copy(selectedFlowLabel = label, hasChanges = true)
    }

    fun setFlowSliderValue(value: Float) = _uiState.update { state ->
        state.copy(
            flowSliderValue = value,
            selectedFlowLabel = PeriodDaySync.flowLabelForSliderValue(value.toInt()),
            hasChanges = true,
        )
    }

    fun toggleSymptom(label: String) = _uiState.update { state ->
        val updated = if (label in state.symptoms) state.symptoms - label else state.symptoms + label
        state.copy(symptoms = updated, symptomsTouched = true, hasChanges = true)
    }

    /**
     * Adds [name] as a new option in the symptoms catalog and selects it for
     * this day. The catalog insert is fire-and-forget; the selection is
     * immediate. Same behaviour as the period screen's Add Symptom dialog.
     */
    fun addNewSymptomToLibrary(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val sympCat = trackingRepository.getSystemCategoryByKey("symptoms") ?: return@launch
            trackingRepository.addValueToCategory(sympCat.id, trimmed)
        }
        _uiState.update { state ->
            state.copy(symptoms = state.symptoms + trimmed, symptomsTouched = true, hasChanges = true)
        }
    }

    fun setPeriodNotes(notes: String) = _uiState.update {
        it.copy(periodNotes = notes, hasChanges = true)
    }

    fun disablePeriodTracking() {
        viewModelScope.launch { preferencesStore?.setPeriodTrackingEnabled(false) }
    }

    // ── Category entry mutators ───────────────────────────────────────────────

    private fun updateEntry(categoryId: Long, transform: (DayMetricEntry) -> DayMetricEntry) =
        _uiState.update { state ->
            val entry = state.entries[categoryId] ?: DayMetricEntry()
            state.copy(
                entries = state.entries + (categoryId to transform(entry)),
                hasChanges = true,
            )
        }

    fun toggleEntryValue(categoryId: Long, label: String) = updateEntry(categoryId) { entry ->
        val selected = if (label in entry.selectedValues) entry.selectedValues - label
            else entry.selectedValues + label
        entry.copy(selectedValues = selected, touched = true)
    }

    /** Replaces the whole selection set (chip sets, and yes_no/time single labels). */
    fun setEntrySelection(categoryId: Long, values: Set<String>) = updateEntry(categoryId) {
        it.copy(selectedValues = values, touched = true)
    }

    fun setEntryNumeric(categoryId: Long, value: Float) = updateEntry(categoryId) {
        it.copy(numericValue = value, touched = true)
    }

    fun setEntryFreeText(categoryId: Long, text: String) = updateEntry(categoryId) {
        it.copy(freeText = text, touched = true)
    }

    fun setEntryNotes(categoryId: Long, notes: String) = updateEntry(categoryId) {
        it.copy(notes = notes, touched = true)
    }

    fun setEntryTrackTime(categoryId: Long, track: Boolean) = updateEntry(categoryId) {
        it.copy(trackTime = track, touched = true)
    }

    fun setActiveCategory(categoryId: Long?) = _uiState.update {
        it.copy(activeCategoryId = categoryId)
    }

    /** Deletes a category's existing log for this day and reloads its entry. */
    fun deleteEntry(categoryId: Long) {
        val log = _uiState.value.entries[categoryId]?.existingLog ?: return
        viewModelScope.launch {
            runCatching {
                trackingRepository.deleteLog(log)
                reloadEntry(categoryId)
            }.onFailure {
                _uiState.update { s -> s.copy(error = "Could not delete the entry. Please try again.") }
            }
        }
    }

    /** Adds a new time-stamped increment entry immediately (increment + trackAgainstTime). */
    fun addTimedIncrement(categoryId: Long) {
        val state = _uiState.value
        if (state.isLoading) return
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        viewModelScope.launch {
            runCatching {
                trackingRepository.saveLog(
                    date           = state.date,
                    categoryId     = categoryId,
                    selectedValues = setOf("1"),
                    notes          = "",
                    allowMultiple  = true,
                    loggedAt       = time,
                )
                reloadEntry(categoryId)
            }.onFailure {
                _uiState.update { s -> s.copy(error = "Could not log the entry. Please try again.") }
            }
        }
    }

    /** Deletes a specific timed entry (increment + trackAgainstTime undo). */
    fun deleteTimedEntry(categoryId: Long, log: TrackingLog) {
        viewModelScope.launch {
            runCatching {
                trackingRepository.deleteLog(log)
                reloadEntry(categoryId)
            }.onFailure {
                _uiState.update { s -> s.copy(error = "Could not delete the entry. Please try again.") }
            }
        }
    }

    // ── Re-filing (the header switcher) ──────────────────────────────────────

    /**
     * Re-files the value entered under [fromId] to the category [toId]: the
     * entered (unsaved) value is carried over, serialised through the same
     * rules a save would use, and the source entry reverts to its stored
     * state. Switching only changes what the entry is filed under; nothing is
     * saved until Save. When the source has no unsaved edits this is a plain
     * focus switch.
     */
    fun refileEntry(fromId: Long, toId: Long) {
        val state = _uiState.value
        if (fromId == toId) {
            _uiState.update { it.copy(activeCategoryId = toId) }
            return
        }
        val fromCat = state.categories.firstOrNull { it.id == fromId }
        val toCat = state.categories.firstOrNull { it.id == toId }
        val fromEntry = state.entries[fromId]
        if (fromCat == null || toCat == null || fromEntry == null || !fromEntry.touched) {
            _uiState.update { it.copy(activeCategoryId = toId) }
            return
        }
        val labels = serialisedValues(fromCat, fromEntry)
        viewModelScope.launch {
            val reset = loadEntry(fromCat, state.date)
            _uiState.update { s ->
                val target = s.entries[toId] ?: DayMetricEntry(trackTime = toCat.trackAgainstTime)
                val refiled = if (labels.isNullOrEmpty()) target else hydrateEntry(toCat, labels, target)
                s.copy(
                    entries = s.entries + (fromId to reset) + (toId to refiled),
                    activeCategoryId = toId,
                    hasChanges = true,
                )
            }
        }
    }

    /** Serialises an entry's current value to the labels a save would store. */
    private fun serialisedValues(cat: TrackingCategory, entry: DayMetricEntry): Set<String>? =
        when (cat.categoryType) {
            "numeric_slider" -> entry.numericValue?.let {
                setOf(formatNumericValue(it, cat.allowDecimals))
            }
            "numeric_free" -> entry.freeText.trim().takeIf { it.isNotEmpty() }?.let { setOf(it) }
            "increment" -> entry.numericValue?.toInt()?.takeIf { it > 0 }?.let { setOf(it.toString()) }
            else -> entry.selectedValues.takeIf { it.isNotEmpty() }
        }

    /** Hydrates stored-shape labels into the state fields [cat]'s input reads. */
    private fun hydrateEntry(
        cat: TrackingCategory,
        labels: Set<String>,
        base: DayMetricEntry,
    ): DayMetricEntry = when (cat.categoryType) {
        "numeric_slider", "increment" ->
            base.copy(numericValue = labels.firstOrNull()?.toFloatOrNull(), touched = true)
        "numeric_free" ->
            base.copy(freeText = labels.firstOrNull() ?: "", touched = true)
        else ->
            base.copy(selectedValues = labels, touched = true)
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    /**
     * Saves the day. When the day is on-period (or being started), the period
     * path mirrors LogPeriodViewModel.save(): mark the day, apply episode
     * boundary edits, write episode meta, then fan the day's flow out to the
     * tracking system, and refresh widgets and prediction reminders. Symptoms
     * and every tracked category then save through the shared per-day rules.
     */
    fun save() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            try {
                val tolerance = state.toleranceDays
                val periodSave = state.periodActive
                if (periodSave) {
                    val episode: PeriodEntry? = if (state.episodeId != null) {
                        repository.logPeriodDay(state.date, tolerance)
                        repository.updateEpisode(
                            id = state.episodeId,
                            start = state.episodeStart ?: state.date,
                            end = state.endDate,
                            notes = state.periodNotes,
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
                            notes = state.periodNotes,
                            flowLevel = state.selectedFlowLabel,
                        )
                    }
                    PeriodDaySync.syncFlowToTrackingLog(
                        trackingRepository, state.date, state.selectedFlowLabel, state.flowSliderValue,
                    )
                }

                // Symptoms: period saves always mirror the set (parity with the
                // period screen, where an emptied set deletes the day's log);
                // otherwise only when the user touched them, so an off-period
                // save never rewrites an untouched symptoms log.
                if (periodSave || state.symptomsTouched) {
                    PeriodDaySync.syncSymptomsToTrackingLog(
                        trackingRepository, state.date, state.symptoms,
                    )
                }

                saveCategoryEntries(state, periodSave)

                if (periodSave) {
                    application?.let { GoFloWidget.updateAllWidgets(it) }
                    // Saving a period day changes the cycle predictions; failure
                    // must not report the (already successful) save as failed.
                    application?.let { runCatching { ReminderScheduler.refreshPredictionReminders(it) } }
                }
                _uiState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not save entry. Please try again.") }
            }
        }
    }

    private suspend fun saveCategoryEntries(state: LogUiState, periodSave: Boolean) {
        for (cat in state.categories) {
            // Timed increments save per tap; never through the day save.
            if (cat.categoryType == "increment" && cat.trackAgainstTime) continue
            val entry = state.entries[cat.id] ?: continue
            val pinnedContext = periodSave && cat.showInLogPeriod
            val values: Set<String>? = if (pinnedContext) {
                // Exact parity with the period screen's pinned fan-out
                // (slider falls back to min, count saves including 0).
                PeriodDaySync.computePinnedValues(
                    cat, entry.numericValue, entry.freeText, entry.selectedValues,
                )
            } else {
                // Only touched entries save: an ignored category must neither
                // gain a fabricated log nor have its stored entry rewritten
                // (rewriting would re-stamp or clear its recorded time).
                if (!entry.touched) null
                else entryValuesToSave(cat, entry)
            }
            if (values == null) continue
            val loggedAt = if (entry.trackTime) {
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else ""
            val existing = entry.existingLog
            if (existing != null) {
                trackingRepository.updateLogInPlace(existing, values, entry.notes, loggedAt)
            } else {
                trackingRepository.saveLog(
                    date           = state.date,
                    categoryId     = cat.id,
                    selectedValues = values,
                    notes          = entry.notes,
                    // The period screen's pinned fan-out always upserts the
                    // day's single log (allowMultiple forced off) — keep that,
                    // so repeated period-day saves never stack duplicates.
                    allowMultiple  = if (pinnedContext) false else cat.allowMultiple,
                    loggedAt       = loggedAt,
                )
            }
        }
    }

    /**
     * LogCategoryViewModel.save()'s per-type rules, applied per entry: an
     * unset slider falls back to its displayed minimum, empty free numeric
     * input and a count of zero or less record nothing (the old screen blocks
     * those saves; here the category is skipped and any existing log is left
     * untouched), and label types persist the selection set.
     */
    private fun entryValuesToSave(cat: TrackingCategory, entry: DayMetricEntry): Set<String>? =
        when (cat.categoryType) {
            "numeric_slider" -> {
                val v = entry.numericValue ?: cat.numericMin
                setOf(formatNumericValue(v, cat.allowDecimals))
            }
            "numeric_free" -> {
                val text = entry.freeText.trim()
                if (text.isEmpty()) null else setOf(text)
            }
            "increment" -> {
                val count = entry.numericValue?.toInt() ?: 0
                if (count <= 0) null else setOf(count.toString())
            }
            // default chips, yes_no ("Yes"/"No") and time ("HH:mm") persist
            // their labels straight from the selection set.
            else -> entry.selectedValues
        }

    private fun formatNumericValue(v: Float, allowDecimals: Boolean): String =
        if (allowDecimals) "%.1f".format(v) else v.toInt().toString()

    // ── Period day removal and episode deletion ───────────────────────────────

    /**
     * Removes this day from the period without touching the day's own tracking
     * logs — a flow or symptom logged on a day that turns out not to be a
     * period day is still a valid, dated record.
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

    /** Deletes the entire episode: its days, its row, and its per-day logs. */
    fun deleteEpisode() {
        val state = _uiState.value
        val id = state.episodeId ?: return
        viewModelScope.launch {
            try {
                val period = repository.getPeriodById(id).first() ?: return@launch
                val days = repository.getDaysForEpisode(period, state.toleranceDays)
                trackingRepository.deleteLogsForPeriod(
                    LocalDate.parse(period.startDate),
                    period.endDate?.let { LocalDate.parse(it) }
                        ?: days.lastOrNull()?.let { LocalDate.parse(it) },
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

    fun clearError() = _uiState.update { it.copy(error = null) }

    class Factory(
        private val repository: PeriodRepository,
        private val trackingRepository: TrackingRepository,
        private val date: LocalDate,
        private val application: Application? = null,
        private val preferencesStore: AppPreferencesStore? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LogViewModel(repository, trackingRepository, date, application, preferencesStore) as T
        }
    }
}
