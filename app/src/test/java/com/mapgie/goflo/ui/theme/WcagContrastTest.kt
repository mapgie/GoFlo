package com.mapgie.goflo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What every built-in theme guarantees about colour contrast, read from the
 * real [colorSchemeFor] rather than a copied table, so a palette tweak in
 * Color.kt can't quietly undo it.
 *
 * Designed palettes meet WCAG AA (4.5:1 text, 3:1 UI). The "WCAG accessible
 * colours" toggle swaps in the AAA variants: text at 7:1 on every ground it
 * sits on, outlines at 4.5:1. `wcag_check.py` and `wcag_check_roles.py` stay as
 * quick local tools; this test is the one CI runs.
 */
class WcagContrastTest {

    private companion object {
        const val AAA_TEXT = 7f
        const val AA_TEXT = 4.5f
        const val UI = 3f

        /** Already maximum-contrast; the toggle leaves them alone. */
        val ACCESSIBILITY_THEMES = setOf(
            AppTheme.HIGH_CONTRAST_LIGHT,
            AppTheme.HIGH_CONTRAST_DARK,
            AppTheme.BLUE_ORANGE,
        )
    }

    private data class Case(val theme: AppTheme, val systemIsDark: Boolean) {
        val label get() = "${theme.name} (system ${if (systemIsDark) "dark" else "light"})"
        fun scheme(wcag: Boolean) = colorSchemeFor(theme, systemIsDark, wcag)
    }

    // CUSTOM is built at runtime from the user's picks, so it has no fixed palette to check.
    private val cases = AppTheme.entries
        .filter { it != AppTheme.CUSTOM }
        .flatMap { listOf(Case(it, systemIsDark = false), Case(it, systemIsDark = true)) }

    private val standardCases = cases.filter { it.theme !in ACCESSIBILITY_THEMES }

    private fun contrastRatio(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    private fun assertReads(what: String, fg: Color, bg: Color, floor: Float) {
        val ratio = contrastRatio(fg, bg)
        assertTrue("$what is ${"%.2f".format(ratio)}:1, below $floor:1", ratio >= floor)
    }

    /** The pairs wcag_check.py checks, with its thresholds. */
    private fun aaPairs(s: ColorScheme) = listOf(
        Triple("onPrimary on primary", s.onPrimary to s.primary, AA_TEXT),
        Triple("onSecondary on secondary", s.onSecondary to s.secondary, AA_TEXT),
        Triple("onTertiary on tertiary", s.onTertiary to s.tertiary, AA_TEXT),
        Triple("onPrimaryContainer", s.onPrimaryContainer to s.primaryContainer, AA_TEXT),
        Triple("onSecondaryContainer", s.onSecondaryContainer to s.secondaryContainer, AA_TEXT),
        Triple("onTertiaryContainer", s.onTertiaryContainer to s.tertiaryContainer, AA_TEXT),
        Triple("onBackground", s.onBackground to s.background, AA_TEXT),
        Triple("onSurface", s.onSurface to s.surface, AA_TEXT),
        Triple("onSurfaceVariant", s.onSurfaceVariant to s.surfaceVariant, AA_TEXT),
        Triple("outline on surfaceVariant", s.outline to s.surfaceVariant, UI),
        Triple("outline on background", s.outline to s.background, UI),
        Triple("primary on primaryContainer", s.primary to s.primaryContainer, UI),
        Triple("primary on surfaceVariant", s.primary to s.surfaceVariant, UI),
        Triple("primary on background", s.primary to s.background, UI),
        Triple("primary on surface", s.primary to s.surface, UI),
    )

    @Test
    fun `every designed theme meets AA for text and UI`() {
        cases.forEach { case ->
            aaPairs(case.scheme(wcag = false)).forEach { (what, pair, floor) ->
                assertReads("${case.label} $what", pair.first, pair.second, floor)
            }
        }
    }

    @Test
    fun `wcag mode lifts every text role to 7 to 1 on its ground`() {
        standardCases.forEach { case ->
            val s = case.scheme(wcag = true)
            val textPairs = listOf(
                "onPrimary on primary" to (s.onPrimary to s.primary),
                "onSecondary on secondary" to (s.onSecondary to s.secondary),
                "onTertiary on tertiary" to (s.onTertiary to s.tertiary),
                "onPrimaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                "onSecondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                "onTertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
                "onBackground" to (s.onBackground to s.background),
                "onSurface" to (s.onSurface to s.surface),
                "onSurfaceVariant on surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
                "onSurfaceVariant on background" to (s.onSurfaceVariant to s.background),
                "primary on background" to (s.primary to s.background),
                "primary on surface" to (s.primary to s.surface),
                "secondary on background" to (s.secondary to s.background),
                "tertiary on background" to (s.tertiary to s.background),
            )
            textPairs.forEach { (what, pair) ->
                assertReads("${case.label} wcag $what", pair.first, pair.second, AAA_TEXT)
            }
        }
    }

    @Test
    fun `wcag mode lifts the outline to 4 and a half to 1 on every ground`() {
        standardCases.forEach { case ->
            val s = case.scheme(wcag = true)
            assertReads("${case.label} wcag outline on background", s.outline, s.background, AA_TEXT)
            assertReads("${case.label} wcag outline on surfaceVariant", s.outline, s.surfaceVariant, AA_TEXT)
        }
    }

    @Test
    fun `wcag mode keeps the primary indicator visible on its container`() {
        standardCases.forEach { case ->
            val s = case.scheme(wcag = true)
            assertReads("${case.label} wcag primary on primaryContainer", s.primary, s.primaryContainer, UI)
        }
    }

    @Test
    fun `every designed palette has its own wcag variant`() {
        // A new palette added without a branch in colorSchemeFor's wcag table
        // falls through to its designed scheme, so the toggle would do nothing.
        standardCases.forEach { case ->
            assertNotSame(case.label, case.scheme(wcag = false), case.scheme(wcag = true))
        }
    }

    @Test
    fun `wcag mode leaves the high contrast and colour-blind themes as they are`() {
        cases.filter { it.theme in ACCESSIBILITY_THEMES }.forEach { case ->
            assertSame(case.label, case.scheme(wcag = false), case.scheme(wcag = true))
        }
    }
}
