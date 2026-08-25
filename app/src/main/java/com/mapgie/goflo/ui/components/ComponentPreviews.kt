package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.GoFloTheme

/**
 * Shared scaffolding for the component-library `@Preview`s: wraps the previewed
 * primitive in [GoFloTheme] (Coral light or Coral dark) on the themed
 * background, so every preview exercises the real colour-resolution path
 * (including [com.mapgie.goflo.ui.theme.LocalExtendedRoles]).
 *
 * Preview-only support code; not for use by screens.
 */
@Composable
internal fun ComponentPreviewSurface(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    GoFloTheme(appTheme = if (dark) AppTheme.CORAL_DARK else AppTheme.CORAL) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                content()
            }
        }
    }
}
