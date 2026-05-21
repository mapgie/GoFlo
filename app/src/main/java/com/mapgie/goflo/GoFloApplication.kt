package com.mapgie.goflo

import android.app.Application
import com.mapgie.goflo.data.database.GoFloDatabase
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.notifications.ReminderScheduler

class GoFloApplication : Application() {

    val database by lazy { GoFloDatabase.getInstance(this) }
    val repository by lazy { PeriodRepository(database.periodDao(), database.symptomDao()) }
    val preferencesStore by lazy { AppPreferencesStore(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
    }
}
