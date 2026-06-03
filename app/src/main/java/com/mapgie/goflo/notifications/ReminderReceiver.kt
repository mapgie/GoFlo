package com.mapgie.goflo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.R

// Unexported — AlarmManager targets this via explicit component intent, so no export needed.
// This keeps other apps from firing fake reminder notifications.
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createChannel(context)
        val useAlarm = intent.getBooleanExtra(EXTRA_USE_ALARM_CHANNEL, false)
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: ""
        when (intent.action) {
            ACTION_PREPERIOD -> showNotification(
                context,
                id = 1,
                title = context.getString(R.string.notification_preperiod_title),
                text = context.getString(R.string.notification_preperiod_text),
                useAlarm = useAlarm,
                alarmLabel = alarmLabel,
            )
            ACTION_OVULATION -> showNotification(
                context,
                id = 2,
                title = context.getString(R.string.notification_ovulation_title),
                text = context.getString(R.string.notification_ovulation_text),
                useAlarm = useAlarm,
                alarmLabel = alarmLabel,
            )
            ACTION_DAILY -> showNotification(
                context,
                id = 3,
                title = context.getString(R.string.notification_daily_title),
                text = context.getString(R.string.notification_daily_text),
                useAlarm = useAlarm,
                alarmLabel = alarmLabel,
            )
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        useAlarm: Boolean = false,
        alarmLabel: String = "",
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (useAlarm) CHANNEL_ID else CHANNEL_NOTIF_ID
        val displayTitle = if (useAlarm && alarmLabel.isNotBlank()) alarmLabel else title

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(displayTitle)
            .setContentText(text)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(if (useAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (useAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (useAlarm) {
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AlarmActivity.EXTRA_ALARM_LABEL, alarmLabel.ifBlank { title })
                putExtra(AlarmActivity.EXTRA_ALARM_TITLE, text)
            }
            val fullScreenPending = PendingIntent.getActivity(
                context, id + 1000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPending, true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }
}
