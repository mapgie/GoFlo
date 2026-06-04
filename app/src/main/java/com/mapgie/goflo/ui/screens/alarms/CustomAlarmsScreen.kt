package com.mapgie.goflo.ui.screens.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlarmsScreen(
    viewModel: CustomAlarmsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewAlarm: () -> Unit,
    onNavigateToEditAlarm: (alarmId: Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete alarm") },
            text = { Text("This alarm will be removed and will no longer fire.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAlarm(pendingDeleteId!!)
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewAlarm) {
                Icon(Icons.Default.Add, contentDescription = "Add alarm")
            }
        },
    ) { padding ->
        if (state.alarms.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No alarms yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(state.alarms, key = { it.alarm.id }) { item ->
                    AlarmRow(
                        item = item,
                        onToggleEnabled = { viewModel.setEnabled(item.alarm.id, !item.alarm.isEnabled) },
                        onTap = { onNavigateToEditAlarm(item.alarm.id) },
                        onDelete = { pendingDeleteId = item.alarm.id },
                    )
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    item: AlarmWithCategories,
    onToggleEnabled: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val alarm = item.alarm
    val timeLabel = "%02d:%02d".format(alarm.hour, alarm.minute)
    val scheduleLabel = scheduleLabel(alarm.scheduleType, alarm.daysOffset, alarm.dayOfPeriod)
    val modeLabel = when (alarm.alarmType) {
        "ALARM" -> "Alarm"
        "SILENT" -> "Silent"
        else -> "Notification"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = buildString {
                    append(scheduleLabel)
                    append(" · $modeLabel")
                    if (alarm.overrideDnd) append(" · Override DND")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.categories.isNotEmpty()) {
                Text(
                    text = item.categories.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = null,
            modifier = Modifier
                .semantics { role = Role.Switch }
                .clickable(onClick = onToggleEnabled),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete alarm",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun scheduleLabel(scheduleType: String, daysOffset: Int, dayOfPeriod: Int): String =
    when (scheduleType) {
        "DAILY" -> "Every day"
        "DURING_PERIOD" -> "During period"
        "NOT_DURING_PERIOD" -> "Outside period"
        "DAYS_BEFORE_PERIOD" -> "$daysOffset day${if (daysOffset == 1) "" else "s"} before period"
        "DAYS_AFTER_PERIOD" -> "$daysOffset day${if (daysOffset == 1) "" else "s"} after period starts"
        "DAY_OF_PERIOD" -> "Day $dayOfPeriod of period"
        else -> scheduleType
    }
