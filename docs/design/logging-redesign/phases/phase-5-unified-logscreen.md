# Phase 5 — Unified `LogScreen` (the big one)

**Goal:** one screen logs a day; a running period is a *state of the day*, not a separate destination. Old screens stay reachable until parity is signed off (removal is Phase 8).

**Prerequisites:** Phases 1-4.
**Read first:** `subsystem-maps/01-logging-screens.md` (all of it — this phase must reproduce both screens' behaviour); `handover/screens/01-03-row.png` (the two states, the header switcher, the grouped card); `PLAN.md` §3 (preservation inventory) and §5.

---

## The core idea (handover row 2-3)
There is **one** log screen. Off-period and on-period share the same components in the same order; only two things change:
- **Off-period:** mood hero leads (amber/`secondary` `ToneHero`, "How did today feel"); flow not rendered; footer = quiet hairline "Period started today ›" row.
- **On-period:** a blue/`primary` **Flow** `StepScale` group slots in at the top; the mood hero compresses to a single `StepScale` row; footer = filled `primaryContainer` status row ("Period ongoing · since Aug 6 · End").
Everything between (symptoms, tracked metrics, notes) is byte-for-byte the same.

## Suggested sub-PRs (this phase is large — split it)
1. **5a — off-period skeleton:** `LogScreen(date)` + VM rendering mood hero, symptoms, grouped tracked-metric cards, notes, save bar, quiet period footer. Behind a new route/flag; entry points still use the old screens.
2. **5b — on-period state:** flow group, compressed mood row, status footer, all period episode/continuation logic ported.
3. **5c — header switcher + grouped multi-metric card:** title-as-button sheet (categories by group, tinted by role, preserves entered value); a group of ≥2 renders one `ListCard` of rows, a group of one renders the single-metric page.
4. **5d — flip entry points:** calendar tap / FAB / quick-log open `LogScreen`; keep old routes registered.

## Files

| File | Change |
|---|---|
| `ui/screens/log/LogScreen.kt` + `LogViewModel.kt` | **New.** Compose the primitives; own the day model. |
| `ui/navigation/Screen.kt` | Add a `Log` route (e.g. `log_day?date={date}`). Keep `LogPeriod`/`LogCategory`. |
| `MainActivity.kt` | Register the new route; wire its VM factory. Flip entry points in 5d. |
| `data/repository/PeriodRepository.kt` / `TrackingRepository.kt` | Reuse existing methods; add read helpers if needed (e.g. "is a period active on date D"). No schema change expected. |

## Behaviour that MUST be ported (from map 01 §4-5)
- **Flow:** slider *or* chips per the flow-category mode; the 1→Spotting / 2→Light / 4→Heavy / else Medium mapping.
- **Symptoms:** chips + inline "Add" (`AddSymptomDialog`) writing to the value catalog (`addValueToCategory`).
- **Pinned categories** (`showInLogPeriod`) render in the flow context while a period runs; in their normal group otherwise.
- **Episode/continuation:** gap tolerance from prefs, episode day number, range vs open-ended, "Remove this day" (`unlogPeriodDay`), "Delete Entire Period" (deletes episode + its per-day tracking logs), "Disable period logging".
- **Unsaved-changes guard:** discard/save dialog on back (LESSONS.md `SwipeToDismissBox`/guard patterns are relevant to how you intercept).
- **Save fan-out:** `syncFlowToTrackingLog`, `syncSymptomsToTrackingLog`, `syncPinnedCategoryLogs` → tracking system; plus `GoFloWidget.updateAllWidgets` and `ReminderScheduler.refreshPredictionReminders`.
- **Header switcher (re-file):** switching category changes the entry's `categoryId`; the entered value is preserved. "The current entry is preserved — switching only changes what the same saved day is filed under."

## Acceptance criteria
- [ ] A day with **no** active period: mood hero + symptoms + tracked metrics + notes all log and persist; "Period started today" starts one.
- [ ] A day **with** an active period: flow leads, mood compresses, footer shows status + End; everything else identical to the off-period layout.
- [ ] Period data appears in Stats / Flow / Symptoms / widget exactly as the old screen produced.
- [ ] Header switcher re-files without losing the entered value.
- [ ] Grouped multi-metric card: ≥2 categories → one card of rows; 1 → single-metric page.
- [ ] TalkBack traversal top→bottom; `StepScale` reads as one control ("Flow, Medium, 3 of 4").
- [ ] `a11y_check.py` green.

## Feature-preservation checklist (tick every item in `PLAN.md` §3 "Logging")
- [ ] Old `LogPeriod` and `LogCategory` routes **still work** and are still registered (do not delete).
- [ ] All five (now seven) input types render via `MetricInput` inside `LogScreen`.
- [ ] Edit an existing period day / category log through the new screen matches old behaviour.
- [ ] Unsaved-changes guard present.
- [ ] Disable-period, remove-day, delete-episode all reachable.

## Gotchas
- This is where "recreate from the mock" most tempts feature loss. Work from map 01 §3 as a **checklist**, not the mock.
- The mock omits some real behaviour (e.g. tolerance/continuation). Those live in `LogPeriodViewModel`; port them even though the mock doesn't show them.
- Keep the flag/route split until 5d so a broken new screen never blocks logging.
- Don't remove `PinnedCategoryInput` yet — Phase 8.

## Changelog fragment
```json
{ "bump": "minor", "changed": ["Redesigned daily logging: one screen for the day, with an active period shown as a state of that day"] }
```
