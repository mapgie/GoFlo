package com.mapgie.goflo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapgie.goflo.GoFloApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // goAsync() extends the BroadcastReceiver's active window so the OS doesn't
        // kill the process before the coroutine finishes rescheduling alarms.
        val pendingResult = goAsync()
        val app = context.applicationContext as GoFloApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val periods = app.repository.getAllPeriods().first()
                val prefs = app.preferencesStore.preferences.first()
                ReminderScheduler.rescheduleAll(context, periods, prefs.reminder)
                val customAlarms = app.customAlarmRepository.getEnabledAlarms()
                ReminderScheduler.rescheduleCustomAlarms(context, customAlarms)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
