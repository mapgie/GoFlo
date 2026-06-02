package com.mapgie.goflo.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapgie.goflo.ui.components.BetaFeedbackBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToCycle: () -> Unit,
    onNavigateToQuickLog: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BetaFeedbackBanner()
            ListItem(
                headlineContent   = { Text("What You Track") },
                supportingContent = { Text("Add and organise tracking categories") },
                leadingContent    = {
                    Icon(Icons.Outlined.Tune, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable(onClick = onNavigateToCategories)
            )
            ListItem(
                headlineContent   = { Text("Cycle") },
                supportingContent = { Text("Cycle length, predictions, and ovulation markers") },
                leadingContent    = {
                    Icon(Icons.Outlined.Autorenew, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable(onClick = onNavigateToCycle)
            )
            ListItem(
                headlineContent   = { Text("One-Tap Quick Log") },
                supportingContent = { Text("Choose what the quick-add button logs") },
                leadingContent    = {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable(onClick = onNavigateToQuickLog)
            )
            ListItem(
                headlineContent   = { Text("Reminders") },
                supportingContent = { Text("Set up period and ovulation alerts") },
                leadingContent    = {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable(onClick = onNavigateToReminders)
            )
        }
    }
}
