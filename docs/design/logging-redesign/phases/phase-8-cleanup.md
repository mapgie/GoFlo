# Phase 8 — Cleanup & removal (only after parity)

**Goal:** retire the superseded paths now that the unified screen and new management flows are proven. This is the **only** phase that deletes code, and every removal must be matched to a proven replacement.

**Prerequisites:** Phases 5, 6, 7 shipped and their acceptance criteria met. The unified `LogScreen` is flagged on and entry points use it.
**Read first:** `PLAN.md` §3 (run the whole inventory as a regression pass); all prior phase docs' preservation checklists.

---

## Gate (do not start until all true)
- [ ] `LogScreen` is the entry point for calendar tap, FAB, and quick-log, and has been for a full test pass.
- [ ] Every item in `PLAN.md` §3 has a working home in the new UI.
- [ ] No open parity gaps recorded in the progress log.

## Remove / consolidate
- `ui/screens/log/LogPeriodScreen.kt` + `LogPeriodViewModel.kt` — **only** once `LogScreen` reproduces all of it (map 01 §5). Move any still-unique helper (e.g. episode math) into `LogViewModel` or the repository first.
- `ui/screens/log/LogCategoryScreen.kt` + `LogCategoryViewModel.kt` — once `LogScreen` + `MetricInput` cover single-category logging (including edit-from-notification / edit-from-stats entry points).
- `PinnedCategoryInput` (in `LogPeriodScreen.kt`) — the duplicated input path; deleted with the file.
- The two private `DatePickerDialogWrapper` copies (map 01 §6) — consolidate into one shared composable if still used.
- Dead routes in `Screen.kt` (`LogPeriod`, `LogCategory`) and their `composable(...)` blocks in `MainActivity.kt` — remove after confirming nothing navigates to them (grep `Screen.LogPeriod`, `Screen.LogCategory`, `withId(`, `newEntry`, `editEntry`).

## Do NOT remove
- Any repository method still used by the new screens (`saveLog`, `updateLogInPlace`, `syncFlowLogsForPeriod`, period episode methods, etc.).
- The `CustomAlarm` system, tracking modes, quick-log, `ManageCategoryValues`, export, Stats/History.
- `SelectableChip` if any non-redesign caller remains (grep first).

## Final passes
- [ ] `python3 a11y_check.py` — green.
- [ ] `python3 wcag_check.py` — green (and the derived-roles spot-check from Phase 1).
- [ ] Full regression over `PLAN.md` §3: log off-period, log on-period, every input type, create/edit/delete category, groups, archive/reorder, tracking modes, quick-log widget, alarms, Stats, History, export, calendar, PIN-locked widget shows neutral placeholder.
- [ ] Grep for now-dead code: unused imports, `internal` helpers with no callers, string resources for removed screens.
- [ ] Update `LESSONS.md` if the unification surfaced a transferable lesson (CLAUDE.md rule).

## Acceptance criteria
- [ ] No references to removed screens/routes anywhere (grep clean, CI compiles).
- [ ] All `PLAN.md` §3 features present via the new surfaces.
- [ ] CI green.

## Changelog fragment
```json
{ "bump": "patch", "changed": ["Removed the superseded separate period and category logging screens now that the unified daily log replaces them"] }
```

## After this phase
Update `PLAN.md` §7 progress log to Done for all phases, record final DB version and the commit, and note any features intentionally changed (with owner sign-off) vs preserved. Consider promoting the redesign out of beta only via the manual `-beta.N` drop described in CLAUDE.md — not as part of this phase.
