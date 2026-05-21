package com.mapgie.goflo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.GoFloApplication
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.preferences.hasPinSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppState { LOADING, DISCLAIMER, LOCKED, READY }

class MainViewModel(
    private val securityPreferences: SecurityPreferences,
    private val app: GoFloApplication
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState.LOADING)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // Exposed for the Settings screen to react to security changes.
    val securitySettings = securityPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            com.mapgie.goflo.data.preferences.SecuritySettings())

    init {
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            _appState.value = resolveState(settings.lastAcknowledgedVersion, settings.hasPinSet)
        }
    }

    // Called from MainActivity.onStart to re-evaluate lock state after the app
    // returns from background (GoFloApplication.isUnlocked was reset to false by the
    // ProcessLifecycleOwner observer registered in GoFloApplication).
    fun onActivityStart() {
        if (_appState.value != AppState.READY && _appState.value != AppState.LOCKED) return
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            if (settings.hasPinSet && !app.isUnlocked) {
                _appState.value = AppState.LOCKED
            }
        }
    }

    fun acknowledgeDisclaimer() {
        viewModelScope.launch {
            securityPreferences.acknowledgeVersion(BuildConfig.VERSION_CODE)
            val settings = securityPreferences.settings.first()
            _appState.value = if (settings.hasPinSet && !app.isUnlocked) {
                AppState.LOCKED
            } else {
                app.isUnlocked = true
                AppState.READY
            }
        }
    }

    fun onUnlocked() {
        app.isUnlocked = true
        _appState.value = AppState.READY
    }

    private fun resolveState(lastAcknowledgedVersion: Int, hasPinSet: Boolean): AppState {
        if (lastAcknowledgedVersion != BuildConfig.VERSION_CODE) return AppState.DISCLAIMER
        if (hasPinSet && !app.isUnlocked) return AppState.LOCKED
        app.isUnlocked = true
        return AppState.READY
    }

    class Factory(
        private val securityPreferences: SecurityPreferences,
        private val app: GoFloApplication
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(securityPreferences, app) as T
        }
    }
}
