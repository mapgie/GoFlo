# Phase 6 — "What You Track" management home (Grouped/Ungrouped)

**Goal:** redesign `ManageCategoriesScreen` to the row-4 mock; groups become first-class in the UI. All existing management actions survive.

**Prerequisites:** Phases 1-3 (roles, group model, `SegmentedToggle`/`ListCard`/`RolePicker`).
**Read first:** `handover/screens/03-row.png` and `04-row.png` (Grouped/Ungrouped, add-to-group sheet); `subsystem-maps/02-category-data-model.md` §4 (repo API); the current `ManageCategoriesScreen.kt` (63KB — read it fully; it already hosts icon/colour pickers, archive, reorder, delete, tracking modes, quick-log).

---

## Target UI (handover row 4)
- Top: `SegmentedToggle` **Grouped / Ungrouped** (view swaps in place).
- **Grouped:** each group is a `ListCard` titled with its colour dot + "Edit", listing its categories, an inline "+ Add category to this group", and a "+ New group" pill at the bottom.
- **Ungrouped:** loose categories in neutral `surfaceVariant`, each with "Add to group ›"; a "+ New category" CTA. Copy: "Ungrouped categories still log — they just show in surfaceVariant until you file them."
- **Add-to-group sheet** (row 4 right / row 5 far-right): pick an existing group (adopts its role immediately) or "New group…" (jumps to group creation with this category pre-filled). Past entries preserved: "filing changes its colour and where it appears, nothing else."

## Files

| File | Change |
|---|---|
| `ui/screens/categories/ManageCategoriesScreen.kt` | Restructure into the two-tab view; add group cards + add-to-group sheet + new-group flow. Reuse `SegmentedToggle`, `ListCard`, `RolePicker`. |
| `ui/screens/categories/ManageCategoriesViewModel.kt` | Add groups Flow, group CRUD, assign/unassign, tab state. |
| `ui/navigation/Screen.kt` / `MainActivity.kt` | If group create/edit is a separate destination, add a route; a bottom sheet in-place is also fine. |

## Existing behaviour to keep reachable (map 02 §4)
Archive / unarchive, reorder, delete (system protected, delete-with-history), rename, icon + colour edit, numeric/scale settings entry, allow-multiple / show-in-period / track-against-time toggles, **tracking modes** (`modeKey` presets), **quick-log** config, and the link into **`ManageCategoryValues`** for value editing. None of these may disappear behind the redesign; re-home them into the group/category rows and the edit flow (edit-flow detail is Phase 7).

## Acceptance criteria
- [ ] Create, rename, recolour (role), reorder, and delete groups; deleting a group unfiles its categories (never deletes them).
- [ ] File / unfile a category via the add-to-group sheet; an inherit-category adopts the group role live.
- [ ] Grouped and Ungrouped tabs both render correctly and swap in place.
- [ ] Every pre-existing management action is still reachable and works.
- [ ] `a11y_check.py` green (segmented = `Role.RadioButton`; rows = `Role.Button`; sheet dismiss = `Role.Button`).

## Feature-preservation checklist
- [ ] Archive/unarchive, reorder, delete-with-history, system-category protection.
- [ ] Tracking modes still present and functional.
- [ ] Quick-log config + Quick Log widget unaffected.
- [ ] `ManageCategoryValues` still reachable; value CRUD + rename-with-history intact.
- [ ] Category counts in the header ("8 categories · 3 groups") reflect reality.

## Gotchas
- `ManageCategoriesScreen.kt` is large and already dense. Prefer extracting the group-card and category-row into private composables (or shared primitives) over growing one function.
- Reorder within a group vs global order: decide whether `displayOrder` is per-group or global. Simplest: keep global `displayOrder` on categories, add `displayOrder` on groups; render grouped by `groupId` then category `displayOrder`.
- Don't regress the existing colour/icon pickers — they move into the Phase 7 edit flow, but until then keep them working here.

## Changelog fragment
```json
{ "bump": "minor", "changed": ["Redesigned the What You Track screen with grouped and ungrouped views and inline group management"] }
```
