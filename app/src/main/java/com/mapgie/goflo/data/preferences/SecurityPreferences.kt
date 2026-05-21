package com.mapgie.goflo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "goflo_security")

data class SecuritySettings(
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val lastAcknowledgedVersion: Int = 0
)

val SecuritySettings.hasPinSet: Boolean get() = pinHash.isNotBlank()

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_ACKNOWLEDGED_VERSION = intPreferencesKey("last_acknowledged_version")
    }

    val settings: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        SecuritySettings(
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            lastAcknowledgedVersion = prefs[Keys.LAST_ACKNOWLEDGED_VERSION] ?: 0
        )
    }

    suspend fun setPin(hash: String, salt: String) {
        context.securityDataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
        }
    }

    suspend fun clearPin() {
        context.securityDataStore.edit {
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun acknowledgeVersion(versionCode: Int) {
        context.securityDataStore.edit { it[Keys.LAST_ACKNOWLEDGED_VERSION] = versionCode }
    }
}
