package com.mapgie.goflo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mapgie.goflo.data.database.GoFloDatabase
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.notifications.ReminderScheduler

class GoFloApplication : Application() {

    val database by lazy { GoFloDatabase.getInstance(this) }
    val repository by lazy { PeriodRepository(database.periodDao(), database.symptomDao(), database.customSymptomDao()) }
    val trackingRepository by lazy { TrackingRepository(database.trackingCategoryDao(), database.trackingLogDao()) }
    val preferencesStore by lazy { AppPreferencesStore(this) }
    val securityPreferences by lazy { SecurityPreferences(this) }

    // In-memory unlock flag — false on every cold start and after the app backgrounds.
    // Not persisted: if the process is killed, the user must re-authenticate.
    var isUnlocked: Boolean = false

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // Re-lock whenever the app is no longer visible. The lock screen
                // will be shown on the next onStart if a PIN is set.
                isUnlocked = false
            }
        })
    }
}
