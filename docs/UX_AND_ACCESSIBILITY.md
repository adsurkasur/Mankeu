---
status: implemented-baseline
last_verified: 2026-10-22
audience: human-and-ai
sensitivity: internal
evidence_basis:
  - owner-confirmed phone and tablet requirement
  - workshop-origin problem statement
  - docs/PRODUCT_REQUIREMENTS.md
---

# UX and Accessibility

## Experience principles

1. **Record first, organize progressively.** A user in the field can save a valid transaction with a few required inputs and add detail later.
2. **Use business language.** Prefer `Uang Masuk`, `Uang Keluar`, `Setoran Modal`, `Ambil Pribadi`, and `Unit Usaha`; accounting detail remains available as explanation.
3. **No silent change.** Every import and every data cleanup leaves a visible notice; import never guesses (DEC-012).
4. **No dead ends.** Error states explain the impact and the next safe action.
5. **One meaning everywhere.** Amount, category, unit, and financing terms must not change meaning across entry, reports, import, and export.
6. **Offline is normal.** Network state is secondary; core screens do not look broken when offline.

## Information architecture

Compact-width destinations (bottom bar; rail from 840dp):

- `Beranda`: cash, key metrics, monthly cash flow, expense distribution, stock, plan feasibility, this month's break-even;
- `Catatan`: entry form and record list with Ubah/Hapus in Realisasi; Rencana Usaha in Proyeksi;
- `Stok`: stock items of the selected unit and shared items;
- `Laporan`: Laba Rugi dan Arus Kas, Kelayakan & BEP, Realisasi vs Target.

Overlays from the top bar: `Berkas & Data` (import, export, backup, categories, import history), `Pengaturan`, and `Pusat Bantuan`. A record speed dial (Uang Masuk, Uang Keluar) sits on Beranda and Catatan.

## Phone behavior

- Use a bottom navigation bar for primary destinations and a prominent quick-entry action.
- Reopen the most recently selected business and unit, while always showing the current scope.
- Transaction entry initially asks for direction, amount, date, description/category, and scope. Optional details expand progressively.
- Amount entry uses an IDR-aware numeric keyboard and formatted confirmation without floating-point behavior.
- Reports use summaries and drill-down cards; core understanding must not require a wide spreadsheet or horizontal scrolling.
- Destructive actions use clear object names and recoverability, not generic `Apakah Anda yakin?` dialogs.

## Tablet behavior

- Use adaptive navigation rail or pane layouts at suitable widths.
- Use the extra width for context (wider report cards, side-by-side metrics) rather than stretching phone cards.
- Support keyboard navigation, visible focus, and hardware-keyboard entry.
- Do not merely stretch phone cards across the screen; take advantage of additional context without creating a separate product.
- Preserve the same domain actions and terminology as phone layouts.

## First-run flow

1. Explain in one screen that data stays on this device by default and no login is required.
2. Ask for a business name. The application never creates one on the user's behalf — see D-015.
3. Ask for a first unit (`Unit Usaha`: branch, field, season). It is not optional and no `Umum` unit is invented; a
   book pair only exists once the user has named the scope it belongs to.
4. Offer page tours as a small card (Lihat / Nanti / Jangan tawarkan lagi), never started on their own (DEC-018).
5. Defer account, Drive, research consent, and optional templates until relevant.

No onboarding page may block offline use merely to obtain analytics, authentication, or marketing data.

## Import experience

Import reads only the Mankeu template (DEC-012). A file that is not the template is refused with the exact missing or renamed sheets and columns and a button to download the template.

The preview states what will be added to the selected unit: transactions with money in and out, plan lines, products, stock, the new categories that will be created, conflicts that block the commit, and a duplicate warning when the same file was imported before. After commit, a notice says what was added. **Batalkan impor** in the import history deletes what the batch added and keeps the batch as history.

## Empty, loading, and error states

- Empty states describe the first useful action and provide it directly.
- Local calculations normally use skeletons only when genuine work is pending; do not animate unnecessarily.
- Storage, import, restore, or migration failures preserve the previous safe state.
- Sync status, when sync is offered (DEC-013), distinguishes `tersimpan di perangkat`, `menunggu cadangan`, and `dicadangkan`; it must not conflate local save with cloud copy.

## Colour system

Every colour in the app is generated from one seed — `#2D6A4F`, the brand green the web prototype
uses as `palette.primary.main` and the app icon is drawn in — by the Material 3 algorithm
(Material Color Utilities, `SchemeTonalSpot`, 2021 spec, contrast level 0). Nothing is picked by
eye any more. The generator and the contrast audit are one script,
`docs/evidence/theme/generate_palette.py`; the tokens it prints are pasted into
`presentation/Theme.kt` verbatim.

This replaced a palette transcribed by hand from the web's MUI theme, then patched token by token
each time a pair failed a contrast check. The hand-patched version worked, but it had no rule: each
correction was local, so the next new component could fail again in a new way. A generated system
fixes the relationships instead of the values — every `on…` role is placed at a tone distance from
its container that guarantees contrast, so a component that uses the right pair is correct by
construction.

**What is generated:** the full Material 3 role set for both themes — primary, secondary, tertiary
and error, each with its `on`, container and `on…Container` partners; background and surface with
their `on` roles; `surfaceVariant`, `surfaceDim`, `surfaceBright`, all five surface-container tiers
from lowest to highest; `outline` and `outlineVariant`; `inverseSurface`, `inverseOnSurface`,
`inversePrimary`, `surfaceTint`, `scrim`; and the fixed roles.

**What is generated but not part of Material 3:** money has meanings the spec has no slot for.
`error` is for something that went wrong, and an expense is not an error — the app draws far more
expenses than errors. So income, expense and plan-mode carry their own roles, built from the web's
own green, coral and gold read at the standard Material tones (light 40/100/90/10, dark
80/20/30/90). Green and gold are harmonised toward the seed first; the expense coral is not.
Harmonising rotates a hue up to 15 degrees toward the target, and the coral sits at hue 33 against
the seed's 165, so it came out at hue 49 — an orange-brown that passed every contrast check and
still looked wrong on device: the two big buttons on Catat stopped reading as "green in, red out"
and started reading as "green and brown". For money, hue is the meaning, and the audit cannot see
that. The expense-breakdown chart's six
category hues are harmonised the same way, at tone 40 for light and 75 for dark; they used to be
six raw Material 2 hues that ignored the palette and stayed identical in dark mode, where they
glared.

## Measured colour contrast

96 pairs, all passing: 4.5:1 for anything carrying text (WCAG 1.4.3), 3:1 for control boundaries
and chart fills (1.4.11). Only pairs the app actually draws are measured — a generated system
passes trivially on combinations nobody renders, so the list is the real fills, the text on them,
the outlines of real controls, and the chart slices against the surface behind them.

Regenerate with `python3 docs/evidence/theme/generate_palette.py --audit`. Full table:

The table audits the generated system as the script produces it, which includes a filled
`warning`/`onWarning` pair. `Theme.kt` does not declare that pair, because nothing in the app draws
text on a filled warning surface — the Proyeksi chip and the notice card both use the container
pair. It is measured here so that the day something does need it, the value is known to be safe.

| Mode | Pair | Foreground | Background | Ratio | Needs | Result |
| --- | --- | --- | --- | --- | --- | --- |
| light | onPrimary / primary | `#FFFFFF` | `#226A4D` | 6.49:1 | 4.5:1 | PASS |
| light | onPrimaryContainer / primaryContainer | `#005237` | `#A9F2CC` | 7.19:1 | 4.5:1 | PASS |
| light | onSecondary / secondary | `#FFFFFF` | `#4D6357` | 6.49:1 | 4.5:1 | PASS |
| light | onSecondaryContainer / secondaryContainer | `#364B40` | `#CFE9D8` | 7.29:1 | 4.5:1 | PASS |
| light | onTertiary / tertiary | `#FFFFFF` | `#3D6473` | 6.42:1 | 4.5:1 | PASS |
| light | onTertiaryContainer / tertiaryContainer | `#244C5A` | `#C0E9FB` | 7.22:1 | 4.5:1 | PASS |
| light | onBackground / background | `#171D19` | `#F5FBF5` | 16.31:1 | 4.5:1 | PASS |
| light | onSurface / surface | `#171D19` | `#F5FBF5` | 16.31:1 | 4.5:1 | PASS |
| light | onSurfaceVariant / surfaceVariant | `#404943` | `#DCE5DD` | 7.24:1 | 4.5:1 | PASS |
| light | onSurface / surfaceContainerLowest | `#171D19` | `#FFFFFF` | 17.13:1 | 4.5:1 | PASS |
| light | onSurface / surfaceContainerLow | `#171D19` | `#F0F5EF` | 15.5:1 | 4.5:1 | PASS |
| light | onSurface / surfaceContainer | `#171D19` | `#EAEFE9` | 14.7:1 | 4.5:1 | PASS |
| light | onSurface / surfaceContainerHigh | `#171D19` | `#E4EAE4` | 14.02:1 | 4.5:1 | PASS |
| light | onSurface / surfaceContainerHighest | `#171D19` | `#DEE4DE` | 13.26:1 | 4.5:1 | PASS |
| light | onSurfaceVariant / surfaceContainerLow | `#404943` | `#F0F5EF` | 8.44:1 | 4.5:1 | PASS |
| light | onSurfaceVariant / surfaceContainerHighest | `#404943` | `#DEE4DE` | 7.22:1 | 4.5:1 | PASS |
| light | onError / error | `#FFFFFF` | `#BA1A1A` | 6.46:1 | 4.5:1 | PASS |
| light | onErrorContainer / errorContainer | `#93000A` | `#FFDAD6` | 7.24:1 | 4.5:1 | PASS |
| light | inverseOnSurface / inverseSurface | `#EDF2EC` | `#2C322E` | 11.55:1 | 4.5:1 | PASS |
| light | primary / surface | `#226A4D` | `#F5FBF5` | 6.18:1 | 4.5:1 | PASS |
| light | primary / surfaceContainerLow | `#226A4D` | `#F0F5EF` | 5.87:1 | 4.5:1 | PASS |
| light | outline / surface | `#707973` | `#F5FBF5` | 4.28:1 | 3.0:1 | PASS |
| light | outline / surfaceContainerLow | `#707973` | `#F0F5EF` | 4.07:1 | 3.0:1 | PASS |
| light | outline / background | `#707973` | `#F5FBF5` | 4.28:1 | 3.0:1 | PASS |
| light | onSuccess / success | `#FFFFFF` | `#006C49` | 6.48:1 | 4.5:1 | PASS |
| light | onSuccessContainer / successContainer | `#002113` | `#91F7C4` | 13.29:1 | 4.5:1 | PASS |
| light | success as text / surface | `#006C49` | `#F5FBF5` | 6.17:1 | 4.5:1 | PASS |
| light | success as text / surfaceContainerLow | `#006C49` | `#F0F5EF` | 5.87:1 | 4.5:1 | PASS |
| light | onWarning / warning | `#FFFFFF` | `#5C631A` | 6.45:1 | 4.5:1 | PASS |
| light | onWarningContainer / warningContainer | `#1A1D00` | `#E1E991` | 13.34:1 | 4.5:1 | PASS |
| light | warning as text / surface | `#5C631A` | `#F5FBF5` | 6.14:1 | 4.5:1 | PASS |
| light | warning as text / surfaceContainerLow | `#5C631A` | `#F0F5EF` | 5.84:1 | 4.5:1 | PASS |
| light | onDanger / danger | `#FFFFFF` | `#9A442D` | 6.48:1 | 4.5:1 | PASS |
| light | onDangerContainer / dangerContainer | `#3C0800` | `#FFDBD2` | 13.3:1 | 4.5:1 | PASS |
| light | danger as text / surface | `#9A442D` | `#F5FBF5` | 6.18:1 | 4.5:1 | PASS |
| light | danger as text / surfaceContainerLow | `#9A442D` | `#F0F5EF` | 5.87:1 | 4.5:1 | PASS |
| light | chart[0] / surface | `#006D39` | `#F5FBF5` | 6.16:1 | 3.0:1 | PASS |
| light | chart[1] / surface | `#954A00` | `#F5FBF5` | 6.14:1 | 3.0:1 | PASS |
| light | chart[2] / surface | `#00639A` | `#F5FBF5` | 6.15:1 | 3.0:1 | PASS |
| light | chart[3] / surface | `#6D43C6` | `#F5FBF5` | 6.12:1 | 3.0:1 | PASS |
| light | chart[4] / surface | `#006A67` | `#F5FBF5` | 6.14:1 | 3.0:1 | PASS |
| light | chart[5] / surface | `#BA192B` | `#F5FBF5` | 6.13:1 | 3.0:1 | PASS |
| dark | onPrimary / primary | `#003824` | `#8ED5B1` | 7.73:1 | 4.5:1 | PASS |
| dark | onPrimaryContainer / primaryContainer | `#A9F2CC` | `#005237` | 7.19:1 | 4.5:1 | PASS |
| dark | onSecondary / secondary | `#20352A` | `#B4CCBD` | 7.68:1 | 4.5:1 | PASS |
| dark | onSecondaryContainer / secondaryContainer | `#CFE9D8` | `#364B40` | 7.29:1 | 4.5:1 | PASS |
| dark | onTertiary / tertiary | `#063543` | `#A5CDDE` | 7.75:1 | 4.5:1 | PASS |
| dark | onTertiaryContainer / tertiaryContainer | `#C0E9FB` | `#244C5A` | 7.22:1 | 4.5:1 | PASS |
| dark | onBackground / background | `#DEE4DE` | `#0F1511` | 14.32:1 | 4.5:1 | PASS |
| dark | onSurface / surface | `#DEE4DE` | `#0F1511` | 14.32:1 | 4.5:1 | PASS |
| dark | onSurfaceVariant / surfaceVariant | `#C0C9C1` | `#404943` | 5.49:1 | 4.5:1 | PASS |
| dark | onSurface / surfaceContainerLowest | `#DEE4DE` | `#0A0F0C` | 14.96:1 | 4.5:1 | PASS |
| dark | onSurface / surfaceContainerLow | `#DEE4DE` | `#171D19` | 13.26:1 | 4.5:1 | PASS |
| dark | onSurface / surfaceContainer | `#DEE4DE` | `#1B211D` | 12.68:1 | 4.5:1 | PASS |
| dark | onSurface / surfaceContainerHigh | `#DEE4DE` | `#262B28` | 11.15:1 | 4.5:1 | PASS |
| dark | onSurface / surfaceContainerHighest | `#DEE4DE` | `#303632` | 9.57:1 | 4.5:1 | PASS |
| dark | onSurfaceVariant / surfaceContainerLow | `#C0C9C1` | `#171D19` | 10.08:1 | 4.5:1 | PASS |
| dark | onSurfaceVariant / surfaceContainerHighest | `#C0C9C1` | `#303632` | 7.28:1 | 4.5:1 | PASS |
| dark | onError / error | `#690005` | `#FFB4AB` | 7.72:1 | 4.5:1 | PASS |
| dark | onErrorContainer / errorContainer | `#FFDAD6` | `#93000A` | 7.24:1 | 4.5:1 | PASS |
| dark | inverseOnSurface / inverseSurface | `#2C322E` | `#DEE4DE` | 10.14:1 | 4.5:1 | PASS |
| dark | primary / surface | `#8ED5B1` | `#0F1511` | 10.83:1 | 4.5:1 | PASS |
| dark | primary / surfaceContainerLow | `#8ED5B1` | `#171D19` | 10.03:1 | 4.5:1 | PASS |
| dark | outline / surface | `#8A938C` | `#0F1511` | 5.84:1 | 3.0:1 | PASS |
| dark | outline / surfaceContainerLow | `#8A938C` | `#171D19` | 5.41:1 | 3.0:1 | PASS |
| dark | outline / background | `#8A938C` | `#0F1511` | 5.84:1 | 3.0:1 | PASS |
| dark | onSuccess / success | `#003824` | `#75DAA9` | 7.77:1 | 4.5:1 | PASS |
| dark | onSuccessContainer / successContainer | `#91F7C4` | `#005236` | 7.21:1 | 4.5:1 | PASS |
| dark | success as text / surface | `#75DAA9` | `#0F1511` | 10.88:1 | 4.5:1 | PASS |
| dark | success as text / surfaceContainerLow | `#75DAA9` | `#171D19` | 10.08:1 | 4.5:1 | PASS |
| dark | onWarning / warning | `#2F3300` | `#C5CD78` | 7.76:1 | 4.5:1 | PASS |
| dark | onWarningContainer / warningContainer | `#E1E991` | `#444B01` | 7.22:1 | 4.5:1 | PASS |
| dark | warning as text / surface | `#C5CD78` | `#0F1511` | 10.9:1 | 4.5:1 | PASS |
| dark | warning as text / surfaceContainerLow | `#C5CD78` | `#171D19` | 10.09:1 | 4.5:1 | PASS |
| dark | onDanger / danger | `#5D1805` | `#FFB4A1` | 7.69:1 | 4.5:1 | PASS |
| dark | onDangerContainer / dangerContainer | `#FFDBD2` | `#7C2E19` | 7.21:1 | 4.5:1 | PASS |
| dark | danger as text / surface | `#FFB4A1` | `#0F1511` | 10.83:1 | 4.5:1 | PASS |
| dark | danger as text / surfaceContainerLow | `#FFB4A1` | `#171D19` | 10.03:1 | 4.5:1 | PASS |
| dark | chart[0] / surface | `#67CD88` | `#0F1511` | 9.39:1 | 3.0:1 | PASS |
| dark | chart[1] / surface | `#FFA35F` | `#0F1511` | 9.38:1 | 3.0:1 | PASS |
| dark | chart[2] / surface | `#76BFFF` | `#0F1511` | 9.39:1 | 3.0:1 | PASS |
| dark | chart[3] / surface | `#C5ABFF` | `#0F1511` | 9.37:1 | 3.0:1 | PASS |
| dark | chart[4] / surface | `#66C8C4` | `#0F1511` | 9.35:1 | 3.0:1 | PASS |
| dark | chart[5] / surface | `#FF9F9C` | `#0F1511` | 9.41:1 | 3.0:1 | PASS |

Failures: 0

## Book-mode colour identity

Proyeksi and Realisasi must be distinguishable at a glance, not only during the transition between
them. An early attempt paired `secondaryContainer` with `successContainer`; in a green-seeded
palette those are two near-identical pale greens and the difference was invisible on device. The
shipped pairing is semantic: a plan is provisional and wears the warning container, a realisation is
confirmed and wears the success container. Each carries its own `on…Container` tone — 13.3:1 in
light, 7.2:1 in dark — so the distinction never costs legibility, and colour is not the only signal:
the segment label and the hero card's wording both change with it.

## Elevation and depth

Light mode separates layers with tone *and* a shadow; dark mode uses tone alone. A drop shadow is a
light-mode device — on a dark surface a black shadow either disappears or turns into a hard grey rim
— so the navigation bar and the scope header drop their `shadowElevation` when the dark scheme is in
effect and rely on `surfaceContainer` / `surfaceContainerLow` sitting above `surface`. The check is
`isDarkScheme()`, which reads the luminance of the scheme actually in effect rather than the stored
preference, so it stays correct mid-animation while the two schemes interpolate.

Content cards sit on `surfaceContainerLow`, bottom sheets on `surfaceContainerLow` with tonal
elevation off, and the navigation bar on `surfaceContainer`. Nothing draws a flat white or flat grey
any more.

## Accessibility baseline

- Meet current Android accessibility guidance and target WCAG 2.2 AA where applicable.
- Interactive targets are at least 48 dp and have meaningful accessibility labels.
- Color is never the only indicator of income/expense, due status, variance, or failure.
- Text supports Android font scaling without clipping critical amounts or actions.
- Currency and dates have readable spoken labels.
- Charts have text summaries and selectable data alternatives.
- Motion is non-essential and respects reduced-motion preferences.
- Screen-reader order follows task order; modal focus is contained and restored.
- Tablet flows are fully usable with keyboard and switch-style navigation where supported.

## Content style

- State the result first: `Impor dibatalkan: 84 catatan dihapus.`
- Name the exact problem: the sheet, the cell, and what was expected.
- Avoid blame, parser jargon, and false certainty.
- Use consistent Indonesian labels; English technical terms may appear only in advanced help.

## Touch targets, tooltips and haptics

These rules apply to every new screen. The reasoning is in D-084 to D-087.

### Touch targets

- Every interactive element is at least **48 × 48 dp**. Use the Material 3 defaults (`IconButton`, `FilterChip`, `NavigationBarItem`); never shrink them with `Modifier.size` below 48dp. Shrink the icon inside instead.
- A read-out is not a control. Status badges and metric cards have no `onClick` and no ripple. An `AssistChip` with `onClick = {}` is not allowed.
- Destructive triggers use the error colour and sit apart from the primary action. Primary actions are filled buttons, secondary ones tonal or text buttons.

### Tooltips

- Use `MankeuTooltip` (Material 3 `TooltipBox` + `PlainTooltip`), never a custom popup.
- Required on: icon-only actions (Ubah, Hapus, Data, Pengaturan, bantuan), status badges (feasibility, shared stock), and figures that can be ellipsized (metric cards).
- The tooltip text says what the element does or what a status is based on. It does not repeat a label that is already visible.

### Haptic taxonomy

| Event | `HapticFeedbackType` | When |
| --- | --- | --- |
| `TAP` | `SegmentTick` | Changing tab, segment or filter; picking a date; dismissing a sheet |
| `LONG_PRESS` | `LongPress` | A tooltip appears |
| `WARN` | `GestureThresholdActivate` | Starting delete or archive |
| `CONFIRM` | `Confirm` | A save, import commit, export or backup succeeded |
| `REJECT` | `Reject` | Form validation or a ledger rule refused the action |

- Fire feedback through `LocalFeedback.current(event)`, never `Vibrator` directly. It respects the phone's vibration settings and the in-app switch.
- No feedback on scrolling, typing, or re-selecting what is already selected.
- ViewModel messages built with `UiText.fromError` are refusals (`isError = true`) and get `REJECT`.

### Forms and the keyboard

- A validation error belongs on the field at fault (`isError` + `supportingText`), and that field takes focus. A line above the submit button is reserved for errors with no single field.
- Text fields set an IME action: `Next` inside a form, `Done` on the last field.
- Money fields use `KeyboardType.Number`. Rupiah is stored as whole integers, so there is no decimal key.
- A sheet containing inputs applies `imePadding()` before `verticalScroll`, so the focused field scrolls above the keyboard.
- Money is never ellipsized. A figure in a fixed-width card uses `autoSize = TextAutoSize.StepBased(...)` with `maxLines = 1`. Check every new money read-out at 130% font scale.

## UX acceptance evidence

A screen is not accepted from a mockup alone. Evidence includes task-based testing on representative phone and tablet sizes, TalkBack checks, font scaling, keyboard navigation for tablet flows, offline behavior, and user comprehension of one import preview, one refused non-template file, and one Batalkan impor.
