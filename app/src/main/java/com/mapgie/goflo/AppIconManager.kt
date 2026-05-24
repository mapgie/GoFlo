package com.mapgie.goflo

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.mapgie.goflo.ui.theme.AppTheme

/**
 * Manages the dynamic launcher icon so it reflects the active [AppTheme].
 *
 * Each colour variant maps to a distinct `<activity-alias>` declared in
 * AndroidManifest.xml.  Only one alias may be enabled at a time; the launcher
 * picks up the change asynchronously (typically within a few seconds on stock
 * Android; timing varies by launcher).
 *
 * Switching is idempotent: calling [applyIcon] with the same theme twice is safe.
 * The implementation always calls [PackageManager.setComponentEnabledSetting] with
 * [PackageManager.DONT_KILL_APP] so the running process is never restarted.
 */
object AppIconManager {

    /**
     * The five launcher-icon variants.  The name of each entry mirrors the
     * `android:name` suffix declared in the corresponding `<activity-alias>`.
     */
    enum class IconVariant(val aliasSimpleName: String) {
        CORAL   ("MainActivityCoral"),
        TEAL    ("MainActivityTeal"),
        GREEN   ("MainActivityGreen"),
        CONTRAST("MainActivityContrast"),
        BLUE    ("MainActivityBlue"),
    }

    /** Maps every [AppTheme] to its matching [IconVariant]. */
    fun variantFor(theme: AppTheme): IconVariant = when (theme) {
        AppTheme.CORAL,
        AppTheme.CORAL_DARK                                          -> IconVariant.CORAL

        AppTheme.TURQUOISE,
        AppTheme.TURQUOISE_DARK,
        AppTheme.SYSTEM                                              -> IconVariant.TEAL

        AppTheme.GREEN,
        AppTheme.GREEN_DARK                                          -> IconVariant.GREEN

        AppTheme.HIGH_CONTRAST_LIGHT,
        AppTheme.HIGH_CONTRAST_DARK                                  -> IconVariant.CONTRAST

        AppTheme.BLUE_ORANGE                                         -> IconVariant.BLUE
    }

    /**
     * Enables the launcher-alias icon that matches [theme] and disables all others.
     *
     * Must be called from a thread that is allowed to make Binder IPC calls
     * (any thread except the main-thread-restricted ones; in practice the main
     * thread is fine as the call returns quickly).
     */
    fun applyIcon(context: Context, theme: AppTheme) {
        val pm  = context.packageManager
        val pkg = context.packageName
        val target = variantFor(theme)

        IconVariant.entries.forEach { variant ->
            val component = ComponentName(pkg, "$pkg.${variant.aliasSimpleName}")
            val state = if (variant == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }
}
