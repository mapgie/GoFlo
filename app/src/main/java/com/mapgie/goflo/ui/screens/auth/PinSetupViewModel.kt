package com.mapgie.goflo.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinSetupStep { VERIFY_CURRENT, ENTER_NEW, CONFIRM_NEW }

data class PinSetupUiState(
    val step: PinSetupStep = PinSetupStep.ENTER_NEW,
    val enteredDigits: String = "",
    val pendingPin: String = "",
    val isError: Boolean = false,
    val errorMessage: String = "",
    val isDone: Boolean = false
)

class PinSetupViewModel(
    private val securityPreferences: SecurityPreferences,
    private val isChanging: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PinSetupUiState(step = if (isChanging) PinSetupStep.VERIFY_CURRENT else PinSetupStep.ENTER_NEW)
    )
    val uiState: StateFlow<PinSetupUiState> = _uiState.asStateFlow()

    fun onDigit(digit: Int) {
        val current = _uiState.value.enteredDigits
        if (current.length >= 6) return
        val updated = current + digit.toString()
        _uiState.update { it.copy(enteredDigits = updated, isError = false) }
        if (updated.length >= 4) advanceStep(updated)
    }

    fun onDelete() {
        val current = _uiState.value.enteredDigits
        if (current.isNotEmpty()) _uiState.update { it.copy(enteredDigits = current.dropLast(1), isError = false) }
    }

    private fun advanceStep(pin: String) {
        when (_uiState.value.step) {
            PinSetupStep.VERIFY_CURRENT -> verifyCurrent(pin)
            PinSetupStep.ENTER_NEW -> {
                _uiState.update { it.copy(
                    step = PinSetupStep.CONFIRM_NEW,
                    pendingPin = pin,
                    enteredDigits = ""
                ) }
            }
            PinSetupStep.CONFIRM_NEW -> confirmAndSave(pin)
        }
    }

    private fun verifyCurrent(pin: String) {
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            val valid = withContext(Dispatchers.Default) {
                settings.hasPinSet && PinManager.verifyPin(pin, settings.pinHash, settings.pinSalt)
            }
            if (valid) {
                _uiState.update { it.copy(step = PinSetupStep.ENTER_NEW, enteredDigits = "", isError = false) }
            } else {
                _uiState.update { it.copy(enteredDigits = "", isError = true, errorMessage = "Incorrect PIN") }
            }
        }
    }

    private fun confirmAndSave(confirmPin: String) {
        val pending = _uiState.value.pendingPin
        if (confirmPin != pending) {
            _uiState.update { it.copy(
                step = PinSetupStep.ENTER_NEW,
                enteredDigits = "",
                pendingPin = "",
                isError = true,
                errorMessage = "PINs did not match — please try again"
            ) }
            return
        }
        viewModelScope.launch {
            val salt = withContext(Dispatchers.Default) { PinManager.generateSalt() }
            val hash = withContext(Dispatchers.Default) { PinManager.hashPin(confirmPin, salt) }
            securityPreferences.setPin(hash, salt)
            _uiState.update { it.copy(isDone = true) }
        }
    }

    class Factory(
        private val securityPreferences: SecurityPreferences,
        private val isChanging: Boolean
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PinSetupViewModel(securityPreferences, isChanging) as T
        }
    }
}
