package com.mapgie.goflo.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Derives the tonal "container" fill for a category role colour by blending the
 * accent toward the surface, mirroring the house pattern in
 * [com.mapgie.goflo.ui.util.ordinalShade].
 *
 * The redesign's selected states are tonal fills (chip fills, hero containers,
 * segment fills). Material's ColorScheme only carries containers for its three
 * built-in accents, so for an arbitrary role colour (quaternary/quinary/senary
 * or a fixed hex) the container is derived here instead. Text placed on the
 * result should use onSurface/onSurfaceVariant: at the default fraction the
 * fill stays close enough to the surface that surface-level text contrast is
 * preserved in both light and dark themes.
 */
fun roleContainerTint(role: Color, surface: Color, fraction: Float = 0.25f): Color =
    lerp(surface, role, fraction.coerceIn(0f, 1f))
