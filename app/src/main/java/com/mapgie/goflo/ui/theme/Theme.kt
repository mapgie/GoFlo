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
    appTheme:        AppTheme = AppTheme.CORAL,
    wcag:            Boolean  = false,
    customHues:      Triple<Float, Float, Float>? = null,
    customArgbs:     Triple<Int, Int, Int>? = null,
    customThemeMode: String   = "LIGHT",
    customLightBackgroundArgb: Int = 0,
    customDarkBackgroundArgb:  Int = 0,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val customIsDark = when (customThemeMode) {
        "DARK"   -> true
        "SYSTEM" -> systemDark
        else     -> false
    }
    val colorScheme = if (appTheme == AppTheme.CUSTOM && customHues != null) {
        buildCustomColorScheme(
            primaryHue    = customHues.first,
            secondaryHue  = customHues.second,
            tertiaryHue   = customHues.third,
            primaryArgb    = customArgbs?.first  ?: 0,
            secondaryArgb  = customArgbs?.second ?: 0,
            tertiaryArgb   = customArgbs?.third  ?: 0,
            backgroundArgb = if (customIsDark) customDarkBackgroundArgb else customLightBackgroundArgb,
            isDark         = customIsDark,
        )
    } else {
        colorSchemeFor(appTheme, systemDark, wcag)
    }

    val systemFollowingThemes = setOf(
        AppTheme.SYSTEM, AppTheme.CORAL_SYSTEM, AppTheme.GREEN_SYSTEM,
        AppTheme.SUMMER_CANDY_SYSTEM, AppTheme.BEACH_VIBES_SYSTEM, AppTheme.PEACH_MELBA_SYSTEM,
        AppTheme.DISCO_SYSTEM, AppTheme.METAL_CHICK_SYSTEM, AppTheme.WHIMSY_SYSTEM,
        AppTheme.COLOUR_HAPPY_SYSTEM, AppTheme.DRAGON_FIRE_SYSTEM, AppTheme.MIDNIGHT_NEON_SYSTEM,
    )

    // Derived "is dark" flag accounts for all system-following themes.
    val effectivelyDark = when {
        appTheme == AppTheme.CUSTOM       -> customIsDark
        appTheme in systemFollowingThemes -> systemDark
        else                              -> appTheme.isDark
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
