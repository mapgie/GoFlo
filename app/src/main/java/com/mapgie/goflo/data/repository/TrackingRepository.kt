package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.TrackingCategoryDao
import com.mapgie.goflo.data.database.dao.TrackingLogDao
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

    fun getCategoryById(id: Long): Flow<TrackingCategory?> =
        categoryDao.getCategoryById(id)

    fun getValuesForCategory(categoryId: Long): Flow<List<TrackingValue>> =
        categoryDao.getValuesForCategory(categoryId)

    suspend fun addCategory(
        name: String,
        iconName: String = "category",
        colorArgb: Int = (0xFF1976D2L).toInt(),
    ): Long {
        val maxOrder = categoryDao.getAllCategories().first()
            .maxOfOrNull { it.displayOrder } ?: -1
        return categoryDao.insertCategory(
            TrackingCategory(
                name        = name.trim(),
                displayOrder = maxOrder + 1,
                iconName    = iconName,
                colorArgb   = colorArgb,
            )
        )
    }

    suspend fun renameCategory(id: Long, newName: String) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(name = newName.trim()))
    }

    /** Updates only the icon and colour of an existing category. */
    suspend fun updateCategoryAppearance(id: Long, iconName: String, colorArgb: Int) {
        val cat = categoryDao.getCategoryByIdOnce(id) ?: return
        categoryDao.updateCategory(cat.copy(iconName = iconName, colorArgb = colorArgb))
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
}
