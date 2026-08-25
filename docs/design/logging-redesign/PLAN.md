# Logging & category-system redesign — phased implementation plan

**Status:** Phase 0 complete (planning + groundwork committed). Phases 1-8 not started.
**Owner handoff doc.** This is written so a *fresh* session can pick up any phase without re-deriving the codebase. Read this file top-to-bottom, then the one subsystem map relevant to your phase, then start.

---

## 0. How to use this document (read first)

1. **Source of truth for the design** is `handover/` in this folder: `README.md` (intent + component-library requirement), `GoFlo Theme Redesign.md` (palette hexes — already applied, see note below), `Log Screens Rethought.html` (pannable mockup canvas), and `screens/01-06-row.png` (rendered reference of each row). Open the PNGs to see the target.
2. **Codebase groundwork** is in `subsystem-maps/` — three stamped maps (logging screens, data model, theme/colour). Each carries the commit + build it was verified against and a staleness check. Trust them, but run the staleness check first if HEAD has moved.
3. **Pick up a phase** from §6. Each phase is self-contained, shippable on its own, and lists its files, data changes, acceptance criteria, and a feature-preservation checklist.
4. **When you finish a phase**, update §7 (Progress log) in the same PR: mark the phase done, record the commit, DB version, and any deviations from this plan. Keep the maps honest — if you changed a file a map describes, update the map or note the drift.
5. **Follow the golden rules in §1 on every phase.** They are what prevent feature loss and CI failures.

---

## 1. Golden rules (non-negotiable, every phase)

- **Never lose a feature.** The current app is feature-rich (§3). The mock shows an *end state*; it is not a licence to delete. Additive-first: build the new path alongside the old, prove parity, *then* remove. The removal phase (Phase 8) is the only place old screens get deleted, and only against the parity checklist.
- **Migrations are additive and versioned.** DB is currently version 23 (`GoFloDatabase.kt`). Every schema change adds a numbered migration (`N → N+1`), never edits an existing one, never uses `fallbackToDestructiveMigration`. Existing user data must survive untouched.
- **Colour is a role, never a literal.** Bind to `MaterialTheme.colorScheme.*` or a resolved category role token. The hexes in the mock are role stand-ins. The one exception is the `FIXED` role, which is a deliberate raw hex (already supported by `colorToken`'s hex path).
- **Every PR that touches app code adds a changelog fragment** at `changelog/unreleased/<slug>.json` (see CLAUDE.md). Do not edit `CHANGELOG.md` or `build.gradle.kts` versions directly.
- **Accessibility is enforced by CI.** `a11y_check.py` requires a `Role.<Type>` on every `.clickable{}`/`.combinedClickable{}`. Colour is never the only signal (chips carry a check, steps carry fill + label, timeline carries the time). Min tap target 44x44dp (design says pad to 48). Run `python3 a11y_check.py` before pushing.
- **WCAG is enforced.** Run `python3 wcag_check.py` after any colour change. Derived roles (quaternary/quinary/senary) are not in the palette tables — add an explicit spot-check for them.
- **No en dashes or em dashes in user-facing text** (CLAUDE.md). Hyphens in compounds are fine. This applies to strings the user sees, not this planning doc.
- **Privacy invariants hold.** No network, no new permissions without discussion, health data stays on device, PIN-locked widgets show a neutral placeholder. Medical disclaimer / privacy surfaces use the body font, never Comfortaa.
- **This environment cannot build** (no Android SDK/Gradle here). CI is the build check. Verify by inspection + the python checks; do not add "couldn't compile" disclaimers.

---

## 2. What this redesign is

A ground-up rethink of the per-day logging screens and the "What You Track" category system, resting on one idea: **a category stores a colour *role*, and belongs to an optional *group*; the group owns the role and a default input type.** From that follow: one unified log screen (period is a *state of the day*, not a separate destination), a small reusable component library that every screen is assembled from, and richer categories (icons already exist; add Yes/No + Time input types, generalised timelines, and per-category alarms surfaced on edit).

The design's four load-bearing rules:
1. **Kill the slider for ratings.** A 1-5 rating is discrete rising tap-steps, not a drag track. Genuine continuous measures (weight, temp) keep a real slider.
2. **Words over numbers.** "Medium", "Still ongoing", "Barely noticeable". Numbers survive only as small step captions where a scale is genuinely numeric.
3. **One card per idea, not per field.** Dates = one list card with rows. Section titles = small uppercase labels with the current value on the right.
4. **Selected state is tonal, not outlined.** Chips fill with a container colour + check; unselected stay hairline.

---

## 3. Feature-preservation inventory (the "do not lose these" list)

Every item below exists today and must keep working through every phase. Tick against this list in each phase's preservation checklist.

**Logging**
- Log a generic category value: chip multi-select (`default`), stepped slider (`numeric_slider`), free numeric input with unit (`numeric_free`), count stepper (`increment`), timed increment with per-tap timestamp + timeline (`increment` + `trackAgainstTime`).
- "Previously recorded (removed from options)" chips for stored labels no longer in the catalog.
- Notes per log (500-char cap). Edit an existing log. Delete a log. Date selection when `canEditDate`.
- Log a period day: start/end dates, ongoing (null end), flow (slider *or* chips with the 1→Spotting/2→Light/4→Heavy/else Medium mapping), symptoms (chips + inline "Add" via `AddSymptomDialog` that writes to the value catalog), pinned categories (`showInLogPeriod`), notes.
- Period episode/continuation logic: gap tolerance from prefs, episode day number, range vs open-ended, "Remove this day", "Delete Entire Period", "Disable period logging".
- Unsaved-changes discard/save guard on the period screen.
- **Period save fans out to the tracking system** (`syncFlowToTrackingLog`, `syncSymptomsToTrackingLog`, `syncPinnedCategoryLogs`) so period data shows in Stats/Flow/Symptoms. Preserve this on the unified screen.
- Side effects on save: widget refresh (`GoFloWidget.updateAllWidgets`), reminder refresh (`ReminderScheduler.refreshPredictionReminders`).

**Categories & management**
- Create / rename / archive / unarchive / delete (system categories protected) / reorder categories.
- Per-category icon (20-icon catalogue), colour token (primary/secondary/tertiary/hex), input type, numeric range/unit/decimals, scale labels, allow-multiple, show-in-log-period, track-against-time, mode key.
- Value catalog CRUD (`TrackingValue` rows) incl. rename-with-history (`bulkRenameLogValues`) and seeded-value protection.
- System categories: Flow, Symptoms (seeded), Ovulation Test (mode-key preset).
- Tracking modes (`modeKey` presets in `ui/util/TrackingModes.kt` — suggested categories per mode).
- Quick-log config + Quick Log widget (`widget/QuickLogWidget.kt`).
- `ManageCategoryValues` screen (value editing).

**Theme & colour**
- 12 named palettes x light/dark/system, HIGH_CONTRAST x2, BLUE_ORANGE, CUSTOM (user hues/argb + light/dark background), WCAG AAA variants for all 12. `AppTheme` names are DataStore keys — never rename.
- `ColorProfile` saved custom-palette slots.
- Category bubbles re-theme with the palette; calendar period dot = primary, ovulation = tertiaryContainer.

**Other surfaces that read this data** (regression-test these when the model changes): Stats (`StatsViewModel`, `ChartDataComputer`), History, Home, `DayLogSheet`, Dashboard, data export (`data/export`), custom alarms (`CustomAlarm` linked to categories).

---

## 4. Current state vs. the mock — gap analysis

The handover's "as-built" understates the current app. Much of the "new" model **already exists**; treat this redesign as *unifying and re-skinning* more than *building from zero*.

| Design concept | Already in the code | Genuinely new work |
|---|---|---|
| Per-category **icon** | `iconName` + `CategoryIcon` (20 icons) | Icon picker in the new create/edit flow (component reuse) |
| Colour as a **role** | `colorToken` = primary/secondary/tertiary **or** hex | **quaternary/quinary/senary** derived roles; `FIXED` = existing hex path |
| **Input type** | `categoryType`: default / numeric_slider / numeric_free / increment | **yes_no** and **time** types; a `MetricInput` façade |
| **Allow multiple / timeline** | `allowMultiple` + `trackAgainstTime` + `loggedAt` + `TimedIncrementSection` | Generalise timeline to all input types via one `Timeline` component |
| **Log with period** | `showInLogPeriod` pin | Reframe: the flag pins the category into the flow context on the unified screen |
| **Groups** | none | **`Group` entity + `groupId` on category** (the core new model) |
| **Unified log screen** | two separate screens (LogPeriod / LogCategory) | **one `LogScreen(date)`**; period becomes a day-state; header switcher to re-file |
| **What You Track home** | `ManageCategoriesScreen` (flat list) | Grouped/Ungrouped segmented view, group cards, add-to-group sheet |
| **Component library** | ad-hoc per-screen composables, duplicated input rendering | **~12 stateless primitives** in `ui/components/` (hard requirement) |
| **Alarms on category edit** | `CustomAlarm` system + `EditAlarm` screen exist | Surface reminders inside the category-edit flow |

> **Note on the theme redesign (`GoFlo Theme Redesign.md`):** the palettes were largely applied in a prior change (see the `// Redesigned 2026-05` header in `Color.kt`), but some values in the code differ from the spec (e.g. Coral tertiary is a rose-magenta in code vs gold in the spec). Reconciling `Color.kt` exactly to that spec is a **separate, bounded task** and is intentionally *out of scope* for this logging plan. If desired, do it as its own PR with `wcag_check.py` as the gate. Do not fold it into a logging phase.

---

## 5. Target architecture

**Data model additions (additive):**
- New `Group` entity: `{ id, name, colorRole: String, defaultInputType: String, displayOrder: Int }`. New `GroupDao`, repository methods, and a migration adding the table.
- `TrackingCategory` gains `groupId: Long?` (nullable). No other column is required — `colorToken` already carries the role/hex, `categoryType` already carries the input type, `allowMultiple`/`showInLogPeriod`/`trackAgainstTime` already exist.
- **Colour inheritance rule (deviation from handover — see §6 Phase 2):** a category resolves its colour from its own `colorToken`; if that is the sentinel `"inherit"` and it has a `groupId`, it uses the group's `colorRole`. Existing categories keep their current `colorToken`, so **they do not turn grey** (the handover's "default to neutral surfaceVariant" would visually wipe existing category colours — we preserve them instead; only categories explicitly set to inherit-with-no-group render neutral).

**Colour roles:** add `quaternary`/`quinary`/`senary` token strings resolved in `CategoryAppearance.kt` by deriving from the active `ColorScheme` (HSL, mirroring `buildCustomColorScheme`), provided via a `CompositionLocal` set up in `Theme.kt` so derivation happens once per theme. `FIXED` reuses the hex path.

**Component library** (`ui/components/`, all stateless, parameterised, driven by a `role: Color`): `SectionHeader`, `ListCard`/`ListRow`, `StepScale`, `ChipToggle`/`ChipRow`, `ToneHero`, `SegmentedToggle`, `RolePicker`, `IconPicker`, `SwitchRow`, `Timeline`/`TimelineEntry`, `PrimarySaveBar`, and the `MetricInput(type, config, value, onChange)` façade that switches on input type. The log screen never branches on type itself — it renders `MetricInput`.

**Unified log screen:** one `LogScreen(date)` composing the primitives. Off-period → mood hero leads (amber/secondary), flow not rendered, footer = quiet "Period started today" row. On-period → flow group slots in at top (blue/primary), mood compresses to a row, footer = filled status row ("Period ongoing · since <date> · End"). Everything between is the same component in the same order. Title is a button → category-switch sheet (re-files the entry, preserves the entered value).

---

## 6. The phases

Each phase is a shippable PR. Order is deliberate: additive foundations first (roles, model, components), then the façade, then the big screen unification, then management, then creation/edit, then removal. **A later phase never starts by deleting an earlier path.**

> **Each phase has a detailed build guide** in [`phases/`](phases/) with exact files, current line refs, code sketches, migration details, and acceptance + preservation checklists. Read the guide for your phase before starting; the summaries below are the index.
>
> - Phase 1 → [`phases/phase-1-color-roles.md`](phases/phase-1-color-roles.md)
> - Phase 2 → [`phases/phase-2-group-model.md`](phases/phase-2-group-model.md)
> - Phase 3 → [`phases/phase-3-component-library.md`](phases/phase-3-component-library.md)
> - Phase 4 → [`phases/phase-4-metricinput.md`](phases/phase-4-metricinput.md)
> - Phase 5 → [`phases/phase-5-unified-logscreen.md`](phases/phase-5-unified-logscreen.md)
> - Phase 6 → [`phases/phase-6-what-you-track.md`](phases/phase-6-what-you-track.md)
> - Phase 7 → [`phases/phase-7-create-edit-alarms.md`](phases/phase-7-create-edit-alarms.md)
> - Phase 8 → [`phases/phase-8-cleanup.md`](phases/phase-8-cleanup.md)

### Phase 1 — Extended colour roles (additive, no migration)
**Goal:** categories can be assigned quaternary/quinary/senary in-theme roles and a fixed off-theme colour, all re-theming correctly.
**Do:**
- Add `QUATERNARY`, `QUINARY`, `SENARY` to `CategoryColor` (CategoryAppearance.kt) with keys `quaternary`/`quinary`/`senary`.
- Derive the three colours from the active `ColorScheme` (HSL hue-offset / interpolation, mirroring `buildCustomColorScheme`); provide them + their on-colours via a `CompositionLocal` installed in `Theme.kt`. Add branches to `String.toCategoryColor()` / `toCategoryOnColor()`.
- Extend the colour picker UI (in `ManageCategoriesScreen.kt`) to a `RolePicker` row offering the 6 roles + a Fixed-colour track (existing hex options). Match the "Pick a colour" mock (row 6).
**Data:** none (colorToken is free-form).
**Acceptance:** assign each new role to a category; it renders and re-themes across a light and a dark palette; on-colour passes contrast; `wcag_check.py` still green; a11y roles present on the picker (Role.RadioButton per the a11y table). Fixed colour does not change on theme switch.
**Preservation:** existing primary/secondary/tertiary/hex tokens unchanged; calendar/DayLogSheet still resolve colours.
**Risk:** low. Fully additive.

### Phase 2 — Group data model (additive migration 23→24)
**Goal:** groups exist in the data layer with colour inheritance, no UI redesign yet.
**Do:**
- `Group` entity + `GroupDao` + repository methods (CRUD, list, reorder). Add to `@Database` entities, bump version to 24, add `MIGRATION_23_24` creating `groups` and adding nullable `groupId` to `tracking_categories` (`ALTER TABLE ADD COLUMN groupId INTEGER`).
- Implement the inheritance rule (§5): resolver falls back to the group's `colorRole` when `colorToken == "inherit"`. Add `"inherit"` handling; keep default `"secondary"` for existing rows.
- Minimal wiring: a category can be assigned/unassigned a group via repository (no full UI yet; a temporary entry point is fine, or defer UI to Phase 6).
**Data:** migration adds `groups` table + `groupId` column, all existing rows `groupId = null`, colours unchanged.
**Acceptance:** migration test (open a v23 DB, migrate, assert data intact + new column present); assigning a group changes an inherit-category's colour; existing categories keep their colours; app builds in CI.
**Preservation:** all category/log reads unaffected; Stats/History/export unchanged.
**Risk:** medium (schema). Mitigate with a migration test and the additive-column approach.

### Phase 3 — Reusable component library (additive, no wiring)
**Goal:** the ~12 primitives exist with `@Preview`s, driven by params + a role colour. No screen consumes them yet (or only previews do).
**Do:** build `SectionHeader`, `ListCard`/`ListRow`, `StepScale`, `ChipToggle`/`ChipRow`, `ToneHero`, `SegmentedToggle`, `RolePicker` (from Phase 1, factored out), `IconPicker`, `SwitchRow`, `Timeline`/`TimelineEntry`, `PrimarySaveBar`. Each carries correct a11y (StepScale exposes as one control reporting "Flow, Medium, 3 of 4"; chips carry check + role; SwitchRow uses Role.Switch + stateDescription). Typography per the spec (Comfortaa on titles/hero words, applied explicitly; uppercase section labels 11sp +0.11em).
**Acceptance:** previews render each primitive in a light and dark theme at default and 200% font scale without clipping; `a11y_check.py` green.
**Preservation:** nothing removed; existing screens untouched.
**Risk:** low.

### Phase 4 — MetricInput façade + new input types (Yes/No, Time)
**Goal:** one façade renders every input type; two new types added; the *category* log screen renders via the façade with no behaviour loss.
**Do:**
- Add `YES_NO` (`yes_no`) and `TIME` (`time`) to `CategoryType`. Decide storage: Yes/No stores "Yes"/"No" as the value label; Time stores `HH:mm` as the value label (reuse `loggedAt` semantics or the value string — document the choice).
- Build `MetricInput(type, config, value, onChange)` switching to `StepScale` (scale) / continuous `Slider` (numeric_free/genuine slider) / `Counter` (increment) / `SegmentedToggle` (yes_no) / `TimeField` (time) / `ChipRow` (default). Preserve the numeric_slider stepped behaviour, numeric_free unit + empty-blocks-save, increment ≤0-blocks-save, timed-increment timeline.
- Refactor `LogCategoryScreen` to render `MetricInput` instead of its inline `when`. Keep the screen otherwise identical (notes, save, date, delete).
**Data:** none (new categoryType strings are free-form; no migration).
**Acceptance:** every existing category type logs and edits exactly as before; Yes/No and Time categories can be created (via existing create flow or a temporary path) and logged; timed timeline still works; save-blocking rules intact.
**Preservation:** this is the riskiest for silent behaviour loss — walk every branch in map 01 §2 and confirm parity. Keep `LogPeriodScreen`'s `PinnedCategoryInput` working (it will be replaced in Phase 5; until then it may also delegate to `MetricInput`).
**Risk:** medium. Parity-driven.

### Phase 5 — Unified `LogScreen` (the big one)
**Goal:** one screen logs a day; period is a state of the day; old screens still reachable until parity is signed off.
**Do:**
- Build `LogScreen(date)` + its ViewModel composing the primitives: mood hero (off-period, secondary) / flow group (on-period, primary), symptoms chips, grouped tracked-metric cards (a group of ≥2 → one `ListCard` of rows; a group of one → single-metric page), notes, period footer state row, `PrimarySaveBar`. Header title is a button → category/day switch sheet organised by group, tinted by role, preserving the entered value.
- Reproduce **all** period behaviour: flow mapping, symptom inline-add, pinned categories in the flow context, episode/continuation, remove-day, delete-episode, disable-period, unsaved-changes guard, the tracking-system fan-out, widget + reminder refresh.
- Route it in behind a flag / new route; keep `LogPeriod`/`LogCategory` routes alive. Entry points (calendar tap, FAB, quick log) can switch to `LogScreen` once parity holds.
**Data:** none expected (derives period state from `period_days`).
**Acceptance:** parity checklist against §3 Logging fully ticked; a day with and without an active period logs correctly and shows in Stats/History/widget; TalkBack traversal is top→bottom and the scale reads as one control.
**Preservation:** do not delete the old screens here. This phase adds; Phase 8 removes.
**Risk:** high. Largest surface. Consider sub-PRs (off-period first, then on-period, then header switcher).

### Phase 6 — "What You Track" management home (Grouped/Ungrouped)
**Goal:** redesign `ManageCategoriesScreen` to the row-4 mock; groups are first-class in the UI.
**Do:** `SegmentedToggle` Grouped/Ungrouped; Grouped = group cards listing categories with inline "+ Add category to this group" and a "+ New group" pill; Ungrouped = neutral `surfaceVariant` categories each with "Add to group ›"; add-to-group sheet (adopt group role, or "New group…" pre-filled, past entries preserved); create-group flow.
**Acceptance:** create/edit groups; file/unfile categories; all existing management actions (archive, reorder, delete-with-history, system protection, tracking modes, quick-log, value editing) still reachable and working.
**Preservation:** `ManageCategoryValues`, tracking modes, quick-log config all intact.
**Risk:** medium.

### Phase 7 — Category creation & edit (2-step) + scale settings + alarms
**Goal:** the row-5/row-6 create/edit flow.
**Do:** New category step 1 (name, `IconPicker`, `RolePicker`, input-type selector, allow-multiple `SwitchRow`, log-with-period `SwitchRow`) → step 2 shown only for Scale (min/max range, per-step word labels, allow-decimals). Edit adds a Reminders section (wire to the existing `CustomAlarm`/`EditAlarm` system) and a danger-zone delete-with-history row. Alarms appear on edit, not first creation.
**Acceptance:** create each input type end-to-end; scale step-2 persists range/labels/decimals; edit surfaces and schedules alarms via the existing system; delete removes category + history with confirmation.
**Preservation:** existing create/edit capabilities and `EditAlarm` scheduling behaviour unchanged; `categoryType` immutability rule honoured (or explicitly relaxed with a migration-safe plan if the design requires changing type post-creation — decide and document).
**Risk:** medium.

### Phase 8 — Cleanup & removal (only after parity)
**Goal:** retire the superseded paths.
**Do:** once `LogScreen` is flagged on and parity signed off, remove `LogPeriodScreen`/`LogCategoryScreen` and the duplicated `PinnedCategoryInput`, consolidate the duplicated `DatePickerDialogWrapper`, drop dead routes. Final `a11y_check.py` + `wcag_check.py` + full regression pass over §3. Consolidate changelog fragments at the next release.
**Acceptance:** no references to removed screens; all §3 features still present via the new surfaces; CI green.
**Preservation:** this is the *checkpoint*, not a free-for-all: every removal is matched to a proven replacement.
**Risk:** medium (deletion). Gate on the parity checklist.

---

## 7. Progress log (update in every phase PR)

| Phase | Status | PR / commit | DB version after | Notes / deviations |
|---|---|---|---|---|
| 0 — Planning & groundwork | Done | this branch (`claude/design-handover-bdq9he`) | 23 | Subsystem maps stamped at `d07d947` / vc116. Theme-reconciliation split out as separate task. |
| 1 — Extended colour roles | Done | `claude/logging-redesign-phase-1-f08e0o` | 23 | Derivation deviates from the sketch in two ways: near-greyscale accents (High Contrast) shift lightness instead of hue (hue rotation of grey is a no-op), and on-colours pick near-black vs white by max contrast rather than a 0.35 luminance threshold (the threshold has a 0.30-0.35 band where white fails 3:1). Spot-check script `wcag_check_roles.py` added (all 12 families + HC + Blue & Orange, worst ratio 4.24:1). Also restored `wcag_check.py`, which was committed as one base64 line in `84624bc`. |
| 2 — Group data model | Done | `claude/logging-redesign-phase-2-nuaywv` | 24 | Colour-inheritance deviation (keep existing colours, opt-in `"inherit"`) confirmed with owner. `"inherit"` is a sentinel constant, not a `CategoryColor` entry, so the Phase 1 picker does not offer it. Group methods live in `TrackingRepository` (new nullable `groupDao` ctor param) rather than a separate repository. No FK on `groupId`; `deleteGroup` unfiles members first. Migration test is a JVM test (`Migration23To24Test`) driving the real migration through sqlite-jdbc, since the project has no instrumented tests and `exportSchema = false`. |
| 3 — Component library | Not started | | | |
| 4 — MetricInput + Yes/No + Time | Not started | | | |
| 5 — Unified LogScreen | Not started | | | Consider sub-PRs. |
| 6 — What You Track home | Not started | | | |
| 7 — Create/edit + scale + alarms | Not started | | | Decide categoryType mutability. |
| 8 — Cleanup & removal | Not started | | | Gate on parity checklist. |

---

## 8. Open decisions for a human (surface these, don't guess)

1. **Colour default for ungrouped existing categories:** this plan keeps their current `colorToken` (no grey wipe), diverging from the handover's "neutral surfaceVariant by default". Confirm that is the desired behaviour, or accept a one-time optional "Organise your categories" nudge that offers (not forces) filing.
2. **`categoryType` mutability:** currently immutable after creation. The new edit flow implies changing type. Allowing it needs a value-migration story (e.g. scale↔count) or a documented "type is fixed once logged" constraint. Decide before Phase 7.
3. **Yes/No and Time storage encoding:** confirm storing as value-label strings ("Yes"/"No", "HH:mm") vs a dedicated column. Value-label keeps zero-migration; a column is cleaner for Stats. Decide before Phase 4.
4. **Theme-spec reconciliation** (`Color.kt` vs `GoFlo Theme Redesign.md`): in scope as a separate PR, or leave as-is? Not part of this logging plan.
