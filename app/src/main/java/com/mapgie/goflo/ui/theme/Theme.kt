package com.mapgie.goflo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun GoFloTheme(appTheme: AppTheme = AppTheme.CORAL, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorSchemeFor(appTheme),
        typography = GoFloTypography,
        content = content
    )
}
