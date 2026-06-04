package com.mapgie.goflo.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mapgie.goflo.MainActivity

// Handles Log and Snooze quick-action taps from custom alarm notifications.
// Unexported — only targeted by explicit component PendingIntents from within the app.
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != -1) manager.cancel(notifId)

        when (intent.action) {
            ACTION_CUSTOM_LOG -> {
                val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    if (categoryId != -1L) putExtra("category_id_deep_link", categoryId)
                }
                context.startActivity(launchIntent)
            }
            ACTION_CUSTOM_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
                val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ACTION_CUSTOM_ALARM
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (40000 + alarmId).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
    }
}
