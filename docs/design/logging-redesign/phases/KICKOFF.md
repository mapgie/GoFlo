# Phase kickoff prompts

Paste one of these into a **fresh session** to implement a phase. Replace `<N>` and the phase name. Each phase is its own branch + PR off `main`.

---

## Reusable template

```
You are implementing ONE phase of the GoFlo logging & category-system redesign.

Repo: mapgie/GoFlo. Do all work on a new branch off main named
`claude/logging-redesign-phase-<N>`. Open a draft PR when done.

READ FIRST, IN THIS ORDER (do not skip):
1. docs/design/logging-redesign/PLAN.md  — golden rules (§1), feature-preservation
   inventory (§3), gap analysis (§4). These are binding.
2. docs/design/logging-redesign/phases/phase-<N>-*.md  — your build guide: exact
   files, code sketches, migration/data details, acceptance + preservation checklists.
3. The one subsystem map your phase touches (docs/design/logging-redesign/subsystem-maps/).
   Run its staleness check first: the maps were stamped at commit d07d947 / DB v23 —
   if HEAD or the DB version has moved, re-verify the files it describes before trusting
   line numbers.

HARD RULES:
- Additive-first. Do NOT delete or rewire any existing screen/feature unless your phase
  guide's removal section explicitly says to (only Phase 8 removes). Every item in
  PLAN.md §3 must still work when you finish.
- DB changes: additive, numbered migration only (check GoFloDatabase.kt's current
  `version`), never fallbackToDestructiveMigration. Add a migration test.
- This environment can't build (no Android SDK) — CI is the build check. Verify by
  inspection + run `python3 a11y_check.py` and `python3 wcag_check.py` before pushing.
  Don't add "couldn't compile" disclaimers.
- Every app-code PR needs a changelog fragment at changelog/unreleased/<slug>.json
  (the guide suggests one). No en/em dashes in user-facing text. Bind colour to roles,
  never literal hex. Respect the a11y rules in .claude/CLAUDE.md (Role on every clickable).
- If your phase hits one of the "open decisions" in PLAN.md §8, STOP and ask me — don't guess.

WHEN DONE:
- Tick your guide's acceptance + preservation checklists in the PR description.
- Update PLAN.md §7 progress log (status, PR/commit, DB version after, any deviations).
- If a file a subsystem map describes changed materially, update that map or note the drift.
- Open a draft PR; do not merge.

Start by reading the three docs above, then tell me your plan for Phase <N> before writing code.
```

---

## Phase 1 (ready to paste)

```
You are implementing Phase 1 (Extended colour roles) of the GoFlo logging redesign.

Repo: mapgie/GoFlo. New branch off main: `claude/logging-redesign-phase-1`. Draft PR when done.

READ FIRST: docs/design/logging-redesign/PLAN.md (§1 golden rules, §3 preservation, §4 gap
analysis), then docs/design/logging-redesign/phases/phase-1-color-roles.md (your build guide),
then docs/design/logging-redesign/subsystem-maps/03-theme-color-machinery.md (run its staleness
check first).

Goal: add quaternary/quinary/senary in-theme colour roles (derived from the active ColorScheme
via HSL, provided through a CompositionLocal) plus a Fixed off-theme colour, and extend the
category colour picker. Fully additive, no DB migration.

HARD RULES: additive only (don't touch existing primary/secondary/tertiary/hex behaviour);
don't change colorSchemeFor's signature (extend around it); derived on-colours must pass WCAG
contrast in all 12 palettes — run python3 wcag_check.py and add a spot-check for the new roles;
run python3 a11y_check.py (picker chips need Role.RadioButton, ≥48dp, not colour-only). Add a
changelog fragment. No en/em dashes in user-facing copy.

When done: tick the guide's acceptance + preservation checklists in the PR, update PLAN.md §7,
open a draft PR (don't merge).

Start by reading the three docs, then give me your plan before writing code.
```

> For later phases, copy the template and point it at the matching `phase-<N>-*.md`. Phase 5 is large — its guide suggests splitting into sub-PRs (5a-5d); you can run those as separate sessions too.
