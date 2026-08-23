# Subsystem map: Theme / colour machinery

> **Mapped against**
> - Commit: `d07d947` (`d07d947f5b2463eaa08e6521d3228026c55b2bef`)
> - versionCode **116**, versionName **0.53.0-beta.1**, DB schema version **23**
> - Date: 2026-08-22
>
> **Staleness check for future sessions:** `colorSchemeFor`'s signature is load-bearing (the handover says extend, don't change it). Confirm it before wiring role derivation: `grep -n "fun colorSchemeFor" app/src/main/java/com/mapgie/goflo/ui/theme/Color.kt`. Run `git diff d07d947 -- app/src/main/java/com/mapgie/goflo/ui/theme/ app/src/main/java/com/mapgie/goflo/ui/util/CategoryAppearance.kt` for drift.

> **Drift since stamp (Phase 1, branch `claude/logging-redesign-phase-1-f08e0o`):**
> - `Color.kt` gained `ExtendedRoles` + `deriveExtendedRoles(scheme)` at the end of the file (after `buildCustomColorScheme`). HSL hue rotation of the three accents (+30/-30/+45 degrees), with a lightness-shift fallback for near-greyscale accents (saturation < 0.10, i.e. High Contrast). On-colours pick near-black/white by max contrast. `colorSchemeFor` untouched.
> - `Theme.kt` gained `LocalExtendedRoles` (static CompositionLocal) and now wraps `MaterialTheme` in a `CompositionLocalProvider`.
> - `CategoryAppearance.kt`: `CategoryColor` now has 6 entries (added QUATERNARY/QUINARY/SENARY); both resolvers have branches for the three new tokens reading `LocalExtendedRoles.current` (read inside the branch only, so hex/legacy tokens still resolve outside `GoFloTheme`).
> - `ManageCategoriesScreen.kt`'s `CategoryColorPicker`: themed swatch circles replaced with 6 role pill chips ("In-theme roles"); "More colours" header replaced with "Fixed colour / Stays put on theme change".
> - New `wcag_check_roles.py` at repo root mirrors the derivation over every palette in `wcag_check.py`'s THEMES table; `wcag_check.py` restored from base64 mangling, and the byte-identical workaround copy `wcag_check_real.py` removed.
> - `Color.kt`'s palette values were reconciled to the finalised colour spec on main (#176) after this map was stamped; the accent hexes quoted elsewhere in this map may be out of date, but the structure (scheme count, `colorSchemeFor`, `buildCustomColorScheme`) is unchanged.

All paths under `app/src/main/java/com/mapgie/goflo/`.

## 1. `ui/theme/Color.kt` (~1520 lines)

**Imports:** `ColorScheme`, `lightColorScheme`, `darkColorScheme`, `Color`, `Color.luminance`, `Color.toArgb`, `androidx.core.graphics.ColorUtils`. **No** `androidx.compose.material3.dynamiccolor`, `Hct`, `TonalPalette`, or `dynamicLight/DarkColorScheme` anywhere in the project. House style for computed colour is **HSL via `Color.hsl(...)` and `ColorUtils.colorToHSL`**, plus `lerp` — not HCT.

**`AppTheme` enum:** constructor `(displayName, group, previewArgb: Long, isDark = false)`. ~45 entries across `ThemeGroup` LIGHT/DARK/SYSTEM/HIGH_CONTRAST/COLOR_BLIND. 12 palette families (Coral, Turquoise/"Teal", Green/"Sage", SummerCandy, BeachVibes, PeachMelba, Disco, MetalChick, Whimsy, ColourHappy, DragonFire, MidnightNeon) each with `_DARK` and `_SYSTEM` variants, plus `HIGH_CONTRAST_LIGHT/DARK`, `BLUE_ORANGE`, `CUSTOM`. **Entry names are persisted to DataStore — do not rename.**

**`colorSchemeFor(...)` — the signature you must NOT break:**
```kotlin
fun colorSchemeFor(theme: AppTheme, systemIsDark: Boolean = false, wcag: Boolean = false): ColorScheme
```
Public, top-level (~line 1281). If `!wcag` or theme is HIGH_CONTRAST_*/BLUE_ORANGE → delegates to `private fun standardColorSchemeFor(theme, systemIsDark)`; otherwise a `when(theme)` returns the `*Wcag` variant. Called from exactly one place: `GoFloTheme` in `Theme.kt`.

**Private `val` ColorScheme count:** ~51. Standard (27): `{Palette}Light`/`{Palette}Dark` + `HighContrastLight/Dark` + `BlueOrange`. WCAG AAA (24): `{Palette}Light Wcag`/`{Palette}DarkWcag` for all 12 families. Each built with `lightColorScheme(...)`/`darkColorScheme(...)` specifying only standard Material3 roles.

**Existing tonal-derivation helper — the pattern to mirror:** `buildCustomColorScheme(...)` (~lines 1383-1519), the ONLY place colours are computed:
```kotlin
fun buildCustomColorScheme(
    primaryHue: Float, secondaryHue: Float, tertiaryHue: Float,
    primaryArgb: Int = 0, secondaryArgb: Int = 0, tertiaryArgb: Int = 0,
    backgroundArgb: Int = 0, isDark: Boolean,
): ColorScheme
```
Derives containers/on-colours/surfaces via `Color.hsl(hue, s, l)`, computes WCAG-safe on-colours from relative luminance (`onArgb`, manual sRGB linearization), uses `ColorUtils.colorToHSL` for background overrides. **This is the reference for deriving quaternary/quinary/senary — HSL manipulation, not HCT/TonalPalette.**

**Material3 "extra role" pattern:** None. `ColorScheme` exposes only primary/secondary/tertiary accents (plus error, surface family). There is no 4th/5th/6th accent slot. **Derived roles cannot live inside the returned `ColorScheme`** — use a parallel holder (data class / CompositionLocal) or derive on the fly at the resolution site.

## 2. `ui/theme/Theme.kt` (~73 lines)

`@Composable fun GoFloTheme(appTheme, wcag, customHues, customArgbs, customThemeMode, customLightBackgroundArgb, customDarkBackgroundArgb, content)`.
- Scheme choice: `appTheme == CUSTOM && customHues != null` → `buildCustomColorScheme(...)`, else `colorSchemeFor(appTheme, systemDark, wcag)`.
- `systemDark = isSystemInDarkTheme()`.
- Computes `effectivelyDark` to flip status-bar icon contrast via `WindowCompat...isAppearanceLightStatusBars` in a `SideEffect`.
- Applies: `MaterialTheme(colorScheme = colorScheme, typography = GoFloTypography, content = content)`. Does NOT read DataStore — the active theme is passed in by MainActivity.

**Where to inject a role holder:** if roles are derived once per theme, compute them here and provide via a `CompositionLocalProvider` wrapping `MaterialTheme`, so `String.toCategoryColor()` can read them without re-deriving per call.

## 3. `ui/theme/Type.kt` (~43 lines)

- Downloadable Google Font provider (`com.google.android.gms.fonts`).
- `val ComfortaaFamily: FontFamily` — single `GoogleFont("Comfortaa")` at `FontWeight.Bold`. **`GoFloTypography` does not wire `ComfortaaFamily` into any TextStyle** (styles set only weight/size/lineHeight); the family is exported for use elsewhere (app title). The design calls for Comfortaa on screen titles and `ToneHero` words — those composables must apply `ComfortaaFamily` explicitly.
- `val GoFloTypography = Typography(...)`: headlineLarge/Medium, titleLarge/Medium, bodyLarge/Medium, labelLarge/Medium.

## 4. Theme persistence — DataStore

Store: `AppPreferencesStore` (in `data/preferences/ReminderPreferences.kt`). Backing store `preferencesDataStore(name = "goflo_prefs")`.
- Key: `val THEME = stringPreferencesKey("theme")`. Read: `preferences: Flow<AppPreferences>` maps `theme = prefs[Keys.THEME] ?: "CORAL"`. Write: `suspend fun setTheme(theme: String)`.
- Also holds CUSTOM theme fields: `customPrimaryHue/Argb`, `customSecondary*`, `customTertiary*`, `customLight/DarkBackgroundArgb`, `customThemeName`, `customThemeMode`.
- Consumers: `MainActivity` reads `preferencesStore.preferences`, `AppTheme.valueOf(appPrefs.theme)` (fallback CORAL) → `GoFloTheme(...)`. `SettingsViewModel.setTheme` wraps `store.setTheme`. Store exposed as `GoFloApplication.preferencesStore`.

## 5. Category colour resolution at render time — the hook to extend

Stored on `TrackingCategory.colorToken: String = "secondary"`. Resolution lives in **`ui/util/CategoryAppearance.kt`**:
- `enum class CategoryColor(key, displayName)`: only `PRIMARY`, `SECONDARY`, `TERTIARY`. **Add new roles here.**
- `@Composable fun String.toCategoryColor(): Color`:
```kotlin
val s = MaterialTheme.colorScheme
return when (this) {
    "primary"   -> s.primary
    "secondary" -> s.secondary
    "tertiary"  -> s.tertiary
    else        -> runCatching { Color(toLong(16)) }.getOrDefault(s.secondary)  // hex path = FIXED role
}
```
- `@Composable fun String.toCategoryOnColor(): Color`: mirror for icon/text tint (onPrimary/… or luminance-based near-black/white for hex).
- Shade helpers: `ordinalShade(base, surface, index, total)`, `continuousShade(base, surface, fraction)` (use `lerp`).
- `CATEGORY_COLOR_OPTIONS: List<Int>`: fixed hex palette in the "More colours" picker (`ManageCategoriesScreen.kt`).

Calendar (`ui/components/CalendarGrid.kt`) does NOT use category tokens — it reads `MaterialTheme.colorScheme.primary/tertiary/secondary` directly for period/ovulation dots.

## Takeaways for adding quaternary/quinary/senary roles

1. **Extend around `colorSchemeFor`, don't change its signature.** Material3 `ColorScheme` has no 4th-6th accent slot, so store derived roles in a parallel holder (CompositionLocal set up in `Theme.kt`) or derive at the resolution site in `CategoryAppearance.kt`.
2. **Derive with HSL** (`Color.hsl`, `ColorUtils.colorToHSL`, `lerp`) mirroring `buildCustomColorScheme` — no new HCT/TonalPalette dependency. A defensible derivation: rotate the hue of primary/secondary/tertiary by a fixed offset (or interpolate between the existing three accents) and pin saturation/lightness to the scheme's own accent band so contrast stays in range.
3. **On-colours must stay WCAG-safe.** Compute the on-colour from luminance (as the hex path already does) so derived roles pass ≥3:1 for icons / ≥4.5:1 for text. Re-run `wcag_check.py` (and note derived roles aren't in the palette tables, so add a spot-check).
4. **`FIXED` needs no new storage** — the existing hex path in `colorToken` already means "don't change on theme switch". Extended in-theme roles are just three new token strings (`quaternary`/`quinary`/`senary`) added to both resolver `when` branches and the `CategoryColor` enum. **No DB migration required to reference them** (colorToken is free-form).
