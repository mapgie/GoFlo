package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapgie.goflo.data.database.entities.SymptomEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms WHERE periodId = :periodId")
    fun getSymptomsForPeriod(periodId: Long): Flow<List<SymptomEntry>>

    @Query("SELECT * FROM symptoms WHERE periodId = :periodId")
    suspend fun getSymptomsForPeriodOnce(periodId: Long): List<SymptomEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntry)

    @Query("DELETE FROM symptoms WHERE periodId = :periodId")
    suspend fun deleteSymptomsByPeriodId(periodId: Long)

    @Query("DELETE FROM symptoms")
    suspend fun deleteAllSymptoms()

    @Query("SELECT * FROM symptoms")
    suspend fun getAllSymptoms(): List<SymptomEntry>

    @Query("SELECT * FROM symptoms")
    fun getAllSymptomsFlow(): Flow<List<SymptomEntry>>

    @Query("UPDATE symptoms SET symptomType = :newLabel WHERE symptomType = :oldLabel")
    suspend fun bulkRenameSymptoms(oldLabel: String, newLabel: String)
}
