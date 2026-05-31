package com.mapgie.goflo.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageCategoryValuesUiState(
    val category: TrackingCategory? = null,
    val values: List<TrackingValue> = emptyList(),
    val isLoading: Boolean = true
)

class ManageCategoryValuesViewModel(
    private val categoryId: Long,
    private val repository: TrackingRepository
) : ViewModel() {

    val uiState: StateFlow<ManageCategoryValuesUiState> = combine(
        repository.getCategoryById(categoryId),
        repository.getValuesForCategory(categoryId)
    ) { category, values ->
        ManageCategoryValuesUiState(
            category = category,
            values = values,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageCategoryValuesUiState()
    )

    fun renameCategory(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameCategory(categoryId, newName) }
    }

    fun updateNumericSettings(
        min: Float,
        max: Float,
        allowDecimals: Boolean,
        unit: String,
        scaleLabels: String = "",
    ) {
        viewModelScope.launch {
            repository.updateNumericSettings(categoryId, min, max, allowDecimals, unit, scaleLabels)
        }
    }

    fun updateUnit(unit: String) {
        viewModelScope.launch { repository.updateNumericUnit(categoryId, unit) }
    }

    fun setShowInLogPeriod(show: Boolean) {
        viewModelScope.launch { repository.updateShowInLogPeriod(categoryId, show) }
    }

    fun setAllowMultiple(allowMultiple: Boolean) {
        viewModelScope.launch { repository.updateAllowMultiple(categoryId, allowMultiple) }
    }

    fun setFlowSliderMode(useSlider: Boolean) {
        viewModelScope.launch { repository.updateFlowCategoryMode(categoryId, useSlider) }
    }

    fun setTrackAgainstTime(track: Boolean) {
        viewModelScope.launch { repository.updateTrackAgainstTime(categoryId, track) }
    }

    fun addValue(label: String) {
        if (label.isBlank()) return
        viewModelScope.launch { repository.addValueToCategory(categoryId, label) }
    }

    fun deleteValue(value: TrackingValue) {
        viewModelScope.launch { repository.deleteValue(value) }
    }

    /**
     * Renames a value. [fixHistorical] = true updates all past log entries too
     * (ideal for typo fixes). false = only renames the catalog option.
     */
    fun renameValue(value: TrackingValue, newLabel: String, fixHistorical: Boolean) {
        viewModelScope.launch { repository.renameValue(value, newLabel, fixHistorical) }
    }

    fun archiveCategory() {
        val cat = uiState.value.category ?: return
        if (cat.isSystem) return
        viewModelScope.launch { repository.archiveCategory(categoryId) }
    }

    fun unarchiveCategory() {
        viewModelScope.launch { repository.unarchiveCategory(categoryId) }
    }

    fun deleteCategory() {
        val cat = uiState.value.category ?: return
        if (cat.isSystem) return
        viewModelScope.launch { repository.deleteCategory(cat) }
    }

    class Factory(
        private val categoryId: Long,
        private val repository: TrackingRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ManageCategoryValuesViewModel(categoryId, repository) as T
        }
    }
}
