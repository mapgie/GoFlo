package com.mapgie.goflo.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.preferences.ReminderSettings
import com.mapgie.goflo.data.repository.PeriodRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val ACTION_PREPERIOD = "com.mapgie.goflo.ACTION_PREPERIOD_REMINDER"
const val ACTION_OVULATION = "com.mapgie.goflo.ACTION_OVULATION_REMINDER"
const val ACTION_DAILY = "com.mapgie.goflo.ACTION_DAILY_PERIOD_REMINDER"
const val ACTION_CUSTOM_ALARM = "com.mapgie.goflo.ACTION_CUSTOM_ALARM"

const val CHANNEL_ID = "goflo_reminders_v1"
const val CHANNEL_NOTIF_ID = "goflo_notifications_v1"
const val CHANNEL_SILENT_ID = "goflo_reminders_silent_v1"
const val CHANNEL_CUSTOM_ALARM_ID = "goflo_custom_alarms_v1"
const val CHANNEL_CUSTOM_DND_ID = "goflo_custom_alarms_dnd_v1"
const val CHANNEL_CUSTOM_NOTIF_ID = "goflo_custom_notifs_v1"
const val CHANNEL_DAILY_CHECK_ID = "goflo_daily_check_v1"

const val EXTRA_USE_ALARM_CHANNEL = "use_alarm_channel"
const val EXTRA_USE_SILENT_CHANNEL = "use_silent_channel"
const val EXTRA_ALARM_LABEL = "alarm_label"
const val EXTRA_ALARM_ID = "alarm_id"

object ReminderScheduler {

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val alarmChannel = NotificationChannel(
                CHANNEL_ID,
                "Period Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm-style reminders for period tracking"
                setSound(alarmUri, alarmAttrs)
            }
            manager.createNotificationChannel(alarmChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_NOTIF_ID) == null) {
            val notifChannel = NotificationChannel(
                CHANNEL_NOTIF_ID,
                "Period Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard notifications for period tracking"
            }
            manager.createNotificationChannel(notifChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_SILENT_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SILENT_ID,
                    "Silent Reminders",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Reminders delivered silently with no sound or vibration"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_CUSTOM_ALARM_ID) == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CUSTOM_ALARM_ID,
                    "Custom Alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sound alarms for custom reminders"
                    setSound(alarmUri, alarmAttrs)
                    enableVibration(true)
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_CUSTOM_DND_ID) == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CUSTOM_DND_ID,
                    "Custom Alarms (Priority)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarms that override Do Not Disturb"
                    setSound(alarmUri, alarmAttrs)
                    enableVibration(true)
                    setBypassDnd(true)
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_CUSTOM_NOTIF_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CUSTOM_NOTIF_ID,
                    "Custom Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Standard notifications for custom reminders"
                }
            )
        }

        if (manager.getNotificationChannel(CHANNEL_DAILY_CHECK_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DAILY_CHECK_ID,
                    "Daily check-ins",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Gentle daily reminders"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    fun rescheduleAll(context: Context, periods: List<PeriodEntry>, settings: ReminderSettings) {
        cancelAll(context)
        val avg = PeriodRepository.calculateAvgCycleLength(periods)
        val reminderHour = settings.reminderHour
        val reminderMinute = settings.reminderMinute
        val useAlarm = settings.deliveryMode == "ALARM"
        val useSilent = settings.deliveryMode == "SILENT"
        val alarmLabel = settings.alarmLabel

        if (settings.preperiodEnabled) {
            val nextStart = PeriodRepository.predictNextStart(periods, avg)
            if (nextStart != null) {
                val triggerDate = nextStart.minusDays(settings.preperiodDaysBefore.toLong())
                scheduleAt(context, ACTION_PREPERIOD, triggerDate, reminderHour, reminderMinute, useAlarm, alarmLabel, useSilent)
            }
        }

        if (settings.ovulationEnabled) {
            val ovulation = PeriodRepository.ovulationDate(periods, avg)
            if (ovulation != null && !ovulation.isBefore(LocalDate.now())) {
                scheduleAt(context, ACTION_OVULATION, ovulation, reminderHour, reminderMinute, useAlarm, alarmLabel, useSilent)
            }
        }

        if (settings.dailyDuringPeriodEnabled) {
            val active = PeriodRepository.activePeriod(periods)
            if (active != null) {
                scheduleDailyRepeating(context, reminderHour, reminderMinute, useAlarm, alarmLabel, useSilent)
            }
        }
    }

    fun cancelAll(context: Context) {
        listOf(ACTION_PREPERIOD, ACTION_OVULATION, ACTION_DAILY).forEach { action ->
            cancel(context, action)
        }
    }

    // ── Custom alarm scheduling ───────────────────────────────────────────────

    fun scheduleCustomAlarm(context: Context, alarm: CustomAlarm) {
        if (!alarm.isEnabled) return
        val triggerAt = nextTriggerMillis(alarm.hour, alarm.minute)
        val pi = customAlarmPendingIntent(context, alarm.id)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancelCustomAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(customAlarmPendingIntent(context, alarmId))
    }

    fun rescheduleCustomAlarms(context: Context, alarms: List<CustomAlarm>) {
        alarms.forEach { alarm ->
            cancelCustomAlarm(context, alarm.id)
            if (alarm.isEnabled) scheduleCustomAlarm(context, alarm)
        }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        var millis = LocalDateTime.of(today, LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= now) millis += 86_400_000L
        return millis
    }

    fun customAlarmPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_CUSTOM_ALARM)
            .putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            (10000 + alarmId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ── Legacy helpers ────────────────────────────────────────────────────────

    private fun scheduleAt(
        context: Context,
        action: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        useAlarm: Boolean,
        alarmLabel: String = "",
        useSilent: Boolean = false,
    ) {
        val triggerAt = LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAt <= System.currentTimeMillis()) return

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, action, useAlarm, alarmLabel, useSilent)

        if (useAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun scheduleDailyRepeating(context: Context, hour: Int, minute: Int, useAlarm: Boolean, alarmLabel: String = "", useSilent: Boolean = false) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        var triggerAt = LocalDateTime.of(today.year, today.month, today.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= now) triggerAt += 24 * 60 * 60 * 1000L

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context, ACTION_DAILY, useAlarm, alarmLabel, useSilent)
        )
    }

    private fun cancel(context: Context, action: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // PendingIntent matching ignores extras, so one cancel covers all delivery modes.
        alarm.cancel(pendingIntent(context, action, useAlarm = false))
    }

    private fun pendingIntent(context: Context, action: String, useAlarm: Boolean, alarmLabel: String = "", useSilent: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_USE_ALARM_CHANNEL, useAlarm)
            .putExtra(EXTRA_USE_SILENT_CHANNEL, useSilent)
            .putExtra(EXTRA_ALARM_LABEL, alarmLabel)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
