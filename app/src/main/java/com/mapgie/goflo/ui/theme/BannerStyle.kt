package com.mapgie.goflo.ui.theme

/**
 * Decorative shape style shown behind the "GoFlo" title in the home-screen banner.
 *
 * Each style is drawn via Canvas using the active theme's secondary and tertiary
 * colour-scheme colours, so it automatically adapts to every theme without any
 * extra configuration.
 *
 * The name is persisted to DataStore as a plain string; do NOT rename entries.
 */
enum class BannerStyle(val displayName: String) {
    /** No decoration — plain solid banner. */
    PLAIN("None"),

    /** Two flowing ribbon bands that sweep in from the right side. */
    WAVES("Waves"),

    /** Scattered circles of varying sizes floating on the right side. */
    BUBBLES("Bubbles"),
}
