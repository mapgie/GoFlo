package com.mapgie.goflo.notifications

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.GoFloApplication
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.GoFloTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = application as GoFloApplication
        val prefs = runBlocking { app.preferencesStore.preferences.first() }
        val theme = runCatching { AppTheme.valueOf(prefs.theme) }
            .getOrDefault(AppTheme.CORAL)

        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL)?.takeIf { it.isNotBlank() }
            ?: "Time to log"
        val alarmTitle = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: ""
        val customAlarmId = intent.getLongExtra(EXTRA_CUSTOM_ALARM_ID, -1L)
        val categoryIdsStr = intent.getStringExtra(EXTRA_CATEGORY_IDS) ?: ""
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

        val categoryIds = categoryIdsStr
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0 }

        setContent {
            GoFloTheme(appTheme = theme, wcag = prefs.wcagMode) {
                if (customAlarmId != -1L) {
                    CustomAlarmScreen(
                        label = alarmLabel,
                        categoryIds = categoryIds,
                        snoozeMinutes = snoozeMinutes,
                        onDismiss = {
                            dismissNotification(notifId)
                            finish()
                        },
                        onSnooze = {
                            dismissNotification(notifId)
                            scheduleSnooze(customAlarmId, snoozeMinutes)
                            finish()
                        },
                        onLog = { categoryId ->
                            dismissNotification(notifId)
                            openLogScreen(categoryId)
                            finish()
                        },
                    )
                } else {
                    AlarmScreen(
                        label = alarmLabel,
                        subtitle = alarmTitle,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    private fun dismissNotification(notifId: Int) {
        if (notifId != -1) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
        }
    }

    private fun scheduleSnooze(alarmId: Long, snoozeMinutes: Int) {
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = ACTION_CUSTOM_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_NOTIF_ID, -1)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        sendBroadcast(snoozeIntent)
    }

    private fun openLogScreen(categoryId: Long) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (categoryId != -1L) putExtra("category_id_deep_link", categoryId)
        }
        startActivity(launchIntent)
    }

    companion object {
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_CUSTOM_ALARM_ID = "custom_alarm_id"
        const val EXTRA_CATEGORY_IDS = "category_ids"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}

@Composable
private fun CustomAlarmScreen(
    label: String,
    categoryIds: List<Long>,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onLog: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(40.dp))
            if (categoryIds.isNotEmpty()) {
                Button(
                    onClick = { onLog(categoryIds.first()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log now")
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                    Text("Snooze ${snoozeMinutes}m")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun AlarmScreen(
    label: String,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
