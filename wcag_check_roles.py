#!/usr/bin/env python3
"""
wcag_check_roles.py — contrast spot-check for the derived category colour roles.

The three extended category roles (quaternary/quinary/senary) are derived at
runtime from the active ColorScheme (see deriveExtendedRoles in
app/src/main/java/com/mapgie/goflo/ui/theme/Color.kt). They are not part of the
static palette tables, so wcag_check.py never sees them. This script mirrors
the Kotlin derivation over every palette's accent trio and asserts the derived
on-colour reaches WCAG AA for UI components / icons (3.0 : 1) against the role.

Keep the derivation here in sync with deriveExtendedRoles in Color.kt.

Usage:
    python3 wcag_check_roles.py                # check all palettes
    python3 wcag_check_roles.py --fails-only   # print only failing roles

Exit code: 0 — all derived roles pass  |  1 — one or more fail
"""

import colorsys
import sys

from wcag_check import THEMES, contrast_ratio

NEAR_BLACK = "1C1B1F"
WHITE = "FFFFFF"

# (source accent, hue rotation degrees, greyscale lightness shift) per role —
# mirrors deriveExtendedRoles in Color.kt.
DERIVATIONS = [
    ("quaternary", "primary",    30.0, 0.25),
    ("quinary",    "secondary", -30.0, 0.45),
    ("senary",     "tertiary",   45.0, 0.65),
]


def derive(base_hex: str, degrees: float, lightness_shift: float) -> str:
    """Python mirror of deriveExtendedRoles' per-role derivation."""
    r = int(base_hex[0:2], 16) / 255.0
    g = int(base_hex[2:4], 16) / 255.0
    b = int(base_hex[4:6], 16) / 255.0
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    if s < 0.10:
        direction = -1.0 if l > 0.5 else 1.0
        l = min(0.85, max(0.15, l + direction * lightness_shift))
    else:
        h = (h + degrees / 360.0) % 1.0
    r2, g2, b2 = colorsys.hls_to_rgb(h, l, s)
    return "%02X%02X%02X" % (round(r2 * 255), round(g2 * 255), round(b2 * 255))


def on_colour(role_hex: str) -> str:
    """Near-black or white, whichever contrasts more (mirror of Color.kt)."""
    if contrast_ratio(NEAR_BLACK, role_hex) >= contrast_ratio(WHITE, role_hex):
        return NEAR_BLACK
    return WHITE


def main() -> int:
    fails_only = "--fails-only" in sys.argv
    accents = {
        name: {k: theme[k] for k in ("primary", "secondary", "tertiary")}
        for name, theme in THEMES.items()
    }

    failures = 0
    checked = 0
    worst = (999.0, "")
    for name, trio in accents.items():
        lines = []
        for role, source, degrees, shift in DERIVATIONS:
            role_hex = derive(trio[source], degrees, shift)
            on_hex = on_colour(role_hex)
            ratio = contrast_ratio(on_hex, role_hex)
            checked += 1
            ok = ratio >= 3.0
            if not ok:
                failures += 1
            if ratio < worst[0]:
                worst = (ratio, f"{name} {role}")
            if not ok or not fails_only:
                mark = "PASS" if ok else "FAIL"
                lines.append(
                    f"  {mark}  {role:<10} #{role_hex} / on #{on_hex}  {ratio:5.2f} : 1"
                )
        if lines:
            print(name)
            print("\n".join(lines))
    print()
    print(f"{checked} derived roles checked across {len(accents)} palettes")
    print(f"Worst ratio: {worst[0]:.2f} : 1 ({worst[1]})")
    print("All passed" if failures == 0 else f"{failures} FAILED")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
