package com.mapgie.goflo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mapgie.goflo.GoFloApplication
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.R
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

const val ACTION_CUSTOM_LOG = "com.mapgie.goflo.ACTION_CUSTOM_LOG"
const val ACTION_CUSTOM_SNOOZE = "com.mapgie.goflo.ACTION_CUSTOM_SNOOZE"
const val EXTRA_NOTIF_ID = "notif_id"
const val EXTRA_CATEGORY_ID = "category_id"
const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"

// Unexported — AlarmManager targets this via explicit component intent, so no export needed.
// This keeps other apps from firing fake reminder notifications.
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createChannel(context)
        when (intent.action) {
            // The three prediction reminders are one-shot alarms: after showing the
            // notification, recompute and re-arm the next occurrences so the chain
            // does not depend on a reboot or a settings visit.
            ACTION_PREPERIOD -> showNotification(
                context,
                id = 1,
                title = context.getString(R.string.notification_preperiod_title),
                text = context.getString(R.string.notification_preperiod_text),
                useAlarm = intent.getBooleanExtra(EXTRA_USE_ALARM_CHANNEL, false),
                useSilent = intent.getBooleanExtra(EXTRA_USE_SILENT_CHANNEL, false),
                alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "",
            )
            ACTION_OVULATION -> showNotification(
                context,
                id = 2,
                title = context.getString(R.string.notification_ovulation_title),
                text = context.getString(R.string.notification_ovulation_text),
                useAlarm = intent.getBooleanExtra(EXTRA_USE_ALARM_CHANNEL, false),
                useSilent = intent.getBooleanExtra(EXTRA_USE_SILENT_CHANNEL, false),
                alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "",
            )
            ACTION_DAILY -> showNotification(
                context,
                id = 3,
                title = context.getString(R.string.notification_daily_title),
                text = context.getString(R.string.notification_daily_text),
                useAlarm = intent.getBooleanExtra(EXTRA_USE_ALARM_CHANNEL, false),
                useSilent = intent.getBooleanExtra(EXTRA_USE_SILENT_CHANNEL, false),
                alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "",
            )
            ACTION_CUSTOM_ALARM -> handleCustomAlarm(context, intent)
        }

        if (intent.action == ACTION_PREPERIOD || intent.action == ACTION_OVULATION || intent.action == ACTION_DAILY) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ReminderScheduler.refreshPredictionReminders(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun handleCustomAlarm(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as GoFloApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = app.customAlarmRepository.getById(alarmId) ?: return@launch
                // A snoozed firing can arrive after the user disabled the alarm; the
                // regular chain is cancelled on disable, but state can also change
                // between arming and firing.
                if (!alarm.isEnabled) return@launch

                // Re-arm the next occurrence before evaluating today's condition so an
                // exception below (or a condition miss) can never break the daily chain.
                if (alarm.isRecurring) {
                    ReminderScheduler.scheduleCustomAlarm(context, alarm)
                }

                val periods = app.repository.getAllPeriods().first()
                val avg = PeriodRepository.calculateAvgCycleLength(periods)
                val today = LocalDate.now()

                val conditionMet = when (alarm.scheduleType) {
                    "DAILY" -> true
                    "DURING_PERIOD" -> PeriodRepository.activePeriod(periods) != null
                    "NOT_DURING_PERIOD" -> PeriodRepository.activePeriod(periods) == null
                    "DAYS_BEFORE_PERIOD" -> {
                        val nextStart = PeriodRepository.predictNextStart(periods, avg)
                        nextStart != null && today == nextStart.minusDays(alarm.daysOffset.toLong())
                    }
                    "DAYS_AFTER_PERIOD" -> {
                        val active = PeriodRepository.activePeriod(periods)
                        active != null && active.startDate.let { s ->
                            runCatching { LocalDate.parse(s) }.getOrNull()
                                ?.plusDays(alarm.daysOffset.toLong()) == today
                        }
                    }
                    "DAY_OF_PERIOD" -> {
                        val active = PeriodRepository.activePeriod(periods)
                        active != null && active.startDate.let { s ->
                            runCatching { LocalDate.parse(s) }.getOrNull()
                                ?.plusDays((alarm.dayOfPeriod - 1).toLong()) == today
                        }
                    }
                    else -> true
                }

                if (conditionMet) {
                    val categoryIds = app.customAlarmRepository.getCategoryIdsForAlarm(alarmId)
                    showCustomAlarmNotification(context, alarm, categoryIds)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showCustomAlarmNotification(
        context: Context,
        alarm: CustomAlarm,
        categoryIds: List<Long>,
    ) {
        val notifId = (10000 + alarm.id).toInt()
        val firstCategoryId = categoryIds.firstOrNull() ?: -1L

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (firstCategoryId != -1L) {
                putExtra("category_id_deep_link", firstCategoryId)
            }
        }
        val tapPending = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_CUSTOM_LOG
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_CATEGORY_ID, firstCategoryId)
        }
        val logPending = PendingIntent.getBroadcast(
            context,
            (20000 + alarm.id).toInt(),
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_CUSTOM_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            (30000 + alarm.id).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when {
            alarm.alarmType == "NOTIFICATION" -> CHANNEL_CUSTOM_NOTIF_ID
            alarm.overrideDnd -> CHANNEL_CUSTOM_DND_ID
            else -> CHANNEL_CUSTOM_ALARM_ID
        }
        val displayTitle = alarm.label.ifBlank { "Time to log" }

        val isSilent = alarm.alarmType == "SILENT"
        val priority = if (alarm.alarmType == "NOTIFICATION") {
            NotificationCompat.PRIORITY_DEFAULT
        } else {
            NotificationCompat.PRIORITY_MAX
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(displayTitle)
            .setContentText(if (categoryIds.isEmpty()) "Tap to open GoFlo" else "Tap to log")
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(priority)
            .setCategory(
                if (alarm.alarmType == "NOTIFICATION") NotificationCompat.CATEGORY_REMINDER
                else NotificationCompat.CATEGORY_ALARM
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_input_add,
                "Log",
                logPending
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze ${alarm.snoozeDurationMinutes}m",
                snoozePending
            )

        if (!isSilent && alarm.alarmType == "ALARM") {
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AlarmActivity.EXTRA_ALARM_LABEL, displayTitle)
                putExtra(AlarmActivity.EXTRA_ALARM_TITLE, if (categoryIds.isEmpty()) "" else "Tap Log to open the logging screen")
                putExtra(AlarmActivity.EXTRA_CUSTOM_ALARM_ID, alarm.id)
                putExtra(AlarmActivity.EXTRA_CATEGORY_IDS, categoryIds.joinToString(","))
                putExtra(AlarmActivity.EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
                putExtra(AlarmActivity.EXTRA_NOTIF_ID, notifId)
            }
            val fullScreenPending = PendingIntent.getActivity(
                context, notifId + 1000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPending, true)
        } else if (isSilent) {
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(AlarmActivity.EXTRA_ALARM_LABEL, displayTitle)
                putExtra(AlarmActivity.EXTRA_ALARM_TITLE, "")
                putExtra(AlarmActivity.EXTRA_CUSTOM_ALARM_ID, alarm.id)
                putExtra(AlarmActivity.EXTRA_CATEGORY_IDS, categoryIds.joinToString(","))
                putExtra(AlarmActivity.EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
                putExtra(AlarmActivity.EXTRA_NOTIF_ID, notifId)
            }
            val fullScreenPending = PendingIntent.getActivity(
                context, notifId + 2000, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPending, true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, builder.build())
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        text: String,
        useAlarm: Boolean = false,
        useSilent: Boolean = false,
        alarmLabel: String = "",
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when {
            useSilent -> CHANNEL_SILENT_ID
            useAlarm  -> CHANNEL_ID
            else      -> CHANNEL_NOTIF_ID
        }
        val displayTitle = if (useAlarm && alarmLabel.isNotBlank()) alarmLabel else title
        val priority = when {
            useSilent -> NotificationCompat.PRIORITY_LOW
            useAlarm  -> NotificationCompat.PRIORITY_MAX
            else      -> NotificationCompat.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(displayTitle)
            .setContentText(text)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .setPriority(priority)
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
