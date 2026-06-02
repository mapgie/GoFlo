package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.dao.TrackingCategoryDao
import com.mapgie.goflo.data.database.dao.TrackingLogDao
import com.mapgie.goflo.data.database.dao.ValueCount
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.database.entities.TrackingLogValue
import com.mapgie.goflo.data.database.entities.TrackingValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Pairs a [TrackingLog] with its selected value labels for display. */
data class TrackingLogWithValues(
    val log: TrackingLog,
    val category: TrackingCategory?,
    val values: List<String>
)

class TrackingRepository(
    private val categoryDao: TrackingCategoryDao,
    private val logDao: TrackingLogDao,
    private val symptomDao: SymptomDao? = null,
) {

    // ── Categories ────────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<TrackingCategory>> =
        categoryDao.getAllCategories()

    fun getActiveCategories(): Flow<List<TrackingCategory>> =
        categoryDao.getActiveCategories()

    suspend fun getAllCategoriesOnce(): List<TrackingCategory> =
        categoryDao.getAllCategoriesOnce()

    fun getCategoryById(id: Long): Flow<TrackingCategory?> =
        categoryDao.getCategoryById(id)

    suspend fun getCategoryByIdOnce(id: Long): TrackingCategory? =
        categoryDao.getCategoryByIdOnce(id)

    fun getValuesForCategory(categoryId: Long): Flow<List<TrackingValue>> =
        categoryDao.getValuesForCategory(categoryId)

    suspend fun getValuesForCategoryOnce(categoryId: Long): List<TrackingValue> =
        categoryDao.getValuesForCategoryOnce(categoryId)

    suspend fun addCategory(
        name: String,
        iconName: String = "category",
        colorToken: String = "secondary",
        categoryType: String = "default",
        numericMin: Float = 0f,
        numericMax: Float = 10f,
        allowDecimals: Boolean = false,
        numericUnit: String = "",
        allowMultiple: Boolean = false,
        showInLogPeriod: Boolean = false,
        trackAgainstTime: Boolean = false,
    ): Long {
        val maxOrder = categoryDao.getAllCategories().first()
            .maxOfOrNull { it.displayOrder } ?: -1
        return categoryDao.insertCategory(
            TrackingCategory(
                name             = name.trim(),
                displayOrder     = maxOrder + 1,
                iconName         = iconName,
                colorToken       = colorToken,
                categoryType     = categoryType,
                numericMin       = numericMin,
                numericMax       = numericMax,
                allowDecimals    = allowDecimals,
                numericUnit      = numericUnit,
                allowMultiple    = allowMultiple,
                showInLogPeriod  = showInLogPeriod,
                trackAgainstTime = trackAgainstTime,
            )
        )
    }

    suspend fun renameCategory(id: Long, newName: String) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(name = newName.trim()))
    }

    /** Updates icon and colour only — name and type are immutable from this path. */
    suspend fun updateCategoryAppearance(id: Long, iconName: String, colorToken: String) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(iconName = iconName, colorToken = colorToken))
    }

    /**
     * Updates name, appearance, and numeric settings of an existing category in one call.
     * Used by the edit dialog which surfaces all these fields together.
     */
    suspend fun updateCategoryFullSettings(
        id: Long,
        name: String,
        iconName: String,
        colorToken: String,
        categoryType: String,
        numericMin: Float,
        numericMax: Float,
        allowDecimals: Boolean,
        numericUnit: String = "",
        allowMultiple: Boolean = false,
        showInLogPeriod: Boolean = false,
        trackAgainstTime: Boolean = false,
    ) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(
            cat.copy(
                name             = name.trim(),
                iconName         = iconName,
                colorToken       = colorToken,
                categoryType     = categoryType,
                numericMin       = numericMin,
                numericMax       = numericMax,
                allowDecimals    = allowDecimals,
                numericUnit      = numericUnit,
                allowMultiple    = allowMultiple,
                showInLogPeriod  = showInLogPeriod,
                trackAgainstTime = trackAgainstTime,
            )
        )
    }

    suspend fun updateTrackAgainstTime(id: Long, track: Boolean) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(trackAgainstTime = track))
    }

    /** Updates the numeric range settings for a category (slider type only). */
    suspend fun updateNumericSettings(
        id: Long,
        numericMin: Float,
        numericMax: Float,
        allowDecimals: Boolean,
        numericUnit: String,
        scaleLabels: String = "",
    ) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(
            cat.copy(
                numericMin    = numericMin,
                numericMax    = numericMax,
                allowDecimals = allowDecimals,
                numericUnit   = numericUnit,
                scaleLabels   = scaleLabels,
            )
        )
    }

    /** Updates just the unit/key label for a numeric category. */
    suspend fun updateNumericUnit(id: Long, unit: String) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(numericUnit = unit.trim()))
    }

    suspend fun updateShowInLogPeriod(id: Long, show: Boolean) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(showInLogPeriod = show))
    }

    suspend fun updateAllowMultiple(id: Long, allowMultiple: Boolean) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(allowMultiple = allowMultiple))
    }

    /**
     * Switches the Flow system category between chip-selection mode ("default") and
     * slider mode ("numeric_slider" with a 1-4 scale pre-labelled Spotting/Light/Medium/Heavy).
     * Switching back to chip mode restores the original default numeric fields.
     */
    suspend fun updateFlowCategoryMode(id: Long, useSlider: Boolean) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        if (useSlider) {
            categoryDao.updateCategory(
                cat.copy(
                    categoryType  = "numeric_slider",
                    numericMin    = 1f,
                    numericMax    = 4f,
                    allowDecimals = false,
                    scaleLabels   = "1=Spotting\n2=Light\n3=Medium\n4=Heavy",
                )
            )
        } else {
            categoryDao.updateCategory(
                cat.copy(
                    categoryType  = "default",
                    numericMin    = 0f,
                    numericMax    = 10f,
                    allowDecimals = false,
                    scaleLabels   = "",
                )
            )
        }
    }

    suspend fun archiveCategory(id: Long) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        if (cat.isSystem) return
        categoryDao.updateCategory(cat.copy(isArchived = true))
    }

    suspend fun unarchiveCategory(id: Long) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(isArchived = false))
    }


    suspend fun deleteCategory(category: TrackingCategory) {
        if (category.isSystem) return   // Guard: system categories cannot be deleted
        categoryDao.deleteCategory(category)
    }

    // ── Category values ───────────────────────────────────────────────────────

    suspend fun addValueToCategory(categoryId: Long, label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        val existing = categoryDao.getValuesForCategoryOnce(categoryId)
        if (existing.any { it.label.equals(trimmed, ignoreCase = true) }) return
        val maxOrder = existing.maxOfOrNull { it.displayOrder } ?: -1
        categoryDao.insertValue(
            TrackingValue(categoryId = categoryId, label = trimmed, displayOrder = maxOrder + 1)
        )
    }

    suspend fun deleteValue(value: TrackingValue) {
        categoryDao.deleteValue(value)
        // Historical tracking_log_values rows with this label are intentionally
        // preserved — they will appear greyed-out in the UI as "removed" values.
    }

    /**
     * Renames a catalog value.
     *
     * @param fixHistorical When true, also bulk-updates every past log entry that
     *   recorded the old label so history shows the corrected spelling. Ideal for
     *   typo fixes. When false, only the catalog option is renamed; past entries
     *   keep the old label intact (better when the meaning has changed).
     */
    suspend fun renameValue(value: TrackingValue, newLabel: String, fixHistorical: Boolean) {
        val trimmed = newLabel.trim()
        if (trimmed.isBlank() || trimmed == value.label) return
        val oldLabel = value.label
        categoryDao.updateValue(value.copy(label = trimmed))
        if (fixHistorical) {
            categoryDao.bulkRenameLogValues(value.categoryId, oldLabel, trimmed)
            // Cascade to the symptoms table when renaming a symptom catalog entry.
            val cat = categoryDao.getCategoryByIdOnce(value.categoryId)
            if (cat?.systemKey == "symptoms") {
                symptomDao?.bulkRenameSymptoms(oldLabel, trimmed)
            }
        }
    }

    // ── Tracking logs ─────────────────────────────────────────────────────────

    /**
     * Returns all tracking logs for [date] combined with their selected values
     * and parent category.
     */
    fun getLogsForDate(date: LocalDate): Flow<List<TrackingLogWithValues>> {
        val dateStr = date.toString()
        return combine(
            logDao.getLogsForDate(dateStr),
            categoryDao.getAllCategories()
        ) { logs, categories ->
            val catMap = categories.associateBy { it.id }
            logs.map { log ->
                val values = logDao.getLogValuesForLogOnce(log.id).map { it.valueLabel }
                TrackingLogWithValues(log = log, category = catMap[log.categoryId], values = values)
            }
        }
    }

    /** Returns a set of all dates that have at least one tracking log entry. */
    fun getAllLogDates(): Flow<Set<LocalDate>> =
        logDao.getAllLogDates().map { dates ->
            dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        }

    /**
     * Saves (upserts) a tracking log for the given date + category.
     * If a log already exists for that (date, category) pair, it is updated in-place,
     * unless [allowMultiple] is true — in which case a new log is always inserted.
     *
     * @return the ID of the saved log.
     */
    suspend fun saveLog(
        date: LocalDate,
        categoryId: Long,
        selectedValues: Set<String>,
        notes: String,
        allowMultiple: Boolean = false,
        loggedAt: String = "",
    ): Long {
        val dateStr = date.toString()
        val logId = if (!allowMultiple) {
            val existing = logDao.getLogForDateAndCategory(dateStr, categoryId)
            if (existing != null) {
                logDao.updateLog(existing.copy(notes = notes, loggedAt = loggedAt))
                existing.id
            } else {
                logDao.insertLog(TrackingLog(date = dateStr, categoryId = categoryId, notes = notes, loggedAt = loggedAt))
            }
        } else {
            logDao.insertLog(TrackingLog(date = dateStr, categoryId = categoryId, notes = notes, loggedAt = loggedAt))
        }
        logDao.deleteLogValuesForLog(logId)
        selectedValues.forEach { label ->
            logDao.insertLogValue(TrackingLogValue(logId = logId, valueLabel = label))
        }
        return logId
    }

    /**
     * Updates an existing log entry in-place, replacing its values and notes.
     * Used when editing a specific log by ID — bypasses the allowMultiple flag so
     * editing never creates a duplicate entry.
     */
    suspend fun updateLogInPlace(
        existingLog: TrackingLog,
        selectedValues: Set<String>,
        notes: String,
        loggedAt: String = "",
    ): Long {
        logDao.updateLog(existingLog.copy(notes = notes, loggedAt = loggedAt))
        logDao.deleteLogValuesForLog(existingLog.id)
        selectedValues.forEach { label ->
            logDao.insertLogValue(TrackingLogValue(logId = existingLog.id, valueLabel = label))
        }
        return existingLog.id
    }

    suspend fun deleteLog(log: TrackingLog) {
        logDao.deleteLog(log)
    }

    /**
     * Increments the running count stored for an "increment" (Plus One) category on
     * [date] by [delta], creating the log if it doesn't exist yet. The single value
     * label holds the whole-number count. Existing notes are preserved.
     *
     * @return the new count after applying [delta] (never below 0).
     */
    suspend fun incrementLog(date: LocalDate, categoryId: Long, delta: Int = 1): Int {
        val dateStr = date.toString()
        val existing = logDao.getLogForDateAndCategory(dateStr, categoryId)
        val logId: Long
        val current: Int
        if (existing != null) {
            logId = existing.id
            current = logDao.getLogValuesForLogOnce(logId)
                .firstOrNull()?.valueLabel?.toIntOrNull() ?: 0
        } else {
            logId = logDao.insertLog(TrackingLog(date = dateStr, categoryId = categoryId))
            current = 0
        }
        val newCount = (current + delta).coerceAtLeast(0)
        logDao.deleteLogValuesForLog(logId)
        logDao.insertLogValue(TrackingLogValue(logId = logId, valueLabel = newCount.toString()))
        return newCount
    }

    /** Returns the existing log (with values) for a specific (date, category), or null. */
    suspend fun getExistingLog(date: LocalDate, categoryId: Long): TrackingLogWithValues? {
        val log = logDao.getLogForDateAndCategory(date.toString(), categoryId) ?: return null
        val values = logDao.getLogValuesForLogOnce(log.id).map { it.valueLabel }
        val category = categoryDao.getCategoryByIdOnce(log.categoryId)
        return TrackingLogWithValues(log = log, category = category, values = values)
    }

    /** Returns all logs (with values) for the given date + category. */
    suspend fun getLogsForDateAndCategory(date: LocalDate, categoryId: Long): List<TrackingLogWithValues> {
        val logs = logDao.getLogsForDateAndCategory(date.toString(), categoryId)
        val category = categoryDao.getCategoryByIdOnce(categoryId)
        return logs.map { log ->
            val values = logDao.getLogValuesForLogOnce(log.id).map { it.valueLabel }
            TrackingLogWithValues(log = log, category = category, values = values)
        }
    }

    /** Returns the log by ID (with values), or null. */
    suspend fun getLogById(logId: Long): TrackingLogWithValues? {
        val log = logDao.getLogByIdOnce(logId) ?: return null
        val values = logDao.getLogValuesForLogOnce(log.id).map { it.valueLabel }
        val category = categoryDao.getCategoryByIdOnce(log.categoryId)
        return TrackingLogWithValues(log = log, category = category, values = values)
    }

    // ── Stats data access ─────────────────────────────────────────────────────

    /** All logs for a category within an inclusive date range. */
    suspend fun getLogsForCategoryInRange(
        categoryId: Long,
        start: LocalDate,
        end: LocalDate
    ): List<TrackingLog> =
        logDao.getLogsForCategoryInRange(categoryId, start.toString(), end.toString())

    /** Frequency of each value label for a category within a date range. */
    suspend fun getValueCountsForCategory(
        categoryId: Long,
        start: LocalDate,
        end: LocalDate
    ): List<ValueCount> =
        logDao.getValueCountsForCategory(categoryId, start.toString(), end.toString())

    /** All logs across all categories within an inclusive date range. */
    suspend fun getAllLogsInRange(start: LocalDate, end: LocalDate): List<TrackingLog> =
        logDao.getAllLogsInRange(start.toString(), end.toString())

    /** The date of the earliest log entry across all categories, or null if no logs exist. */
    suspend fun getEarliestLogDate(): LocalDate? =
        logDao.getEarliestLogDate()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** The date of the most recent log entry across all categories, or null if no logs exist. */
    suspend fun getLatestLogDate(): LocalDate? =
        logDao.getLatestLogDate()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Value labels for a specific log. Thin wrapper for Stats use. */
    suspend fun getValuesForLog(logId: Long): List<String> =
        logDao.getLogValuesForLogOnce(logId).map { it.valueLabel }

    /**
     * Fetches all logs (with values) for the given category IDs, optionally filtered
     * to an inclusive date range. Pass null for both dates to retrieve all history.
     */
    suspend fun exportTrackingLogs(
        categoryIds: List<Long>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<TrackingLogWithValues> {
        if (categoryIds.isEmpty()) return emptyList()
        val logs = if (startDate != null && endDate != null) {
            logDao.getLogsForCategoriesInRange(categoryIds, startDate.toString(), endDate.toString())
        } else {
            logDao.getAllLogsForCategories(categoryIds)
        }
        if (logs.isEmpty()) return emptyList()
        val logIds = logs.map { it.id }
        val valuesByLog = logDao.getLogValuesForLogs(logIds).groupBy { it.logId }
        val catMap = categoryDao.getAllCategoriesOnce().associateBy { it.id }
        return logs.map { log ->
            TrackingLogWithValues(
                log = log,
                category = catMap[log.categoryId],
                values = valuesByLog[log.id]?.map { it.valueLabel } ?: emptyList()
            )
        }
    }

    /** Returns active categories marked to appear on the Log Period screen. */
    suspend fun getShowInLogPeriodCategories(): List<TrackingCategory> =
        categoryDao.getShowInLogPeriodCategoriesOnce()

    /** System category lookup by name — used for Flow data migration. */
    suspend fun getSystemCategoryByName(name: String): TrackingCategory? =
        categoryDao.getSystemCategoryByName(name)

    /** System category lookup by stable key ("flow", "symptoms") — survives user renames. */
    suspend fun getSystemCategoryByKey(key: String): TrackingCategory? =
        categoryDao.getSystemCategoryByKey(key)

    /** Category lookup by name (any type) — used for import matching. */
    suspend fun getCategoryByName(name: String): TrackingCategory? =
        categoryDao.getCategoryByName(name)

    /**
     * Permanently removes all tracking log entries and their values.
     * Categories and their value definitions are preserved.
     *
     * NOTE: whenever new logged-data tables are added, this method must be
     * updated to include them — and the same applies to exportTrackingLogs,
     * importData (SettingsViewModel), and deleteAllData (SettingsViewModel).
     */
    suspend fun deleteAllLogs() {
        logDao.deleteAllLogs()
    }

    /**
     * Ensures a TrackingLog + TrackingLogValue exists for each date in [dates]
     * under [flowCategoryId] with value label [flowLabel].
     * Skips dates that already have a log for this category (preserves manual entries).
     */
    suspend fun syncFlowLogsForPeriod(
        flowCategoryId: Long,
        dates: List<LocalDate>,
        flowLabel: String
    ) {
        for (date in dates) {
            val existing = logDao.getLogForDateAndCategory(date.toString(), flowCategoryId)
            if (existing == null) {
                val logId = logDao.insertLog(
                    TrackingLog(date = date.toString(), categoryId = flowCategoryId)
                )
                logDao.insertLogValue(TrackingLogValue(logId = logId, valueLabel = flowLabel))
            }
        }
    }

    suspend fun reorderCategories(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { newOrder, id ->
            val cat = categoryDao.getCategoryByIdOnce(id) ?: return@forEachIndexed
            if (cat.displayOrder != newOrder) {
                categoryDao.updateCategory(cat.copy(displayOrder = newOrder))
            }
        }
    }
}
