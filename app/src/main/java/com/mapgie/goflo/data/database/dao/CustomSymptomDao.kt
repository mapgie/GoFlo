package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapgie.goflo.data.database.entities.CustomSymptomEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSymptomDao {
    /** Observe the full library of custom symptoms, ordered alphabetically. */
    @Query("SELECT * FROM custom_symptoms ORDER BY name ASC")
    fun getAllCustomSymptoms(): Flow<List<CustomSymptomEntry>>

    /** Insert a new symptom; silently ignores duplicates (case-sensitive at DB level,
     *  but callers are expected to normalise to lowercase before insertion). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomSymptom(symptom: CustomSymptomEntry)

    @Delete
    suspend fun deleteCustomSymptom(symptom: CustomSymptomEntry)
}
