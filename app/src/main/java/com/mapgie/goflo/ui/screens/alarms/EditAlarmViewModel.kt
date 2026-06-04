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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EditAlarmStep { PICK_TIME, CONFIGURE }

data class EditAlarmUiState(
    val step: EditAlarmStep = EditAlarmStep.PICK_TIME,
    val hour: Int = 8,
    val minute: Int = 0,
    val label: String = "",
    val alarmType: String = "NOTIFICATION",
    val overrideDnd: Boolean = false,
    val isRecurring: Boolean = true,
    val scheduleType: String = "DAILY",
    val daysOffset: Int = 1,
    val dayOfPeriod: Int = 1,
    val snoozeDurationMinutes: Int = 10,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val allCategories: List<TrackingCategory> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
)

class EditAlarmViewModel(
    private val alarmRepository: CustomAlarmRepository,
    private val trackingRepository: TrackingRepository,
    private val context: Context,
    private val editAlarmId: Long,
    private val preselectedCategoryId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EditAlarmUiState())
    val uiState: StateFlow<EditAlarmUiState> = combine(
        _state,
        trackingRepository.getAllCategories(),
    ) { state, categories ->
        state.copy(allCategories = categories.filter { !it.isArchived })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditAlarmUiState(),
    )

    init {
        viewModelScope.launch {
            if (preselectedCategoryId != -1L) {
                _state.value = _state.value.copy(
                    selectedCategoryIds = setOf(preselectedCategoryId)
                )
            }
            if (editAlarmId != -1L) {
                loadAlarm(editAlarmId)
            }
        }
    }

    private suspend fun loadAlarm(id: Long) {
        _state.value = _state.value.copy(isLoading = true)
        val alarm = alarmRepository.getById(id) ?: return
        val categoryIds = alarmRepository.getCategoryIdsForAlarm(id).toSet()
        _state.value = _state.value.copy(
            step = EditAlarmStep.CONFIGURE,
            hour = alarm.hour,
            minute = alarm.minute,
            label = alarm.label,
            alarmType = alarm.alarmType,
            overrideDnd = alarm.overrideDnd,
            isRecurring = alarm.isRecurring,
            scheduleType = alarm.scheduleType,
            daysOffset = alarm.daysOffset,
            dayOfPeriod = alarm.dayOfPeriod,
            snoozeDurationMinutes = alarm.snoozeDurationMinutes,
            selectedCategoryIds = categoryIds,
            isLoading = false,
        )
    }

    fun confirmTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute, step = EditAlarmStep.CONFIGURE)
    }

    fun goBackToTimePicker() {
        _state.value = _state.value.copy(step = EditAlarmStep.PICK_TIME)
    }

    fun setLabel(label: String) { _state.value = _state.value.copy(label = label) }
    fun setAlarmType(type: String) { _state.value = _state.value.copy(alarmType = type) }
    fun setOverrideDnd(v: Boolean) { _state.value = _state.value.copy(overrideDnd = v) }
    fun setRecurring(v: Boolean) { _state.value = _state.value.copy(isRecurring = v) }
    fun setScheduleType(t: String) { _state.value = _state.value.copy(scheduleType = t) }
    fun setDaysOffset(n: Int) { _state.value = _state.value.copy(daysOffset = n) }
    fun setDayOfPeriod(n: Int) { _state.value = _state.value.copy(dayOfPeriod = n) }
    fun setSnoozeDuration(minutes: Int) { _state.value = _state.value.copy(snoozeDurationMinutes = minutes) }

    fun toggleCategory(id: Long) {
        val current = _state.value.selectedCategoryIds
        _state.value = _state.value.copy(
            selectedCategoryIds = if (id in current) current - id else current + id
        )
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val alarm = CustomAlarm(
            id = if (editAlarmId != -1L) editAlarmId else 0,
            hour = s.hour,
            minute = s.minute,
            label = s.label.trim(),
            alarmType = s.alarmType,
            overrideDnd = s.overrideDnd,
            isRecurring = s.isRecurring,
            scheduleType = s.scheduleType,
            daysOffset = s.daysOffset,
            dayOfPeriod = s.dayOfPeriod,
            snoozeDurationMinutes = s.snoozeDurationMinutes,
            isEnabled = true,
        )
        val savedId = alarmRepository.saveAlarm(alarm, s.selectedCategoryIds.toList())
        val saved = alarmRepository.getById(savedId)
        if (saved != null) ReminderScheduler.scheduleCustomAlarm(context, saved)
        _state.value = _state.value.copy(isSaved = true)
    }

    class Factory(
        private val alarmRepository: CustomAlarmRepository,
        private val trackingRepository: TrackingRepository,
        private val context: Context,
        private val editAlarmId: Long,
        private val preselectedCategoryId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditAlarmViewModel(
                alarmRepository, trackingRepository, context,
                editAlarmId, preselectedCategoryId
            ) as T
    }
}
