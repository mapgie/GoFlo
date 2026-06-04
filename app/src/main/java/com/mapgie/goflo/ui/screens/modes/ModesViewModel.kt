package com.mapgie.goflo.ui.screens.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.util.AppMode
import com.mapgie.goflo.ui.util.SuggestedCategory
import com.mapgie.goflo.ui.util.toActiveModeSet
import com.mapgie.goflo.ui.util.toActiveModeString
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ModesUiState(
    val activeModes: Set<AppMode> = emptySet(),
    val existingModeKeys: Set<String> = emptySet(),
    val temperatureUnitCelsius: Boolean = true,
    val latestPeriodDate: LocalDate? = null,
)

class ModesViewModel(
    private val trackingRepository: TrackingRepository,
    private val periodRepository: PeriodRepository,
    private val store: AppPreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<ModesUiState> = combine(
        trackingRepository.getAllCategories(),
        store.preferences,
        periodRepository.getAllPeriods(),
    ) { cats, prefs, periods ->
        val latestPeriodDate = periods.maxByOrNull { it.startDate }
            ?.startDate?.let { LocalDate.parse(it) }
        ModesUiState(
            activeModes           = prefs.activeModes.toActiveModeSet(),
            existingModeKeys      = cats.filter { it.modeKey.isNotEmpty() }.map { it.modeKey }.toSet(),
            temperatureUnitCelsius = prefs.temperatureUnitCelsius,
            latestPeriodDate      = latestPeriodDate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModesUiState())

    /**
     * Activates [mode], creating the [selected] categories that don't already exist.
     * For PREGNANCY mode, [pregnancyDateStr] and [pregnancyStartType] are persisted.
     * For FERTILITY mode, the temperature unit preference is set from [temperatureUnitCelsius].
     */
    fun activateMode(
        mode: AppMode,
        selected: List<SuggestedCategory>,
        pregnancyDateStr: String = "",
        pregnancyStartType: String = "EDD",
        temperatureUnitCelsius: Boolean = true,
    ) {
        viewModelScope.launch {
            val existingKeys = trackingRepository.getExistingModeKeys()

            for (cat in selected) {
                if (cat.modeKey in existingKeys) continue
                val (resolvedMin, resolvedMax, resolvedUnit) = if (cat.modeKey == "bbt_temperature") {
                    if (temperatureUnitCelsius) Triple(35.0f, 42.0f, "°C")
                    else                        Triple(96.0f, 104.0f, "°F")
                } else Triple(cat.numericMin, cat.numericMax, cat.numericUnit)

                val id = trackingRepository.addCategory(
                    name          = cat.name,
                    iconName      = cat.iconName,
                    colorToken    = cat.colorToken,
                    categoryType  = cat.categoryType,
                    numericMin    = resolvedMin,
                    numericMax    = resolvedMax,
                    allowDecimals = cat.allowDecimals,
                    numericUnit   = resolvedUnit,
                    modeKey       = cat.modeKey,
                )
                cat.values.forEachIndexed { index, label ->
                    // Insert via DAO directly since addValueToCategory goes through repository
                    trackingRepository.addValueToCategory(id, label)
                }
            }

            // Persist mode-specific settings
            if (mode == AppMode.PREGNANCY && pregnancyDateStr.isNotEmpty()) {
                store.setPregnancyDate(pregnancyDateStr, pregnancyStartType)
            }
            if (mode == AppMode.FERTILITY) {
                store.setTemperatureUnitCelsius(temperatureUnitCelsius)
            }

            val current = store.preferences.first().activeModes.toActiveModeSet().toMutableSet()
            current.add(mode)
            store.setActiveModes(current.toActiveModeString())
        }
    }

    /**
     * Deactivates [mode]. Categories created by the mode are left in place;
     * the user can archive them manually if wanted.
     */
    fun deactivateMode(mode: AppMode) {
        viewModelScope.launch {
            val current = store.preferences.first().activeModes.toActiveModeSet().toMutableSet()
            current.remove(mode)
            store.setActiveModes(current.toActiveModeString())
        }
    }

    fun setTemperatureUnit(celsius: Boolean) {
        viewModelScope.launch { store.setTemperatureUnitCelsius(celsius) }
    }

    class Factory(
        private val trackingRepository: TrackingRepository,
        private val periodRepository: PeriodRepository,
        private val store: AppPreferencesStore,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ModesViewModel(trackingRepository, periodRepository, store) as T
        }
    }
}
