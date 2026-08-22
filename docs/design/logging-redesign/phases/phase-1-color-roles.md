# Phase 1 — Extended colour roles

**Goal:** categories can use three extra in-theme colour roles (quaternary/quinary/senary) that re-theme with the palette, plus a fixed off-theme colour. Fully additive, no DB migration.

**Prerequisites:** none (first build phase).
**Read first:** `PLAN.md` §1, §5; `subsystem-maps/03-theme-color-machinery.md` (the whole thing); `handover/screens/06-row.png` ("Pick a colour" mock).

---

## Why

The whole redesign rests on "a category stores a colour *role*, never a hex." Today only `primary`/`secondary`/`tertiary` (+ raw hex) exist. The mock offers six in-theme roles plus a Fixed track. Material3's `ColorScheme` has no 4th-6th accent slot, so we derive them and expose them through a `CompositionLocal`.

## Files to touch

| File | Change |
|---|---|
| `ui/theme/Color.kt` | Add a derivation function producing 3 extra roles + on-colours from a `ColorScheme`. Add a small holder data class. |
| `ui/theme/Theme.kt` | Derive roles once per theme; provide via a new `CompositionLocal` wrapping `MaterialTheme`. |
| `ui/util/CategoryAppearance.kt` | Add enum entries + resolver branches for the 3 new tokens. |
| `ui/screens/categories/ManageCategoriesScreen.kt` | Colour picker becomes a role row of 6 + a Fixed track (factor a `RolePicker` here or inline; it is extracted into a shared component in Phase 3). |

## Step-by-step

### 1. Derivation (`Color.kt`)
Add near `buildCustomColorScheme` (mirror its HSL house style — do **not** add an HCT/TonalPalette dependency):

```kotlin
/** Three extra in-theme accent roles derived from a ColorScheme, + WCAG-safe on-colours. */
data class ExtendedRoles(
    val quaternary: Color, val onQuaternary: Color,
    val quinary: Color,    val onQuinary: Color,
    val senary: Color,     val onSenary: Color,
)

fun deriveExtendedRoles(scheme: ColorScheme): ExtendedRoles {
    // Rotate hue off the three existing accents so the extras are harmonious but distinct.
    // Pin S/L into the scheme's own accent band so contrast stays in range.
    fun rotate(base: Color, degrees: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        hsl[0] = (hsl[0] + degrees + 360f) % 360f
        return Color(ColorUtils.HSLToColor(hsl))
    }
    val q  = rotate(scheme.primary,   30f)
    val qi = rotate(scheme.secondary, -30f)
    val s  = rotate(scheme.tertiary,  45f)
    fun on(c: Color): Color = if (c.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
    return ExtendedRoles(q, on(q), qi, on(qi), s, on(s))
}
```
> The exact rotation is a judgement call — tune it so each role is visually separated from the base three in every palette. An alternative is interpolating between existing accents (`lerp`). Whatever you pick, verify contrast (step 5).

### 2. Provide it (`Theme.kt`)
Add a CompositionLocal and wrap the existing `MaterialTheme(...)`:

```kotlin
val LocalExtendedRoles = staticCompositionLocalOf<ExtendedRoles> { error("ExtendedRoles not provided") }
```
Inside `GoFloTheme`, after `colorScheme` is chosen:
```kotlin
CompositionLocalProvider(LocalExtendedRoles provides deriveExtendedRoles(colorScheme)) {
    MaterialTheme(colorScheme = colorScheme, typography = GoFloTypography, content = content)
}
```
Keep the `SideEffect` status-bar logic as-is.

### 3. Resolver (`CategoryAppearance.kt`)
Extend the enum:
```kotlin
enum class CategoryColor(val key: String, val displayName: String) {
    PRIMARY("primary","Primary"), SECONDARY("secondary","Secondary"), TERTIARY("tertiary","Accent"),
    QUATERNARY("quaternary","Quaternary"), QUINARY("quinary","Quinary"), SENARY("senary","Senary"),
}
```
Extend both resolvers (they are `@Composable`, so they can read the local):
```kotlin
@Composable fun String.toCategoryColor(): Color {
    val s = MaterialTheme.colorScheme
    val ext = LocalExtendedRoles.current
    return when (this) {
        "primary" -> s.primary; "secondary" -> s.secondary; "tertiary" -> s.tertiary
        "quaternary" -> ext.quaternary; "quinary" -> ext.quinary; "senary" -> ext.senary
        else -> runCatching { Color(toLong(16)) }.getOrDefault(s.secondary)   // FIXED = hex
    }
}
```
Mirror in `toCategoryOnColor()` with `onQuaternary`/`onQuinary`/`onSenary`.

### 4. Picker UI (`ManageCategoriesScreen.kt`, existing "More colours" area ~line 1141)
Render a row of 6 role chips (selected = filled with the role colour + a check/ring) and, below, the existing fixed hex track labelled so the user knows it "stays put on theme change" (mock row 6). Each role chip: `Role.RadioButton` in `.semantics`, min 48dp, `contentDescription` naming the role. Selected state must not be colour-only (add a ring/check).

### 5. Verify contrast
- Run `python3 wcag_check.py` (base palettes unaffected, must stay green).
- Add a spot-check: for each of the 12 light+dark schemes, derive the 3 roles and assert on-colour contrast ≥ 3:1 (icon) against the role. If a derived role fails in some palette, adjust the rotation/L clamp until all pass. Consider adding this as a small script (`wcag_check_roles.py`) so it is repeatable.

## Data changes
None. `colorToken` is a free-form string; new tokens need no migration.

## Acceptance criteria
- [ ] A category set to each of quaternary/quinary/senary renders that colour and **re-themes** when the palette changes (verify in one light + one dark palette).
- [ ] A category set to a Fixed colour does **not** change on theme switch.
- [ ] Derived on-colours pass contrast in all 12 palettes (spot-check script green).
- [ ] `wcag_check.py` and `a11y_check.py` both green.
- [ ] Picker chips carry `Role.RadioButton`, are ≥48dp, and selection is not colour-only.

## Feature-preservation checklist
- [ ] Existing `primary`/`secondary`/`tertiary`/hex tokens resolve exactly as before.
- [ ] Calendar dots, `DayLogSheet` bubbles, Stats colour usage unchanged.
- [ ] The `CUSTOM` theme path (`buildCustomColorScheme`) still works — `deriveExtendedRoles` runs on the custom scheme too.

## Gotchas
- `LocalExtendedRoles` is read inside `@Composable` resolvers only. Anything resolving a category colour **outside** composition (rare) needs the scheme passed explicitly.
- `staticCompositionLocalOf` won't recompose on change; theme changes swap the whole `GoFloTheme` subtree so that's fine. If you see stale colours, switch to `compositionLocalOf`.
- Comfortaa/typography untouched here.

## Changelog fragment
`changelog/unreleased/extended-color-roles.json`:
```json
{ "bump": "minor", "added": ["Three extra in-theme colour roles for categories, plus a fixed off-theme colour option"] }
```
