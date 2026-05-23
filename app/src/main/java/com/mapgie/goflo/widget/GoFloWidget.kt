package com.mapgie.goflo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mapgie.goflo.GoFloApplication
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.R
import com.mapgie.goflo.data.repository.PeriodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 2×1 home-screen widget showing:
 *  - While period is active: "Period · day N"
 *  - Otherwise: "Period in N days" (or "Period due today/tomorrow")
 *  - Secondary line: current cycle day / avg cycle length
 *
 * Tapping anywhere on the widget opens MainActivity.
 *
 * Data is refreshed every 30 minutes by the OS (the system minimum) and also
 * whenever [updateAllWidgets] is called — e.g. after the user logs a period.
 */
class GoFloWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // goAsync() tells the OS the broadcast isn't done yet; finish() releases it.
        // Each widget ID gets its own async slot to avoid a single slow DB read
        // blocking the others (in practice they all share the same data).
        appWidgetIds.forEach { widgetId ->
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    pushUpdate(context, appWidgetManager, widgetId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {

        /**
         * Pushes a fresh data snapshot to every GoFlo widget on the home screen.
         * Safe to call from any coroutine; runs the DB read on [Dispatchers.IO].
         */
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, GoFloWidget::class.java)
            )
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                ids.forEach { id -> pushUpdate(context, manager, id) }
            }
        }

        // ── Private ──────────────────────────────────────────────────────────

        private suspend fun pushUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
        ) {
            val app        = context.applicationContext as GoFloApplication
            val repository = app.repository
            val periods    = repository.getAllPeriods().first()

            val avg          = PeriodRepository.calculateAvgCycleLength(periods)
            val activePeriod = PeriodRepository.activePeriod(periods)
            val cycleDay     = PeriodRepository.cycleDay(periods)
            val nextStart    = PeriodRepository.predictNextStart(periods, avg)

            val (status, subtitle) = buildDisplayStrings(
                periodsEmpty = periods.isEmpty(),
                activePeriod = activePeriod != null,
                cycleDay     = cycleDay,
                avgCycle     = avg,
                nextStart    = nextStart,
            )

            val views = RemoteViews(context.packageName, R.layout.widget_goflo).apply {
                setTextViewText(R.id.widget_status, status)
                setTextViewText(R.id.widget_subtitle, subtitle)
                setViewVisibility(
                    R.id.widget_subtitle,
                    if (subtitle.isNotEmpty()) View.VISIBLE else View.GONE
                )
                // Tap → launch app
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun buildDisplayStrings(
            periodsEmpty: Boolean,
            activePeriod: Boolean,
            cycleDay: Int?,
            avgCycle: Int,
            nextStart: LocalDate?,
        ): Pair<String, String> {
            if (periodsEmpty) return "Tap to get started" to ""

            if (activePeriod) {
                val day = cycleDay ?: 1
                return "Period · day $day" to "Avg cycle: $avgCycle days"
            }

            if (nextStart != null) {
                val today  = LocalDate.now()
                val daysTo = ChronoUnit.DAYS.between(today, nextStart).toInt()
                val primary = when {
                    daysTo <= 0 -> "Period due today"
                    daysTo == 1 -> "Period due tomorrow"
                    else        -> "Period in $daysTo days"
                }
                val secondary = "Day ${cycleDay ?: "?"} of ~$avgCycle"
                return primary to secondary
            }

            return "GoFlo" to ""
        }
    }
}
