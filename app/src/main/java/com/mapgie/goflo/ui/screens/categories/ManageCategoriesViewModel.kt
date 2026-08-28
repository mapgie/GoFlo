package com.mapgie.goflo.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.Group
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.util.COLOR_TOKEN_INHERIT
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageCategoriesUiState(
    val categories: List<TrackingCategory> = emptyList(),
    val groups: List<Group> = emptyList(),
    val archiveWarningDisabled: Boolean = false,
)

class ManageCategoriesViewModel(
    private val repository: TrackingRepository,
    private val store: AppPreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<ManageCategoriesUiState> =
        combine(
            repository.getAllCategories(),
            repository.getAllGroups(),
            store.preferences,
        ) { cats, groups, prefs ->
            ManageCategoriesUiState(
                categories = cats,
                groups = groups,
                archiveWarningDisabled = prefs.archiveWarningDisabled,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ManageCategoriesUiState()
            )

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

    // ── Groups (logging redesign Phase 6) ─────────────────────────────────────

    /**
     * Creates a group and reports the new id so the caller can chain a
     * file-this-category step (the "New group" path of the add-to-group sheet).
     */
    fun addGroup(
        name: String,
        colorRole: String,
        defaultInputType: String,
        onCreated: (Long) -> Unit = {},
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.addGroup(
                name             = name.trim(),
                colorRole        = colorRole,
                defaultInputType = defaultInputType,
            )
            onCreated(id)
        }
    }

    /** Saves the edited name, colour role, and default input type of a group. */
    fun updateGroup(id: Long, name: String, colorRole: String, defaultInputType: String) {
        viewModelScope.launch {
            repository.renameGroup(id, name)
            repository.updateGroupRole(id, colorRole)
            repository.updateGroupDefaultInputType(id, defaultInputType)
        }
    }

    /** Moves a group up (-1) or down (+1) in the display order. */
    fun moveGroup(id: Long, delta: Int) {
        val ordered = uiState.value.groups.map { it.id }.toMutableList()
        val index = ordered.indexOf(id)
        val target = index + delta
        if (index == -1 || target !in ordered.indices) return
        ordered.add(target, ordered.removeAt(index))
        viewModelScope.launch { repository.reorderGroups(ordered) }
    }

    /**
     * Deletes a group. The repository unfiles its member categories first;
     * no category or log is ever deleted by this action.
     */
    fun deleteGroup(id: Long) {
        viewModelScope.launch { repository.deleteGroup(id) }
    }

    /**
     * Files [category] into the group, optionally switching its colour token to
     * the "inherit" sentinel so it adopts the group's colour role (and follows
     * any later recolour of the group).
     */
    fun assignCategoryToGroup(category: TrackingCategory, groupId: Long, adoptGroupColor: Boolean) {
        viewModelScope.launch {
            repository.assignCategoryToGroup(category.id, groupId)
            if (adoptGroupColor && category.colorToken != COLOR_TOKEN_INHERIT) {
                repository.updateCategoryAppearance(category.id, category.iconName, COLOR_TOKEN_INHERIT)
            }
        }
    }

    /** Unfiles a category from its group. Past logs and settings are untouched. */
    fun unassignCategory(categoryId: Long) {
        viewModelScope.launch { repository.unassignCategory(categoryId) }
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
