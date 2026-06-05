package com.mapgie.goflo.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.TrackingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ── Grid (heatmap) configuration ───────────────────────────────────────────────

/** Day window covered by the Grid columns. Windows over 90 days bucket by week. */
enum class HeatmapWindow(val days: Int, val label: String) {
    D30(30, "30d"),
    D60(60, "60d"),
    D90(90, "90d"),
    D180(180, "6m"),
    D365(365, "1y");

    /** True when columns should aggregate by ISO week rather than by day. */
    val weekly: Boolean get() = days > 90

    companion object {
        fun fromDays(days: Int): HeatmapWindow =
            entries.firstOrNull { it.days == days } ?: D30
    }
}

/** Per-cell aggregation of the logged levels that fall in a column. */
enum class HeatmapAggregation { SUM, AVERAGE }

/**
 * How a category's per-cell magnitude is derived:
 * - [NUMERIC]: the numeric value logged (slider / free / plus-one categories).
 * - [ORDINAL]: the level (value displayOrder) of a single-select default category, e.g. Flow.
 * - [COUNT]:  the number of entries, for multi-select default categories, e.g. Symptoms.
 */
enum class MagnitudeKind { NUMERIC, ORDINAL, COUNT }

// ── Grid data models ───────────────────────────────────────────────────────────

/** One column of the grid: a single day or a single ISO week. */
data class HeatmapColumn(
    val key: String,
    val label: String,
    val accessibilityLabel: String,
)

/** One category row: magnitudes aligned 1:1 with the column list (null = no data). */
data class HeatmapRow(
    val categoryId: Long,
    val name: String,
    val iconName: String,
    val colorToken: String,
    val magnitudes: List<Float?>,
    val rowMin: Float,
    val rowMax: Float,
    val kind: MagnitudeKind,
    val unit: String,
    /** Maps a value displayOrder to its label, for ORDINAL cell descriptions. */
    val ordinalLabels: Map<Int, String>,
    /** Maps a whole-number step to its scale label, for NUMERIC slider descriptions. */
    val numericScaleLabels: Map<Int, String>,
)

sealed class HeatmapData {
    data object Empty : HeatmapData()
    data object Loading : HeatmapData()
    data class Grid(
        val columns: List<HeatmapColumn>,
        val rows: List<HeatmapRow>,
        val bucketingIsWeekly: Boolean,
    ) : HeatmapData()
}

// ── UI state ───────────────────────────────────────────────────────────────────

data class HeatmapUiState(
    val categories: List<TrackingCategory> = emptyList(),
    val selectedCategoryIds: List<Long> = emptyList(),
    val window: HeatmapWindow = HeatmapWindow.D30,
    val aggregation: HeatmapAggregation = HeatmapAggregation.AVERAGE,
    val zoomLevel: Int = 1,
    val grid: HeatmapData = HeatmapData.Empty,
)

// ── ViewModel ──────────────────────────────────────────────────────────────────

class HeatmapViewModel(
    private val repository: TrackingRepository,
    private val preferencesStore: AppPreferencesStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())

    private var savedCategoryIds: List<Long> = emptyList()
    private var hasRestoredSelection: Boolean = false
    private var reloadJob: Job? = null

    init {
        viewModelScope.launch {
            // Restore persisted settings once, before categories start arriving, so the
            // saved selection is available when the first category emission is handled.
            preferencesStore?.preferences?.first()?.let { prefs ->
                savedCategoryIds = parseIdList(prefs.heatmapCategoryIds)
                _uiState.update {
                    it.copy(
                        window = HeatmapWindow.fromDays(prefs.heatmapWindowDays),
                        aggregation = parseAggregation(prefs.heatmapAggregation),
                        zoomLevel = prefs.heatmapZoomLevel.coerceIn(0, 2),
                    )
                }
            }

            repository.getAllCategories().collect { cats ->
                val active = cats.filter { !it.isArchived }
                val activeIds = active.map { it.id }.toSet()
                _uiState.update { state ->
                    // First load only: restore the saved selection. Afterwards just drop any
                    // ids that have since been archived, so a manual clear is not undone.
                    val selected = if (!hasRestoredSelection) {
                        savedCategoryIds.filter { it in activeIds }
                    } else {
                        state.selectedCategoryIds.filter { it in activeIds }
                    }
                    state.copy(categories = active, selectedCategoryIds = selected)
                }
                hasRestoredSelection = true
                reloadGrid()
            }
        }
    }

    fun toggleCategory(id: Long) {
        _uiState.update { state ->
            val selected = if (id in state.selectedCategoryIds)
                state.selectedCategoryIds - id
            else
                state.selectedCategoryIds + id
            state.copy(selectedCategoryIds = selected)
        }
        persistSelection()
        reloadGrid()
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCategoryIds = emptyList(), grid = HeatmapData.Empty) }
        persistSelection()
    }

    fun setWindow(window: HeatmapWindow) {
        _uiState.update { it.copy(window = window) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setHeatmapWindowDays(window.days) }
        }
        reloadGrid()
    }

    fun setAggregation(aggregation: HeatmapAggregation) {
        _uiState.update { it.copy(aggregation = aggregation) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setHeatmapAggregation(aggregation.name) }
        }
        reloadGrid()
    }

    fun setZoomLevel(level: Int) {
        val clamped = level.coerceIn(0, 2)
        _uiState.update { it.copy(zoomLevel = clamped) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setHeatmapZoomLevel(clamped) }
        }
    }

    private fun persistSelection() {
        preferencesStore?.let { store ->
            val ids = _uiState.value.selectedCategoryIds
            viewModelScope.launch { store.setHeatmapCategoryIds(ids.joinToString(",")) }
        }
    }

    private fun reloadGrid() {
        val state = _uiState.value
        val ordered = state.selectedCategoryIds
            .mapNotNull { id -> state.categories.firstOrNull { it.id == id } }
        if (ordered.isEmpty()) {
            reloadJob?.cancel()
            _uiState.update { it.copy(grid = HeatmapData.Empty) }
            return
        }
        _uiState.update { it.copy(grid = HeatmapData.Loading) }
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val end = LocalDate.now()
            val start = end.minusDays((state.window.days - 1).toLong())
            val data = computeHeatmapData(
                categories = ordered,
                repository = repository,
                start = start,
                end = end,
                weekly = state.window.weekly,
                aggregation = state.aggregation,
            )
            _uiState.update { it.copy(grid = data) }
        }
    }

    class Factory(
        private val repository: TrackingRepository,
        private val preferencesStore: AppPreferencesStore? = null,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HeatmapViewModel(repository, preferencesStore) as T
        }
    }
}

// ── Prefs parsing helpers (package-level) ──────────────────────────────────────

private fun parseIdList(csv: String): List<Long> =
    csv.split(",").mapNotNull { it.trim().toLongOrNull() }

private fun parseAggregation(s: String): HeatmapAggregation =
    runCatching { HeatmapAggregation.valueOf(s) }.getOrDefault(HeatmapAggregation.AVERAGE)
