# Transferable Lessons

Keep entries short and principle-focused — the "why" matters more than the project-specific "what". Merge or remove entries if they become redundant.

Entries within each section are ordered by risk to a new project if forgotten: build failures and data loss first, UX degradation last.

---

### Android / Compose

**`SwipeToDismissBox`: use `confirmValueChange` returning `false` to intercept — not `LaunchedEffect` + `reset()`**
When a swipe should show a confirmation dialog before committing, the natural-looking approach is `confirmValueChange = { true }` (allow the state change) then call `state.reset()` in the dialog's Cancel handler. This causes two bugs: (1) if the composition survives navigation, the `LaunchedEffect` key hasn't changed on return so the dialog silently re-appears; (2) `reset()` takes one animation frame, leaving a brief window where swiping is disabled. The correct pattern is `confirmValueChange = { newValue -> if (newValue == EndToStart) { showConfirm = true; false } else true }`. Returning `false` rejects the transition entirely — the box springs back immediately, no `reset()` call is needed, and the dialog fully controls the outcome. Remove the `LaunchedEffect` and the coroutine scope from the composable.

**`SwipeToDismissBox` `backgroundContent` clips to a rectangle — clip it to match the foreground card's shape**
`backgroundContent` in `SwipeToDismissBox` fills the full layout bounds, which is a rectangle. If the dismissible card uses rounded corners (e.g. Material 3 `Card` with `MaterialTheme.shapes.medium`), the coloured background bleeds outside those corners and appears as a square halo. Fix: add `.clip(MaterialTheme.shapes.medium)` as the first modifier on the `Box` inside `backgroundContent` to confine it to the same shape.


Compose's high-level interactive components (Button, IconButton, Card with onClick, etc.) declare their role automatically. Any element that uses a raw `.clickable {}` modifier instead — typically a Row, Box, or ListItem acting as a button — has no role by default and is therefore unreachable by keyboard navigation, switch access, and TalkBack's explore-by-touch linear mode. Always append `.semantics { role = Role.Button }` (or `RadioButton`, `Switch`, `Checkbox`) after `.clickable {}`. For toggle controls, also set `stateDescription` to the current state string (e.g. "Expanded") so TalkBack announces the result of the tap, not just the label. Pattern: `Modifier.clickable { … }.semantics { role = Role.Button; stateDescription = "…" }`.

**Status changes need `liveRegion` — visibility changes alone are silent to screen readers**
When text appears or changes in response to user action (error messages, validation hints, loading confirmators, counts), screen readers only notice if the node carries `liveRegion = LiveRegionMode.Polite` (for non-urgent updates) or `LiveRegionMode.Assertive` (for errors that must interrupt). A Composable that conditionally adds a `Text` to the tree on error will recompose visually but TalkBack will not announce it without the live region. Apply the modifier to the Text itself, not to a wrapper: `modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }`.

**Icon-only FABs and SmallFABs need an explicit `contentDescription` on the container, not just the icon**
When a SmallFloatingActionButton contains an Icon with `contentDescription = null` and its label lives in an adjacent Surface (as in a speed-dial layout), TalkBack focuses on the FAB alone and reads nothing. Setting `contentDescription` on the Icon inside fixes the raw Icon composable but TalkBack still reads the FAB as unlabelled if the two are in separate composable trees. The reliable fix is `Modifier.semantics { contentDescription = label }` on the FAB itself, which takes precedence.

**Inline text links: use `LinkAnnotation` (`withLink`), not the deprecated `ClickableText`**
`ClickableText` is deprecated and, more importantly, gives the tappable span no link role — screen readers announce it as plain text, so a sighted-only "underlined = tappable" cue is the only affordance, and the hit region is a single tap offset rather than the whole phrase. Build the link with `withLink(LinkAnnotation.Url(url, styles = TextLinkStyles(...)) { handle })` inside `buildAnnotatedString`, then render with a plain `Text`. This announces the span with the correct link role, exposes a proper touch region, and lets a custom handler launch an Intent instead of relying on the default URL open. Available from Compose 1.7+.

**`ModalBottomSheetProperties` requires all parameters explicitly in Material3 1.2.x**
The constructor has no default values in this version — passing only `shouldDismissOnBackPress` fails to compile. Always supply all three: `securePolicy = SecureFlagPolicy.Inherit, isFocusable = true, shouldDismissOnBackPress = false`. `SecureFlagPolicy` also needs an explicit import from `androidx.compose.ui.window`.

**Parallel write paths must each respect every category setting**
When two code paths write to the same store (e.g. `LogPeriodViewModel.syncSymptomsToTrackingLog` and `LogCategoryViewModel.save` both writing to `tracking_logs`), each path must independently read and apply every relevant category flag. If a new flag is added (like `trackAgainstTime`) and only one path is updated, the other silently ignores the setting. When adding a per-category behaviour flag, grep for all call sites of the underlying `saveLog` / `updateLogInPlace` and confirm they all handle the new flag.

**Room generates no SQLite DEFAULTs without `@ColumnInfo(defaultValue=…)` — fresh-install seeds rot as the schema grows**
Room emits `NOT NULL` with no SQL `DEFAULT` for every entity field that lacks a `@ColumnInfo(defaultValue=…)` annotation. Migrations protect existing users because `ALTER TABLE … ADD COLUMN … DEFAULT …` always supplies a value. The `onCreate` seed INSERT is hand-written and must list every column explicitly — omitting any `NOT NULL` column causes a constraint violation on first open, crashing the app before any screen is shown. Two defences: (1) enumerate all non-PK columns in seed INSERTs; (2) annotate every entity field with `@ColumnInfo(defaultValue=…)` so Room's generated DDL also includes SQL `DEFAULT` clauses and the two stay in sync automatically.

**Commit a stable debug keystore to the repo**
Android generates a fresh debug keystore per machine/CI runner. Without a committed keystore, every CI build has a different signature and OTA updates are blocked — users get a "conflicting package" error and must uninstall first. Commit a single debug keystore and wire it into `signingConfigs.debug`.

**`ACCESS_NOTIFICATION_POLICY` must be in the manifest for the app to appear in the DND access list**
Android populates Settings > Apps > Special app access > Do Not Disturb from the manifest declaration alone — it does not discover the app from runtime API usage. Without `<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />`, the app is simply absent from that list, so users can never grant the access that makes `setBypassDnd(true)` on a notification channel effective. The runtime check (`isNotificationPolicyAccessGranted`) and any UI prompting the user to grant access are both silently pointless until the declaration is present.

**Time-critical notifications need the alarm audio stream, not the notification stream**
A notification channel created without explicit `AudioAttributes` plays at notification volume and respects Do Not Disturb — both of which users routinely silence. For any reminder that must be heard (alarm, timer, urgent alert), call `setSound(uri, AudioAttributes(USAGE_ALARM))` on the channel so it plays at alarm volume and bypasses Do Not Disturb. Channel properties are written once and then immutable — changing stream type requires a new channel ID, since the OS ignores `createNotificationChannel()` for properties on an existing channel.

**`onDismissRequest = {}` causes a stuck invisible sheet overlay**
A no-op `onDismissRequest` lets the sheet animate to its hidden state (e.g. via swipe-down), but the parent `show` flag stays `true` so the composable stays in the tree. The result: an invisible `ModalBottomSheet` overlay that blocks all touches behind it, with no way to re-open or dismiss it. Fix: bounce the sheet back to expanded in `onDismissRequest` using `sheetScope.launch { sheetState.show() }`. This preserves data-loss protection while preventing the stuck state.

**Prevent accidental dismissal of data-entry sheets**
`ModalBottomSheet` dismisses on backdrop tap and back gesture by default. For any sheet containing a form, set `onDismissRequest = { sheetScope.launch { sheetState.show() } }` and `properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)`. The only exit paths should be an explicit close button and a save/submit button.

**Hoist SheetState above the composable that uses it**
If `SheetState` (or similar stateful objects) is created inside a composable, it gets reset on recomposition. Hoist it to the parent screen so it survives the child composable's lifecycle. This also allows the parent to programmatically show/hide the sheet without losing form state.

**Never hardcode colours in TextStyle / typography**
Hardcoded colours in `TextStyle` entries override Material3's `LocalContentColor`, breaking contrast in non-default themes. Always omit `color` from `TextStyle` and let the theme propagate it.

**Alias `android.graphics.Color` when mixing with Compose `Color`**
Both `android.graphics.Color` and `androidx.compose.ui.graphics.Color` are named `Color`, causing an unresolvable conflict. Always import the Android one with an alias: `import android.graphics.Color as AndroidColor`. Use `AndroidColor.colorToHSV(argb, hsvArray)` and `AndroidColor.HSVToColor(hsvArray)` for HSV conversion; Compose's `Color(argb: Int)` constructor wraps the result into a Compose `Color`. This pattern is the idiomatic way to build custom HSV colour pickers in Compose.

**M3 `NavigationBarItem` default indicator is `secondaryContainer` — use `primary` in High Contrast themes**
The default `indicatorColor` for `NavigationBarItem` is `secondaryContainer`, which in high-contrast colour schemes may have almost no contrast against the `NavigationBar` surface. Override it with `NavigationBarItemDefaults.colors(indicatorColor = colorScheme.primary, selectedIconColor = colorScheme.onPrimary, selectedTextColor = colorScheme.primary)` to ensure the active tab is clearly visible in all themes.

---

### UI / UX

**Never ship partially visible controls**
`LazyRow` (and similar clipping containers) gives no affordance that content is hidden — it just clips silently. A partially visible chip or control looks like a bug and may hide required input. For short chip groups (5–8 items) in a vertical-scroll form, use `FlowRow` (wraps to next line) so nothing is hidden. Apply `@OptIn(ExperimentalLayoutApi::class)` per composable.

**Half-implemented validation is worse than no validation**
A guard that sometimes fails to catch bad input creates false confidence — users stop double-checking because they believe the app is doing it for them. If a feature is meant to prevent a harmful outcome (data corruption, wrong entry, unsafe action), implement it completely or don't ship it. A partial check that occasionally passes bad data is the worst outcome.

**UI feedback must always reference the current action**
If actions can occur in rapid succession, snackbars or toasts queue up. An UNDO or CANCEL on what *appears* to be the latest notification may actually target an earlier queued one. Cancel the current notification before showing the next so feedback always belongs to the most recent action.

**Dark theme colour tokens drift if not pinned to exact hex values**
Colour values in dark themes can drift incrementally (e.g. toward olive or gray) without anyone noticing the change in isolation. Treat all design token hex values as spec, not preference — any change is a deliberate decision reviewed against the full palette. Regular visual QA against the canonical token table catches drift before it compounds.

**Separate immediate completion feedback from long-term progress**
A "done today" indicator (e.g. 3 of 4 tasks complete) and a long-term progress indicator (e.g. level or streak progress) serve different cognitive needs — immediate reinforcement vs. sustained motivation. Conflating them in a single bar weakens both signals and makes it hard for the user to read their current state at a glance.

**Reduce border opacity to lower visual weight — don't remove borders**
Removing chip, text-field, or button borders entirely loses the affordance that a control is interactive. Setting unfocused / unselected border colour to ~40 % opacity (`outline.copy(alpha = 0.4f)`) reduces visual noise and improves form hierarchy without sacrificing interactivity cues. Apply consistently across all interactive controls on the same screen so they share a single visual weight tier.

**Section labels and values need different visual tiers — colour is the cheapest separator**
When a form has section labels ("Flow", "Symptoms") and entered values ("Medium", "2 hours"), both at the same font size and colour, nothing reads as primary. Applying `onSurfaceVariant` to labels (without changing their size) immediately pushes them into a supporting role and lets the values — already in the accent/primary colour — become the visual hero. No size changes needed; colour difference alone establishes the hierarchy.

---

### Data / State

**Gate prediction display on window end, not window start**
A prediction window (e.g. a 5-day expected period) should remain visible as long as any part of the window is current — gate on `windowEnd >= today`, not `windowStart >= today`. Gating on the start collapses the display to zero the moment the window begins, which is precisely when it matters most. Apply the same principle to any "active range" feature: fertility windows, ovulation windows, reminders that span multiple days.

**Don't double-count with offset when SQLite immediately reflects inserts in aggregate queries**
In a Room migration loop that inserts rows one-by-one, calling `MAX(displayOrder)+1` in a subquery correctly reflects all previously-inserted rows in the same transaction — SQLite is not a snapshot. Adding a separate `offset` counter on top of that result double-counts and produces gaps (e.g., displayOrder 7, 9, 11 instead of 7, 8, 9). Remove the offset variable and let the `MAX+1` subquery self-increment across the loop.

**Date-range entities with an "ongoing" (null end) state need an existence check before quick-log entry points create a new record — and an adjacency check once the range is fixed**
When an entity represents a date range and a null end date means "ongoing, extends through today" (e.g. an active period), any UI shortcut that creates a *new* record for a tapped date (calendar tap, FAB, speed dial) must first check whether that date already falls within an existing record's range. Otherwise the user ends up with two overlapping "ongoing" records for what they consider a single continuous span. Add a shared `recordForDate(records, date)` helper that treats a null end as `today`, and route quick-log entry points through it: if a match exists, navigate to edit it; otherwise create new. This "coverage" check alone is not enough: the moment a record's end date is fixed (set explicitly, or via any confirm-dialog default that isn't the date the user meant), logging the very next day no longer falls inside any range and silently starts a second, disconnected record — with the day in between unreachable in either one. Extend the same entry point with an adjacency check: a tapped date exactly one day after some record's end (or one day before another's start) should extend that record instead of creating a new one; a date that bridges a one-day gap between two records should merge them. Also give the user a manual "merge with adjacent record" action in the list/history view as a backstop for gaps the auto-adjacency logic can't infer (larger gaps, pre-existing fragmented data).

**"Ongoing" should mean unbounded, not "through today" — and adjacency needs its own check, separate from containment**
A `recordForDate` helper that treats a null end date as `LocalDate.now()` only works while "today" is on or after the date being checked, and only reads clean while a session stays open past midnight without new writes. Reopen the app on a later date, or check a future date, and the same ongoing record silently stops matching — a fresh "new record" branch fires instead of "extend existing," fragmenting what the user considers one continuous span. Treat the ongoing case as having no upper bound at all (any date `>= start` matches) instead of substituting `now()`. Separately, containment (`start <= date <= end`) only catches dates *inside* a closed record's range — it does nothing for the day immediately *after* a closed record's end, which is the single most common way these entities fragment in practice (a user who closes a date-range record explicitly each day, rather than leaving it open, and logs again the next day). Add `date == end.plusDays(1)` as an explicit adjacency match alongside containment; don't assume containment implies "continues."


**Insert/upsert flags need a separate edit-by-ID path**
A flag like `allowMultiple` controls whether saving a log upserts an existing row (keyed by date + category) or always inserts a new one. Neither branch handles "update this specific existing row by ID." Routing an edit through `allowMultiple = false` works only when the existing row is uniquely keyed by the natural key; using `allowMultiple = true` creates a duplicate instead. The correct pattern is a dedicated `updateInPlace(existingLog, …)` method. Callers check `existingLog != null` and take this path directly, bypassing the insert/upsert decision entirely.

**Remap all foreign keys on data import**
When importing data that generates new primary IDs (e.g. JSON/CSV restore), every foreign key referencing those IDs must also be remapped. Importing parent records with new IDs but leaving child records pointing at old IDs silently breaks relational integrity.

**Toggling a category's storage representation orphans previously logged values**
When a category can switch modes (e.g. Flow's "default" chip mode vs "numeric_slider" mode), each mode stores `TrackingLogValue.valueLabel` differently: chip mode stores a label string ("Medium"), slider mode stores a numeric step ("3"). Any reader that assumes one representation (e.g. `toFloatOrNull()` for numeric categories) silently drops every log written under the old mode via `continue`/`?:`, making historical entries vanish from charts and grids without an error. When a numeric category has `scaleLabels` (step -> label), add a reverse-lookup fallback so old label strings still resolve to their numeric step.

**A `null` "not yet interacted" state must not be read as "no value to save"**
When a UI shows a default value (e.g. a slider rendered at `numericMin` via `value ?: min`) but the backing state field stays `null` until the user actively changes it, a save handler that does `val v = state.value ?: return` silently no-ops for anyone who accepts the displayed default without touching the control. The UI looks identical (same value shown) whether or not the user interacted, so there's no visual cue that "Save" did nothing. Fix at the save site by falling back to the same default the UI displays (`state.value ?: category.min`), not by requiring interaction.

**Store the per-event delta on the event record, not only in the running aggregate**
When an action adds to a running total (points, balance, count), store the per-event amount on the event itself. Every deletion and undo path can then read and subtract that stored delta, keeping the aggregate in sync. An aggregate that's only ever incremented drifts away from the true value over time — the only reliable fix is a symmetric decrement path that reads from the event record.

**`remember` state resets on every tab switch — use ViewModel `StateFlow` for session-persistent UI state**
In a Compose Navigation graph with a bottom nav bar, navigating between tabs destroys and recreates each destination composable. Any state held in `remember { mutableStateOf(...) }` resets on every tab switch. For UI state that should persist within a session (e.g. a banner was dismissed, a filter was applied), hoist it into the ViewModel as a `MutableStateFlow` — the ViewModel survives recomposition. Reserve `remember` for truly ephemeral UI state (hover, in-flight animation) that is fine to lose.

**SQL `:param IS NULL` in a parameterised `OR` condition matches every row when the param is null**
A Room DAO query like `AND (:id IS NULL OR col = :id)` evaluates to `AND TRUE` when `:id` is null, returning all rows rather than only those where `col IS NULL`. This silently broadens the result set in ways that are hard to spot in testing. Fix by splitting into separate query methods — one for the null case and one for the non-null case — or handle the branch in application code before calling the DAO.

**When a save bug corrupts a primary field, use its mirror store as the repair source**
If a field is mirrored to a secondary store (e.g. `PeriodEntry.flowLevel` is also written to `TrackingLog`), a bug that writes a blank value to the primary record leaves the secondary intact. The forward backfill that populates the secondary from the primary will skip blank rows, so the secondary stays correct. A one-time reverse migration — reading the secondary and writing back to the primary — is the right repair: it is idempotent (skip if already non-blank), guarded by a preference flag, and requires no schema change. When mirroring data across stores, make sure the secondary write path is independent of the primary so it succeeds even when the primary is corrupted.

**Non-reactive suspend calls inside a `combine` lambda silently break reactivity**
Calling a `suspend` DAO function inside a `combine { }` transform reads data once at emission time and never again. If the queried table changes, the outer flow won't re-emit. Fix: promote the query to a `Flow` and include it as an additional `combine` argument so the pipeline re-fires on every table change.

**Design data models for anticipated features before you need the UI**
For features you know are coming (e.g. user export, audit logging, sharing), design the schema to accommodate them even if the UI isn't built. Retrofitting relational structure after data has accumulated is expensive and risky; an extra nullable column now costs nothing.

**Store raw values; enrich for display at render time**
Numeric values stored in the database should be raw (e.g. `"3"`, `"2.0"`) so they stay usable for stats, aggregation, and future transforms. Unit suffixes ("hours", "explosions") and scale-label mappings ("3" → "What's my name again?") belong only at the display layer, resolved by a single `enrichDisplayValue(rawValue, category)` function called just before the `Text` composable renders. This keeps storage stable, lets stats code operate directly on numbers, and means a unit change or label edit is reflected immediately without a data migration.

**Chip selected states must be unambiguous at a glance**
The default Material3 `FilterChip` selected treatment (slightly brighter text, subtle border change) requires interpretation — it fails the "readable in 100ms" bar. Override `FilterChipDefaults.filterChipColors(selectedContainerColor, selectedLabelColor)` with a high-contrast fill (e.g. amber + dark text) to make selection state immediately obvious. Encapsulate this in a shared `FormChip` wrapper so the treatment is consistent everywhere.

**Thick colored outlines read as alerts, not selections**
High-contrast colored borders (primary-colored, 2 dp or more) on chips and tiles carry the visual semantics of form-validation errors, not selection. Users must decode them rather than read them. Use filled containers for selection state — fill is visually distinct from the outlined-means-warning convention. Reserve thick colored outlines for genuine warnings or required-field indicators.

**One selection language per screen**
Using different visual paradigms for "selected" across components on the same screen (filled pill for one, thick colored border for another, filled tile for a third) forces users to decode three visual languages at once. Pick one treatment — fill-based or outline-based — and apply it to every selectable element. Spot-check by covering the labels and asking whether you can still tell what is selected and how it differs from its neighbours.

**All section headers must sit at least one weight step above body text**
When every card or section header uses the same style as its neighbours — even if it is technically a heading style — the screen reads as a flat wall. Use at minimum `titleMedium` for section headers and `bodyMedium` for content within a card; the step between them is what creates scannability. If headers and body text look the same in a quick squint test, the hierarchy is broken.

**Key configuration summaries need a visual container**
State that captures the user's entire current configuration (e.g. "X axis = Category A · Y axis = Category B") carries more decision weight than inline annotation text implies. Burying it as low-style inline text treats the most important context on the screen as incidental. Wrap configuration summaries in a surface container (even a subtle `surfaceVariant` pill) so they read as a distinct UI element rather than a label.

---

### Code Quality / Review

**Branch protection blocks force push — use merge, not rebase, for conflict resolution**
When a branch is protected against force push and upstream has moved on, `git rebase origin/main` rewrites local history that can no longer be pushed. The only forward path is `git merge origin/main`, which creates a merge commit but preserves the existing remote history. If both branches claimed the same version string, resolve by bumping the lower-priority branch's version upward in the same merge commit — don't leave the version collision for the reviewer to spot.

**A parameter present in a function signature but never forwarded at the call site**
A function may accept a flag (`wcag: Boolean = false`) and correctly wire it through internally, yet if the call site omits it the flag silently takes its default for every caller. Function signature looks correct, internal logic looks correct — only the gap between them is wrong. This is especially common in theming chains, feature flags, and composable parameter cascades where defaults mask the omission. When adding a parameter to a shared function, grep all call sites and verify each one explicitly passes the new argument.

---

### Auth

**Auth state transitions need explicit guards**
Don't assume UI flow enforces auth invariants. Guard at the data layer: block enabling biometric if no PIN exists; clear dependent auth factors when a prerequisite is removed. Silent auth gaps (biometric enabled, PIN removed, lock screen never triggers) are worse than a visible error.
