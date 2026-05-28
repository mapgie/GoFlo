package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.database.entities.TrackingLogValue
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingLogDao {

    // ── Logs ───────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tracking_logs WHERE date = :date ORDER BY categoryId ASC")
    fun getLogsForDate(date: String): Flow<List<TrackingLog>>

    @Query("SELECT * FROM tracking_logs WHERE date = :date ORDER BY categoryId ASC")
    suspend fun getLogsForDateOnce(date: String): List<TrackingLog>

    @Query("SELECT DISTINCT date FROM tracking_logs ORDER BY date DESC")
    fun getAllLogDates(): Flow<List<String>>

    @Query("SELECT * FROM tracking_logs WHERE id = :id")
    fun getLogById(id: Long): Flow<TrackingLog?>

    @Query("SELECT * FROM tracking_logs WHERE id = :id")
    suspend fun getLogByIdOnce(id: Long): TrackingLog?

    @Query("SELECT * FROM tracking_logs WHERE date = :date AND categoryId = :categoryId LIMIT 1")
    suspend fun getLogForDateAndCategory(date: String, categoryId: Long): TrackingLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TrackingLog): Long

    @Update
    suspend fun updateLog(log: TrackingLog)

    @Delete
    suspend fun deleteLog(log: TrackingLog)

    // ── Log values ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tracking_log_values WHERE logId = :logId")
    fun getLogValuesForLog(logId: Long): Flow<List<TrackingLogValue>>

    @Query("SELECT * FROM tracking_log_values WHERE logId = :logId")
    suspend fun getLogValuesForLogOnce(logId: Long): List<TrackingLogValue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogValue(value: TrackingLogValue)

    @Query("DELETE FROM tracking_log_values WHERE logId = :logId")
    suspend fun deleteLogValuesForLog(logId: Long)

    // ── Stats queries ────────────────────────────────────────────────────────

    /** All logs for a given category within an inclusive date range (ISO-8601 strings). */
    @Query("SELECT * FROM tracking_logs WHERE categoryId = :categoryId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getLogsForCategoryInRange(
        categoryId: Long,
        startDate: String,
        endDate: String
    ): List<TrackingLog>

    /**
     * Frequency of each value label for a category within a date range.
     * Returns a [ValueCount] projection ordered by count descending.
     */
    @Query("""
        SELECT tlv.valueLabel, COUNT(*) as count
        FROM tracking_logs tl
        JOIN tracking_log_values tlv ON tl.id = tlv.logId
        WHERE tl.categoryId = :categoryId
          AND tl.date >= :startDate
          AND tl.date <= :endDate
        GROUP BY tlv.valueLabel
        ORDER BY count DESC
    """)
    suspend fun getValueCountsForCategory(
        categoryId: Long,
        startDate: String,
        endDate: String
    ): List<ValueCount>

    /** All logs across all categories within an inclusive date range. */
    @Query("SELECT * FROM tracking_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getAllLogsInRange(startDate: String, endDate: String): List<TrackingLog>

    @Query("SELECT * FROM tracking_logs WHERE categoryId IN (:categoryIds) AND date >= :startDate AND date <= :endDate ORDER BY date ASC, categoryId ASC")
    suspend fun getLogsForCategoriesInRange(categoryIds: List<Long>, startDate: String, endDate: String): List<TrackingLog>

    @Query("SELECT * FROM tracking_logs WHERE categoryId IN (:categoryIds) ORDER BY date ASC, categoryId ASC")
    suspend fun getAllLogsForCategories(categoryIds: List<Long>): List<TrackingLog>

    @Query("SELECT * FROM tracking_log_values WHERE logId IN (:logIds)")
    suspend fun getLogValuesForLogs(logIds: List<Long>): List<TrackingLogValue>

    /** The ISO-8601 date string of the earliest log entry, or null if there are none. */
    @Query("SELECT MIN(date) FROM tracking_logs")
    suspend fun getEarliestLogDate(): String?

    /** The ISO-8601 date string of the most recent log entry, or null if there are none. */
    @Query("SELECT MAX(date) FROM tracking_logs")
    suspend fun getLatestLogDate(): String?

    /** Permanently removes every tracking log row (values cascade via FK). */
    @Query("DELETE FROM tracking_logs")
    suspend fun deleteAllLogs()
}
