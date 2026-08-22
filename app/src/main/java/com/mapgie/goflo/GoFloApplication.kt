package com.mapgie.goflo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mapgie.goflo.data.database.GoFloDatabase
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.repository.CustomAlarmRepository
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.notifications.DailyCheckWorker
import com.mapgie.goflo.notifications.ReminderScheduler
import com.mapgie.goflo.widget.GoFloWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class GoFloApplication : Application() {

    val database by lazy { GoFloDatabase.getInstance(this) }
    val repository by lazy { PeriodRepository(database.periodDao(), database.symptomDao(), database.periodDayDao()) }
    val trackingRepository by lazy {
        TrackingRepository(database.trackingCategoryDao(), database.trackingLogDao(), database.symptomDao())
    }
    val customAlarmRepository by lazy { CustomAlarmRepository(database.customAlarmDao()) }
    val colorProfileDao by lazy { database.colorProfileDao() }
    val preferencesStore by lazy { AppPreferencesStore(this) }
    val securityPreferences by lazy { SecurityPreferences(this) }

    // Application-level coroutine scope for one-shot background work.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory unlock flag — false on every cold start and after the app backgrounds.
    // Not persisted: if the process is killed, the user must re-authenticate.
    var isUnlocked: Boolean = false

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS).build()
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // Re-lock whenever the app is no longer visible. The lock screen
                // will be shown on the next onStart if a PIN is set.
                isUnlocked = false
            }
        })
        appScope.launch { runFlowBackfillIfNeeded() }
        appScope.launch { runSymptomsBackfillIfNeeded() }
        appScope.launch { runFlowLevelRestoreIfNeeded() }
        appScope.launch { reconcileEpisodes() }
    }

    /**
     * Re-derives period episodes from the per-day log on every app start.
     *
     * This is what "deems a period ended": an ongoing episode whose last
     * logged day is now beyond the user's gap-tolerance window gets closed at
     * that last day. The same rebuild also repairs any fragmented or
     * overlapping episodes left behind by older versions, replacing the
     * one-time overlap/adjacency merge fixups that previously ran here.
     */
    private suspend fun reconcileEpisodes() {
        val prefs = preferencesStore.preferences.first()
        repository.reconcile(prefs.periodGapToleranceDays)
        GoFloWidget.updateAllWidgets(this)
    }

    /**
     * One-time migration: copies period flow levels into the TrackingLog system so
     * the Stats screen can query all categories uniformly.
     *
     * Only runs once — guarded by the [flowBackfillDone] preference flag.
     * Skips any date that already has a TrackingLog for the Flow category (preserves
     * manually logged entries and avoids duplicate writes on re-install).
     */
    private suspend fun runFlowBackfillIfNeeded() {
        val prefs = preferencesStore.preferences.first()
        if (prefs.flowBackfillDone) return

        val flowCategory = trackingRepository.getSystemCategoryByKey("flow") ?: run {
            // Flow category not seeded yet — mark done and skip; it will be seeded
            // on next DB open and the user has no historical data yet.
            preferencesStore.setFlowBackfillDone(true)
            return
        }

        val periods = repository.getAllPeriodsOnce()
        for (period in periods) {
            // Support both old enum names ("MEDIUM") and current display labels ("Medium").
            val flowLabel = PeriodRepository.FLOW_ENUM_TO_LABEL[period.flowLevel] ?: period.flowLevel
            if (flowLabel.isBlank()) continue

            val start = runCatching { LocalDate.parse(period.startDate) }.getOrNull() ?: continue
            val end = period.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start

            val dates = generateSequence(start) { d -> if (d < end) d.plusDays(1) else null }.toList()
            trackingRepository.syncFlowLogsForPeriod(flowCategory.id, dates, flowLabel)
        }

        preferencesStore.setFlowBackfillDone(true)
    }

    /**
     * One-time migration: copies period symptoms from the PeriodSymptom table into
     * the TrackingLog system (symptoms category, period start date) so the day sheet
     * shows them under Tracked rather than as an inherent period property.
     */
    private suspend fun runSymptomsBackfillIfNeeded() {
        val prefs = preferencesStore.preferences.first()
        if (prefs.symptomsBackfillDone) return

        val symptomsCategory = trackingRepository.getSystemCategoryByKey("symptoms") ?: run {
            preferencesStore.setSymptomsBackfillDone(true)
            return
        }

        val periods = repository.getAllPeriodsOnce()
        for (period in periods) {
            val start = runCatching { LocalDate.parse(period.startDate) }.getOrNull() ?: continue
            val symptoms = repository.getSymptomsParsed(period.id)
            if (symptoms.isEmpty()) continue
            trackingRepository.saveLog(
                date           = start,
                categoryId     = symptomsCategory.id,
                selectedValues = symptoms,
                notes          = "",
                allowMultiple  = false,
            )
        }

        preferencesStore.setSymptomsBackfillDone(true)
    }

    /**
     * One-time reverse migration: reads each period's flow TrackingLog entry and writes
     * the resolved label back into PeriodEntry.flowLevel for periods that were saved with
     * an empty flowLevel (due to the save bug that wrote "" instead of the selected label).
     *
     * Numeric slider values ("1"–"4") are mapped back to their text labels
     * ("Spotting"/"Light"/"Medium"/"Heavy") so PeriodEntry.flowLevel is always a label.
     * Periods that already have a non-blank flowLevel are skipped.
     */
    private suspend fun runFlowLevelRestoreIfNeeded() {
        val prefs = preferencesStore.preferences.first()
        if (prefs.flowLevelRestoreDone) return

        val flowCategory = trackingRepository.getSystemCategoryByKey("flow") ?: run {
            preferencesStore.setFlowLevelRestoreDone(true)
            return
        }

        val periods = repository.getAllPeriodsOnce()
        for (period in periods) {
            if (period.flowLevel.isNotBlank()) continue
            val startDate = runCatching { LocalDate.parse(period.startDate) }.getOrNull() ?: continue
            val logValue = trackingRepository.getExistingLog(startDate, flowCategory.id)
                ?.values?.firstOrNull() ?: continue
            val label = when (logValue) {
                "1"  -> "Spotting"
                "2"  -> "Light"
                "3"  -> "Medium"
                "4"  -> "Heavy"
                else -> logValue
            }
            if (label.isBlank()) continue
            repository.updateFlowLevel(period, label)
        }

        preferencesStore.setFlowLevelRestoreDone(true)
    }

}
