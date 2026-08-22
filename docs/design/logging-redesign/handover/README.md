# Handoff: GoFlo logging & category system redesign

## Overview
A ground-up rethink of GoFlo's per-day logging screens and the "What You Track" category system. It replaces the current slider-driven, one-shape-per-input log screens with a single, consistent surface language, and introduces a **group / role** model that unifies category colour, the Settings theme picker, and category organisation. It also adds user-authored categories with icons, multiple input types, timelines, and alarms.

## About the design files
The file in this bundle — `Log Screens Rethought.html` — is a **design reference created in HTML**. It is a pannable canvas of ~22 phone mockups plus annotation cards, not production code to copy. The task is to **recreate these designs in the existing GoFlo Android app** (Kotlin + Jetpack Compose + Material 3) using its established patterns — `Color.kt`, the `AppTheme` enum, `colorSchemeFor()`, DataStore persistence, Compose navigation. Do **not** port HTML/CSS; translate intent into Compose.

`GoFlo Theme Redesign.md` is the companion spec for the 12 colour palettes and is the source of truth for token hex values.

## Fidelity
**High-fidelity.** Colours, type scale, spacing, corner radii and interactions are final-intent. Recreate pixel-close using Compose Material 3 components and the app's colour scheme. Where the mock hardcodes a hex (e.g. `#1265AF`), that value stands in for a **theme role** — bind to `MaterialTheme.colorScheme.*`, never a literal.

---

## The core model change (read this first)
Everything visual rests on one idea: **a category stores a colour *role*, never a hex.**

- **Group**: `{ id, name, colorRole, defaultInputType }`. A group owns a role and a default.
- **Category**: gains `groupId` (**nullable**), `colorRole` (nullable — inherits group's when null), `icon`, `inputType`, `inputConfig`, `allowMultiplePerDay: Boolean`, `logWithPeriod: Boolean`.
- **colorRole** is an enum referencing the theme's tonal roles: `PRIMARY, SECONDARY, TERTIARY, QUATERNARY, QUINARY, SENARY`, plus `FIXED(argb)` for a deliberate off-theme colour.
  - The first three map to existing M3 roles. **Quaternary/quinary/senary must be derived** from the active scheme (generate 3 extra harmonious tonal roles per theme, or extend each palette in `Color.kt`). They re-theme with everything else.
  - `FIXED` stores a raw ARGB and is intentionally exempt from theme changes.
- **Migration is additive.** Add nullable columns; existing categories default to `groupId = null`, `colorRole = null` → render neutral (`surfaceVariant`) and keep working. Offer a one-time optional "Organise your categories" prompt. **No destructive rewrite.**

Colour semantics in the mock: blue = body/cycle facts, amber = subjective feeling, indigo = environment. These are group choices, not hardcoded — the user assigns them.

---

## Screens / views
The canvas is organised in rows. Left→right, top→bottom:

### Row 1 — Log Period, rethought
- **As-built (reference of current state):** every input a different shape, giant blue numeral, two-tone drag track that reads like a progress bar, mauve slabs.
- **Rethought:** one surface language — **facts in a list card, judgements on a tap-scale, states on chips.**
  - **Dates** = one white list card, two rows (Started / Ended), tabular values, chevrons. Not two outlined pills.
  - **Flow** = 4 discrete **rising tap-steps** (Spot/Light/Med/Heavy), selected step filled `primary`, current value shown as a word in the section header. **No slider, no numeral.**
  - **Symptoms** = chips; selected = filled `primaryContainer` + check; unselected = hairline outline.
  - **Rage** = 6 warm (`secondary`) tap-steps, Calm→Volcanic end labels.
  - Sticky **Save log** bar (`primary`, 52px, radius 26).
- **Single-metric log:** the metric *is* the page — tonal `primaryContainer` hero holds the reading as words ("Barely noticeable"), 5-step scale, notes fill former dead space, a small 7-day sparkline.

### Row 2 — One log screen, two states
- **Off-period daily log:** mood leads in an **amber hero** ("How did today feel"); flow not rendered; symptoms, tracked metrics, notes below; a hairline "Period started today" footer row.
- **On-period:** a blue **Flow** group slots in at the top, mood hero compresses to one row, footer becomes a filled `primaryContainer` status row ("Period ongoing · since Aug 6 · End"). **Everything between is byte-for-byte identical** — one screen, period is a state of the day, not a separate destination.

### Row 3 — Groups tie it together
- **Settings → Categories & groups:** theme swatch row on top; each group is a row with a colour dot + role label. Switching theme recolours every group.
- **Grouped multi-metric card:** Environment (Weather/Rainfall/Dampness) = **one card of rows**, not three separate logs. A group of one renders as the single-metric page.
- **Header switcher:** the screen title is a button → a sheet of categories **organised by group, tinted by role**. Switching re-files the entry; the value already entered is preserved.

### Row 4 — What You Track (management home)
- **Grouped / Ungrouped** segmented toggle.
- **Grouped:** each group a card listing its categories, inline "+ Add category to this group", and a "+ New group" pill at the bottom.
- **Ungrouped:** loose categories in neutral `surfaceVariant`, each with "Add to group ›"; "+ New category" CTA.
- **Add to group** sheet: pick an existing group (adopts its role) or "New group…" pre-filled. Past entries preserved.

### Row 5 — Category creation & input types
- **New category (step 1 of 2):** name, **icon** picker, colour (role chips), **input type** (Scale / Slider / Count / Yes-no / Time), **Allow multiple per day** toggle, **Log with period** toggle. Footer "Next ›". Step 1 stays short.
- **Timeline:** with *allow multiple* on, the day becomes a **list of timestamped entries** (08:10 · 13:40 · 19:05), each an appended reading with its time. Log screen swaps to this component automatically.
- **Input types render differently:** Slider = genuine continuous value (72.4 kg); Count = −/+ stepper; Yes/no = two-button segmented; Time = time field. Scale keeps the stepped bars. ("Kill the slider" applied only to ratings.)

### Row 6 — Scale config, alarms, colour
- **Step 2 — Scale settings:** custom **min/max range**, per-step **word labels**, **Allow decimals** toggle. Only shown when type = Scale.
- **Category edit — alarms:** reminder times with day rules (Every day / Weekdays), on/off switches; link back into scale settings; delete-with-history danger row. Alarms appear on **edit**, not first creation.
- **Colour — extended theme roles:** primary/secondary/tertiary + 3 auto-derived in-theme roles (quaternary/quinary/senary), all re-theme; **Fixed colour** track for a deliberately clashing pick, treated as "don't change on theme switch".

---

## Interactions & behaviour
- **Tap-steps:** single tap selects; the bar fills with the scale's role colour and its caption highlights. No drag. Steps rise in height left→right to encode magnitude.
- **Chips:** toggle selected/unselected; selected fills `primaryContainer` and gains a leading check.
- **Header title:** tappable → category-switch bottom sheet. Switching changes the entry's `categoryId`; the entered value is retained.
- **Grouped/Ungrouped toggle:** M3 segmented; view swaps in place.
- **Save bar:** sticky to the bottom, gradient fade over the scroll content.
- **Multiple-per-day:** primary action appends a timestamped entry; existing entries listed with an overflow (edit/delete) per row.
- **Alarms:** local notifications; per-alarm enable switch.

## State / data
- Add nullable columns to Category (`groupId`, `colorRole`, `icon`, `inputType`, `inputConfig` JSON, `allowMultiplePerDay`, `logWithPeriod`). New `Group` table.
- `LogEntry`: for single-value categories, one row/day; for `allowMultiplePerDay`, many rows/day each with a timestamp.
- Period is derived state (is a period active on date D?) driving whether Flow renders and the footer variant — not a per-category screen.
- Theme selection already persists to DataStore (`AppTheme` enum); extend `colorSchemeFor()` to expose the 3 extra derived roles.

## Design tokens (Beach Vibes light — see GoFlo Theme Redesign.md for all 12)
- `primary #1265AF` · `onPrimary #FFFFFF` · `primaryContainer #D5E3FF` · `onPrimaryContainer #001C3D`
- `secondary #9A6800` · `secondaryContainer #FFE08D` · `onSecondaryContainer #251A00`
- `tertiary #4B5BAC` (indigo, used for Environment group)
- `surface / background #F8FAFE` · `onSurface #181C22` · `surfaceVariant #E1E3EE` · `onSurfaceVariant #444751` · `outline #757782`
- Radii: cards 16–20, chips/pills full (19–26), phone frame 26. Save bar height 52.
- Type: Comfortaa (display / screen titles), Inter/system for body. Section labels 11px 700 uppercase +0.11em tracking. Values 14–15px 600.
- Elevation: cards `0 0 0 1px rgba(20,30,50,.06)` hairline; sheets add a soft drop shadow.

## Design rules & guidelines
Apply these consistently — they are what make the screens feel like one system rather than 22 layouts.

### Typography
- **Two families only.** Comfortaa (700) for screen titles and `ToneHero` words; Inter/system for everything else. Never introduce a third.
- **Scale (sp):** screen title 21 · hero word 22–24 · section value 14–15 (600) · body/list value 13.5–14.5 · **section label 11 (700, UPPERCASE, +0.11em tracking)** · step caption / helper 11–11.5 · subtitle under title 11.5.
- **Floor: nothing below 11sp**, and 11 only for non-essential captions. Body content is 13.5sp minimum. Respect the user's font-scale setting — use `sp`, never `dp`, for text.
- Weights: 400 body, 600 values/labels/emphasis, 700 titles + uppercase labels. Don't use 500.
- `letter-spacing` only on the uppercase labels (+0.11em) and slightly negative on titles (−0.02em). Body stays default.

### Spacing, sizes, hit targets
- **Base unit 4dp.** Common gaps: 8 (chips, tight rows), 10–12 (within a card), 16 (screen horizontal padding), 20 (between groups).
- **Minimum touch target 48×48dp** — tap-steps, chips, counter buttons, switches, list rows all meet it even when the visible bar is shorter (pad the hit area).
- Corner radii: cards 16–20 · pills/chips full (height/2) · phone frame 26 · save bar 26 · icon tiles 12.
- Save bar 52dp, sticky, with a gradient fade of the surface colour over scrolling content.
- Elevation: cards use a **hairline** `1px surfaceVariant/6%` outline, not a drop shadow; only sheets/menus get a soft shadow. Keep the UI flat.
- List rows ≥ 52dp; divider is a 1px hairline at ~7% onSurface, never a full-contrast line.

### Colour usage
- **Bind to roles, never hex.** `MaterialTheme.colorScheme.*` or a resolved `colorRole`. The hexes in the mock are role stand-ins.
- One role per meaning: a scale, its selected step, its header value, and its icon all share the category's role. Don't mix roles within one control.
- Selected = **tonal fill** (`primaryContainer`/role container), unselected = hairline outline. Never rely on outline-only to show selection.
- Reserve warm `secondary` for subjective/feeling categories, cool roles for facts — but this is the *user's* group choice, not hardcoded.

### When to use small explanatory text (and when not)
- **Use it** for: an end-scale anchor pair (Calm ↔ Volcanic), a one-line consequence the user can't infer ("A fixed colour won't change with the theme", "Sleep keeps all its past entries"), and optional-field hints ("optional words").
- **Don't** use it to restate a label ("Flow — set your flow"), to fill empty space, or on every field. If a control is self-evident, no helper. Aim for **at most one helper line per group**, and prefer showing the current value in the section header over explaining the control.
- Helper text is 11.5sp, `onSurfaceVariant`, never coloured unless it's a warning (`secondary`/amber on a tonal chip).

### Accessibility
- **Contrast:** all text ≥ 4.5:1, large text/UI ≥ 3:1. The 12 palettes were tuned to pass — run `wcag_check.py` (see theme spec) after any colour change.
- **Never colour-only.** Selected states carry a check (chips) or fill + label emphasis (steps); the timeline shows the time, not just a coloured dot. A colour-blind or greyscale user must still parse every screen.
- **Touch:** 48dp minimum, 8dp minimum between independent targets.
- **Content descriptions** on icon-only controls (icon picker, counter ±, alarm toggle, header switcher caret).
- **Dynamic type:** layouts must reflow at the OS's largest font scale without clipping — test tap-steps and chips at 200%.
- **Motion:** step/chip selection is an instant tonal change or a ≤150ms fade; honour reduce-motion. No essential info conveyed by animation alone.
- **Focus & TalkBack:** logical traversal order top→bottom; a scale exposes as a single control reporting "Flow, Medium, 3 of 4", not four unlabelled buttons.

## Files
- `Log Screens Rethought.html` — the full design canvas (open in a browser; pan/zoom).
- `GoFlo Theme Redesign.md` — the 12-palette colour spec and QA checklist.
- `screens/01–06-row.png` — rendered reference of each row (Log Period → colour/alarms), including the annotation cards.

## Reusable component library (build once, reuse everywhere)
**This is a hard requirement: implement the design as a small set of stateless, parameterised Compose components — not per-screen one-offs.** Every screen in the mock is assembled from the same ~10 primitives. Reinventing any of these per screen is a defect. Put them in a shared `ui/components/` module and drive them entirely by parameters + a `colorRole`, so a category's role colour flows in from one place.

Suggested primitives and their APIs (names indicative):
- `SectionHeader(label: String, value: String? = null, valueColor: Color = role)` — the uppercase label + right-aligned current value used above every group.
- `ListCard { rows }` + `ListRow(key, value, valueEmphasis, trailing = Chevron)` — the white rounded card with hairline-divided rows. Powers Dates, Tracking, alarms, step-labels, add-to-group.
- `StepScale(range, labels: List<String>?, value, role, onSelect)` — the discrete rising tap-steps. **One component** serves flow, rage, severity, day-overall; range + labels + role are the only differences. No bespoke scales.
- `ChipToggle(text, selected, onToggle)` + `ChipRow` — filled `primaryContainer`+check when on, hairline when off.
- `ToneHero(word, caption, role, content)` — the tonal container that makes one metric the page (blue or amber by role).
- `SegmentedToggle(options, selected)` — Grouped/Ungrouped and Yes/No both use it.
- `RolePicker(roles, selected, onPick)` and `IconPicker` — reused in create + edit.
- `SwitchRow(title, subtitle, checked)` — allow-multiple, log-with-period, allow-decimals, alarm enable all share it.
- `Timeline(entries, onAppend)` + `TimelineEntry(time, value, sub)` — the multiple-per-day list.
- `PrimarySaveBar(label)` — the sticky bottom action.
- Input controls behind one `MetricInput(type, config, value, onChange)` façade that switches on `inputType` → `StepScale` / `ContinuousSlider` / `Counter` / `SegmentedToggle` / `TimeField`. **The log screen never branches on type itself** — it renders `MetricInput` and this façade picks the control.

Because colour is a **role**, each primitive takes a `role` (or reads the category's) and resolves it through `colorSchemeFor()` — so theme switches and the quaternary/quinary/senary roles need zero per-component work. A single `LogScreen(category, date)` composes these primitives; there is no separate "Log Period" vs "Log X" screen.

## Target codebase notes
Kotlin + Jetpack Compose + Material 3. Bind every colour to `MaterialTheme.colorScheme.*` or a derived role — never a literal hex from the mock. Reuse existing navigation, DataStore, and `AppTheme` machinery. Don't rename `AppTheme` entries (persisted) or change `colorSchemeFor()`'s signature; extend it.
