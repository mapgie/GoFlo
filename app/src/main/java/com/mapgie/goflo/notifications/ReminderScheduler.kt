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
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.preferences.ReminderSettings
import com.mapgie.goflo.data.repository.PeriodRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

const val ACTION_PREPERIOD = "com.mapgie.goflo.ACTION_PREPERIOD_REMINDER"
const val ACTION_OVULATION = "com.mapgie.goflo.ACTION_OVULATION_REMINDER"
const val ACTION_DAILY = "com.mapgie.goflo.ACTION_DAILY_PERIOD_REMINDER"
const val CHANNEL_ID = "goflo_reminders_v1"

object ReminderScheduler {

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Period Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for period tracking"
            setSound(alarmUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    fun rescheduleAll(context: Context, periods: List<PeriodEntry>, settings: ReminderSettings) {
        cancelAll(context)
        val avg = PeriodRepository.calculateAvgCycleLength(periods)
        val reminderHour = settings.reminderHour
        val reminderMinute = settings.reminderMinute

        if (settings.preperiodEnabled) {
            val nextStart = PeriodRepository.predictNextStart(periods, avg)
            if (nextStart != null) {
                val triggerDate = nextStart.minusDays(settings.preperiodDaysBefore.toLong())
                scheduleAt(context, ACTION_PREPERIOD, triggerDate, reminderHour, reminderMinute)
            }
        }

        if (settings.ovulationEnabled) {
            val ovulation = PeriodRepository.ovulationDate(periods, avg)
            if (ovulation != null && !ovulation.isBefore(LocalDate.now())) {
                scheduleAt(context, ACTION_OVULATION, ovulation, reminderHour, reminderMinute)
            }
        }

        if (settings.dailyDuringPeriodEnabled) {
            val active = PeriodRepository.activePeriod(periods)
            if (active != null) {
                scheduleDailyRepeating(context, reminderHour, reminderMinute)
            }
        }
    }

    fun cancelAll(context: Context) {
        listOf(ACTION_PREPERIOD, ACTION_OVULATION, ACTION_DAILY).forEach { action ->
            cancel(context, action)
        }
    }

    private fun scheduleAt(
        context: Context,
        action: String,
        date: LocalDate,
        hour: Int,
        minute: Int
    ) {
        val triggerAt = LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAt <= System.currentTimeMillis()) return

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) return
        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, action)
        )
    }

    private fun scheduleDailyRepeating(context: Context, hour: Int, minute: Int) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        var triggerAt = LocalDateTime.of(today.year, today.month, today.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= now) triggerAt += 24 * 60 * 60 * 1000L

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) return
        alarm.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context, ACTION_DAILY)
        )
    }

    private fun cancel(context: Context, action: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, action))
    }

    private fun pendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
