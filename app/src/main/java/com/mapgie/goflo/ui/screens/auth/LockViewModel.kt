package com.mapgie.goflo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LockUiState(
    val enteredDigits: String = "",
    val biometricEnabled: Boolean = false,
    val isError: Boolean = false,
    val isUnlocked: Boolean = false
)

class LockViewModel(private val securityPreferences: SecurityPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            _uiState.update { it.copy(biometricEnabled = settings.biometricEnabled) }
        }
    }

    fun onDigit(digit: Int) {
        val current = _uiState.value.enteredDigits
        if (current.length >= 6) return
        val updated = current + digit.toString()
        _uiState.update { it.copy(enteredDigits = updated, isError = false) }
        if (updated.length >= 4) verifyPin(updated)
    }

    fun onDelete() {
        val current = _uiState.value.enteredDigits
        if (current.isNotEmpty()) _uiState.update { it.copy(enteredDigits = current.dropLast(1), isError = false) }
    }

    fun onBiometricSuccess() = _uiState.update { it.copy(isUnlocked = true) }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            val valid = withContext(Dispatchers.Default) {
                PinManager.verifyPin(pin, settings.pinHash, settings.pinSalt)
            }
            if (valid) {
                _uiState.update { it.copy(isUnlocked = true) }
            } else {
                _uiState.update { it.copy(enteredDigits = "", isError = true) }
            }
        }
    }

    class Factory(private val securityPreferences: SecurityPreferences) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LockViewModel(securityPreferences) as T
        }
    }
}
