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
        when (intent.action) {
            ACTION_PREPERIOD -> showNotification(
                context,
                id = 1,
                title = context.getString(R.string.notification_preperiod_title),
                text = context.getString(R.string.notification_preperiod_text)
            )
            ACTION_OVULATION -> showNotification(
                context,
                id = 2,
                title = context.getString(R.string.notification_ovulation_title),
                text = context.getString(R.string.notification_ovulation_text)
            )
            ACTION_DAILY -> showNotification(
                context,
                id = 3,
                title = context.getString(R.string.notification_daily_title),
                text = context.getString(R.string.notification_daily_text)
            )
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
