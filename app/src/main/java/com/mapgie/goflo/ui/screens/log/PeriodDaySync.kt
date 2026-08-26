package com.mapgie.goflo.ui.screens.log

import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.repository.TrackingRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Period-day logic shared between [LogPeriodViewModel] (the standalone period
 * screen) and [LogViewModel] (the unified day screen).
 *
 * Extracted rather than duplicated so the flow slider mapping and the save
 * fan-out into the tracking system (which make period data appear under
 * Flow/Symptoms/pinned categories in Stats) cannot drift between the two
 * surfaces. Behaviour is byte-for-byte the pre-extraction LogPeriodViewModel
 * logic.
 */
internal object PeriodDaySync {

    /**
     * Maps a flow slider position to the built-in label stored for the day:
     * 1 = Spotting, 2 = Light, 4 = Heavy, anything else = Medium.
     */
    fun flowLabelForSliderValue(value: Int): String = when (value) {
        1    -> "Spotting"
        2    -> "Light"
        4    -> "Heavy"
        else -> "Medium"
    }

    /** Inverse mapping; "Medium" and any custom label default to the middle. */
    fun flowLabelToSliderValue(label: String): Float = when (label) {
        "Spotting" -> 1f
        "Light"    -> 2f
        "Heavy"    -> 4f
        else       -> 3f
    }

    /** 1-based day number of [date] within an episode starting at [start], or null when before it. */
    fun dayNumber(start: LocalDate, date: LocalDate): Int? {
        val n = ChronoUnit.DAYS.between(start, date).toInt() + 1
        return if (n >= 1) n else null
    }

    /**
     * Mirrors the day's flow level into the TrackingLog system so logged days
     * appear in the Stats screen under the Flow category.
     * No-op if [trackingRepository] is null (e.g. in tests or legacy callers).
     */
    suspend fun syncFlowToTrackingLog(
        trackingRepository: TrackingRepository?,
        date: LocalDate,
        selectedFlowLabel: String,
        flowSliderValue: Float?,
    ) {
        val tr = trackingRepository ?: return
        val flowCategory = tr.getSystemCategoryByKey("flow") ?: return
        if (flowCategory.isArchived) return
        val flowLabel = if (flowCategory.categoryType == "numeric_slider") {
            val v = flowSliderValue ?: flowLabelToSliderValue(selectedFlowLabel)
            v.toInt().toString()
        } else {
            selectedFlowLabel
        }
        tr.saveLog(
            date           = date,
            categoryId     = flowCategory.id,
            selectedValues = setOf(flowLabel),
            notes          = "",
            allowMultiple  = false,
        )
    }

    /**
     * Mirrors the day's symptom set into the TrackingLog system. An empty set
     * deletes the day's existing symptoms log (deselecting everything clears
     * the record rather than leaving a stale one).
     */
    suspend fun syncSymptomsToTrackingLog(
        trackingRepository: TrackingRepository?,
        date: LocalDate,
        symptoms: Set<String>,
    ) {
        val tr = trackingRepository ?: return
        val symptomsCategory = tr.getSystemCategoryByKey("symptoms") ?: return
        if (symptomsCategory.isArchived) return
        if (symptoms.isEmpty()) {
            val existing = tr.getExistingLog(date, symptomsCategory.id) ?: return
            tr.deleteLog(existing.log)
        } else {
            val loggedAt = if (symptomsCategory.trackAgainstTime) {
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else ""
            tr.saveLog(
                date           = date,
                categoryId     = symptomsCategory.id,
                selectedValues = symptoms,
                notes          = "",
                allowMultiple  = false,
                loggedAt       = loggedAt,
            )
        }
    }

    /**
     * The value set a pinned ("Log with period") category saves for the day,
     * or null when there is nothing to record:
     * - slider: falls back to numericMin so the displayed position always saves
     * - free numeric: skipped while empty
     * - count: always saves, including 0 (a zero count is meaningful data for a
     *   category the user chose to track alongside periods)
     * - everything else: the selection set, skipped while empty
     */
    fun computePinnedValues(
        cat: TrackingCategory,
        numericValue: Float?,
        freeText: String,
        selections: Set<String>,
    ): Set<String>? = when (cat.categoryType) {
        "numeric_slider" -> {
            val v = numericValue ?: cat.numericMin
            setOf(if (cat.allowDecimals) "%.1f".format(v) else v.toInt().toString())
        }
        "numeric_free" -> {
            val text = freeText.trim()
            if (text.isEmpty()) null else setOf(text)
        }
        "increment" -> {
            val count = numericValue?.toInt() ?: 0
            setOf(count.toString())
        }
        else -> {
            if (selections.isEmpty()) null else selections
        }
    }
}
