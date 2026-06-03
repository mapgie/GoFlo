package com.mapgie.goflo.notifications

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.GoFloApplication
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

        setContent {
            GoFloTheme(appTheme = theme, wcag = prefs.wcagMode) {
                AlarmScreen(
                    label = alarmLabel,
                    subtitle = alarmTitle,
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_TITLE = "alarm_title"
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
