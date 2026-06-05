package com.mapgie.goflo.ui.screens.manage

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Hub that groups everything to do with alarms and notifications: the two
 * scheduling screens (custom alarms, period reminders) plus the system
 * permissions they depend on.
 *
 * Permissions are surfaced here with live granted/not-granted status so the
 * user can grant them in context as they set things up, rather than hunting
 * for a separate settings list. State is re-read on every resume so a grant
 * made in system settings is reflected when the user returns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsHubScreen(
    onBack: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToReminders: () -> Unit,
) {
    val context = LocalContext.current

    var notificationsGranted by remember { mutableStateOf(areNotificationsGranted(context)) }
    var exactAlarmsGranted   by remember { mutableStateOf(areExactAlarmsGranted(context)) }
    var dndAccessGranted     by remember { mutableStateOf(isDndAccessGranted(context)) }

    // Re-read permission state whenever the screen resumes, so a grant made in
    // the system settings screen is reflected as soon as the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = areNotificationsGranted(context)
                exactAlarmsGranted   = areExactAlarmsGranted(context)
                dndAccessGranted     = isDndAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms & Notifications") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HubNavItem(
                title    = "Custom alarms",
                subtitle = "One-off and recurring alarms tied to your tracking",
                icon     = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick  = onNavigateToAlarms,
            )
            HubNavItem(
                title    = "Period reminders",
                subtitle = "Period and ovulation alerts",
                icon     = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick  = onNavigateToReminders,
            )

            HubSectionHeader("Permissions")

            PermissionRow(
                title    = "Notifications",
                granted  = notificationsGranted,
                grantedText   = "Allowed",
                ungrantedText = "Tap to allow",
                icon     = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick  = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionRow(
                    title    = "Exact alarms",
                    granted  = exactAlarmsGranted,
                    grantedText   = "Allowed",
                    ungrantedText = "Tap to allow",
                    icon     = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick  = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }

            PermissionRow(
                title    = "Do Not Disturb access",
                subtitle = "Lets alarms sound when Do Not Disturb is on",
                granted  = dndAccessGranted,
                grantedText   = "Allowed",
                ungrantedText = "Tap to allow",
                icon     = { Icon(Icons.Outlined.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick  = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
    }
}

@Composable
private fun HubSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun HubNavItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent    = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        },
        trailingContent   = {
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    )
}

/**
 * A permission entry showing its live grant state. The status is communicated
 * by both an icon (shape) and a text label, never colour alone, so it remains
 * legible for colour-blind users.
 */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    grantedText: String,
    ungrantedText: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val statusText = if (granted) grantedText else ungrantedText
    val statusIcon = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
    val statusTint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = subtitle?.let { s -> { Text(s) } },
        leadingContent    = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        },
        trailingContent   = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(18.dp))
                Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusTint)
            }
        },
        modifier = Modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    )
}

// ── Permission state helpers ────────────────────────────────────────────────

private fun areNotificationsGranted(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun areExactAlarmsGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    } else true

private fun isDndAccessGranted(context: Context): Boolean =
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .isNotificationPolicyAccessGranted
