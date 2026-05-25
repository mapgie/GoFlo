package com.mapgie.goflo.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageCategoriesUiState(
    val categories: List<TrackingCategory> = emptyList()
)

class ManageCategoriesViewModel(
    private val repository: TrackingRepository
) : ViewModel() {

    val uiState: StateFlow<ManageCategoriesUiState> =
        repository.getAllCategories()
            .map { ManageCategoriesUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ManageCategoriesUiState()
            )

    fun addCategory(name: String, iconName: String, colorArgb: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name, iconName, colorArgb) }
    }

    fun updateCategoryAppearance(id: Long, iconName: String, colorArgb: Int) {
        viewModelScope.launch { repository.updateCategoryAppearance(id, iconName, colorArgb) }
    }

    fun deleteCategory(category: TrackingCategory) {
        if (category.isSystem) return
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    class Factory(private val repository: TrackingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ManageCategoriesViewModel(repository) as T
        }
    }
}
