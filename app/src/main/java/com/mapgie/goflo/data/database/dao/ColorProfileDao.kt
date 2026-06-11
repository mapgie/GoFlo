package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapgie.goflo.data.database.entities.ColorProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ColorProfileDao {
    @Query("SELECT * FROM color_profiles ORDER BY name ASC")
    fun getAll(): Flow<List<ColorProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ColorProfile): Long

    @Delete
    suspend fun delete(profile: ColorProfile)
}
