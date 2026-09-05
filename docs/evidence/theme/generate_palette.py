#!/usr/bin/env python3
"""Regenerate the app's Material 3 colour system from the brand seed, and audit its contrast.

    pip install materialyoucolor
    python3 docs/evidence/theme/generate_palette.py            # print the Kotlin token blocks
    python3 docs/evidence/theme/generate_palette.py --audit    # print the WCAG table

The numbers in `presentation/Theme.kt` and the contrast table in `docs/UX_AND_ACCESSIBILITY.md`
both come from this file. If the seed ever changes, change it here, paste the output into
Theme.kt, and paste the audit into the accessibility doc — do not hand-tune a single token, because
the whole point of a generated system is that the relationships between tones hold.

Why constants instead of generating at runtime: the seed is fixed, so a runtime generator would
spend startup time producing the same numbers every launch, and the values would stop being visible
in code review.
"""
import argparse
import json

from materialyoucolor.blend import Blend
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors as M
from materialyoucolor.hct import Hct
from materialyoucolor.palettes.tonal_palette import TonalPalette
from materialyoucolor.scheme.scheme_tonal_spot import SchemeTonalSpot

# The brand blue for Mankeu financial management.
SEED = 0xFF0061A4

# TonalSpot is Material 3's default variant — it keeps the seed's hue and gives it a restrained
# chroma, which is what a finance app wants. Fidelity/Content preserve more of the seed's own
# chroma but push `primary` to tone 30 and make `primaryContainer` the seed itself, which reads as
# a much heavier green across large fills. Spec "2021" is what Compose Material3 implements.
SPEC = "2021"

ROLES = [
    "primary", "onPrimary", "primaryContainer", "onPrimaryContainer", "inversePrimary",
    "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
    "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
    "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
    "surfaceTint", "inverseSurface", "inverseOnSurface", "surfaceBright", "surfaceDim",
    "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
    "surfaceContainerHigh", "surfaceContainerHighest",
    "outline", "outlineVariant", "scrim",
    "error", "onError", "errorContainer", "onErrorContainer",
    "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
    "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
    "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
]

# Money semantics Material 3 has no slot for. Each source is the web prototype's own colour.
CUSTOM_SOURCES = {"success": 0xFF52B788, "warning": 0xFFF4E285, "danger": 0xFFE07A5F}

# Whether to pull the source hue toward the seed before building its palette.
#
# `danger` is deliberately left alone. Harmonising rotates a hue up to 15 degrees toward the target,
# and the expense coral sits at hue 33 while the seed sits at 165 — so harmonising pushed it to
# hue 49, and tone 40 came out an orange-brown (#964913). It passed every contrast check and still
# looked wrong on device: the two big buttons on Catat stopped reading as "green in, red out" and
# started reading as "green and brown". For income and expense the hue *is* the meaning, so expense
# keeps its own. Income and plan-mode are harmonised because green and gold are already close
# enough to the brand that the rotation costs them nothing.
HARMONIZE = {"success": True, "warning": True, "danger": False}

# The expense-breakdown chart. Six distinguishable hues, all pulled toward the brand.
CHART_SOURCES = [0xFF2E7D32, 0xFFE65100, 0xFF1565C0, 0xFF6A1B9A, 0xFF00838F, 0xFFC2185B]


def hexs(argb: int) -> str:
    return "%06X" % (argb & 0xFFFFFF)


def scheme(dark: bool) -> SchemeTonalSpot:
    return SchemeTonalSpot(Hct.from_int(SEED), dark, 0.0, spec_version=SPEC)


def roles(dark: bool) -> dict:
    s = scheme(dark)
    return {r: hexs(getattr(M, r).get_hct(s).to_int()) for r in ROLES}


def custom(dark: bool) -> dict:
    out = {}
    for name, src in CUSTOM_SOURCES.items():
        source = Blend.harmonize(src, SEED) if HARMONIZE[name] else src
        p = TonalPalette.from_int(source)
        # Standard Material tone assignments for a colour role pair.
        tones = (80, 20, 30, 90) if dark else (40, 100, 90, 10)
        out[name] = dict(zip(("main", "on", "container", "onContainer"), (hexs(p.tone(t)) for t in tones)))
    return out


def chart(dark: bool) -> list:
    tone = 75 if dark else 40
    return [hexs(TonalPalette.from_int(Blend.harmonize(c, SEED)).tone(tone)) for c in CHART_SOURCES]


def relative_luminance(h: str) -> float:
    h = h.lstrip("#")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    linear = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def ratio(fg: str, bg: str) -> float:
    a, b = relative_luminance(fg), relative_luminance(bg)
    hi, lo = max(a, b), min(a, b)
    return (hi + 0.05) / (lo + 0.05)


# Only pairs the app actually draws. A generated system passes trivially on pairs nobody renders;
# the ones worth measuring are the fills, the text on them, and the borders of real controls.
TEXT_PAIRS = [
    ("onPrimary", "primary"), ("onPrimaryContainer", "primaryContainer"),
    ("onSecondary", "secondary"), ("onSecondaryContainer", "secondaryContainer"),
    ("onTertiary", "tertiary"), ("onTertiaryContainer", "tertiaryContainer"),
    ("onBackground", "background"), ("onSurface", "surface"),
    ("onSurfaceVariant", "surfaceVariant"),
    ("onSurface", "surfaceContainerLowest"), ("onSurface", "surfaceContainerLow"),
    ("onSurface", "surfaceContainer"), ("onSurface", "surfaceContainerHigh"),
    ("onSurface", "surfaceContainerHighest"),
    ("onSurfaceVariant", "surfaceContainerLow"), ("onSurfaceVariant", "surfaceContainerHighest"),
    ("onError", "error"), ("onErrorContainer", "errorContainer"),
    ("inverseOnSurface", "inverseSurface"),
    ("primary", "surface"), ("primary", "surfaceContainerLow"),
]
NON_TEXT_PAIRS = [("outline", "surface"), ("outline", "surfaceContainerLow"), ("outline", "background")]


def audit() -> list:
    rows = []
    for dark in (False, True):
        mode = "dark" if dark else "light"
        s, c, ch = roles(dark), custom(dark), chart(dark)
        for fg, bg in TEXT_PAIRS:
            rows.append((mode, f"{fg} / {bg}", s[fg], s[bg], 4.5))
        for fg, bg in NON_TEXT_PAIRS:
            rows.append((mode, f"{fg} / {bg}", s[fg], s[bg], 3.0))
        for name, v in c.items():
            rows.append((mode, f"on{name.title()} / {name}", v["on"], v["main"], 4.5))
            rows.append((mode, f"on{name.title()}Container / {name}Container", v["onContainer"], v["container"], 4.5))
            rows.append((mode, f"{name} as text / surface", v["main"], s["surface"], 4.5))
            rows.append((mode, f"{name} as text / surfaceContainerLow", v["main"], s["surfaceContainerLow"], 4.5))
        for i, h in enumerate(ch):
            rows.append((mode, f"chart[{i}] / surface", h, s["surface"], 3.0))
    return [(*r, round(ratio(r[2], r[3]), 2)) for r in rows]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--audit", action="store_true")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    if args.audit:
        failures = 0
        print(f"| Mode | Pair | Foreground | Background | Ratio | Needs | Result |")
        print("| --- | --- | --- | --- | --- | --- | --- |")
        for mode, label, fg, bg, need, got in audit():
            ok = got >= need
            failures += 0 if ok else 1
            print(f"| {mode} | {label} | `#{fg}` | `#{bg}` | {got}:1 | {need}:1 | {'PASS' if ok else 'FAIL'} |")
        print(f"\nFailures: {failures}")
        return

    if args.json:
        print(json.dumps({
            "seed": hexs(SEED),
            "light": {"roles": roles(False), "custom": custom(False), "chart": chart(False)},
            "dark": {"roles": roles(True), "custom": custom(True), "chart": chart(True)},
        }, indent=1))
        return

    for dark in (False, True):
        print(f"// --- {'dark' if dark else 'light'} ---")
        for role, value in roles(dark).items():
            print(f"    {role} = Color(0xFF{value}),")
        print(f"// --- {'dark' if dark else 'light'} semantic ---")
        for name, v in custom(dark).items():
            print(f"    {name} = Color(0xFF{v['main']}),")
            print(f"    on{name.title()} = Color(0xFF{v['on']}),")
            print(f"    {name}Container = Color(0xFF{v['container']}),")
            print(f"    on{name.title()}Container = Color(0xFF{v['onContainer']}),")
        print("    chart = listOf(")
        for value in chart(dark):
            print(f"        Color(0xFF{value}),")
        print("    ),")


if __name__ == "__main__":
    main()
