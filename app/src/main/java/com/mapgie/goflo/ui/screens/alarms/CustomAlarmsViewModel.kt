package com.mapgie.goflo.ui.screens.alarms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.repository.CustomAlarmRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmWithCategories(
    val alarm: CustomAlarm,
    val categories: List<TrackingCategory>,
)

data class CustomAlarmsUiState(
    val alarms: List<AlarmWithCategories> = emptyList(),
    val isLoading: Boolean = true,
)

class CustomAlarmsViewModel(
    private val alarmRepository: CustomAlarmRepository,
    private val trackingRepository: TrackingRepository,
    private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<CustomAlarmsUiState> = combine(
        alarmRepository.getAllAlarms(),
        trackingRepository.getAllCategories(),
        alarmRepository.getAllAlarmCategoryLinks(),
    ) { alarms, categories, links ->
        val categoryMap = categories.associateBy { it.id }
        val linksByAlarm = links.groupBy { it.alarmId }
        val alarmsWithCats = alarms.map { alarm ->
            val catIds = linksByAlarm[alarm.id]?.map { it.categoryId } ?: emptyList()
            AlarmWithCategories(
                alarm = alarm,
                categories = catIds.mapNotNull { categoryMap[it] },
            )
        }
        CustomAlarmsUiState(alarms = alarmsWithCats, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CustomAlarmsUiState(),
    )

    fun deleteAlarm(id: Long) = viewModelScope.launch {
        ReminderScheduler.cancelCustomAlarm(context, id)
        alarmRepository.deleteAlarm(id)
    }

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        alarmRepository.setEnabled(id, enabled)
        val alarm = alarmRepository.getById(id) ?: return@launch
        if (enabled) {
            ReminderScheduler.scheduleCustomAlarm(context, alarm)
        } else {
            ReminderScheduler.cancelCustomAlarm(context, id)
        }
    }

    class Factory(
        private val alarmRepository: CustomAlarmRepository,
        private val trackingRepository: TrackingRepository,
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CustomAlarmsViewModel(alarmRepository, trackingRepository, context) as T
    }
}
