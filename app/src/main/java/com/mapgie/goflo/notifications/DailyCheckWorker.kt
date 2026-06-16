package com.mapgie.goflo.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.data.database.GoFloDatabase
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/** Notification ID used for daily-check nudges. Replaces itself so only one fires per day. */
private const val DAILY_CHECK_NOTIF_ID = 2000

/**
 * Periodic worker that runs once a day and sends a gentle nudge if:
 *  1. A predicted period start date has passed 1-3 days ago without a period being logged, OR
 *  2. No tracking-category entry has been logged today (and Check 1 did not fire).
 *
 * Only fires between 09:00 and 21:00 local time. Skipped silently outside that window.
 * The worker is enqueued by [com.mapgie.goflo.GoFloApplication] with
 * [androidx.work.ExistingPeriodicWorkPolicy.KEEP] so it survives app restarts.
 */
class DailyCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferencesStore(context).preferences.first()
        if (!prefs.dailyCheckEnabled) return Result.success()

        val now = LocalTime.now()
        if (now.isBefore(LocalTime.of(9, 0)) || now.isAfter(LocalTime.of(21, 0))) {
            return Result.success()
        }

        val db = GoFloDatabase.getInstance(context)
        val periodRepo = PeriodRepository(db.periodDao(), db.symptomDao())
        val today = LocalDate.now()

        // ── Check 1: predicted period missed ──────────────────────────────────
        val periods = periodRepo.getAllPeriodsOnce()
        if (periods.isNotEmpty()) {
            val avgCycle = PeriodRepository.calculateAvgCycleLength(periods)
            val predicted = PeriodRepository.predictNextStart(periods, avgCycle)
            if (predicted != null) {
                val daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(predicted, today).toInt()
                if (daysOverdue in 1..3) {
                    // Check if any period was logged for the predicted date or the days since.
                    val loggedInWindow = (0 until daysOverdue).any { offset ->
                        val checkDate = predicted.plusDays(offset.toLong())
                        PeriodRepository.periodForDate(periods, checkDate) != null
                    }
                    if (!loggedInWindow) {
                        sendNotification(context, "Your period may have started. Tap to log it.")
                        return Result.success()
                    }
                }
            }
        }

        // ── Check 2: daily tracking not yet logged ────────────────────────────
        val categoryDao = db.trackingCategoryDao()
        val logDao = db.trackingLogDao()

        val activeCategories = categoryDao.getAllCategoriesOnce()
            .filter { !it.isArchived && !it.isSystem }

        if (activeCategories.isNotEmpty()) {
            val todayStr = today.toString()
            val loggedCategoryIds = logDao.getLogsForDateOnce(todayStr)
                .map { it.categoryId }
                .toSet()

            val anyUnlogged = activeCategories.any { it.id !in loggedCategoryIds }
            if (anyUnlogged) {
                sendNotification(context, "Don't forget to log your daily health tracking.")
            }
        }

        return Result.success()
    }

    private fun sendNotification(context: Context, message: String) {
        val tapIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tapPi = PendingIntent.getActivity(
            context,
            DAILY_CHECK_NOTIF_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_CHECK_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GoFlo")
            .setContentText(message)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(DAILY_CHECK_NOTIF_ID, notification)
    }
}
