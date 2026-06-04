package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.CustomAlarmCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlarmDao {

    @Query("SELECT * FROM custom_alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<CustomAlarm>>

    @Query("SELECT * FROM custom_alarms WHERE id = :id")
    suspend fun getById(id: Long): CustomAlarm?

    @Query("SELECT * FROM custom_alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<CustomAlarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: CustomAlarm): Long

    @Update
    suspend fun update(alarm: CustomAlarm)

    @Delete
    suspend fun delete(alarm: CustomAlarm)

    @Query("DELETE FROM custom_alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM custom_alarm_categories WHERE alarmId = :alarmId")
    fun getCategoriesForAlarm(alarmId: Long): Flow<List<CustomAlarmCategory>>

    @Query("SELECT * FROM custom_alarm_categories WHERE alarmId = :alarmId")
    suspend fun getCategoriesForAlarmOnce(alarmId: Long): List<CustomAlarmCategory>

    @Query("SELECT * FROM custom_alarm_categories WHERE categoryId = :categoryId")
    fun getAlarmsForCategory(categoryId: Long): Flow<List<CustomAlarmCategory>>

    @Query(
        "SELECT ca.* FROM custom_alarms ca " +
        "INNER JOIN custom_alarm_categories cac ON ca.id = cac.alarmId " +
        "WHERE cac.categoryId = :categoryId ORDER BY ca.hour ASC, ca.minute ASC"
    )
    fun getAlarmsByCategory(categoryId: Long): Flow<List<CustomAlarm>>

    @Query("SELECT * FROM custom_alarm_categories")
    fun getAllAlarmCategoryLinks(): Flow<List<CustomAlarmCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkCategory(link: CustomAlarmCategory)

    @Query("DELETE FROM custom_alarm_categories WHERE alarmId = :alarmId AND categoryId = :categoryId")
    suspend fun unlinkCategory(alarmId: Long, categoryId: Long)

    @Query("DELETE FROM custom_alarm_categories WHERE alarmId = :alarmId")
    suspend fun clearCategoriesForAlarm(alarmId: Long)
}
