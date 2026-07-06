package com.mapgie.goflo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mapgie.goflo.GoFloApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Besides boot, this also covers app updates (alarms registered by the old version
// should be recomputed) and timezone/clock changes (all alarms are armed at absolute
// epochs computed from local times, so they drift when the zone or clock moves).
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_TIME_CHANGED
        ) return

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
