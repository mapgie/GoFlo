package com.mapgie.goflo.ui.screens.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.Group
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.repository.CustomAlarmRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the 2-step category create/edit flow (logging redesign Phase 7).
 *
 * - Create mode: [category] is null; [group] carries the create-in-group
 *   context (pre-selected input type, adopt-colour switch).
 * - Edit mode: [category] is the category being edited, [group] its group (if
 *   any), [alarms] the custom alarms linked to it, and [hasLogs] whether any
 *   tracking log exists — the owner-decided rule is that the input type stays
 *   editable only until the first log is recorded.
 */
data class CategoryEditUiState(
    val category: TrackingCategory? = null,
    val group: Group? = null,
    val alarms: List<CustomAlarm> = emptyList(),
    val hasLogs: Boolean = false,
    val isLoading: Boolean = true,
)

class CategoryEditViewModel(
    private val categoryId: Long,
    private val groupId: Long,
    private val repository: TrackingRepository,
    private val alarmRepository: CustomAlarmRepository,
    private val context: Context,
) : ViewModel() {

    val isEditing: Boolean get() = categoryId > 0

    private val hasLogsFlow = MutableStateFlow(false)

    init {
        if (categoryId > 0) {
            viewModelScope.launch { hasLogsFlow.value = repository.hasLogs(categoryId) }
        }
    }

    val uiState: StateFlow<CategoryEditUiState> = combine(
        if (categoryId > 0) repository.getCategoryById(categoryId) else flowOf(null),
        repository.getAllGroups(),
        if (categoryId > 0) alarmRepository.getAlarmsByCategory(categoryId) else flowOf(emptyList()),
        hasLogsFlow,
    ) { category, groups, alarms, hasLogs ->
        val contextGroupId = category?.groupId ?: groupId.takeIf { it > 0 }
        CategoryEditUiState(
            category = category,
            group = groups.firstOrNull { it.id == contextGroupId },
            alarms = alarms,
            hasLogs = hasLogs,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryEditUiState(),
    )

    /**
     * Creates the category (create mode, filing it into the context group when
     * one was given) or saves every edited field through
     * [TrackingRepository.updateCategoryFullSettings] (edit mode).
     *
     * Edit-mode guards: the stored input type is kept whenever the category
     * already has logs (type is fixed once logged) or is a system category;
     * allow-multiple and log-with-period are likewise kept for system
     * categories, whose switches the edit surface does not show. The mode key
     * is always carried through unchanged.
     */
    fun save(
        name: String,
        iconName: String,
        colorToken: String,
        categoryType: String,
        numericMin: Float,
        numericMax: Float,
        allowDecimals: Boolean,
        numericUnit: String,
        scaleLabels: String,
        allowMultiple: Boolean,
        showInLogPeriod: Boolean,
        trackAgainstTime: Boolean,
        onSaved: (Long) -> Unit = {},
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            if (categoryId > 0) {
                val category = uiState.value.category ?: return@launch
                val typeLocked = uiState.value.hasLogs || category.isSystem
                repository.updateCategoryFullSettings(
                    id               = categoryId,
                    name             = name,
                    iconName         = iconName,
                    colorToken       = colorToken,
                    categoryType     = if (typeLocked) category.categoryType else categoryType,
                    numericMin       = numericMin,
                    numericMax       = numericMax,
                    allowDecimals    = allowDecimals,
                    numericUnit      = numericUnit,
                    scaleLabels      = scaleLabels,
                    allowMultiple    = if (category.isSystem) category.allowMultiple else allowMultiple,
                    showInLogPeriod  = if (category.isSystem) category.showInLogPeriod else showInLogPeriod,
                    trackAgainstTime = trackAgainstTime,
                    modeKey          = category.modeKey,
                )
                onSaved(categoryId)
            } else {
                val id = repository.addCategory(
                    name             = name,
                    iconName         = iconName,
                    colorToken       = colorToken,
                    categoryType     = categoryType,
                    numericMin       = numericMin,
                    numericMax       = numericMax,
                    allowDecimals    = allowDecimals,
                    numericUnit      = numericUnit,
                    scaleLabels      = scaleLabels,
                    allowMultiple    = allowMultiple,
                    showInLogPeriod  = showInLogPeriod,
                    trackAgainstTime = trackAgainstTime,
                )
                if (groupId > 0) repository.assignCategoryToGroup(id, groupId)
                onSaved(id)
            }
        }
    }

    /** Deletes the category and its whole log history. System categories are protected. */
    fun deleteCategory() {
        val category = uiState.value.category ?: return
        if (category.isSystem) return
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    /**
     * Enables or disables one linked alarm, mirroring the existing
     * CustomAlarmsViewModel behaviour: persist the flag, then (re)schedule or
     * cancel through the existing [ReminderScheduler] — no parallel alarm
     * machinery.
     */
    fun setAlarmEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.setEnabled(alarmId, enabled)
            val alarm = alarmRepository.getById(alarmId) ?: return@launch
            if (enabled) {
                ReminderScheduler.scheduleCustomAlarm(context, alarm)
            } else {
                ReminderScheduler.cancelCustomAlarm(context, alarmId)
            }
        }
    }

    class Factory(
        private val categoryId: Long,
        private val groupId: Long,
        private val repository: TrackingRepository,
        private val alarmRepository: CustomAlarmRepository,
        private val context: Context,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CategoryEditViewModel(
                categoryId, groupId, repository, alarmRepository, context
            ) as T
        }
    }
}
