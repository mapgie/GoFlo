# Logging & category-system redesign

This folder holds the design handover for GoFlo's logging/category redesign and a phased plan to implement it across multiple sessions **without losing existing features**.

## Start here

1. **[`PLAN.md`](PLAN.md)** — the master plan. Golden rules, feature-preservation inventory, gap analysis, the 8 phases with acceptance criteria, a progress log, and open decisions. A fresh session should read this first.
2. **[`subsystem-maps/`](subsystem-maps/)** — stamped groundwork so you don't have to re-explore the codebase:
   - [`01-logging-screens.md`](subsystem-maps/01-logging-screens.md) — the two current log screens, routes, input controls, save flows.
   - [`02-category-data-model.md`](subsystem-maps/02-category-data-model.md) — entities, DB version + migration chain, DAOs, repository API.
   - [`03-theme-color-machinery.md`](subsystem-maps/03-theme-color-machinery.md) — `colorSchemeFor`, palettes, colour-role resolution hook.
   Each map is stamped with the **commit + build** it was verified against (`d07d947`, versionCode 116, DB v23) and a staleness check. If HEAD has moved, run the staleness check before trusting line numbers.
3. **[`handover/`](handover/)** — the original design bundle (verbatim):
   - `README.md` — design intent + the reusable-component-library requirement.
   - `GoFlo Theme Redesign.md` — the 12-palette colour spec (largely already applied; reconciliation is a separate task).
   - `Log Screens Rethought.html` — the pannable mockup canvas (open in a browser).
   - `screens/01-06-row.png` — rendered reference of each design row.

## The one-paragraph version

A category stores a colour **role** and an optional **group**; the group owns the role and a default input type. From that: three extra in-theme colour roles (quaternary/quinary/senary) plus a fixed off-theme colour; a small library of reusable Compose primitives that every screen is built from; and one unified log screen where a running period is a *state of the day*, not a separate screen. Most of the per-category model (icons, input types, allow-multiple, timeline, show-in-period) **already exists** in the code — see the gap analysis in `PLAN.md` §4. The work is unification and a few genuinely new pieces (the group model, the extra roles, the unified screen, the component library, Yes/No + Time input types), done additively so nothing currently shipping is lost.
