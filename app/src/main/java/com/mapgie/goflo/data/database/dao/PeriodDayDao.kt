package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapgie.goflo.data.database.entities.PeriodDayEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDayDao {
    @Query("SELECT * FROM period_days ORDER BY date ASC")
    fun getAllDays(): Flow<List<PeriodDayEntry>>

    @Query("SELECT * FROM period_days ORDER BY date ASC")
    suspend fun getAllDaysOnce(): List<PeriodDayEntry>

    @Query("SELECT * FROM period_days WHERE date = :date LIMIT 1")
    suspend fun getDay(date: String): PeriodDayEntry?

    @Query("SELECT * FROM period_days WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getDaysInRange(start: String, end: String): List<PeriodDayEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDay(day: PeriodDayEntry): Long

    @Query("DELETE FROM period_days WHERE date = :date")
    suspend fun deleteDay(date: String)

    @Query("DELETE FROM period_days WHERE date BETWEEN :start AND :end")
    suspend fun deleteDaysInRange(start: String, end: String)

    @Query("DELETE FROM period_days")
    suspend fun deleteAllDays()

    @Query("SELECT COUNT(*) FROM period_days")
    suspend fun countDays(): Int
}
