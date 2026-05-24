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

    // ── Logs ─────────────────────────────────────────────────────────────────

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
}
