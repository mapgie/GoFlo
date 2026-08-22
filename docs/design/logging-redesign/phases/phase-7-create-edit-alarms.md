# Phase 7 — Category creation & edit (2-step) + scale settings + alarms

**Goal:** the row-5 / row-6 create and edit flows. Step 1 stays short; step 2 is scale-only; alarms and danger-zone appear on **edit**, not first creation.

**Prerequisites:** Phases 1-4 (roles, group model, primitives, input types).
**Read first:** `handover/screens/04-row.png` (New category full option set), `05-row.png` (step 2 scale settings + category-edit alarms), `06-row.png` (colour roles). Current `ManageCategoriesScreen.kt` (hosts today's create/edit) and the `EditAlarm` screen + `CustomAlarm` system.

---

## New category — step 1 (row 5)
Fields, in order: **Name**; **Icon** (`IconPicker`); **Colour** (`RolePicker` — 6 roles + Fixed); **Input type** (Scale / Slider / Count / Yes-no / Time via `SegmentedToggle` or a row of chips); **Allow multiple per day** (`SwitchRow` — "Log it several times — keeps the time of each"); **Log with period** (`SwitchRow` — "Surface it in the flow context while a period runs"). Footer "Next ›". Helper (one line): "Scale, range and alarms live on step 2 — the first screen stays short."

Map to existing model: Scale → `numeric_slider`; Slider → `numeric_free` (genuine continuous) or a dedicated continuous type — **decide and document**; Count → `increment`; Yes-no → `yes_no`; Time → `time`. Allow-multiple → `allowMultiple`; Log-with-period → `showInLogPeriod`. Colour → `colorToken` (role key or hex, or `"inherit"` if filed in a group).

## New category — step 2 (row 6, Scale only)
Shown only when type = Scale: **Range** (min/max), **Step labels** (per-step optional words → `scaleLabels` via `encodeScaleLabels`), **Allow decimals** (`SwitchRow` → `allowDecimals`). "Save".

## Category edit (row 5 middle + row 6)
Same two screens **plus**:
- **Reminders** section: list of alarm times with day rules (Every day / Weekdays) and on/off `SwitchRow`s; "+ Add alarm". Wire to the **existing** `CustomAlarm` system + `EditAlarm` route (`Screen.EditAlarm.newForCategory(categoryId)` already exists). Do not build a parallel alarm system.
- Link into scale settings.
- **Danger zone:** "Delete category & its history" (confirm; system categories protected).
- Alarms appear on edit only.

## Files

| File | Change |
|---|---|
| `ui/screens/categories/` | New `CategoryEditScreen` (2-step, create + edit) or restructure the existing create/edit path. Reuse `IconPicker`/`RolePicker`/`SwitchRow`/`SegmentedToggle`/`ListCard`. |
| `ui/navigation/Screen.kt` / `MainActivity.kt` | Routes for create (step1→step2) and edit; wire `EditAlarm` linkage. |
| `data/repository/TrackingRepository.kt` | Reuse `addCategory(...)` and `updateCategoryFullSettings(...)`; both already accept the full field set (map 02 §4). |

## Open decision (§8 #2): categoryType mutability
Today `categoryType` is immutable after creation. The edit flow shows a Type row. Either:
- **Keep immutable:** show type read-only on edit (simplest, safe). Or
- **Allow change:** needs a value-migration story (e.g. switching scale↔count reinterprets stored labels). If allowed, write a repository method that converts existing `TrackingLogValue`s or explicitly warns they'll be reinterpreted. **Decide with the owner before building.** Default to read-only-on-edit unless told otherwise.

## Acceptance criteria
- [ ] Create a category of each of the 5 types end-to-end; correct fields persist (`addCategory`).
- [ ] Scale step-2 persists range, per-step labels (`scaleLabels`), and decimals.
- [ ] Edit surfaces Reminders; adding one schedules via the existing `CustomAlarm` system and fires.
- [ ] Delete-with-history works with confirmation; system categories cannot be deleted.
- [ ] Step 1 has at most one helper line per group (handover copy discipline).
- [ ] `a11y_check.py` green (icon/role pickers = `Role.RadioButton`; switches = `Role.Switch` + `stateDescription`; content descriptions on icon-only controls).

## Feature-preservation checklist
- [ ] All fields the current create/edit path can set are still settable (icon, colour, type, numeric range/unit/decimals, scale labels, allow-multiple, show-in-period, track-against-time, mode key).
- [ ] Existing `CustomAlarm`/`EditAlarm` scheduling behaviour unchanged; no duplicate alarm system.
- [ ] Value catalog editing (`ManageCategoryValues`) still reachable for `default` categories.
- [ ] `modeKey` categories (tracking modes) still creatable/editable.

## Gotchas
- Reuse `addCategory` / `updateCategoryFullSettings` — do not invent new persistence.
- Alarms use `SCHEDULE_EXACT_ALARM` / `POST_NOTIFICATIONS` (already granted). Don't add permissions.
- Medical-disclaimer/privacy surfaces (if touched) must use the body font, never Comfortaa.
- No en/em dashes in any of the new user-facing copy.

## Changelog fragment
```json
{ "bump": "minor", "added": ["Redesigned category creation with icon, colour role, input type, and per-category reminders"] }
```
