# Phase 4 — MetricInput façade + new input types (Yes/No, Time)

**Goal:** one façade renders every input type; add `yes_no` and `time`; refactor `LogCategoryScreen` to render through the façade with **zero behaviour loss**. This is the phase most at risk of silent feature loss — parity is the whole job.

**Prerequisites:** Phase 3 (primitives exist).
**Read first:** `subsystem-maps/01-logging-screens.md` §2 and §4 (walk every branch); `handover/screens/05-row.png` and `06-row.png` (input types render differently); `PLAN.md` §8 decision #3 (storage encoding).

---

## The five (now seven) input paths that must all keep working

From map 01 §2, today's `LogCategoryScreen` renders:
1. `numeric_slider` → stepped `Slider` (steps = range-1, or 0 if `allowDecimals`), value readout, optional `scaleLabels` word.
2. `numeric_free` → `OutlinedTextField` decimal, unit label, **empty blocks save**.
3. `increment` + `trackAgainstTime` → per-tap immediate save with `HH:mm`, timeline list, per-entry delete, **no Save button**.
4. `increment` (untimed) → big count + add/decrement, **≤0 blocks save**.
5. `default` → `SelectableChip` multi-select + "previously recorded (removed)" chips.

New:
6. `yes_no` → `SegmentedToggle` (Yes/No), stores "Yes"/"No".
7. `time` → `TimeField`, stores `HH:mm`.

## Storage decision (resolve §8 #3 before coding)
Default recommendation, zero-migration: store the value as a `TrackingLogValue.valueLabel` string — "Yes"/"No" for yes_no, "HH:mm" for time. Stats already counts value labels (`getValueCountsForCategory`), so Yes/No charts for free. Time as a label is display-only in Stats (acceptable). Document whatever you choose at the top of the façade file.

## Files to touch

| File | Change |
|---|---|
| `ui/util/CategoryAppearance.kt` | Add `YES_NO("yes_no",…)`, `TIME("time",…)` to `CategoryType`. |
| `ui/components/MetricInput.kt` | Flesh out the façade `when(type)` → the right primitive; define `MetricConfig`/`MetricValue`. |
| `ui/components/` | Add `Counter` and `TimeField` if not built in Phase 3. |
| `ui/screens/log/LogCategoryScreen.kt` | Replace the inline `when` (map 01 §2) with a single `MetricInput(...)`. Keep notes/save/date/delete/timed-timeline behaviour. |
| `ui/screens/log/LogCategoryViewModel.kt` | Add value get/set for yes_no + time; keep save-blocking rules. |
| `ui/screens/log/LogPeriodScreen.kt` | Optional: point `PinnedCategoryInput` at `MetricInput` too, to kill the duplication early (it is fully replaced in Phase 5). |

## The façade
```kotlin
@Composable
fun MetricInput(
    type: CategoryType, config: MetricConfig, value: MetricValue,
    role: Color, onColor: Color, onChange: (MetricValue) -> Unit,
) = when (type) {
    CategoryType.NUMERIC_SLIDER -> StepScale(config.range, config.labels, value.int, role, onColor, { onChange(value.copy(int = it)) }, config.endLabels)
    CategoryType.NUMERIC_FREE   -> ContinuousInput(value.text, config.unit, config.allowDecimals, onChange = { onChange(value.copy(text = it)) })
    CategoryType.INCREMENT      -> Counter(value.int ?: 0, onChange = { onChange(value.copy(int = it)) })   // timed variant → Timeline, handled by screen
    CategoryType.YES_NO         -> SegmentedToggle(listOf("Yes","No"), if (value.text == "Yes") 0 else 1) { onChange(value.copy(text = if (it==0) "Yes" else "No")) }
    CategoryType.TIME           -> TimeField(value.text) { onChange(value.copy(text = it)) }
    CategoryType.DEFAULT        -> ChipRow(config.options, value.selected, role, onColor, onToggle = { onChange(value.toggle(it)) })
}
```
> The **log screen never branches on type** after this — it renders `MetricInput`. The timed-increment timeline is a screen-level concern (it appends immediately); keep that branch in the screen but have it render the `Timeline` primitive.

## Parity procedure (do this, don't skip)
For each of the 7 paths: create/pick a category of that type, log a value, edit it, and confirm the stored `TrackingLogValue` and behaviour match `main`. Specifically re-verify:
- slider steps/labels/decimals + readout word
- numeric_free empty-blocks-save + unit label
- increment ≤0-blocks-save
- timed increment: per-tap save, timestamp, timeline order, per-entry delete, no Save button
- default chips: multi-select + "previously recorded (removed)" chips render
- notes 500-char cap; date selection when `canEditDate`; edit vs new; delete

## Acceptance criteria
- [ ] All 7 paths log + edit correctly; stored values identical to `main` for the 5 existing ones.
- [ ] Yes/No and Time categories can be created (via the current create flow or a temporary path — full create UI is Phase 7) and logged.
- [ ] `LogCategoryScreen` no longer contains a `categoryType` `when` for rendering (it calls `MetricInput`).
- [ ] `a11y_check.py` green.

## Feature-preservation checklist
- [ ] Every bullet in the parity procedure passes.
- [ ] `LogPeriodScreen` still works (whether or not you rewired `PinnedCategoryInput`).
- [ ] Period fan-out (`syncFlowToTrackingLog` etc.) unaffected.
- [ ] Stats/History still read the same value labels.

## Gotchas
- Timed increment is genuinely different (saves per tap, no Save button). Don't force it through the same "collect then save" flow; keep it screen-driven, rendering `Timeline`.
- `MetricValue` needs to represent: an int (slider/count), a text (free/yes_no/time), and a selected-set (chips). Model it so `onChange` round-trips without lossy conversions.
- Keep `categoryType` immutability for now (§8 #2 is a Phase 7 decision).

## Changelog fragment
```json
{ "bump": "minor", "added": ["New category input types: Yes/No and Time"], "changed": ["Unified how category inputs are rendered (no change to existing categories)"] }
```
