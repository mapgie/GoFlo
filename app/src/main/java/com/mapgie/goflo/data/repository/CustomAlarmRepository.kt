package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.CustomAlarmDao
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.CustomAlarmCategory
import kotlinx.coroutines.flow.Flow

class CustomAlarmRepository(private val dao: CustomAlarmDao) {

    fun getAllAlarms(): Flow<List<CustomAlarm>> = dao.getAllAlarms()

    fun getAlarmsByCategory(categoryId: Long): Flow<List<CustomAlarm>> =
        dao.getAlarmsByCategory(categoryId)

    fun getCategoryLinks(alarmId: Long): Flow<List<CustomAlarmCategory>> =
        dao.getCategoriesForAlarm(alarmId)

    fun getAllAlarmCategoryLinks(): Flow<List<CustomAlarmCategory>> =
        dao.getAllAlarmCategoryLinks()

    suspend fun getById(id: Long): CustomAlarm? = dao.getById(id)

    suspend fun getEnabledAlarms(): List<CustomAlarm> = dao.getEnabledAlarms()

    suspend fun getCategoryIdsForAlarm(alarmId: Long): List<Long> =
        dao.getCategoriesForAlarmOnce(alarmId).map { it.categoryId }

    suspend fun saveAlarm(alarm: CustomAlarm, categoryIds: List<Long>): Long {
        val id = dao.insert(alarm)
        dao.clearCategoriesForAlarm(id)
        categoryIds.forEach { categoryId ->
            dao.linkCategory(CustomAlarmCategory(alarmId = id, categoryId = categoryId))
        }
        return id
    }

    suspend fun deleteAlarm(id: Long) = dao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = dao.getById(id) ?: return
        dao.update(alarm.copy(isEnabled = enabled))
    }
}
