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
        "primary":               "C15542",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDAD4",
        "onPrimaryContainer":    "3E0400",
        "secondary":             "B0544A",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDAD6",
        "onSecondaryContainer":  "3E000A",
        "tertiary":              "B85C00",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFDCBE",
        "onTertiaryContainer":   "3C1A00",
        "background":            "FFF8F7",
        "onBackground":          "231917",
        "surface":               "FFF8F7",
        "onSurface":             "231917",
        "surfaceVariant":        "F5DEDA",
        "onSurfaceVariant":      "534341",
        "outline":               "857370",
    },
    "Teal (Light)": {
        "primary":               "00696F",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "9CF0F5",
        "onPrimaryContainer":    "001F22",
        "secondary":             "4A6364",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "CCE8E9",
        "onSecondaryContainer":  "051F20",
        "tertiary":              "4E6078",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "D6E4FF",
        "onTertiaryContainer":   "091C31",
        "background":            "F4FBFB",
        "onBackground":          "161D1E",
        "surface":               "F4FBFB",
        "onSurface":             "161D1E",
        "surfaceVariant":        "DAE4E5",
        "onSurfaceVariant":      "3F4949",
        "outline":               "6F7979",
    },
    "Sage (Light)": {
        "primary":               "386A20",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "B7F397",
        "onPrimaryContainer":    "072100",
        "secondary":             "55624C",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "D8E8CB",
        "onSecondaryContainer":  "131F0D",
        "tertiary":              "386669",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "BBEBEE",
        "onTertiaryContainer":   "002022",
        "background":            "F7FBF1",
        "onBackground":          "1A1C18",
        "surface":               "F7FBF1",
        "onSurface":             "1A1C18",
        "surfaceVariant":        "DEE4D8",
        "onSurfaceVariant":      "42493E",
        "outline":               "72796D",
    },
    "Coral (Dark)": {
        "primary":               "FFB4A8",
        "onPrimary":             "5F1612",
        "primaryContainer":      "7D2B20",
        "onPrimaryContainer":    "FFDAD4",
        "secondary":             "E7BDB8",
        "onSecondary":           "442925",
        "secondaryContainer":    "5D3F3B",
        "onSecondaryContainer":  "FFDAD6",
        "tertiary":              "FFB787",
        "onTertiary":            "532200",
        "tertiaryContainer":     "743100",
        "onTertiaryContainer":   "FFDCBE",
        "background":            "201A19",
        "onBackground":          "EDE0DE",
        "surface":               "201A19",
        "onSurface":             "EDE0DE",
        "surfaceVariant":        "534341",
        "onSurfaceVariant":      "D8C2BE",
        "outline":               "A28E8C",  # bumped from A08C8A
    },
    "Teal (Dark)": {
        "primary":               "80D5DB",
        "onPrimary":             "003739",
        "primaryContainer":      "004F52",
        "onPrimaryContainer":    "9CF0F5",
        "secondary":             "B0CCCD",
        "onSecondary":           "1B3435",
        "secondaryContainer":    "324B4C",
        "onSecondaryContainer":  "CCE8E9",
        "tertiary":              "B3C8E8",
        "onTertiary":            "1E3148",
        "tertiaryContainer":     "354860",
        "onTertiaryContainer":   "D6E4FF",
        "background":            "191C1D",
        "onBackground":          "E1E3E3",
        "surface":               "191C1D",
        "onSurface":             "E1E3E3",
        "surfaceVariant":        "3F4949",
        "onSurfaceVariant":      "BEC8C9",
        "outline":               "8B9595",  # bumped from 899393
    },
    "Sage (Dark)": {
        "primary":               "9DD679",
        "onPrimary":             "0D3900",
        "primaryContainer":      "254F0A",
        "onPrimaryContainer":    "B8F397",
        "secondary":             "BBCBAD",
        "onSecondary":           "273420",
        "secondaryContainer":    "3D4A35",
        "onSecondaryContainer":  "D8E8CB",
        "tertiary":              "A1CECE",
        "onTertiary":            "013737",
        "tertiaryContainer":     "1F4E4E",
        "onTertiaryContainer":   "BBEBEE",
        "background":            "1A1C18",
        "onBackground":          "E3E3DC",
        "surface":               "1A1C18",
        "onSurface":             "E3E3DC",
        "surfaceVariant":        "42493E",
        "onSurfaceVariant":      "C2C9BB",
        "outline":               "8E958A",  # bumped from 8C9388
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
        "primary":               "C2185B",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFD6E8",
        "onPrimaryContainer":    "3E001C",
        "secondary":             "B03070",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD8EF",
        "onSecondaryContainer":  "3A0028",
        "tertiary":              "994F00",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFDDB8",
        "onTertiaryContainer":   "311600",
        "background":            "FFF8FA",
        "onBackground":          "201217",
        "surface":               "FFF8FA",
        "onSurface":             "201217",
        "surfaceVariant":        "F5DEE7",
        "onSurfaceVariant":      "534349",
        "outline":               "856069",
    },
    "Summer Candy (Dark)": {
        "primary":               "FFB1CA",
        "onPrimary":             "67003A",
        "primaryContainer":      "8C0052",
        "onPrimaryContainer":    "FFD6E8",
        "secondary":             "F3B4D2",
        "onSecondary":           "4E0038",
        "secondaryContainer":    "69004E",
        "onSecondaryContainer":  "FFD8EF",
        "tertiary":              "FFB878",
        "onTertiary":            "4E2700",
        "tertiaryContainer":     "6E3900",
        "onTertiaryContainer":   "FFDDB8",
        "background":            "201217",
        "onBackground":          "EDD9E2",
        "surface":               "201217",
        "onSurface":             "EDD9E2",
        "surfaceVariant":        "534349",
        "onSurfaceVariant":      "D7C0C7",
        "outline":               "A48E94",  # bumped from A38D93
    },
    "Beach Vibes (Light)": {
        "primary":               "1565C0",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "D6E4FF",
        "onPrimaryContainer":    "001848",
        "secondary":             "7C5500",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDDB0",
        "onSecondaryContainer":  "271800",
        "tertiary":              "3D6B3E",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "BCEDBE",
        "onTertiaryContainer":   "002108",
        "background":            "F8FAFF",
        "onBackground":          "181C22",
        "surface":               "F8FAFF",
        "onSurface":             "181C22",
        "surfaceVariant":        "D8E3F4",
        "onSurfaceVariant":      "3C4756",
        "outline":               "6C7888",
    },
    "Beach Vibes (Dark)": {
        "primary":               "9EC5FF",
        "onPrimary":             "003171",
        "primaryContainer":      "004A9D",
        "onPrimaryContainer":    "D6E4FF",
        "secondary":             "FFBB6E",
        "onSecondary":           "422C00",
        "secondaryContainer":    "5E3F00",
        "onSecondaryContainer":  "FFDDB0",
        "tertiary":              "9EDEA0",
        "onTertiary":            "09380F",
        "tertiaryContainer":     "245228",
        "onTertiaryContainer":   "BAEBBC",
        "background":            "181C22",
        "onBackground":          "DDE3ED",
        "surface":               "181C22",
        "onSurface":             "DDE3ED",
        "surfaceVariant":        "3C4756",
        "onSurfaceVariant":      "BCC8D9",
        "outline":               "8A939C",  # bumped from 879099
    },
    "Peach Melba (Light)": {
        "primary":               "9C5119",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDCC7",
        "onPrimaryContainer":    "341200",
        "secondary":             "8A5835",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFDCBE",
        "onSecondaryContainer":  "321200",
        "tertiary":              "4E6539",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "D0ECBA",
        "onTertiaryContainer":   "0C2000",
        "background":            "FFF8F5",
        "onBackground":          "201A16",
        "surface":               "FFF8F5",
        "onSurface":             "201A16",
        "surfaceVariant":        "F2DDD4",
        "onSurfaceVariant":      "52423C",
        "outline":               "85726A",
    },
    "Peach Melba (Dark)": {
        "primary":               "FFB98A",
        "onPrimary":             "542400",
        "primaryContainer":      "763400",
        "onPrimaryContainer":    "FFDCC7",
        "secondary":             "FFBB8D",
        "onSecondary":           "4C2800",
        "secondaryContainer":    "6B3B00",
        "onSecondaryContainer":  "FFDCBE",
        "tertiary":              "B5CD9A",
        "onTertiary":            "21360B",
        "tertiaryContainer":     "374D1F",
        "onTertiaryContainer":   "D1EDBA",
        "background":            "201A16",
        "onBackground":          "EDE0D8",
        "surface":               "201A16",
        "onSurface":             "EDE0D8",
        "surfaceVariant":        "52423C",
        "onSurfaceVariant":      "D5C5BB",
        "outline":               "9E8E87",
    },
    "All-Night Disco Party (Light)": {
        "primary":               "7B0EA0",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "F2DAFF",
        "onPrimaryContainer":    "2D0047",
        "secondary":             "9C006E",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD8F2",
        "onSecondaryContainer":  "380030",
        "tertiary":              "8B6A00",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "FFE077",
        "onTertiaryContainer":   "292000",
        "background":            "FFF8FE",
        "onBackground":          "1E1020",
        "surface":               "FFF8FE",
        "onSurface":             "1E1020",
        "surfaceVariant":        "EDDFF0",
        "onSurfaceVariant":      "4C3E50",
        "outline":               "7E6D83",
    },
    "All-Night Disco Party (Dark)": {
        "primary":               "E0ABFF",
        "onPrimary":             "4B0075",
        "primaryContainer":      "6500A3",
        "onPrimaryContainer":    "F2DAFF",
        "secondary":             "FFADE3",
        "onSecondary":           "5B0047",
        "secondaryContainer":    "800063",
        "onSecondaryContainer":  "FFD8F2",
        "tertiary":              "EFCC2A",
        "onTertiary":            "3B2E00",
        "tertiaryContainer":     "544200",
        "onTertiaryContainer":   "FFE47E",
        "background":            "1E1020",
        "onBackground":          "EBD7F0",
        "surface":               "1E1020",
        "onSurface":             "EBD7F0",
        "surfaceVariant":        "4C3E50",
        "onSurfaceVariant":      "CFC3D4",
        "outline":               "9A8D9E",
    },
    "Metal Chick (Light)": {
        "primary":               "4A4A5A",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "E2E0F0",
        "onPrimaryContainer":    "0D0D1A",
        "secondary":             "6B2D3E",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "FFD9E1",
        "onSecondaryContainer":  "270010",
        "tertiary":              "585868",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "DEDEDEA",
        "onTertiaryContainer":   "151520",
        "background":            "F6F5FA",
        "onBackground":          "1B1B22",
        "surface":               "F6F5FA",
        "onSurface":             "1B1B22",
        "surfaceVariant":        "E0DEE8",
        "onSurfaceVariant":      "474450",
        "outline":               "787580",
    },
    "Metal Chick (Dark)": {
        "primary":               "C5C3D1",
        "onPrimary":             "2D2B3B",
        "primaryContainer":      "434153",
        "onPrimaryContainer":    "E1DFF0",
        "secondary":             "F1B8C6",
        "onSecondary":           "4A1927",
        "secondaryContainer":    "63303F",
        "onSecondaryContainer":  "FFD9E1",
        "tertiary":              "CCCADA",
        "onTertiary":            "333243",
        "tertiaryContainer":     "4A495B",
        "onTertiaryContainer":   "E8E6F8",
        "background":            "1B1B22",
        "onBackground":          "E5E3ED",
        "surface":               "1B1B22",
        "onSurface":             "E5E3ED",
        "surfaceVariant":        "474450",
        "onSurfaceVariant":      "C9C6D2",
        "outline":               "939099",
    },
    "Whimsy Whispers (Light)": {
        "primary":               "5050A0",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "E3DFFF",
        "onPrimaryContainer":    "0F0060",
        "secondary":             "5C5A78",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "E3DFFF",
        "onSecondaryContainer":  "191640",
        "tertiary":              "2D7A6E",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "B8EDE7",
        "onTertiaryContainer":   "002622",
        "background":            "FBF8FF",
        "onBackground":          "1C1A24",
        "surface":               "FBF8FF",
        "onSurface":             "1C1A24",
        "surfaceVariant":        "E5DFFE",
        "onSurfaceVariant":      "49454F",
        "outline":               "7A7580",
    },
    "Whimsy Whispers (Dark)": {
        "primary":               "C4C0FF",
        "onPrimary":             "230070",
        "primaryContainer":      "3C3A8C",
        "onPrimaryContainer":    "E3DFFF",
        "secondary":             "C7C4E3",
        "onSecondary":           "2F2D4A",
        "secondaryContainer":    "464362",
        "onSecondaryContainer":  "E3DFFF",
        "tertiary":              "9DD6D0",
        "onTertiary":            "003D38",
        "tertiaryContainer":     "145750",
        "onTertiaryContainer":   "B8EDE7",
        "background":            "1C1A24",
        "onBackground":          "E6E1F0",
        "surface":               "1C1A24",
        "onSurface":             "E6E1F0",
        "surfaceVariant":        "49454F",
        "onSurfaceVariant":      "CBC5D0",
        "outline":               "96909F",  # bumped from 958F9E
    },
    "Colour Me Happy (Light)": {
        "primary":               "C13A00",
        "onPrimary":             "FFFFFF",
        "primaryContainer":      "FFDDD5",
        "onPrimaryContainer":    "400F00",
        "secondary":             "1B6FA8",
        "onSecondary":           "FFFFFF",
        "secondaryContainer":    "D1E5FF",
        "onSecondaryContainer":  "001E36",
        "tertiary":              "4D7C00",
        "onTertiary":            "FFFFFF",
        "tertiaryContainer":     "CBF176",
        "onTertiaryContainer":   "122500",
        "background":            "FFF8F5",
        "onBackground":          "201A16",
        "surface":               "FFF8F5",
        "onSurface":             "201A16",
        "surfaceVariant":        "F4DDDA",
        "onSurfaceVariant":      "514442",
        "outline":               "847271",
    },
    "Colour Me Happy (Dark)": {
        "primary":               "FFB59A",
        "onPrimary":             "601E00",
        "primaryContainer":      "882B00",
        "onPrimaryContainer":    "FFDDD5",
        "secondary":             "9BCAFF",
        "onSecondary":           "003358",
        "secondaryContainer":    "004C7D",
        "onSecondaryContainer":  "D1E5FF",
        "tertiary":              "B2D958",
        "onTertiary":            "243E00",
        "tertiaryContainer":     "395900",
        "onTertiaryContainer":   "CBF173",
        "background":            "201A16",
        "onBackground":          "EDE0DA",
        "surface":               "201A16",
        "onSurface":             "EDE0DA",
        "surfaceVariant":        "514442",
        "onSurfaceVariant":      "D5C3C0",
        "outline":               "A18F8D",  # bumped from 9E8C8A
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
