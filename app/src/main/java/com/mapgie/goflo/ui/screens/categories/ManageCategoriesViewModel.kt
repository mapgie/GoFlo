package com.mapgie.goflo.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageCategoriesUiState(
    val categories: List<TrackingCategory> = emptyList(),
    val archiveWarningDisabled: Boolean = false,
)

class ManageCategoriesViewModel(
    private val repository: TrackingRepository,
    private val store: AppPreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<ManageCategoriesUiState> =
        combine(
            repository.getAllCategories(),
            store.preferences,
        ) { cats, prefs ->
            ManageCategoriesUiState(
                categories = cats,
                archiveWarningDisabled = prefs.archiveWarningDisabled,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ManageCategoriesUiState()
            )

    fun addCategory(
        name: String,
        iconName: String,
        colorToken: String,
        categoryType: String = "default",
        numericMin: Float = 0f,
        numericMax: Float = 10f,
        allowDecimals: Boolean = false,
        numericUnit: String = "",
        allowMultiple: Boolean = false,
        showInLogPeriod: Boolean = false,
        onCreated: (Long) -> Unit = {},
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addCategory(
                name            = name,
                iconName        = iconName,
                colorToken      = colorToken,
                categoryType    = categoryType,
                numericMin      = numericMin,
                numericMax      = numericMax,
                allowDecimals   = allowDecimals,
                numericUnit     = numericUnit,
                allowMultiple   = allowMultiple,
                showInLogPeriod = showInLogPeriod,
            )
            onCreated(id)
        }
    }

    fun updateCategoryAppearance(id: Long, iconName: String, colorToken: String) {
        viewModelScope.launch { repository.updateCategoryAppearance(id, iconName, colorToken) }
    }

    fun archiveCategory(category: TrackingCategory) {
        viewModelScope.launch { repository.archiveCategory(category.id) }
    }

    fun unarchiveCategory(category: TrackingCategory) {
        viewModelScope.launch { repository.unarchiveCategory(category.id) }
    }

    fun deleteCategory(category: TrackingCategory) {
        if (category.isSystem) return
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun reorderCategories(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderCategories(orderedIds) }
    }

    fun setArchiveWarningDisabled(disabled: Boolean) {
        viewModelScope.launch { store.setArchiveWarningDisabled(disabled) }
    }

    class Factory(
        private val repository: TrackingRepository,
        private val store: AppPreferencesStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ManageCategoriesViewModel(repository, store) as T
        }
    }
}
