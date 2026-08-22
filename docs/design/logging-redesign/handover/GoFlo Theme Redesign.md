# GoFlo theme redesign — handoff spec

**Target file:** `app/src/main/java/com/mapgie/goflo/ui/theme/Color.kt`
**Scope:** replace the colour values of all 12 named palettes (the 3 "classic" and 9 "fun" / "bold" themes). Leave `HighContrastLight`, `HighContrastDark`, and `BlueOrange` alone — those are accessibility schemes with constraints of their own.
**Do not change:** the `AppTheme` enum names (they're persisted to DataStore), the `ThemeGroup` structure, the function signature of `colorSchemeFor()`, or `Typography.kt`.
**Do change:** every `previewArgb` value to match the new primary of its scheme.

---

## Why this redesign

The current palettes have great names — Summer Candy, All-Night Disco Party, Dragon Fire — but the colours don't deliver. Specific problems being fixed:

| Palette | Problem in current values |
|---|---|
| **Coral** | Primary, secondary, tertiary are all red-orange. No hue contrast. |
| **Teal** | Primary, secondary, tertiary are all desaturated teals/blue-greys. |
| **Sage** | Same — three greens. |
| **Summer Candy** | Burnt-orange #BF360C + deep teal reads as autumn stew, not sherbet. |
| **Beach Vibes** | Terra cotta secondary is wrong climate. No sun, no sand. |
| **Peach Melba** | Brick #BF360C + magenta-wine #880E4F + forest green has no peach and no cream. |
| **All-Night Disco Party** | Tertiary is mustard #8B6A00. Discos are *neon*. |
| **Metal Chick** | All three colours are grey. No edge. |
| **Whimsy Whispers** | Secondary is a violet-grey that drags the whole palette into the mud. |
| **Colour Me Happy** | Two oranges fighting + a lime. Not the joyful primary chord the name promises. |
| **Dragon Fire** | Tertiary is electric indigo. Indigo is not on fire. |
| **Midnight Neon** light | Has no neon — it's lavender, teal and olive. |

## Design principles applied

1. **Three real hues per palette.** Primary, secondary and tertiary now live in different parts of the colour wheel — not three shades of the same orange. Each palette has a deliberate three-note chord.
2. **Backgrounds tinted to character.** Surface and surfaceVariant carry the lightest tint of the palette's dominant hue family, not a generic near-white.
3. **Containers stay in family.** primaryContainer is a desaturated tint of primary; same for secondary/tertiary. This was already the convention — preserved.
4. **WCAG AA preserved.** All primary/onPrimary, container/onContainer, and outline/surfaceVariant pairs target ≥4.5:1 for text and ≥3:1 for UI elements. Re-run `wcag_check.py` after applying.
5. **Names deliver.** Every palette now matches the noun in its name. Summer Candy is candy. Dragon Fire is fire. Midnight Neon glows in both modes.

---

## The palettes

Each section gives the **intent**, the **three-note chord**, then the full Material 3 token sets for light and dark.

### 1. Coral *(classic)*

**Intent:** Living coral reef — vivid coral against lagoon water under a high sun.
**Chord:** coral red · lagoon teal · sun-bleached gold.

**Light**
```
primary               #BC4D3F   onPrimary               #FFFFFF
primaryContainer      #FFDAD2   onPrimaryContainer      #3F0900
secondary             #007E7A   onSecondary             #FFFFFF
secondaryContainer    #B4ECE7   onSecondaryContainer    #002523
tertiary              #956800   onTertiary              #FFFFFF
tertiaryContainer     #FFE08D   onTertiaryContainer     #251A00
background            #FFF6F2   onBackground            #221915
surface               #FFF6F2   onSurface               #221915
surfaceVariant        #F4DDD5   onSurfaceVariant        #524340
outline               #856E68
```
`previewArgb` = `0xFFBC4D3FL`

**Dark**
```
primary               #FFB4A4   onPrimary               #5C1600
primaryContainer      #7C2A18   onPrimaryContainer      #FFDAD0
secondary             #5BD8D0   onSecondary             #003734
secondaryContainer    #00504C   onSecondaryContainer    #B4EEE9
tertiary              #FFD787   onTertiary              #3D2D00
tertiaryContainer     #574200   onTertiaryContainer     #FFE08D
background            #1C110E   onBackground            #F1DED8
surface               #1C110E   onSurface               #F1DED8
surfaceVariant        #523F3A   onSurfaceVariant        #D7C2BC
outline               #A28B86
```
`previewArgb` = `0xFFFFB4A4L` (unchanged)

---

### 2. Teal *(classic — enum still named TURQUOISE)*

**Intent:** Clear lagoon water with warm clay and ink-blue as counterpoints.
**Chord:** deep teal · terra-cotta · indigo.

**Light**
```
primary               #00747C   onPrimary               #FFFFFF
primaryContainer      #9DEFF6   onPrimaryContainer      #002023
secondary             #B3572A   onSecondary             #FFFFFF
secondaryContainer    #FFDBC8   onSecondaryContainer    #381300
tertiary              #4B5BAC   onTertiary              #FFFFFF
tertiaryContainer     #DEE1FF   onTertiaryContainer     #00115A
background            #F0FBFC   onBackground            #161C1D
surface               #F0FBFC   onSurface               #161C1D
surfaceVariant        #DAE4E5   onSurfaceVariant        #3F4949
outline               #6F7979
```
`previewArgb` = `0xFF00747CL`

**Dark**
```
primary               #80D5DB   onPrimary               #003739
primaryContainer      #004F52   onPrimaryContainer      #9DEFF6
secondary             #FFB28F   onSecondary             #5A1B00
secondaryContainer    #8C3B16   onSecondaryContainer    #FFDBC8
tertiary              #BCC2FF   onTertiary              #1C257B
tertiaryContainer     #353F93   onTertiaryContainer     #DEE1FF
background            #0E1818   onBackground            #E0E3E3
surface               #0E1818   onSurface               #E0E3E3
surfaceVariant        #3F4949   onSurfaceVariant        #BEC8C9
outline               #8B9595
```
`previewArgb` = `0xFF80D5DBL` (unchanged)

---

### 3. Sage *(classic — enum still named GREEN)*

**Intent:** Herb garden at dawn — soft green with warm earth and honey.
**Chord:** sage green · terra-cotta clay · honey gold.

**Light**
```
primary               #4F7D2B   onPrimary               #FFFFFF
primaryContainer      #C8F09F   onPrimaryContainer      #112100
secondary             #B5532A   onSecondary             #FFFFFF
secondaryContainer    #FFDBC8   onSecondaryContainer    #3A1100
tertiary              #8C6212   onTertiary              #FFFFFF
tertiaryContainer     #FFDF9C   onTertiaryContainer     #2A1D00
background            #F7FBEE   onBackground            #1A1C16
surface               #F7FBEE   onSurface               #1A1C16
surfaceVariant        #DEE5D2   onSurfaceVariant        #424A3B
outline               #72796A
```
`previewArgb` = `0xFF4F7D2BL`

**Dark**
```
primary               #ACD888   onPrimary               #1B3900
primaryContainer      #2D530B   onPrimaryContainer      #C8F09F
secondary             #FFB28F   onSecondary             #5A1B00
secondaryContainer    #8B3914   onSecondaryContainer    #FFDBC8
tertiary              #F4C16D   onTertiary              #422C00
tertiaryContainer     #5E4300   onTertiaryContainer     #FFDF9C
background            #14170F   onBackground            #E2E4D7
surface               #14170F   onSurface               #E2E4D7
surfaceVariant        #424A3B   onSurfaceVariant        #C2C9B6
outline               #919888
```
`previewArgb` = `0xFFACD888L`

---

### 4. Summer Candy

**Intent:** Bubblegum, sherbet and slushie. Sugary, saturated, joyful — not autumnal.
**Chord:** bubblegum raspberry · mint aqua · lemon gold.

**Light**
```
primary               #D81B60   onPrimary               #FFFFFF
primaryContainer      #FFD8E5   onPrimaryContainer      #40001B
secondary             #007F76   onSecondary             #FFFFFF
secondaryContainer    #6FF5E5   onSecondaryContainer    #00201D
tertiary              #9A6700   onTertiary              #FFFFFF
tertiaryContainer     #FFE08D   onTertiaryContainer     #271B00
background            #FFF7FA   onBackground            #1E1316
surface               #FFF7FA   onSurface               #1E1316
surfaceVariant        #F4DDE5   onSurfaceVariant        #534249
outline               #856D74
```
`previewArgb` = `0xFFD81B60L`

**Dark**
```
primary               #FFB1CA   onPrimary               #650033
primaryContainer      #8E0049   onPrimaryContainer      #FFD8E5
secondary             #6DDFD0   onSecondary             #003A35
secondaryContainer    #00514B   onSecondaryContainer    #6FF5E5
tertiary              #FFCD66   onTertiary              #3F2D00
tertiaryContainer     #5A4200   onTertiaryContainer     #FFE08D
background            #1D1014   onBackground            #ECDFE3
surface               #1D1014   onSurface               #ECDFE3
surfaceVariant        #534249   onSurfaceVariant        #D6C1C8
outline               #A39095
```
`previewArgb` = `0xFFFFB1CAL`

---

### 5. Beach Vibes

**Intent:** Sand, sea, sky and sun. Bright, breezy, holiday-postcard.
**Chord:** clear sea blue · sun-gold sand · sea-foam green. (Terra cotta is gone.)

**Light**
```
primary               #1265AF   onPrimary               #FFFFFF
primaryContainer      #D5E3FF   onPrimaryContainer      #001C3D
secondary             #9A6800   onSecondary             #FFFFFF
secondaryContainer    #FFE08D   onSecondaryContainer    #251A00
tertiary              #297F6C   onTertiary              #FFFFFF
tertiaryContainer     #A8F0DC   onTertiaryContainer     #002019
background            #F8FAFE   onBackground            #181C22
surface               #F8FAFE   onSurface               #181C22
surfaceVariant        #E1E3EE   onSurfaceVariant        #444751
outline               #757782
```
`previewArgb` = `0xFF1265AFL`

**Dark**
```
primary               #A8C8FF   onPrimary               #002E66
primaryContainer      #00478A   onPrimaryContainer      #D5E3FF
secondary             #F4C16D   onSecondary             #422C00
secondaryContainer    #5E4300   onSecondaryContainer    #FFE08D
tertiary              #84D7BC   onTertiary              #003828
tertiaryContainer     #00513B   onTertiaryContainer     #A8F0DC
background            #101820   onBackground            #DDE3EB
surface               #101820   onSurface               #DDE3EB
surfaceVariant        #444751   onSurfaceVariant        #C5C7D2
outline               #93959F
```
`previewArgb` = `0xFFA8C8FFL`

---

### 6. Peach Melba

**Intent:** The Escoffier dessert — peach halves, raspberry coulis, vanilla cream. Delicate, soft, dessert-shop.
**Chord:** apricot peach · raspberry · vanilla-caramel cream.

**Light**
```
primary               #B35535   onPrimary               #FFFFFF
primaryContainer      #FFDBC9   onPrimaryContainer      #3A1300
secondary             #B53369   onSecondary             #FFFFFF
secondaryContainer    #FFD9E3   onSecondaryContainer    #3F0024
tertiary              #8A6926   onTertiary              #FFFFFF
tertiaryContainer     #FFE2A0   onTertiaryContainer     #2A1F00
background            #FFF6F0   onBackground            #201813
surface               #FFF6F0   onSurface               #201813
surfaceVariant        #F4DED1   onSurfaceVariant        #534439
outline               #867262
```
`previewArgb` = `0xFFB35535L`

**Dark**
```
primary               #FFB694   onPrimary               #571F00
primaryContainer      #7A3015   onPrimaryContainer      #FFDBC9
secondary             #FFB1C9   onSecondary             #65003A
secondaryContainer    #8C1F4F   onSecondaryContainer    #FFD9E3
tertiary              #F4C16D   onTertiary              #432D00
tertiaryContainer     #5F4300   onTertiaryContainer     #FFE2A0
background            #1E1410   onBackground            #EDE0D8
surface               #1E1410   onSurface               #EDE0D8
surfaceVariant        #534439   onSurfaceVariant        #D7C3B5
outline               #A59185
```
`previewArgb` = `0xFFFFB694L`

---

### 7. All-Night Disco Party

**Intent:** Mirrorball, strobes, glitter. Actual neon, not mustard.
**Chord:** hot magenta · electric violet · glitter gold.

**Light**
```
primary               #C1127A   onPrimary               #FFFFFF
primaryContainer      #FFD6EA   onPrimaryContainer      #3D003C
secondary             #6E1FB5   onSecondary             #FFFFFF
secondaryContainer    #ECDCFF   onSecondaryContainer    #270060
tertiary              #966900   onTertiary              #FFFFFF
tertiaryContainer     #FFE08C   onTertiaryContainer     #2A1D00
background            #FFF7FB   onBackground            #1E1020
surface               #FFF7FB   onSurface               #1E1020
surfaceVariant        #F0DEEC   onSurfaceVariant        #4F4452
outline               #807385
```
`previewArgb` = `0xFFC1127AL`

**Dark** — full neon mode.
```
primary               #FF66B8   onPrimary               #5C0040
primaryContainer      #890062   onPrimaryContainer      #FFD6EA
secondary             #D5B2FF   onSecondary             #3F0090
secondaryContainer    #5800B0   onSecondaryContainer    #ECDCFF
tertiary              #FFD350   onTertiary              #3A2A00
tertiaryContainer     #523D00   onTertiaryContainer     #FFE08C
background            #170820   onBackground            #ECDAEC
surface               #170820   onSurface               #ECDAEC
surfaceVariant        #4F4452   onSurfaceVariant        #D2C2D2
outline               #A092A2
```
`previewArgb` = `0xFFFF66B8L`

---

### 8. Metal Chick

**Intent:** Black leather and chrome with one shocking lipstick accent. Edge, attitude.
**Chord:** gunmetal · crimson-lipstick · brushed chrome.

**Light**
```
primary               #2E2E3A   onPrimary               #FFFFFF
primaryContainer      #DCDBE9   onPrimaryContainer      #0E0E1C
secondary             #C8235A   onSecondary             #FFFFFF
secondaryContainer    #FFD8E0   onSecondaryContainer    #3F001A
tertiary              #6A6A78   onTertiary              #FFFFFF
tertiaryContainer     #E3E2F0   onTertiaryContainer     #1C1C2A
background            #F4F3F8   onBackground            #1B1B22
surface               #F4F3F8   onSurface               #1B1B22
surfaceVariant        #E2E0EC   onSurfaceVariant        #45434E
outline               #76737E
```
`previewArgb` = `0xFF2E2E3AL`

**Dark**
```
primary               #C7C5D6   onPrimary               #2F2D3D
primaryContainer      #454354   onPrimaryContainer      #DCDBE9
secondary             #FF8FB0   onSecondary             #5C0028
secondaryContainer    #88133E   onSecondaryContainer    #FFD8E0
tertiary              #9D9DAC   onTertiary              #2F2F3E
tertiaryContainer     #4A4A58   onTertiaryContainer     #E3E2F0
background            #0E0E13   onBackground            #E5E3ED
surface               #0E0E13   onSurface               #E5E3ED
surfaceVariant        #45434E   onSurfaceVariant        #C8C5D0
outline               #918E99
```
`previewArgb` = `0xFFC7C5D6L`

---

### 9. Whimsy Whispers

**Intent:** Fairy-tale soft pastels — but actually saturated, not muddy. Soft-focus, dreamy.
**Chord:** lavender · blush rose · spearmint.

**Light**
```
primary               #6E5DC4   onPrimary               #FFFFFF
primaryContainer      #E5DFFF   onPrimaryContainer      #1A0067
secondary             #BD4878   onSecondary             #FFFFFF
secondaryContainer    #FFD9E4   onSecondaryContainer    #3D0024
tertiary              #2B7F68   onTertiary              #FFFFFF
tertiaryContainer     #A6F0D6   onTertiaryContainer     #002016
background            #FBF8FF   onBackground            #1C1A24
surface               #FBF8FF   onSurface               #1C1A24
surfaceVariant        #E5DFF0   onSurfaceVariant        #49454F
outline               #7A7580
```
`previewArgb` = `0xFF6E5DC4L`

**Dark**
```
primary               #C7BEFF   onPrimary               #260092
primaryContainer      #3D2EAE   onPrimaryContainer      #E5DFFF
secondary             #FFB1C6   onSecondary             #650033
secondaryContainer    #8E1F50   onSecondaryContainer    #FFD9E4
tertiary              #80DAB8   onTertiary              #003828
tertiaryContainer     #00513B   onTertiaryContainer     #A6F0D6
background            #16131F   onBackground            #E6E1F0
surface               #16131F   onSurface               #E6E1F0
surfaceVariant        #49454F   onSurfaceVariant        #CBC5D0
outline               #96909F
```
`previewArgb` = `0xFFC7BEFFL`

---

### 10. Colour Me Happy

**Intent:** Kindergarten primary colours — tomato, sky, grass. Joyful, optimistic, clear.
**Chord:** tomato red · sky blue · grass green.

**Light**
```
primary               #D63A26   onPrimary               #FFFFFF
primaryContainer      #FFDAD2   onPrimaryContainer      #410000
secondary             #1872BD   onSecondary             #FFFFFF
secondaryContainer    #D1E4FF   onSecondaryContainer    #001D36
tertiary              #428129   onTertiary              #FFFFFF
tertiaryContainer     #C6F0A1   onTertiaryContainer     #0F2200
background            #FFFCF5   onBackground            #1F1A14
surface               #FFFCF5   onSurface               #1F1A14
surfaceVariant        #EFE3D5   onSurfaceVariant        #4E443A
outline               #80766A
```
`previewArgb` = `0xFFD63A26L`

**Dark**
```
primary               #FFB4A4   onPrimary               #5C1500
primaryContainer      #882000   onPrimaryContainer      #FFDAD2
secondary             #A0CAFF   onSecondary             #002F66
secondaryContainer    #00497D   onSecondaryContainer    #D1E4FF
tertiary              #ACD688   onTertiary              #1A3900
tertiaryContainer     #2D530B   onTertiaryContainer     #C6F0A1
background            #1A1612   onBackground            #EDE0D2
surface               #1A1612   onSurface               #EDE0D2
surfaceVariant        #4E443A   onSurfaceVariant        #D2C4B5
outline               #9F9184
```
`previewArgb` = `0xFFFFB4A4L`

---

### 11. Dragon Fire

**Intent:** Smaug's hoard burning — embers, molten metal, gold leaf. No more random indigo.
**Chord:** ember red · molten orange · furnace gold.

**Light**
```
primary               #B0181F   onPrimary               #FFFFFF
primaryContainer      #FFCFCC   onPrimaryContainer      #3F0001
secondary             #C04A0E   onSecondary             #FFFFFF
secondaryContainer    #FFDBC9   onSecondaryContainer    #3B1100
tertiary              #996800   onTertiary              #FFFFFF
tertiaryContainer     #FFDE8E   onTertiaryContainer     #2D1F00
background            #FFF8F4   onBackground            #1E140F
surface               #FFF8F4   onSurface               #1E140F
surfaceVariant        #F3DED5   onSurfaceVariant        #524338
outline               #87715F
```
`previewArgb` = `0xFFB0181FL`

**Dark**
```
primary               #FF8A82   onPrimary               #680000
primaryContainer      #960000   onPrimaryContainer      #FFDAD4
secondary             #FFB28F   onSecondary             #561A00
secondaryContainer    #8E2D00   onSecondaryContainer    #FFDBC9
tertiary              #FFCD66   onTertiary              #422C00
tertiaryContainer     #5E4300   onTertiaryContainer     #FFDE8E
background            #1C0907   onBackground            #F1DDD7
surface               #1C0907   onSurface               #F1DDD7
surfaceVariant        #523F38   onSurfaceVariant        #D7C2B8
outline               #A28D81
```
`previewArgb` = `0xFFFF8A82L` (unchanged)

---

### 12. Midnight Neon

**Intent:** Cyberpunk arcade. Glows in **both** modes — light mode is "neon sign at dusk", dark is "Akira after midnight".
**Chord:** neon magenta · electric cyan · acid lime.

**Light**
```
primary               #C5128A   onPrimary               #FFFFFF
primaryContainer      #FFD6E8   onPrimaryContainer      #3D003C
secondary             #006D90   onSecondary             #FFFFFF
secondaryContainer    #B8E9FF   onSecondaryContainer    #001E2F
tertiary              #4C7A0E   onTertiary              #FFFFFF
tertiaryContainer     #C2F088   onTertiaryContainer     #0F2300
background            #FBF7FF   onBackground            #161020
surface               #FBF7FF   onSurface               #161020
surfaceVariant        #EDDEEC   onSurfaceVariant        #4D4350
outline               #7F7484
```
`previewArgb` = `0xFFC5128AL`

**Dark** — go full glow.
```
primary               #FF55C8   onPrimary               #5C0048
primaryContainer      #870068   onPrimaryContainer      #FFD6EA
secondary             #5EE0FF   onSecondary             #00374A
secondaryContainer    #00516C   onSecondaryContainer    #B8E9FF
tertiary              #C2F052   onTertiary              #1D3300
tertiaryContainer     #2D4900   onTertiaryContainer     #C2F088
background            #0A0A18   onBackground            #EDE5FA
surface               #0A0A18   onSurface               #EDE5FA
surfaceVariant        #4D4350   onSurfaceVariant        #D0C3D0
outline               #9D90A4
```
`previewArgb` = `0xFFFF55C8L`

---

## Implementation notes for Claude Code

1. **Edit `Color.kt` in place.** Replace the colour `Color(0x…)` values inside each of these schemes:
   `CoralLight`, `CoralDark`, `TurquoiseLight`, `TurquoiseDark`, `GreenLight`, `GreenDark`, `SummerCandyLight`, `SummerCandyDark`, `BeachVibesLight`, `BeachVibesDark`, `PeachMelbaLight`, `PeachMelbaDark`, `DiscoLight`, `DiscoDark`, `MetalChickLight`, `MetalChickDark`, `WhimsyLight`, `WhimsyDark`, `ColourHappyLight`, `ColourHappyDark`, `DragonFireLight`, `DragonFireDark`, `MidnightNeonLight`, `MidnightNeonDark`.
2. **Update `previewArgb`** on the matching `AppTheme` enum entries (and the corresponding `*_SYSTEM` rows — light system rows use the light primary, dark system rows the dark primary). Specifically the `*_SYSTEM` previews should match the LIGHT primary so the settings chip stays recognisable in either system mode — though current code uses dark primaries for system previews; keep whichever you prefer but be consistent.
3. **Leave alone:** `HighContrastLight`, `HighContrastDark`, `BlueOrange`, the `ThemeGroup` enum, the `AppTheme` *names*, and `colorSchemeFor()`.
4. **Run `wcag_check.py`** before committing. The values were chosen to clear 4.5:1 (text) / 3:1 (UI) but verify on the real script. If any pair fails by <0.2, darken/lighten the offending hex by ~3 % luminance toward black or white; the target hue should not shift.
5. **Don't change** the comment headers above each scheme (e.g. `// ── Classic light color schemes ──`) — keep the file structure recognisable.
6. **Drop the obsolete contrast comments** like `// Coral: primary darkened to #C15542 (WCAG AA audit 2026-05-23…)` — they refer to old values. Add a single line above each section: `// Redesigned 2026-05 — see GoFlo Theme Redesign.md`.
7. **CHANGELOG entry suggestion:** `Themes: rebuilt all 12 named palettes so each name's three-hue chord actually delivers (e.g. Summer Candy is now sherbet, Dragon Fire is now fire, Midnight Neon glows in both modes). Accessibility schemes unchanged.`

## Quick QA checklist after Claude Code applies it

- [ ] App builds; no missing references.
- [ ] Settings → Theme picker shows updated swatches next to each row.
- [ ] Open one light and one dark variant of each palette and verify the calendar (period dot uses primary, ovulation uses tertiaryContainer) reads correctly.
- [ ] FAB (uses primaryContainer / onPrimaryContainer) has clear text contrast in every theme.
- [ ] `wcag_check.py` passes.
