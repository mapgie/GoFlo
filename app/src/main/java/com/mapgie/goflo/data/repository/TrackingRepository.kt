package com.mapgie.goflo.data.repository

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
    private val logDao: TrackingLogDao
) {

    // ── Categories ────────────────────────────────────────────────────────────

    fun getAllCategories(): Flow<List<TrackingCategory>> =
        categoryDao.getAllCategories()

    fun getActiveCategories(): Flow<List<TrackingCategory>> =
        categoryDao.getActiveCategories()

    fun getCategoryById(id: Long): Flow<TrackingCategory?> =
        categoryDao.getCategoryById(id)

    fun getValuesForCategory(categoryId: Long): Flow<List<TrackingValue>> =
        categoryDao.getValuesForCategory(categoryId)

    suspend fun addCategory(
        name: String,
        iconName: String = "category",
        colorToken: String = "secondary",
        categoryType: String = "default",
        numericMin: Float = 0f,
        numericMax: Float = 10f,
        allowDecimals: Boolean = false,
        numericUnit: String = "",
    ): Long {
        val maxOrder = categoryDao.getAllCategories().first()
            .maxOfOrNull { it.displayOrder } ?: -1
        return categoryDao.insertCategory(
            TrackingCategory(
                name          = name.trim(),
                displayOrder  = maxOrder + 1,
                iconName      = iconName,
                colorToken    = colorToken,
                categoryType  = categoryType,
                numericMin    = numericMin,
                numericMax    = numericMax,
                allowDecimals = allowDecimals,
                numericUnit   = numericUnit,
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

    /** Updates the numeric range settings for a category (slider type only). */
    suspend fun updateNumericSettings(
        id: Long,
        numericMin: Float,
        numericMax: Float,
        allowDecimals: Boolean,
        numericUnit: String,
    ) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(
            cat.copy(
                numericMin    = numericMin,
                numericMax    = numericMax,
                allowDecimals = allowDecimals,
                numericUnit   = numericUnit,
            )
        )
    }

    /** Updates just the unit/key label for a numeric category. */
    suspend fun updateNumericUnit(id: Long, unit: String) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(numericUnit = unit.trim()))
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
     * If a log already exists for that (date, category) pair, it is updated in-place.
     *
     * @return the ID of the saved log.
     */
    suspend fun saveLog(
        date: LocalDate,
        categoryId: Long,
        selectedValues: Set<String>,
        notes: String
    ): Long {
        val dateStr = date.toString()
        val existing = logDao.getLogForDateAndCategory(dateStr, categoryId)
        val logId = if (existing != null) {
            logDao.updateLog(existing.copy(notes = notes))
            existing.id
        } else {
            logDao.insertLog(TrackingLog(date = dateStr, categoryId = categoryId, notes = notes))
        }
        logDao.deleteLogValuesForLog(logId)
        selectedValues.forEach { label ->
            logDao.insertLogValue(TrackingLogValue(logId = logId, valueLabel = label))
        }
        return logId
    }

    suspend fun deleteLog(log: TrackingLog) {
        logDao.deleteLog(log)
    }

    /** Returns the existing log (with values) for a specific (date, category), or null. */
    suspend fun getExistingLog(date: LocalDate, categoryId: Long): TrackingLogWithValues? {
        val log = logDao.getLogForDateAndCategory(date.toString(), categoryId) ?: return null
        val values = logDao.getLogValuesForLogOnce(log.id).map { it.valueLabel }
        val category = categoryDao.getCategoryByIdOnce(log.categoryId)
        return TrackingLogWithValues(log = log, category = category, values = values)
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

    /** Value labels for a specific log. Thin wrapper for Stats use. */
    suspend fun getValuesForLog(logId: Long): List<String> =
        logDao.getLogValuesForLogOnce(logId).map { it.valueLabel }

    /** System category lookup by name — used for Flow data migration. */
    suspend fun getSystemCategoryByName(name: String): TrackingCategory? =
        categoryDao.getSystemCategoryByName(name)

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
}
