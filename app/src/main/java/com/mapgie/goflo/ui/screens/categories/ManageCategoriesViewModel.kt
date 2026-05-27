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

    fun addCategory(
        name: String,
        iconName: String,
        colorToken: String,
        categoryType: String = "default",
        numericUnit: String = "",
        onCreated: (Long) -> Unit = {},
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addCategory(
                name         = name,
                iconName     = iconName,
                colorToken   = colorToken,
                categoryType = categoryType,
                numericUnit  = numericUnit,
            )
            onCreated(id)
        }
    }

    fun updateCategoryAppearance(id: Long, iconName: String, colorToken: String) {
        viewModelScope.launch {
            repository.updateCategoryAppearance(id, iconName, colorToken)
        }
    }

    fun archiveCategory(category: TrackingCategory) {
        if (category.isSystem) return
        viewModelScope.launch { repository.archiveCategory(category.id) }
    }

    fun unarchiveCategory(category: TrackingCategory) {
        viewModelScope.launch { repository.unarchiveCategory(category.id) }
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
