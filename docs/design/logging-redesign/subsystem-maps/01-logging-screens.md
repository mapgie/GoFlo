# Subsystem map: Per-day logging screens

> **Mapped against**
> - Commit: `d07d947` (`d07d947f5b2463eaa08e6521d3228026c55b2bef`)
> - versionCode **116**, versionName **0.53.0-beta.1**, DB schema version **23**
> - Date: 2026-08-22
>
> **Staleness check for future sessions:** run `git diff d07d947 -- app/src/main/java/com/mapgie/goflo/ui/screens/log/` before trusting the line numbers below. If any log-screen file changed, re-read it. The *shape* of the description (two separate screens, `categoryType` discriminator) is durable; exact line numbers drift.
>
> **Phase 4 drift (branch `claude/logging-redesign-phase-4`):** `LogCategoryScreen` no longer contains the per-type section composables described in §2 — every non-timed input renders through the `MetricInput` facade (`ui/components/MetricInput.kt`), and the timed-increment path renders the `Timeline` primitive via a screen-level `TimedIncrementTimeline`. The screen keeps a small `when` only to map `LogCategoryUiState` onto a `MetricValue` (`metricValueFor`) and to frame card vs bare-chip layouts. Two new `categoryType` strings exist: `"yes_no"` (stores "Yes"/"No" value labels) and `"time"` (stores 24h "HH:mm" value labels); both flow through `LogCategoryUiState.selectedValues` as a single-label set, and `LogCategoryViewModel.save()`'s else-branch persists them. `PinnedCategoryInput` in `LogPeriodScreen` gained additive `"yes_no"`/`"time"` branches delegating to `MetricInput` (its four pre-existing branches and `LogPeriodViewModel.computePinnedValues` are unchanged; the new types save through the existing else/selection-set path plus a new `setPinnedSingleValue`). Line numbers below refer to the pre-Phase-4 files; the save-flow description in §4 remains accurate.

> **Phase 5 drift (branch `claude/logging-redesign-phase-5`):** a third, additive destination now exists: the unified day screen `LogScreen` (`ui/screens/log/LogScreen.kt`) + `LogViewModel`, route `log_day?date={date}` (`Screen.LogDay`), reached only via an opt-in "Try the new day log (preview)" row in `DayLogSheet` — every pre-existing entry point still targets the two screens below, and both remain registered and byte-for-byte functional. `LogViewModel` holds one `DayMetricEntry` per active non-system category plus the period-day state (episode continuation, flow, symptoms, episode notes) and reuses the period logic through `PeriodDaySync` (`ui/screens/log/PeriodDaySync.kt`), an extraction of `LogPeriodViewModel`'s former private helpers: the 1→Spotting/2→Light/4→Heavy/else-Medium flow mapping, `syncFlowToTrackingLog`, `syncSymptomsToTrackingLog`, and the pinned-category value rules (`computePinnedValues`). `LogPeriodViewModel` now delegates to that object; its public behaviour is unchanged. `LogCategoryScreen`'s `metricConfigFor` and `TimedIncrementTimeline` were widened from `private` to `internal` so `LogScreen` renders the identical config mapping and timed-increment surface. The §4 save-flow description applies to the unified screen as follows: on-period saves run the LogPeriodViewModel sequence (day + episode + meta + fan-out + widget/reminder refresh) and pinned categories keep `computePinnedValues` semantics; off-period saves write only touched categories using `LogCategoryViewModel.save()`'s per-type rules (empty free text / count ≤ 0 skip that category instead of blocking the day).

## Overview: two truly separate destinations

"Log Period" and "Log Category" are **fully separate screens, routes, ViewModels, and repositories**. They share only two small helpers: the `LogEntryTopBar` composable and a private (duplicated) `DatePickerDialogWrapper`. Period logging is a bespoke multi-section day editor on `PeriodRepository`; category logging is a single generic input on `TrackingRepository`.

| | Log Category | Log Period |
|---|---|---|
| Route | `log_category/{categoryId}?date={date}&logId={logId}` (`Screen.kt:60-68`) | `log_period?periodId={periodId}&startDate={startDate}` (`Screen.kt:12-17`) |
| ViewModel | `LogCategoryViewModel` (`TrackingRepository` only) | `LogPeriodViewModel` (`PeriodRepository` + optional `TrackingRepository`, `Application`, `AppPreferencesStore`) |
| Back callback param | `onNavigateBack` | `onBack` |

## 1. Route arguments & navigation wiring

**LogCategory** — `MainActivity.kt:567-592`
- Args: `categoryId: Long` (required), `date: String?` (nullable ISO, defaults null→today), `logId: Long` (default `-1L`, `-1` coerced to null via `.takeIf { it != -1L }`).
- Entry builders (`Screen.kt:63-67`): `newEntry(categoryId, date)`, `editEntry(categoryId, logId)`. New vs edit is distinguished purely by whether `logId` is present.
- VM key: `"log_cat_${categoryId}_${dateStr}_${logId}"`.

**LogPeriod** — `MainActivity.kt:358-373`
- Args: `periodId: Long` (default `-1L`), `startDate: String?`. `periodId > 0` ⇒ edit existing episode; `-1` ⇒ new entry.
- Builders (`Screen.kt:13-16`): `withId(periodId, targetDate?)`, `newEntry`, `newEntryForDate(date)`.
- VM key: `"log_${periodId}_${startDate}"`.

## 2. Input control types (the full catalog)

Control selection is driven by `TrackingCategory.categoryType` (a `String`) plus flags `trackAgainstTime`, `allowDecimals`, `allowMultiple`, and range fields `numericMin`/`numericMax`/`numericUnit`/`scaleLabels`.

### In LogCategoryScreen — branch at `LogCategoryScreen.kt:499-586`
```kotlin
val cat = state.category
when {
    cat?.categoryType == "numeric_slider" -> NumericSliderSection(...)
    cat?.categoryType == "numeric_free"   -> NumericFreeInputSection(...)
    cat?.categoryType == "increment" && cat.trackAgainstTime -> TimedIncrementSection(...)
    cat?.categoryType == "increment"      -> IncrementSection(...)
    else -> /* text value chips (SelectableChip in a FlowRow) */
}
```
Five rendered controls:
1. **`numeric_slider`** → `NumericSliderSection` (`:144-230`). Material3 `Slider` bounded to `numericMin..numericMax`; `steps = 0` when `allowDecimals` else `range-1` whole steps; large value readout; optional per-value text label from `scaleLabels.decodeScaleLabels()`.
2. **`numeric_free`** → `NumericFreeInputSection` (`:232-262`). `OutlinedTextField`, `KeyboardType.Decimal`, label = `numericUnit` or "Value".
3. **`increment` + `trackAgainstTime`** → `TimedIncrementSection` (`:330-411`). "Log +1 now" button; each tap **saves a new log immediately** with an `HH:mm` timestamp; lists today's timestamped entries with per-entry delete. No notes/Save button.
4. **`increment`** (untimed) → `IncrementSection` (`:271-324`). Big count + "Add one" button and a decrement `IconButton`; count held in `numericValue`.
5. **else (text)** → `SelectableChip` chips in a `FlowRow` (`:534-585`), multi-select. Also renders "Previously recorded (removed from options)" chips for stored labels no longer in the catalog.

Shared extras (only when NOT timed-increment, `:591-635`): optional "Track against time" `Checkbox`, a Notes `OutlinedTextField` (500-char cap), and the Save/Update `Button`. A `DateSelectorCard` (`:76-110`) appears at top only when `state.canEditDate`.

### In LogPeriodScreen — pinned-category branch at `LogPeriodScreen.kt:483-616`
`PinnedCategoryInput` re-implements the **same** four-way `when(category.categoryType)` (`numeric_slider` / `numeric_free` / `increment` / else-chips). This is a **parallel, duplicated** rendering path — the two screens do not share input composables. The period increment control uses a "+1" button and, unlike the category screen, is not timed.

## 3. How LogCategoryScreen decides which control to render

Solely on `state.category?.categoryType`, with a secondary check on `trackAgainstTime` to split timed vs untimed increment. There is no separate "input type" enum — `categoryType` is the discriminator string.

## 4. Exact save flow

### LogCategoryViewModel.save() — `LogCategoryViewModel.kt:243-296`
1. Early return if timed-increment (those save per-tap).
2. Build `valuesToSave: Set<String>` by type: `numeric_slider` → `numericValue ?: numericMin` formatted; `numeric_free` → trimmed text, **blocks save if empty**; `increment` → `numericValue.toInt()`, **blocks if ≤ 0**; else → `state.selectedValues` (chip set).
3. `loggedAt` = `HH:mm` if `trackTime` else `""`.
4. Persist: edit → `repository.updateLogInPlace(...)`; else → `repository.saveLog(date, categoryId, selectedValues, notes, allowMultiple, loggedAt)`.
5. `saved=true` → screen `LaunchedEffect` pops back.

Other VM methods: `setNumericValue`, `setNumericFreeText`, `toggleValue`, `setNotes`, `setTrackTime`, `setDate`, `addTimedIncrement`, `deleteTimedEntry`, `delete`.

### LogPeriodViewModel.save() — `LogPeriodViewModel.kt:353-394`
Heavier; the unit is a single **day**:
1. Editing → `logPeriodDay(date)` then `updateEpisode(id, start, end, notes, tolerance)`.
2. New with end date → `logPeriodRange(date, endDate, tolerance)`.
3. New single day → `logPeriodDay(date, tolerance)`.
4. If episode returned → `updateEpisodeMeta(id, notes, flowLevel)`.
5. **Mirror per-day values into the tracking system**: `syncFlowToTrackingLog`, `syncSymptomsToTrackingLog`, `syncPinnedCategoryLogs` — each calls `trackingRepository.saveLog(...)` so period data also appears under Flow/Symptoms/pinned categories in Stats.
6. Side effects: `GoFloWidget.updateAllWidgets`, `ReminderScheduler.refreshPredictionReminders`.
7. `saved=true` → screen pops.

Extra period-only actions: `removeDay()` → `unlogPeriodDay`, `delete()` → deletes episode + its per-day tracking logs, `disablePeriodTracking()`.

## 5. How period logging differs from generic category logging

- **Composite fixed layout.** Period screen stacks: Day picker, Period-dates/End-date, dedicated **Flow** section (slider *or* chips), **Symptoms** section (chips + an "Add" `AssistChip` opening `AddSymptomDialog`), **pinned categories** (`state.pinnedCategories` each via `PinnedCategoryInput`), Notes, Save, edit-only "Remove this day" / "Delete Entire Period". The category screen renders exactly **one** input for **one** category.
- **Flow slider→label mapping.** 1→Spotting, 2→Light, 4→Heavy, else Medium (`LogPeriodViewModel.kt:299-308`, `:542-547`), unlike generic sliders which store the raw numeric string.
- **Episode/continuation logic.** Period VM computes `continuesEpisodeStart`, `episodeDayNumber`, gap `toleranceDays` from prefs, open-ended vs range periods — absent from category logging.
- **Unsaved-changes guard.** Period screen tracks `hasChanges` and shows a discard/save dialog on back; category screen has none.
- **Overflow menu** with "Disable period logging".
- **Symptoms added inline** via `AddSymptomDialog` → `addNewSymptomToLibrary` → `trackingRepository.addValueToCategory`; category screen cannot create new values (directs to Settings).
- **Repository split.** Period save writes to `PeriodRepository` (episode model) *and* fans out to `TrackingRepository`; category save writes only `TrackingRepository`.

## 6. Shared pieces (redesign-relevant)

- **`LogEntryTopBar`** (`LogEntryTopBar.kt:23-55`): `internal` composable, params `title, subtitle, onBack, actions`. Used by both screens. primaryContainer-coloured header.
- **`DatePickerDialogWrapper`**: defined **privately in each screen file** (`LogCategoryScreen.kt:114-134` vs `LogPeriodScreen.kt:626-650`; period version adds a `minDate`). Duplicated, not shared.
- **`SelectableChip`** (`ui.components`) and **`decodeScaleLabels()`** (`ui.util`) are shared.
- **`SectionLabel`** is private to `LogPeriodScreen.kt:620-622` only.

## Key duplication to consolidate

The four-way `categoryType` rendering exists in **two independent implementations**: `LogCategoryScreen`'s top-level `when` + its section composables, and `LogPeriodScreen`'s `PinnedCategoryInput`. The slider step/label/formatting logic is copy-pasted between them. A unified `MetricInput` façade consolidates these two paths — see the plan's Phase 4.
