package com.mapgie.goflo.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.components.BetaFeedbackBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToCycle: () -> Unit,
    onNavigateToQuickLog: () -> Unit,
    onNavigateToModes: () -> Unit,
    onNavigateToNotifications: () -> Unit,
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
            ManageNavItem(
                title    = "Tracking Modes",
                subtitle = "Fertility, pregnancy, PCOS, endo, HRT, and more",
                icon     = { Icon(Icons.Outlined.ViewList, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick  = onNavigateToModes,
            )
            ManageNavItem(
                title = "What You Track",
                subtitle = "Add and organise tracking categories",
                icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToCategories
            )
            ManageNavItem(
                title = "Cycle",
                subtitle = "Cycle length, predictions, and ovulation markers",
                icon = { Icon(Icons.Outlined.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToCycle
            )
            ManageNavItem(
                title = "One-Tap Quick Log",
                subtitle = "Choose what the quick-add button logs",
                icon = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToQuickLog
            )
            ManageNavItem(
                title = "Alarms & Notifications",
                subtitle = "Alarms, period reminders, and their permissions",
                icon = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToNotifications
            )
        }
    }
}

@Composable
private fun ManageNavItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent    = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                icon()
            }
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
