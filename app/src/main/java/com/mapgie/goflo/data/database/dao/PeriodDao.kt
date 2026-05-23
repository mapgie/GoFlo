package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapgie.goflo.data.database.entities.PeriodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<PeriodEntry>>

    @Query("SELECT * FROM periods WHERE id = :id")
    fun getPeriodById(id: Long): Flow<PeriodEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodEntry): Long

    @Update
    suspend fun updatePeriod(period: PeriodEntry)

    @Delete
    suspend fun deletePeriod(period: PeriodEntry)

    @Query("DELETE FROM periods")
    suspend fun deleteAllPeriods()

    @Query("SELECT COUNT(*) FROM periods")
    suspend fun countPeriods(): Int
}
