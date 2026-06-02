package com.mapgie.goflo.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCycleScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val customEnabled = prefs.preferredCycleLength > 0

            ListItem(
                headlineContent    = { Text("Custom cycle length") },
                supportingContent  = {
                    Text(if (customEnabled) "Using ${prefs.preferredCycleLength} days"
                         else "Auto. calculated from your history")
                },
                trailingContent    = {
                    Switch(
                        checked         = customEnabled,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier
                    .semantics { role = Role.Switch }
                    .clickable {
                        viewModel.setPreferredCycleLength(
                            if (customEnabled) 0
                            else prefs.preferredCycleLength.coerceIn(21, 45).let { if (it == 0) 28 else it }
                        )
                    }
            )

            if (customEnabled) {
                var sliderDays by remember(prefs.preferredCycleLength) {
                    mutableStateOf(prefs.preferredCycleLength.toFloat())
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Cycle length: ${sliderDays.toInt()} days",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value                 = sliderDays,
                        onValueChange         = { sliderDays = it },
                        onValueChangeFinished = { viewModel.setPreferredCycleLength(sliderDays.toInt()) },
                        valueRange            = 21f..45f,
                        steps                 = 23
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("21 days", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("45 days", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Show period predictions") },
                supportingContent = { Text("Display predicted future period days on the calendar") },
                trailingContent   = { Switch(checked = prefs.showPeriodPrediction, onCheckedChange = null) },
                modifier          = Modifier
                    .semantics { role = Role.Switch }
                    .clickable { viewModel.setShowPeriodPrediction(!prefs.showPeriodPrediction) }
            )

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Show ovulation markers") },
                supportingContent = { Text("Display ovulation day and fertility window on the calendar") },
                trailingContent   = { Switch(checked = prefs.showOvulationMarkers, onCheckedChange = null) },
                modifier          = Modifier
                    .semantics { role = Role.Switch }
                    .clickable { viewModel.setShowOvulationMarkers(!prefs.showOvulationMarkers) }
            )
        }
    }
}
