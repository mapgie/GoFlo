package com.mapgie.goflo.ui.screens.licenses

// MAINTAINER NOTE: keep this list in sync with gradle/libs.versions.toml.
// Add an entry here whenever a new RUNTIME dependency is added to the project.
// Compose library versions are pinned via the Compose BOM (currently 2025.05.01).
// Last verified against libs.versions.toml: added androidx-lifecycle-runtime-compose (Alarms & Notifications hub).
//
// Excluded — not shipped in the release APK:
//   junit                    (testImplementation only)
//   androidx-room-compiler   (ksp — annotation processor, compile-time only)
//   androidx-ui-tooling      (debugImplementation only)

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private data class Library(val name: String, val copyright: String, val url: String)

private data class LicenseGroup(
    val spdxId: String,
    val title: String,
    val preamble: String,
    val url: String,
    val libraries: List<Library>
)

private val licenseGroups = listOf(
    LicenseGroup(
        spdxId = "Apache-2.0",
        title = "Apache 2.0 Licence",
        preamble = "The following libraries are included under the Apache 2.0 Licence:",
        url = "https://www.apache.org/licenses/LICENSE-2.0",
        libraries = listOf(
            Library("AndroidX Core KTX", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Lifecycle Runtime KTX", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Lifecycle Runtime Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Lifecycle ViewModel Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Lifecycle Process", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Activity Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose BOM", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose UI", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose UI Graphics", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose UI Tooling Preview", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose Material3", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose Material Icons Extended", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose UI Text Google Fonts", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Navigation Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Room Runtime", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Room KTX", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX DataStore Preferences", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Biometric", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("KotlinX Coroutines Android", "JetBrains s.r.o.", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
        )
    )
)

private data class FontAsset(val name: String, val author: String, val license: String, val url: String)

private val oflFonts = listOf(
    FontAsset("Comfortaa", "Johan Aakerlund", "SIL Open Font License 1.1", "https://scripts.sil.org/OFL"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
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
            licenseGroups.forEach { group ->
                Text(group.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    group.preamble,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                group.libraries.forEach { lib ->
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
                    group.spdxId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .semantics { role = Role.Button }
                        .clickable { uriHandler.openUri(group.url) }
                        .padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
            }
            Spacer(Modifier.height(8.dp))
            Text("SIL Open Font Licence 1.1", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "The following fonts are included under the SIL Open Font Licence 1.1 (OFL):",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            oflFonts.forEach { font ->
                Column {
                    Text("• ${font.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "  Copyright © ${font.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "OFL-1.1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable { uriHandler.openUri(oflFonts.first().url) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}
