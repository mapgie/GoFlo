package com.mapgie.goflo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun GoFloTheme(
    appTheme:    AppTheme = AppTheme.CORAL,
    wcag:        Boolean  = false,
    customHues:  Triple<Float, Float, Float>? = null,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = if (appTheme == AppTheme.CUSTOM && customHues != null) {
        buildCustomColorScheme(customHues.first, customHues.second, customHues.third, isDark = false)
    } else {
        colorSchemeFor(appTheme, systemDark, wcag)
    }

    // Derived "is dark" flag accounts for all system-following themes.
    val effectivelyDark = when (appTheme) {
        AppTheme.SYSTEM,
        AppTheme.CORAL_SYSTEM,
        AppTheme.GREEN_SYSTEM -> systemDark
        else                  -> appTheme.isDark
    }

    // Flip status-bar icon contrast so they remain readable on dark surfaces.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !effectivelyDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = GoFloTypography,
        content     = content,
    )
}
