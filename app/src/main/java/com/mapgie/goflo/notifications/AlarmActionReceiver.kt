package com.mapgie.goflo.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                // Shared identity with cancelCustomAlarm, so deleting or disabling the
                // alarm also cancels a pending snoozed firing.
                ReminderScheduler.setAlarm(
                    context,
                    triggerAt,
                    ReminderScheduler.customAlarmSnoozePendingIntent(context, alarmId)
                )
            }
        }
    }
}
