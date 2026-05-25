package com.mapgie.goflo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.theme.BannerStyle
import com.mapgie.goflo.ui.theme.ComfortaaFamily

/**
 * Home-screen top bar with an optional decorative Canvas layer drawn between the
 * solid [BannerStyle] background and the "GoFlo" title text.
 *
 * Window-inset handling (status-bar padding) is delegated to [TopAppBar] exactly
 * as before.  The [Color.Transparent] containerColor prevents the underlying
 * Surface from painting its own background, letting [Modifier.drawWithContent]
 * control the full draw order:
 *
 *   1. Solid primaryContainer fill
 *   2. Decorative shapes (secondary + tertiary colours)
 *   3. Title text and any TopAppBar chrome
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerTopBar(bannerStyle: BannerStyle) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
    val secondary      = MaterialTheme.colorScheme.secondary
    val tertiary       = MaterialTheme.colorScheme.tertiary

    TopAppBar(
        title = {
            Text(
                text       = "GoFlo",
                style      = MaterialTheme.typography.headlineMedium,
                fontFamily = ComfortaaFamily,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = Color.Transparent,   // our drawWithContent owns the bg
            titleContentColor = contentColor,
        ),
        modifier = Modifier.drawWithContent {
            // 1 — solid background
            drawRect(containerColor)
            // 2 — decorative shapes behind text
            drawBannerDecoration(bannerStyle, secondary, tertiary)
            // 3 — title text (and any future actions / navigation icon)
            drawContent()
        },
    )
}

// ── Shared draw dispatcher (used by SettingsScreen preview too) ───────────────

/**
 * Draws the shapes for [style] into the current [DrawScope].
 * Exposed as `internal` so the settings banner-style picker can reuse the same
 * drawing code for its mini-previews without duplicating the path maths.
 */
internal fun DrawScope.drawBannerDecoration(
    style:     BannerStyle,
    secondary: Color,
    tertiary:  Color,
) = when (style) {
    BannerStyle.PLAIN   -> Unit
    BannerStyle.WAVES   -> drawWaves(secondary, tertiary)
    BannerStyle.BUBBLES -> drawBubbles(secondary, tertiary)
}

// ── Wave shapes ───────────────────────────────────────────────────────────────

/**
 * Two filled ribbon bands that sweep in from the right edge of the banner.
 *
 * Each band is a cubic bezier that rises from the bottom-right toward the
 * top-right corner, filling the triangular area below the curve down to the
 * bottom edge. The tertiary band is drawn first (further back), the secondary
 * band on top.
 *
 * Fractional coordinates keep the shapes proportionate on any screen width / density.
 */
private fun DrawScope.drawWaves(secondary: Color, tertiary: Color) {
    val w = size.width
    val h = size.height

    // Band 1 — tertiary (behind)
    drawPath(
        path = Path().apply {
            moveTo(w * 0.28f, h)
            cubicTo(
                w * 0.50f, h * 0.60f,
                w * 0.72f, h * 0.18f,
                w,         0f,
            )
            lineTo(w, h)
            close()
        },
        color = tertiary.copy(alpha = 0.78f),
    )

    // Band 2 — secondary (in front)
    drawPath(
        path = Path().apply {
            moveTo(w * 0.46f, h)
            cubicTo(
                w * 0.62f, h * 0.52f,
                w * 0.80f, h * 0.10f,
                w,         h * -0.08f,
            )
            lineTo(w, h)
            close()
        },
        color = secondary.copy(alpha = 0.78f),
    )
}

// ── Bubble shapes ─────────────────────────────────────────────────────────────

private data class BubbleSpec(
    val xFrac: Float,
    val yFrac: Float,
    val rFrac: Float,      // radius as a fraction of banner width
    val isSecondary: Boolean,
)

/**
 * Fixed set of circles scattered on the right side of the banner.
 * Positions are deterministic (not random) so they don't shift on recomposition.
 * Some circles intentionally extend past the right or top edge to give a
 * "floating out of frame" effect — clipping is handled naturally by the
 * composable's draw bounds.
 */
private val BUBBLE_SPECS = listOf(
    BubbleSpec(0.92f, 0.50f, 0.20f, false),  // large,  tertiary, partially clipped right
    BubbleSpec(0.76f, 0.22f, 0.13f, true),   // medium, secondary
    BubbleSpec(0.85f, 0.84f, 0.09f, false),  // small,  tertiary
    BubbleSpec(0.63f, 0.55f, 0.06f, true),   // tiny,   secondary
    BubbleSpec(1.02f, 0.14f, 0.16f, true),   // medium, secondary, clipped at right edge
)

private fun DrawScope.drawBubbles(secondary: Color, tertiary: Color) {
    val w = size.width
    val h = size.height
    BUBBLE_SPECS.forEach { spec ->
        drawCircle(
            color  = (if (spec.isSecondary) secondary else tertiary).copy(alpha = 0.65f),
            center = Offset(w * spec.xFrac, h * spec.yFrac),
            radius = w * spec.rFrac,
        )
    }
}

// ── Mini-preview canvas (used by SettingsScreen) ──────────────────────────────

/**
 * A small [Canvas] that renders a thumbnail-scale preview of [style].
 * The preview uses live theme colours so it always looks correct regardless of
 * which theme the user has active.
 */
@Composable
fun BannerStylePreview(
    style:    BannerStyle,
    modifier: Modifier = Modifier,
) {
    val bg        = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(80.dp, 40.dp)) {
        drawRect(bg)
        drawBannerDecoration(style, secondary, tertiary)
    }
}
