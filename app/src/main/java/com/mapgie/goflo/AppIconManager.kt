package com.mapgie.goflo

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The icon choices available to the user.  Each entry maps to a distinct
 * `<activity-alias>` declared in AndroidManifest.xml.
 *
 * Changing the icon is now fully independent of the colour theme; users pick
 * whichever option they prefer (including discreet icons that give no hint
 * about the app's purpose).
 */
enum class AppIconChoice(
    /** The simple name of the corresponding `<activity-alias>` entry. */
    val aliasSimpleName: String,
    /** Short label shown in the icon picker. */
    val displayName: String,
) {
    // ── Drop icons (themed water-drop shape) ──────────────────────────────────
    DROP_CORAL   ("MainActivityCoral",    "Coral"),
    DROP_TEAL    ("MainActivityTeal",     "Teal"),
    DROP_GREEN   ("MainActivityGreen",    "Sage"),
    DROP_CONTRAST("MainActivityContrast", "Dark"),
    DROP_BLUE    ("MainActivityBlue",     "Blue"),
    // ── Discreet icons (no period-app association) ────────────────────────────
    LEAF         ("MainActivityLeaf",     "Leaf"),
    MOON         ("MainActivityMoon",     "Moon"),
    STAR         ("MainActivityStar",     "Star"),
}

/**
 * Manages the dynamic launcher icon independently of the selected colour theme.
 *
 * Switching is idempotent: calling [applyIcon] with the same choice twice is safe.
 * [PackageManager.DONT_KILL_APP] ensures the running process is never restarted.
 */
object AppIconManager {

    /**
     * Enables the launcher alias for [choice] and disables all others.
     *
     * Safe to call from any thread.  The launcher picks up the change
     * asynchronously (typically within a few seconds on stock Android).
     */
    fun applyIcon(context: Context, choice: AppIconChoice) {
        val pm  = context.packageManager
        val pkg = context.packageName

        AppIconChoice.entries.forEach { c ->
            val component = ComponentName(pkg, "$pkg.${c.aliasSimpleName}")
            val state = if (c == choice) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            // setComponentEnabledSetting can throw if the alias is missing from the
            // manifest (e.g., in a test build) or if Android rate-limits rapid calls.
            // Swallow safely — a stale launcher icon is preferable to a crash.
            runCatching {
                pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
