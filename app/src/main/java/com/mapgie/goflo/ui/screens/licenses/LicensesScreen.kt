package com.mapgie.goflo.ui.screens.licenses

// MAINTAINER NOTE: keep this list in sync with gradle/libs.versions.toml.
// Add an entry here whenever a new runtime dependency is added to the project.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Library(val name: String, val copyright: String)

private val apache2Libraries = listOf(
    Library("AndroidX Core KTX", "The Android Open Source Project"),
    Library("AndroidX Lifecycle Runtime KTX", "The Android Open Source Project"),
    Library("AndroidX Lifecycle ViewModel Compose", "The Android Open Source Project"),
    Library("AndroidX Lifecycle Process", "The Android Open Source Project"),
    Library("AndroidX Activity Compose", "The Android Open Source Project"),
    Library("AndroidX Compose BOM", "The Android Open Source Project"),
    Library("AndroidX Compose UI", "The Android Open Source Project"),
    Library("AndroidX Compose Material3", "The Android Open Source Project"),
    Library("AndroidX Compose Material Icons Extended", "The Android Open Source Project"),
    Library("AndroidX Navigation Compose", "The Android Open Source Project"),
    Library("AndroidX Room Runtime", "The Android Open Source Project"),
    Library("AndroidX Room KTX", "The Android Open Source Project"),
    Library("AndroidX DataStore Preferences", "The Android Open Source Project"),
    Library("AndroidX Biometric", "The Android Open Source Project"),
    Library("KotlinX Coroutines Android", "JetBrains s.r.o."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Apache 2.0 Licence", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "The following libraries are included under the Apache 2.0 Licence:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            apache2Libraries.forEach { lib ->
                Column {
                    Text("• ${lib.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "  Copyright © ${lib.copyright}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "See: https://www.apache.org/licenses/LICENSE-2.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
