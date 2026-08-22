# Phase 3 — Reusable component library

**Goal:** build the ~12 stateless, parameterised Compose primitives every later screen is assembled from. With `@Preview`s. No screen consumes them yet (previews only). This is a **hard requirement** from the handover — per-screen one-offs are a defect.

**Prerequisites:** Phase 1 (roles). Phase 2 not strictly required but helpful.
**Read first:** `handover/README.md` §"Reusable component library" (the API list); `handover/screens/01-06-row.png`; `PLAN.md` §1 (a11y rules), §2 (the four design rules).

---

## Where

All in `ui/components/` (new files, one per primitive or a small cluster). Each is stateless: state in, callbacks out. Each accents from a `role: Color` param (plus its on-colour) so a category's role flows in from one place — never reads `MaterialTheme.colorScheme.primary` directly for accent.

## The primitives (names indicative; match the handover)

| Component | API sketch | Notes / a11y |
|---|---|---|
| `SectionHeader` | `(label: String, value: String? = null, valueColor: Color = role)` | Uppercase label 11sp/700/+0.11em; value right-aligned. Above every group. |
| `ListCard` + `ListRow` | `ListCard { rows }`; `ListRow(key, value, valueEmphasis, trailing = Chevron, onClick)` | White rounded card, hairline dividers (1px ~7% onSurface). Powers Dates, Tracking, alarms, step-labels, add-to-group. Row ≥52dp. `Role.Button` when clickable. |
| `StepScale` | `(range: IntRange, labels: List<String>?, value: Int?, role: Color, onColor: Color, onSelect: (Int) -> Unit, endLabels: Pair<String,String>? = null)` | Discrete rising tap-steps; selected step filled `role`, caption highlighted. No drag. **Expose as ONE control**: `clearAndSetSemantics { role = Role.RadioButton; contentDescription = "<name>, <selectedWord>, <n> of <total>" }`. Rising heights encode magnitude; pad hit area to 48dp. Serves flow, rage, severity, day-overall. |
| `ChipToggle` + `ChipRow` | `ChipToggle(text, selected, role, onToggle)`; `ChipRow(...)` | Selected = filled role container + leading check; unselected = hairline. `Role.Checkbox`/`FilterChip` semantics. Reuse/replace `SelectableChip`. Never colour-only (check icon). |
| `ToneHero` | `(word: String, caption: String?, role: Color, onColor: Color, content: @Composable () -> Unit)` | Tonal container that makes one metric the page (blue primary or amber secondary by role). Word in **Comfortaa** (apply `ComfortaaFamily` explicitly — see map 03 §3). |
| `SegmentedToggle` | `(options: List<String>, selected: Int, onSelect: (Int) -> Unit)` | M3 segmented buttons. Powers Grouped/Ungrouped and Yes/No. `Role.RadioButton` per segment. |
| `RolePicker` | `(roles: List<CategoryColor>, selected, onPick)` + a Fixed track | Extract from Phase 1's inline picker. `Role.RadioButton`, ≥48dp, ring/check not colour-only. |
| `IconPicker` | `(icons: List<CategoryIcon>, selected, onPick)` | Grid; `Role.RadioButton`; `contentDescription` = icon displayName. |
| `SwitchRow` | `(title, subtitle: String?, checked, onCheckedChange)` | allow-multiple, log-with-period, allow-decimals, alarm-enable. `Role.Switch` + `stateDescription`. Set inner `Switch onClick = null` if the row handles the click (avoid double-focus, per CLAUDE.md a11y). |
| `Timeline` + `TimelineEntry` | `Timeline(entries, onAppend)`; `TimelineEntry(time, value, sub, onOverflow)` | Multiple-per-day list: `08:10 · Level 2 · mild`. Shows the **time**, not just a coloured dot (colour-blind safe). Per-row overflow (edit/delete). |
| `PrimarySaveBar` | `(label: String, onClick)` | Sticky bottom, 52dp, radius 26, `role`/primary. Gradient fade of surface over scrolling content. Icon-less; if icon-only variant, add `contentDescription`. |
| `MetricInput` (façade) | `(type: CategoryType, config: MetricConfig, value: MetricValue, onChange)` | **Stub the switch here** (returns each control) but full behaviour lands in Phase 4. Define `MetricConfig`/`MetricValue` value types now. |

## Typography & spacing (apply consistently — this is what makes it one system)
- Two families only: **Comfortaa (700)** for screen titles + `ToneHero` words (apply `ComfortaaFamily` from `Type.kt` explicitly — it is not wired into `GoFloTypography`); **system/Inter** for everything else.
- Scale (sp): title 21 · hero word 22-24 · section value 14-15 (600) · body 13.5-14.5 · section label 11 (700 UPPERCASE +0.11em) · caption 11-11.5. Floor 11sp; body ≥13.5sp. **Use `sp`, never `dp`, for text.**
- Radii: cards 16-20 · pills/chips full · save bar 26 · icon tiles 12. Base unit 4dp; gaps 8/10-12/16/20.
- Elevation: cards use a **hairline** `1px surfaceVariant/6%` outline, not a drop shadow. Only sheets get a soft shadow.

## Acceptance criteria
- [ ] Every primitive has an `@Preview` (light + dark) rendering correctly.
- [ ] Previews at **200% font scale** reflow without clipping (test `StepScale` and `ChipRow` especially — handover requirement).
- [ ] `a11y_check.py` green: every `.clickable{}` carries a `Role`; `StepScale` exposes as one control; `SwitchRow` announces state.
- [ ] No primitive reads a hardcoded accent — accent always comes from `role`.
- [ ] Motion: selection is instant or ≤150ms fade; honour reduce-motion.

## Feature-preservation checklist
- [ ] Nothing existing is removed or rewired. `SelectableChip` may stay until Phase 4/5 migrate callers; do not delete it here.
- [ ] Existing screens compile unchanged.

## Gotchas
- `ComfortaaFamily` is a downloadable Google Font — first render may fall back until fetched; provide a sane fallback stack.
- LESSONS.md has directly-relevant entries: inline links via `LinkAnnotation`, `liveRegion` for status text, icon-only controls need `contentDescription` on the container, `ModalBottomSheetProperties` needs all three params in this Material3 version. Read the "Android / Compose" section before building sheets/links.
- Keep components **stateless** — no `remember`ed domain state inside; hoist it. This is what lets Phase 5 compose them freely.

## Changelog fragment
Component-library-only PRs have no direct user-facing change, but they touch `.kt`, so a fragment is still required by CI. Use:
```json
{ "bump": "patch", "changed": ["Internal: shared UI component library for the logging redesign (no user-facing change yet)"] }
```
> If the project prefers not to surface internal notes in "What's New", coordinate with the owner; CI only requires a valid fragment, and the release dialog shows the 5 most recent entries.
