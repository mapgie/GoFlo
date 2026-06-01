package com.mapgie.goflo.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
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
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToReminders: () -> Unit,
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
            ListItem(
                headlineContent   = { Text("What You Track") },
                supportingContent = { Text("Add and organise tracking categories") },
                leadingContent    = {
                    Icon(Icons.Outlined.Category, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onNavigateToCategories)
            )
            ListItem(
                headlineContent   = { Text("Reminders") },
                supportingContent = { Text("Set up period and ovulation alerts") },
                leadingContent    = {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onNavigateToReminders)
            )
        }
    }
}
