#!/usr/bin/env python3
"""
wcag_check.py — WCAG AA contrast checker for GoFlo themes.

Mirrors the colour values in app/src/main/java/com/mapgie/goflo/ui/theme/Color.kt.
Keep both files in sync whenever a theme colour changes.

WCAG AA thresholds:
  4.5 : 1  —  normal body text
  3.0 : 1  —  large text (≥ 18 pt / ≥ 14 pt bold) and UI components
               (borders, icons, focus rings, indicator dots)

Usage:
    python3 wcag_check.py                  # check all themes
    python3 wcag_check.py coral            # filter by theme name (partial, case-insensitive)
    python3 wcag_check.py --fails-only     # print only failing pairs
    python3 wcag_check.py coral --fails-only

Exit code: 0 — all pairs pass  |  1 — one or more pairs fail
"""

import sys

# ── WCAG maths ────────────────────────────────────────────────────────────────

def _linearize(c: int) -> float:
    """sRGB gamma expansion for a single 0-255 channel."""
    v = c / 255.0
    return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4


def luminance(hex_color: str) -> float:
    """WCAG relative luminance of a 6-digit hex colour (# prefix optional)."""
    h = hex_color.lstrip("#")
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return 0.2126 * _linearize(r) + 0.7152 * _linearize(g) + 0.0722 * _linearize(b)


def contrast_ratio(fg: str, bg: str) -> float:
    """WCAG contrast ratio between two hex colours."""
    l1, l2 = luminance(fg), luminance(bg)
    hi, lo = (l1, l2) if l1 >= l2 else (l2, l1)
    return (hi + 0.05) / (lo + 0.05)


# ── Colour schemes — mirrors Color.kt exactly ─────────────────────────────────
# Key: human-readable theme name used in output only (not stored anywhere).

THEMES: dict[str, dict[str, str]] = {
    "Coral (Light)": {
        "primary":               "BC4D3F",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDAD2",
        "onPrimaryContainer":    "3F0900",
        "secondary":             "007E7A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "B4ECE7",
        "onSecondaryContainer":  "002523",
        "tertiary":              "956800",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFE08D",
        "onTertiaryContainer":   "251A00",
        "background":            "FFF6F2",
        "onBackground":          "221915",
        "surface":               "FFF6F2",
        "onSurface":             "221915",
        "surfaceVariant":        "F4DDD5",
        "onSurfaceVariant":      "524340",
        "outline":               "856E68",
    },
    "Teal (Light)": {
        "primary":               "00747C",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "9DEFF6",
        "onPrimaryContainer":    "002023",
        "secondary":             "B3572A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDBC8",
        "onSecondaryContainer":  "381300",
        "tertiary":              "4B5BAC",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "DEE1FF",
        "onTertiaryContainer":   "00115A",
        "background":            "F0FBFC",
        "onBackground":          "161C1D",
        "surface":               "F0FBFC",
        "onSurface":             "161C1D",
        "surfaceVariant":        "DAE4E5",
        "onSurfaceVariant":      "3F4949",
        "outline":               "6F7979",
    },
    "Sage (Light)": {
        "primary":               "4F7D2B",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "C8F09F",
        "onPrimaryContainer":    "112100",
        "secondary":             "B5532A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDBC8",
        "onSecondaryContainer":  "3A1100",
        "tertiary":              "8C6212",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFDF9C",
        "onTertiaryContainer":   "2A1D00",
        "background":            "F7FBEE",
        "onBackground":          "1A1C16",
        "surface":               "F7FBEE",
        "onSurface":             "1A1C16",
        "surfaceVariant":        "DEE5D2",
        "onSurfaceVariant":      "424A3B",
        "outline":               "72796A",
    },
    "Coral (Dark)": {
        "primary":               "FFB4A4",
        "onPrimary":             "5C1600",
        "primaryContainer":      "7C2A18",
        "onPrimaryContainer":    "FFDAD0",
        "secondary":             "5BD8D0",
        "onSecondary":           "003734",
        "secondaryContainer":    "00504C",
        "onSecondaryContainer":  "B4EEE9",
        "tertiary":              "FFD787",
        "onTertiary":            "3D2D00",
        "tertiaryContainer":     "574200",
        "onTertiaryContainer":   "FFE08D",
        "background":            "1C110E",
        "onBackground":          "F1DED8",
        "surface":               "1C110E",
        "onSurface":             "F1DED8",
        "surfaceVariant":        "523F3A",
        "onSurfaceVariant":      "D7C2BC",
        "outline":               "A28B86",
    },
    "Teal (Dark)": {
        "primary":               "80D5DB",
        "onPrimary":             "003739",
        "primaryContainer":      "004F52",
        "onPrimaryContainer":    "9DEFF6",
        "secondary":             "FFB28F",
        "onSecondary":           "5A1B00",
        "secondaryContainer":    "8C3B16",
        "onSecondaryContainer":  "FFDBC8",
        "tertiary":              "BCC2FF",
        "onTertiary":            "1C257B",
        "tertiaryContainer":     "353F93",
        "onTertiaryContainer":   "DEE1FF",
        "background":            "0E1818",
        "onBackground":          "E0E3E3",
        "surface":               "0E1818",
        "onSurface":             "E0E3E3",
        "surfaceVariant":        "3F4949",
        "onSurfaceVariant":      "BEC8C9",
        "outline":               "8B9595",
    },
    "Sage (Dark)": {
        "primary":               "ACD888",
        "onPrimary":             "1B3900",
        "primaryContainer":      "2D530B",
        "onPrimaryContainer":    "C8F09F",
        "secondary":             "FFB28F",
        "onSecondary":           "5A1B00",
        "secondaryContainer":    "8B3914",
        "onSecondaryContainer":  "FFDBC8",
        "tertiary":              "F4C16D",
        "onTertiary":            "422C00",
        "tertiaryContainer":     "5E4300",
        "onTertiaryContainer":   "FFDF9C",
        "background":            "14170F",
        "onBackground":          "E2E4D7",
        "surface":               "14170F",
        "onSurface":             "E2E4D7",
        "surfaceVariant":        "424A3B",
        "onSurfaceVariant":      "C2C9B6",
        "outline":               "919888",
    },
    "High Contrast (Light)": {
        "primary":               "1A1A1A",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "E8E8E8",
        "onPrimaryContainer":    "000000",
        "secondary":             "1A1A1A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "E8E8E8",
        "onSecondaryContainer":  "000000",
        "tertiary":              "1A1A1A",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "E8E8E8",
        "onTertiaryContainer":   "000000",
        "background":            "FFFFFF",
        "onBackground":          "000000",
        "surface":               "FFFFFF",
        "onSurface":             "000000",
        "surfaceVariant":        "F0F0F0",
        "onSurfaceVariant":      "000000",
        "outline":               "1A1A1A",
    },
    "High Contrast (Dark)": {
        "primary":               "FFFFFF",
        "onPrimary":             "000000",
        "primaryContainer":      "1A1A1A",
        "onPrimaryContainer":    "FFFFFF",
        "secondary":             "FFFFFF",
        "onSecondary":           "000000",
        "secondaryContainer":    "1A1A1A",
        "onSecondaryContainer":  "FFFFFF",
        "tertiary":              "FFFFFF",
        "onTertiary":            "000000",
        "tertiaryContainer":     "1A1A1A",
        "onTertiaryContainer":   "FFFFFF",
        "background":            "000000",
        "onBackground":          "FFFFFF",
        "surface":               "000000",
        "onSurface":             "FFFFFF",
        "surfaceVariant":        "1A1A1A",
        "onSurfaceVariant":      "FFFFFF",
        "outline":               "DEDEDE",
    },
    "Blue & Orange": {
        "primary":               "005FAD",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "D1E4FF",
        "onPrimaryContainer":    "001D36",
        "secondary":             "8B5000",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDDB8",
        "onSecondaryContainer":  "2D1600",
        "tertiary":              "6B5F00",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "F5E400",
        "onTertiaryContainer":   "201C00",
        "background":            "F8FAFF",
        "onBackground":          "191C1E",
        "surface":               "F8FAFF",
        "onSurface":             "191C1E",
        "surfaceVariant":        "DDE3EA",
        "onSurfaceVariant":      "404A51",
        "outline":               "70787F",
    },
    # SYSTEM uses Teal (Light/Dark) depending on device preference —
    # covered by those two entries above; no separate entry needed.

    # ── Fun palettes ─────────────────────────────────────────────────────────
    "Summer Candy (Light)": {
        "primary":               "D81B60",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFD8E5",
        "onPrimaryContainer":    "40001B",
        "secondary":             "007F76",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "6FF5E5",
        "onSecondaryContainer":  "00201D",
        "tertiary":              "9A6700",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFE08D",
        "onTertiaryContainer":   "271B00",
        "background":            "FFF7FA",
        "onBackground":          "1E1316",
        "surface":               "FFF7FA",
        "onSurface":             "1E1316",
        "surfaceVariant":        "F4DDE5",
        "onSurfaceVariant":      "534249",
        "outline":               "856D74",
    },
    "Summer Candy (Dark)": {
        "primary":               "FFB1CA",
        "onPrimary":             "650033",
        "primaryContainer":      "8E0049",
        "onPrimaryContainer":    "FFD8E5",
        "secondary":             "6DDFD0",
        "onSecondary":           "003A35",
        "secondaryContainer":    "00514B",
        "onSecondaryContainer":  "6FF5E5",
        "tertiary":              "FFCD66",
        "onTertiary":            "3F2D00",
        "tertiaryContainer":     "5A4200",
        "onTertiaryContainer":   "FFE08D",
        "background":            "1D1014",
        "onBackground":          "ECDFE3",
        "surface":               "1D1014",
        "onSurface":             "ECDFE3",
        "surfaceVariant":        "534249",
        "onSurfaceVariant":      "D6C1C8",
        "outline":               "A39095",
    },
    "Beach Vibes (Light)": {
        "primary":               "1265AF",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "D5E3FF",
        "onPrimaryContainer":    "001C3D",
        "secondary":             "9A6800",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFE08D",
        "onSecondaryContainer":  "251A00",
        "tertiary":              "297F6C",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "A8F0DC",
        "onTertiaryContainer":   "002019",
        "background":            "F8FAFE",
        "onBackground":          "181C22",
        "surface":               "F8FAFE",
        "onSurface":             "181C22",
        "surfaceVariant":        "E1E3EE",
        "onSurfaceVariant":      "444751",
        "outline":               "757782",
    },
    "Beach Vibes (Dark)": {
        "primary":               "A8C8FF",
        "onPrimary":             "002E66",
        "primaryContainer":      "00478A",
        "onPrimaryContainer":    "D5E3FF",
        "secondary":             "F4C16D",
        "onSecondary":           "422C00",
        "secondaryContainer":    "5E4300",
        "onSecondaryContainer":  "FFE08D",
        "tertiary":              "84D7BC",
        "onTertiary":            "003828",
        "tertiaryContainer":     "00513B",
        "onTertiaryContainer":   "A8F0DC",
        "background":            "101820",
        "onBackground":          "DDE3EB",
        "surface":               "101820",
        "onSurface":             "DDE3EB",
        "surfaceVariant":        "444751",
        "onSurfaceVariant":      "C5C7D2",
        "outline":               "93959F",
    },
    "Peach Melba (Light)": {
        "primary":               "B35535",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDBC9",
        "onPrimaryContainer":    "3A1300",
        "secondary":             "B53369",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD9E3",
        "onSecondaryContainer":  "3F0024",
        "tertiary":              "8A6926",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFE2A0",
        "onTertiaryContainer":   "2A1F00",
        "background":            "FFF6F0",
        "onBackground":          "201813",
        "surface":               "FFF6F0",
        "onSurface":             "201813",
        "surfaceVariant":        "F4DED1",
        "onSurfaceVariant":      "534439",
        "outline":               "867262",
    },
    "Peach Melba (Dark)": {
        "primary":               "FFB694",
        "onPrimary":             "571F00",
        "primaryContainer":      "7A3015",
        "onPrimaryContainer":    "FFDBC9",
        "secondary":             "FFB1C9",
        "onSecondary":           "65003A",
        "secondaryContainer":    "8C1F4F",
        "onSecondaryContainer":  "FFD9E3",
        "tertiary":              "F4C16D",
        "onTertiary":            "432D00",
        "tertiaryContainer":     "5F4300",
        "onTertiaryContainer":   "FFE2A0",
        "background":            "1E1410",
        "onBackground":          "EDE0D8",
        "surface":               "1E1410",
        "onSurface":             "EDE0D8",
        "surfaceVariant":        "534439",
        "onSurfaceVariant":      "D7C3B5",
        "outline":               "A59185",
    },
    "All-Night Disco Party (Light)": {
        "primary":               "C1127A",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFD6EA",
        "onPrimaryContainer":    "3D003C",
        "secondary":             "6E1FB5",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "ECDCFF",
        "onSecondaryContainer":  "270060",
        "tertiary":              "966900",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFE08C",
        "onTertiaryContainer":   "2A1D00",
        "background":            "FFF7FB",
        "onBackground":          "1E1020",
        "surface":               "FFF7FB",
        "onSurface":             "1E1020",
        "surfaceVariant":        "F0DEEC",
        "onSurfaceVariant":      "4F4452",
        "outline":               "807385",
    },
    "All-Night Disco Party (Dark)": {
        "primary":               "FF66B8",
        "onPrimary":             "5C0040",
        "primaryContainer":      "890062",
        "onPrimaryContainer":    "FFD6EA",
        "secondary":             "D5B2FF",
        "onSecondary":           "3F0090",
        "secondaryContainer":    "5800B0",
        "onSecondaryContainer":  "ECDCFF",
        "tertiary":              "FFD350",
        "onTertiary":            "3A2A00",
        "tertiaryContainer":     "523D00",
        "onTertiaryContainer":   "FFE08C",
        "background":            "170820",
        "onBackground":          "ECDAEC",
        "surface":               "170820",
        "onSurface":             "ECDAEC",
        "surfaceVariant":        "4F4452",
        "onSurfaceVariant":      "D2C2D2",
        "outline":               "A092A2",
    },
    "Metal Chick (Light)": {
        "primary":               "2E2E3A",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "DCDBE9",
        "onPrimaryContainer":    "0E0E1C",
        "secondary":             "C8235A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD8E0",
        "onSecondaryContainer":  "3F001A",
        "tertiary":              "6A6A78",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "E3E2F0",
        "onTertiaryContainer":   "1C1C2A",
        "background":            "F4F3F8",
        "onBackground":          "1B1B22",
        "surface":               "F4F3F8",
        "onSurface":             "1B1B22",
        "surfaceVariant":        "E2E0EC",
        "onSurfaceVariant":      "45434E",
        "outline":               "76737E",
    },
    "Metal Chick (Dark)": {
        "primary":               "C7C5D6",
        "onPrimary":             "2F2D3D",
        "primaryContainer":      "454354",
        "onPrimaryContainer":    "DCDBE9",
        "secondary":             "FF8FB0",
        "onSecondary":           "5C0028",
        "secondaryContainer":    "88133E",
        "onSecondaryContainer":  "FFD8E0",
        "tertiary":              "9D9DAC",
        "onTertiary":            "2F2F3E",
        "tertiaryContainer":     "4A4A58",
        "onTertiaryContainer":   "E3E2F0",
        "background":            "0E0E13",
        "onBackground":          "E5E3ED",
        "surface":               "0E0E13",
        "onSurface":             "E5E3ED",
        "surfaceVariant":        "45434E",
        "onSurfaceVariant":      "C8C5D0",
        "outline":               "918E99",
    },
    "Whimsy Whispers (Light)": {
        "primary":               "6E5DC4",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "E5DFFF",
        "onPrimaryContainer":    "1A0067",
        "secondary":             "BD4878",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD9E4",
        "onSecondaryContainer":  "3D0024",
        "tertiary":              "2B7F68",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "A6F0D6",
        "onTertiaryContainer":   "002016",
        "background":            "FBF8FF",
        "onBackground":          "1C1A24",
        "surface":               "FBF8FF",
        "onSurface":             "1C1A24",
        "surfaceVariant":        "E5DFF0",
        "onSurfaceVariant":      "49454F",
        "outline":               "7A7580",
    },
    "Whimsy Whispers (Dark)": {
        "primary":               "C7BEFF",
        "onPrimary":             "260092",
        "primaryContainer":      "3D2EAE",
        "onPrimaryContainer":    "E5DFFF",
        "secondary":             "FFB1C6",
        "onSecondary":           "650033",
        "secondaryContainer":    "8E1F50",
        "onSecondaryContainer":  "FFD9E4",
        "tertiary":              "80DAB8",
        "onTertiary":            "003828",
        "tertiaryContainer":     "00513B",
        "onTertiaryContainer":   "A6F0D6",
        "background":            "16131F",
        "onBackground":          "E6E1F0",
        "surface":               "16131F",
        "onSurface":             "E6E1F0",
        "surfaceVariant":        "49454F",
        "onSurfaceVariant":      "CBC5D0",
        "outline":               "96909F",
    },
    "Colour Me Happy (Light)": {
        "primary":               "D63A26",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDAD2",
        "onPrimaryContainer":    "410000",
        "secondary":             "1872BD",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "D1E4FF",
        "onSecondaryContainer":  "001D36",
        "tertiary":              "428129",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "C6F0A1",
        "onTertiaryContainer":   "0F2200",
        "background":            "FFFCF5",
        "onBackground":          "1F1A14",
        "surface":               "FFFCF5",
        "onSurface":             "1F1A14",
        "surfaceVariant":        "EFE3D5",
        "onSurfaceVariant":      "4E443A",
        "outline":               "80766A",
    },
    "Colour Me Happy (Dark)": {
        "primary":               "FFB4A4",
        "onPrimary":             "5C1500",
        "primaryContainer":      "882000",
        "onPrimaryContainer":    "FFDAD2",
        "secondary":             "A0CAFF",
        "onSecondary":           "002F66",
        "secondaryContainer":    "00497D",
        "onSecondaryContainer":  "D1E4FF",
        "tertiary":              "ACD688",
        "onTertiary":            "1A3900",
        "tertiaryContainer":     "2D530B",
        "onTertiaryContainer":   "C6F0A1",
        "background":            "1A1612",
        "onBackground":          "EDE0D2",
        "surface":               "1A1612",
        "onSurface":             "EDE0D2",
        "surfaceVariant":        "4E443A",
        "onSurfaceVariant":      "D2C4B5",
        "outline":               "9F9184",
    },
    "Dragon Fire (Light)": {
        "primary":               "B0181F",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFCFCC",
        "onPrimaryContainer":    "3F0001",
        "secondary":             "C04A0E",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDBC9",
        "onSecondaryContainer":  "3B1100",
        "tertiary":              "996800",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFDE8E",
        "onTertiaryContainer":   "2D1F00",
        "background":            "FFF8F4",
        "onBackground":          "1E140F",
        "surface":               "FFF8F4",
        "onSurface":             "1E140F",
        "surfaceVariant":        "F3DED5",
        "onSurfaceVariant":      "524338",
        "outline":               "87715F",
    },
    "Dragon Fire (Dark)": {
        "primary":               "FF8A82",
        "onPrimary":             "680000",
        "primaryContainer":      "960000",
        "onPrimaryContainer":    "FFDAD4",
        "secondary":             "FFB28F",
        "onSecondary":           "561A00",
        "secondaryContainer":    "8E2D00",
        "onSecondaryContainer":  "FFDBC9",
        "tertiary":              "FFCD66",
        "onTertiary":            "422C00",
        "tertiaryContainer":     "5E4300",
        "onTertiaryContainer":   "FFDE8E",
        "background":            "1C0907",
        "onBackground":          "F1DDD7",
        "surface":               "1C0907",
        "onSurface":             "F1DDD7",
        "surfaceVariant":        "523F38",
        "onSurfaceVariant":      "D7C2B8",
        "outline":               "A28D81",
    },
    "Midnight Neon (Light)": {
        "primary":               "C5128A",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFD6E8",
        "onPrimaryContainer":    "3D003C",
        "secondary":             "006D90",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "B8E9FF",
        "onSecondaryContainer":  "001E2F",
        "tertiary":              "4C7A0E",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "C2F088",
        "onTertiaryContainer":   "0F2300",
        "background":            "FBF7FF",
        "onBackground":          "161020",
        "surface":               "FBF7FF",
        "onSurface":             "161020",
        "surfaceVariant":        "EDDEEC",
        "onSurfaceVariant":      "4D4350",
        "outline":               "7F7484",
    },
    "Midnight Neon (Dark)": {
        "primary":               "FF55C8",
        "onPrimary":             "5C0048",
        "primaryContainer":      "870068",
        "onPrimaryContainer":    "FFD6EA",
        "secondary":             "5EE0FF",
        "onSecondary":           "00374A",
        "secondaryContainer":    "00516C",
        "onSecondaryContainer":  "B8E9FF",
        "tertiary":              "C2F052",
        "onTertiary":            "1D3300",
        "tertiaryContainer":     "2D4900",
        "onTertiaryContainer":   "C2F088",
        "background":            "0A0A18",
        "onBackground":          "EDE5FA",
        "surface":               "0A0A18",
        "onSurface":             "EDE5FA",
        "surfaceVariant":        "4D4350",
        "onSurfaceVariant":      "D0C3D0",
        "outline":               "9D90A4",
    },
}

# ── Pairs to check ────────────────────────────────────────────────────────────
# (foreground_key, background_key, threshold, description)
# Threshold: 4.5 = WCAG AA text | 3.0 = WCAG AA UI component

PAIRS: list[tuple[str, str, float, str]] = [
    # Body text — 4.5 : 1
    ("onPrimary",            "primary",            4.5, "text on primary (buttons, period circles)"),
    ("onSecondary",          "secondary",          4.5, "text on secondary elements"),
    ("onTertiary",           "tertiary",           4.5, "text on tertiary elements"),
    ("onPrimaryContainer",   "primaryContainer",   4.5, "text in primary containers"),
    ("onSecondaryContainer", "secondaryContainer", 4.5, "text in secondary containers"),
    ("onTertiaryContainer",  "tertiaryContainer",  4.5, "text in tertiary containers"),
    ("onBackground",         "background",         4.5, "body text on background"),
    ("onSurface",            "surface",            4.5, "body text on surface"),
    ("onSurfaceVariant",     "surfaceVariant",     4.5, "subtitle / caption text in cards"),
    # UI components — 3.0 : 1
    ("outline",              "surfaceVariant",     3.0, "borders / dividers on cards"),
    ("outline",              "background",         3.0, "borders on background"),
    ("primary",              "primaryContainer",   3.0, "focused outlines / active chip border"),
    ("primary",              "surfaceVariant",     3.0, "ovulation dot / indicator on card"),
    ("primary",              "background",         3.0, "period circles on background"),
    ("primary",              "surface",            3.0, "primary on surface"),
]

# ── CLI ───────────────────────────────────────────────────────────────────────

def main() -> int:
    args = sys.argv[1:]
    fails_only = "--fails-only" in args
    name_filter = next((a for a in args if not a.startswith("--")), "").lower()

    themes_to_check = {
        name: scheme for name, scheme in THEMES.items()
        if name_filter in name.lower()
    }

    if not themes_to_check:
        print(f"No themes matched '{name_filter}'. Available themes:")
        for name in THEMES:
            print(f"  {name}")
        return 1

    total = 0
    failures = 0

    for theme_name, scheme in themes_to_check.items():
        theme_failures: list[str] = []
        theme_passes: list[str] = []

        for fg_key, bg_key, threshold, description in PAIRS:
            fg = scheme.get(fg_key)
            bg = scheme.get(bg_key)
            if fg is None or bg is None:
                # Skip pairs where the theme doesn't define both colours
                continue

            ratio = contrast_ratio(fg, bg)
            total += 1
            passed = ratio >= threshold
            marker = "PASS" if passed else "FAIL"
            line = (
                f"  {marker}  {ratio:5.2f}:1  "
                f"#{fg} / #{bg}  "
                f"{fg_key} / {bg_key}  —  {description}"
            )
            if passed:
                theme_passes.append(line)
            else:
                failures += 1
                theme_failures.append(line)

        # Print theme block
        print(f"\n{'─'*70}")
        print(f"  {theme_name}")
        print(f"{'─'*70}")
        if not fails_only:
            for line in theme_passes:
                print(line)
        for line in theme_failures:
            print(line)
        if not theme_failures:
            count = len(theme_passes)
            print(f"  All {count} pairs pass ✓")

    print(f"\n{'═'*70}")
    print(f"  {total} pairs checked across {len(themes_to_check)} theme(s)")
    if failures:
        print(f"  {failures} FAILED  ✗")
    else:
        print(f"  All passed  ✓")
    print(f"{'═'*70}")

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
