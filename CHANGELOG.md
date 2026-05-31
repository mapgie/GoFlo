# Changelog

## Versioning policy

Format: `MAJOR.MINOR.PATCH-beta.N` (pre-release) or `MAJOR.MINOR.PATCH` (release).

| Bump | When |
|---|---|
| MAJOR | Breaking change, destructive DB migration (data loss risk), incompatible backup format |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation |
| PATCH | Backward-compatible fix: bug fix, copy change, perf improvement, internal refactor |

Rules:
- MINOR bump resets PATCH to 0 (`1.4.2 → 1.5.0`); MAJOR resets MINOR and PATCH (`1.4.2 → 2.0.0`)
- Increment `versionCode` by 1 and update `versionName` in `app/build.gradle.kts` with every PR — no exceptions
- Add a changelog entry in the same commit as the feature/fix
- Released versions are immutable — never re-tag, never amend, never delete an entry
- Merge conflicts must preserve both sides; if both branches used the same version string, renumber the lower-priority one upward

---

## [0.14.0-beta.4] - 2026-05-31

### Changed
- **Navigation — Settings moved to top app bar** — the Settings tab has been removed from the bottom navigation bar. A gear icon button in the top-right of the Home screen app bar now opens Settings, keeping the bottom bar focused on the three core destinations: Home, History, and Stats.
- **Settings — flat Material list layout** — the expandable accordion card layout has been replaced with a flat Material `ListItem` layout. Each section entry is a dense list row; tapping it opens a dedicated sub-screen with its own top app bar and back arrow, rather than expanding inline.
- **Settings — section headers** — headers (TRACKING, NOTIFICATIONS, etc.) are now `SemiBold`, rendered in the primary colour, with increased top padding (20 dp) to clearly separate grouped items.
- **Settings — navigation items** — items that open a sub-screen show a trailing Chevron Right icon. Items that control a binary setting show a trailing Material Switch that toggles immediately without navigating.
- **Settings — section dividers** — `HorizontalDivider` separates the major setting groups (Tracking, Notifications, Personalisation, Privacy & Data, Widgets, Help & Feedback, About).
- **Settings — icon alignment** — leading icons are constrained to 24×24 dp bounding boxes for consistent vertical alignment across all list items.
- **Settings — standardised background** — list items use the `surface` container colour, removing the "boxed" `surfaceVariant` card look of the old layout.

---

## [0.14.0-beta.3] - 2026-05-31

### Changed
- **FAB — icon-only at rest** — the floating action button now shows only the Add icon when the speed dial is closed, removing the "Log…" label from the resting state so the button feels like a proper FAB rather than a labelled afterthought.
- **FAB — expands on open** — tapping the FAB opens the speed dial and simultaneously expands the button to show the Close icon alongside a "Log" label (ellipsis removed); font is `titleMedium` to match the visual weight of the icon.
- **FAB — Comfortaa typeface on label** — the "Log" label uses the GoFlo brand font (Comfortaa Bold) for consistency with the rest of the app's typographic identity.

---

## [0.14.0-beta.2] - 2026-05-31

### Changed
- **Stats screen — unified selection language** — category chips now use filled `primaryContainer` (X axis) and `secondaryContainer` (Y axis) containers instead of thick primary/secondary-coloured outline borders. All three selector sections (time range, category chips, chart type tiles) now share the same filled-container selection language; the variable-width coloured borders are gone.
- **Stats screen — section header hierarchy** — "Time range", "Pick up to 2 categories", and "Chart type" headers promoted from `titleSmall` to `titleMedium` so sections are scannable rather than reading as a flat wall of equal-weight text.
- **Stats screen — axis summary readout** — the X/Y axis configuration summary ("X: Category A · Y: Category B") now sits in a `surfaceVariant` pill with `labelLarge` text, giving it visual weight proportional to its importance instead of appearing as incidental annotation.
- **Stats screen — chart type tiles** — tile width reduced from 100 dp to 80 dp; the fourth tile no longer clips on standard screen widths.

---

## [0.14.0-beta.1] - 2026-05-30

### Added
- **Track against time** — a new per-category toggle ("Track against time" in the category edit screen) records the HH:mm timestamp alongside every log entry. When enabled: the log screen shows a pre-ticked time checkbox; Plus One categories log each tap as a separate timestamped entry (shown as a live list with delete buttons); the Day Log sheet gains a 3-dot "Display logs against time" option to view entries in chronological "13:37 Heavy" / "14:56 Medium" format (only visible when a category has time data).
- **Stats — Over Time chart** — numeric categories now offer an "Over Time" chart in the Stats screen. Two numeric categories together also support this view alongside the existing Scatter and Dual Time Series options.

### Fixed
- **Period log save-on-back** — pressing back (or swiping) while creating or editing a period entry now prompts "Save" or "Discard" instead of silently discarding any changes.
- **Flow / Symptoms section labels** — the Log Period screen now shows your user-renamed names for Flow and Symptoms (rather than the original hardcoded labels).
- **Multiple-value day view** — categories that allow multiple logs per day now show all values joined inline (e.g. "Heavy · Clots") in the Day Log sheet. Time-tracked categories show the stacked time view instead when "Display logs against time" is enabled.
- **Feedback banner** — link text shortened to "Feedback encouraged."
- **Quick-tap Plus One with time tracking** — tapping a Plus One category from the home speed dial now saves a timestamped entry when "Track against time" is on, and undo correctly deletes that entry rather than decrementing a counter.

### Changed
- Database schema bumped to v12 (backward-compatible `ALTER TABLE` migration adding `trackAgainstTime` to `tracking_categories` and `loggedAt` to `tracking_logs`; existing data unaffected).

---

## [0.13.8-beta.1] - 2026-05-30

### Changed
- **Day view shows units and scale labels** — numeric category values in the day sheet now include context alongside the number. Categories with a unit (e.g. "hours", "explosions") display as "2 hours" or "3 explosions". Slider categories with scale labels show the label text instead of the raw number (e.g. "What's my name again?" instead of "3"). Raw numbers are still used for stats charts and data storage; this is a display-only change.

---

## [0.13.7-beta.1] - 2026-05-30

### Fixed
- **Build error in DayLogSheet** — `Alignment.Baseline` is not a valid Compose `Row` vertical alignment; replaced with `Alignment.CenterVertically` in `AttributeValueLine`.

---

## [0.13.6-beta.1] - 2026-05-30

### Changed
- **Daily log view hierarchy inverted** — the logged value is now the visual focus on the day sheet. Category names shrink to muted supporting labels, single values display in `titleMedium` text coloured with the category accent, multi-value entries render as inline dot-separated text, and the edit action recedes to a small ghost button. Replaces `ChipRow` + `CategorySectionHeader` with `LogEntryRow` and `AttributeValueLine`.

---

## [0.13.5-beta.1] - 2026-05-30

### Changed
- **Log Period hierarchy** — section labels now render in `onSurfaceVariant` so they recede behind entered values; slider value text promoted from `headlineMedium` to `headlineLarge` (32 sp Bold) making "2 hours", "1 Clear Head" etc. clearly primary information
- **Tighter tracking cards** — slider and increment card padding reduced (20/16 → 16/10-12 dp); main column section gap tightened 20 → 16 dp so containers feel deliberate rather than spacious-but-empty
- **Unified border contrast** — unselected `FilterChip` and `AssistChip` borders, `OutlinedTextField` unfocused borders, and `OutlinedButton` borders all reduced to 40 % outline opacity for a coherent visual weight across controls; selected chip border removed (transparent) to eliminate the box-inside-a-box effect

---

## [0.13.4-beta.1] - 2026-05-30

### Fixed
- **Unsaved changes lost on back navigation** — editing a Numeric Slider or Numeric Input category and pressing back (or using the system gesture) without tapping Save now shows a dialog offering **Save** or **Discard**. Tapping outside the dialog keeps you on the screen.

---

## [0.13.3-beta.1] - 2026-05-30

### Changed
- **Palette icon is aesthetics only** — the Edit Appearance dialog (palette icon on each category row) now contains only the icon picker and colour picker. Name, type, and behaviour settings have been removed from it.
- **Category settings consolidated into the edit screen** — "Log with period" and "Allow multiple per day" toggles now live at the top of the category edit screen for all custom category types (default, slider, free-input, increment). Both auto-save on change with no Save button required.
- **Plus One — "Allow multiple per day" hidden** — the toggle is not shown for increment categories in the creation dialog or edit screen; the increment model always records a single running count per day regardless of this flag.

### Added
- **Rename default categories** — Flow and Symptoms can now be renamed via the pencil icon on their edit screen. A stable internal key (DB v11, `systemKey` column) is used for all system lookups so flow sync continues to work after a rename.

### Fixed
- **"Allow multiple" stale state on category creation** — switching to Plus One type after enabling "Allow multiple" in the creation dialog no longer persists the flag; it is clamped to false at save time when the final type is increment.

---

## [0.13.1-beta.1] - 2026-05-29

### Added
- **Plus One in Log Period** — increment-type categories pinned to the Log Period screen now show a counter with + and − buttons, matching the dedicated log screen.

### Fixed
- **App crash on fresh install** — a SQLite NOT NULL constraint violation in the database seed function caused the app to crash immediately after installation. Room's generated schema has no DEFAULT clauses for columns without `@ColumnInfo(defaultValue=…)`, so all columns must be supplied explicitly in the initial INSERT; the seed now provides values for all 14 non-PK columns. Upgrading from v0.12.5 or v0.13.0 is unaffected.
- **WCAG accessible toggle had no effect** — the WCAG mode flag was never passed to `GoFloTheme`, so toggling it in Settings did not change the colour scheme. Enabling "WCAG accessible" now correctly applies the high-contrast palette variant.
- **Plus One category in "Manage category" screen** — opening the settings screen for a Plus One category showed the text-values editor ("+ Add a value") instead of an informational message. It now explains that Plus One categories use a running count rather than predefined values.
- **Editing a tracking log added a new entry** — tapping Edit on a log entry from the Day Log sheet and saving wrote a second log row instead of updating the existing one. The log screen now updates the original row in-place regardless of the "Allow multiple per day" setting.
- **Default launcher icon reset** — the icon preference defaulted to "LEAF" but the manifest enables "DEFAULT" on installation, causing the icon to switch to the Leaf variant on every cold start. The preference now defaults to "DEFAULT".

---

## [0.13.0-beta.1] - 2026-05-29

### Added
- **Slider scale labels** — each whole-number step on a Slider category can now be given a text label (e.g. 1 = Good, 3 = Neutral, 5 = Bad). Labels are set in Settings → Tracking Categories → [category] and appear below the current value on both the log screen and the Log Period screen. Stats distribution charts also use these labels on the value axis.

### Changed
- Database schema bumped to v10 (backward-compatible `ALTER TABLE` migration adding `scaleLabels` column; existing data is untouched).

---

## [0.12.5-beta.1] - 2026-05-28

### Added
- **Log with period** — custom tracking categories can now be pinned to the Log Period screen. Enable "Log with period" in the category's create or edit dialog; the category then appears as a section (chip picker, slider, or text input depending on type) between Symptoms and Notes on the Log Period screen. Selections are saved as tracking logs for the period start date when the period entry is saved.

---

## [0.12.4-beta.1] - 2026-05-28

### Changed
- **Home screen FAB → M3 speed dial** — the "Log…" pill button is replaced by an Extended FAB that expands a speed dial on tap: small FABs with labels appear above it (Log Period at top, each tracking category below), a 32 % scrim covers the rest of the screen, and the FAB icon animates between + and ✕. Tapping outside the menu or the Close button collapses it. "Log more…" from the Day Log sheet closes the sheet first, then opens the speed dial for that specific date.

---

## [0.12.3-beta.1] - 2026-05-28

### Fixed
- **Numeric category toggle stuck ON** — the redundant "Numeric input" toggle inside the category creation and edit dialogs is removed; the type selector (Default / Numeric Slider / Numeric Input) already controls this, and the toggle had no effect.
- **No way to log a category multiple times per day** — a new "Allow multiple per day" setting (in both the create and edit dialogs) lets numeric and text categories be logged more than once on the same day; existing categories default to single-entry behaviour.
- **Save in category settings didn't navigate back** — tapping Save on the Numeric Slider and Numeric Input settings screens now returns to the category list immediately.

---

## [0.12.2-beta.1] - 2026-05-28

### Fixed
- **Changelog line breaks** — hard-wrapped continuation lines in changelog entries no longer render as separate text items in the "What's New" dialog; each bullet is now a single unbroken line.
- **No way to revert custom launcher icon** — the icon picker now shows a "GoFlo" option (the original coral drop icon) at the top so users can switch back to the default after choosing a discreet icon.

---

## [0.12.1-beta.1] - 2026-05-28

### Fixed
- **Launcher "app has a bug" pop-up** — `AppIconManager.applyIcon` now checks the current component state before calling `setComponentEnabledSetting`; no-ops when the state is already correct so the launcher is never notified on every cold start.
- **Changelog showing raw markdown** — the "What's New" dialog now renders `### headings`, `- bullets`, and `**bold**` text instead of displaying raw syntax characters.
- **Delete All Data only deleted periods** — the action now also removes all tracking log entries. Category definitions and their value options are preserved.
- **Import only imported periods** — importing a v2 JSON file now also restores tracking logs; categories are matched by name and created if missing; existing logs are skipped in merge mode.
- **Numeric category → management screen shown unnecessarily** — after creating a numeric category all settings are already captured in the creation dialog; the app no longer navigates to the values management screen. Default categories still navigate there so the user can add their value options.
- **Numeric category creation discarded min/max/decimals** — the values entered in the creation dialog are now passed through to the repository correctly.

### Changed
- **Export dialog** — the "What to include" section now shows chips instead of checkboxes, matching the date-range and format sections and preventing content overflow.

### Added
- **Home screen — tap month name to jump** — tapping the month/year label in the calendar header opens a scrollable month picker (3 years back, 1 year forward) for fast navigation.
- **Stats screen — tap range label to re-pick** — when Year or Month view is active, tapping the displayed range label reopens the picker without requiring a re-tap of the segmented button.

---

## [0.11.0-beta.1] - 2026-05-27

### Added
- **Configurable data export** — the two fixed "Export JSON / Export CSV" buttons are replaced by a single "Export Data" button that opens a dialog. Users can now choose a date range (all time, last 3/6/12 months, or custom from–to dates), select which categories and period data to include, and pick the output format (JSON or CSV).
- Exported JSON (v2) wraps everything in a versioned object with separate `periods` and `tracking` sections. Import remains backward-compatible with the original array format.
- Exported CSV separates periods and tracking logs into labelled sections within a single file.

### Fixed
- **History delete lost on navigation** — swiping to delete a period then navigating away before the Undo snackbar timed out silently cancelled the deletion; the period reappeared after an app restart. The DB delete now happens immediately inside `viewModelScope` so it completes regardless of navigation. Undo re-inserts the full period and symptoms from an in-memory cache.
- **"All time" in Stats showed 300+ empty chart buckets** — the hardcoded `2000-01-01` start date is replaced with a live `MIN(date)` query, so the chart range starts at the user's actual first log entry.
- **"All time" export metadata showed `null/null` date range** — the JSON `dateRange.from/to` now reflects the actual earliest log date instead of null.
- Enabled SQLite foreign-key constraints (`PRAGMA foreign_keys = ON`) so `ON DELETE CASCADE` rules on symptom and tracking-log tables actually fire.

---

## [0.10.0-beta.1] - 2026-05-27

### Added
- **Numeric tracking categories** — categories can now be set to "Numeric" mode (toggle in Add/Edit dialogs). Numeric categories record a single number per day via a draggable slider with configurable min/max range and optional decimal steps.
- **Average chart** — Stats screen shows average numeric value per week/month for numeric categories.
- **Distribution chart** — Stats screen shows a value-frequency histogram for numeric categories.
- Chart type selector adapts automatically to the type(s) of selected categories.

### Changed
- Database schema bumped to v6 (backward-compatible `ALTER TABLE` migration; existing categories and logs unaffected).

---

## [0.9.2-beta.1] - 2026-05-24

### Changed
- Version bump

---

## [0.9.1-beta.1] - 2026-05-24

### Fixed
- **Crash on theme change** — changing the colour theme no longer crashes.  The root cause was `PackageManager.setComponentEnabledSetting()` being called five times in rapid succession inside a `LaunchedEffect(currentTheme)` every time the user tapped a new palette, which could destabilise the launcher on some devices.  The app icon is now managed entirely separately from the colour theme.

### Added
- **App icon picker** (Settings → Appearance) — choose your launcher icon independently of the colour theme:
  - *Drop icons* — five colour-tinted variants (Coral, Teal, Sage, Dark, Blue)
  - *Discreet icons* — **Leaf**, **Moon**, and **Star** shapes that give no hint the app is a period tracker; ideal for privacy on a shared or visible home screen
  - *Your own icon* — pick any image from the gallery; GoFlo creates a pinned home-screen shortcut with a 512 × 512 px crop of that image as the icon, which can then replace the original in the launcher's app drawer.  On-screen instructions cover image format (PNG or JPEG), recommended size (512 × 512 px), and how to hide the original icon.

---

## [0.9.0-beta.1] - 2026-05-24

### Added
- **Custom tracking categories** — create any category (Mood, Discharge, Weather…) with user-defined value options; log entries per day via the FAB or long-pressing a calendar day
- **Flow & Symptoms as system categories** — pre-seeded with their existing values; editable but not deletable; the start of a unified per-day tracking model
- **Manage Categories screen** (Settings → Tracking Categories) — add, rename, and delete categories and their values; system categories show a lock icon
- **Value rename dialog** — choose "Fix everywhere" (updates all past log entries) or "Rename option only" (leaves historical entries unchanged); ideal for typo corrections
- **Day Log bottom sheet** — tap any calendar day with data to see a summary of the period entry and all tracking logs for that date; Edit buttons for each section
- **"Log more…" button** inside the Day Log sheet — opens the log-type picker for that day
- **Quick Log setting** (Settings) — choose whether the FAB short-press opens Log Period or any custom tracking category
- **Calendar tracking dots** — days with tracking entries (but no period) show a secondary-colour dot for at-a-glance visibility

### Changed
- **FAB redesigned** — pill-shaped "Log…" button; short-press = Quick Log, long-press = menu listing Log Period + each tracking category
- **Calendar tap behaviour** — tapping a day that already has data opens the Day Log summary sheet instead of jumping straight to the log form
- **Calendar long-press** — long-pressing any day now runs Quick Log for that specific date
- **DB bumped to version 3** — migration creates four new tracking tables; existing data is untouched; fresh installs seed Flow and Symptoms system categories automatically

---

## [0.8.3-beta.1] - 2026-05-23

### Changed
- **Settings screen reorganised** — sections now collapse/expand with an animated chevron, reducing visual clutter; order changed to priority-first: Reminders → Cycle → Appearance → Security & Privacy → Data → About
- **Compact theme picker** — replaced the stacked chip rows with a three-segment Light / Dark / Auto mode control and three tappable colour circles (Coral / Teal / Sage); Accessibility themes (High Contrast, Blue & Orange) remain as chips below a divider; palette row hides automatically in Auto mode
- **Data section** — Export JSON and Export CSV promoted to a side-by-side row; Delete All Data separated from safe actions by a divider
- **About section** — Privacy Policy and Licences promoted to a side-by-side row

---

## [0.8.2-beta.1] - 2026-05-23

### Changed
- **Swipe-to-delete UX** — replaced the confirmation dialog with a Snackbar + Undo pattern; swiping right-to-left now removes the card immediately and shows a 10-second "Period deleted · Undo" snackbar; tapping Undo restores the period with no DB write; the DB deletion is committed only after the snackbar times out

### Technical
- `HistoryViewModel`: replaced single `deletePeriod()` with three-stage lifecycle — `stageDeletion()` (hide from list), `undoDeletion()` (restore), `commitDeletion()` (DB write); the `periods` StateFlow now combines with `_pendingDeleteIds` to filter out staged entries; `symptomTrends` continues to use the raw repository flow so trends are unaffected by transient pending-delete state
- `HistoryScreen`: `SwipeToDismissBox.confirmValueChange` returns `true` for `EndToStart` (card slides off); `LaunchedEffect(state.currentValue)` calls `onDelete` when settled; snackbar coroutine launched on screen-level `rememberCoroutineScope` so it survives card composable disposal; `Modifier.animateItem()` on each card for smooth list collapse on removal

---

## [0.8.1-beta.1] - 2026-05-23

### Fixed
- **Security — widget PIN bypass**: the home screen widget now shows a neutral placeholder ("GoFlo — tap to open") instead of cycle data when PIN lock is enabled; sensitive health data is no longer visible on the home screen without authentication (regression introduced in 0.8.0-beta.1)
- **Security — CSV formula injection**: `exportAsCsv()` now prefixes any free-text field (notes, custom symptoms) whose first character is `=`, `+`, `-`, `@`, `\t`, or `\r` with a tab so spreadsheet apps never interpret the content as a formula (DDE/CSV injection defence)
- **Widget — custom cycle length ignored**: the widget now reads `AppPreferencesStore.preferredCycleLength` and uses the user-set override instead of always falling back to the auto-calculated average
- **"Set end date" button**: tapping "Set end date" in the no-end-date confirmation dialog now immediately opens the end-date picker; previously it only dismissed the dialog, leaving the user to manually find the picker
- **Cycle slider — DataStore write on every drag frame**: the cycle-length slider now uses a local `Float` state while dragging and writes to DataStore only in `onValueChangeFinished`; eliminates dozens of disk writes per second during drag
- **Unmanaged CoroutineScope in widget**: `GoFloWidget.updateAllWidgets()` now uses a module-level `CoroutineScope(SupervisorJob() + Dispatchers.IO)` instead of creating a new orphaned scope on every call
- **No validation guard on `setPreferredCycleLength`**: the DataStore setter now `require`s that the value is either 0 (auto) or within 21–45, throwing `IllegalArgumentException` on out-of-range input to prevent silent prediction corruption

---

## [0.8.0-beta.1] - 2026-05-23

### Added
- **Home screen widget** — a 2×1 cell AppWidget showing cycle status at a glance:
  - While a period is active: "Period · day N" + "Avg cycle: N days"
  - Otherwise: "Period in N days" / "Period due today" / "Period due tomorrow" + "Day N of ~N"
  - No data logged yet: "Tap to get started"
  - Tapping the widget opens the app
  - Updated every 30 minutes by the OS (the system minimum); data is read from Room on `Dispatchers.IO` via `goAsync()` so the main thread is never blocked
  - Registered in AndroidManifest as `.widget.GoFloWidget` with `@xml/widget_info` (minWidth 180 dp, targetCellWidth 2, minSdk 26 compat)
  - Background: dark semi-transparent rounded rectangle (`widget_background.xml`) visible on both dark and light launcher wallpapers

---

## [0.7.0-beta.1] - 2026-05-23

### Added
- **CSV export** — Settings → Data → Export Data (CSV) serialises all period logs to a standard CSV file (RFC 4180) with columns: start_date, end_date, duration_days, flow_level, symptoms (semicolon-separated), notes; shared via the Android share sheet using the existing FileProvider; compatible with spreadsheet apps and data analysis tools
- **Swipe-to-delete in History** — swipe any period card right-to-left to reveal a red trash background; releasing past the threshold shows a confirmation dialog ("Delete / Cancel"); the card always snaps back so no accidental deletes occur
- **Symptom trends** — a "Symptom Trends" card appears at the top of the History screen once ≥3 periods are logged; shows up to 5 most-common symptoms with their occurrence count, percentage-of-periods, and a thin progress bar for quick visual comparison

---

## [0.6.0-beta.1] - 2026-05-23

### Added
- **Cycle length personalisation** — Settings → Cycle section with a toggle to switch between "Auto" (calculated from logged history) and a custom fixed length (21–45 days, controlled by a slider); preference is persisted in DataStore and feeds HomeViewModel via a combined flow so the calendar and all cycle predictions update instantly without restart
- **Ovulation window (±2 days)** — the calendar now marks the two days before and after the peak ovulation day with a softer 4 dp, 50%-alpha dot; the home screen Cycle Info card now shows the full five-day range (e.g. "May 20 – May 24") instead of a single date; TalkBack announces surrounding days as "fertility window" and the peak day as "ovulation day"

---

## [0.5.1-beta.1] - 2026-05-23

### Fixed
- **Accessibility**: bottom navigation bar icons now have content descriptions ("Home", "History", "Settings") so TalkBack announces them correctly
- **End-date warning**: tapping Save on the Log Period screen without setting an end date now shows a confirmation dialog ("Save as ongoing / Set end date") explaining that ongoing entries are excluded from average cycle calculations

### Added
- **Privacy Policy** button in Settings → About navigates to the full privacy & medical disclaimer — previously the disclaimer was only shown on install/update

---

## [0.5.0-beta.1] - 2026-05-23

### Added
- **10 themes** — Settings → Appearance now shows a grouped theme picker with a colour-swatch dot on each chip so you can preview the hue before selecting:
  - **Light** — Coral, Teal (was "Turquoise"), Sage (was "Green")
  - **Dark** — Coral, Teal, Sage; each is a Material3 dark colour scheme with light primary tones on deep backgrounds; status-bar icons automatically flip to light when a dark theme is active
  - **Follow system** — adopts the Teal palette in light or dark based on your device's system-wide dark-mode preference
  - **High Contrast** — Light (near-black on pure white) and Dark (pure white on pure black); every contrast pair exceeds 15:1
  - **Blue & Orange** — deuteranopia- and protanopia-safe palette; uses blue as the primary colour instead of red, safe for the ~9 % of users with red-green colour vision deficiency; period days render as blue circles
- Existing "Turquoise" and "Green" preferences stored in DataStore continue to resolve correctly — no data migration needed

### Changed
- All 10 themes (110 measured colour pairs) verified against WCAG AA before shipping; three dark-theme outline colours bumped by 2 RGB points to clear the 3.0:1 UI-component threshold on dark surfaceVariant backgrounds

---

## [0.4.2-beta.1] - 2026-05-23

### Fixed
- **WCAG AA contrast — Coral theme**: primary colour darkened from `#D9604A` to `#C15542` to fix three failing contrast pairs:
  - White day-number text on period-filled circles: was 3.7:1, now **4.5:1** (threshold 4.5:1)
  - Primary on `surfaceVariant` (chip borders, ovulation dot): was 2.9:1, now **3.5:1** (threshold 3.0:1)
  - Primary on `primaryContainer` (focused outlines): was 2.8:1, now **3.5:1** (threshold 3.0:1)
  - Turquoise and Green themes were already fully compliant; no changes needed
- `template_requirements.md`: WCAG AA checkbox now checked — all 33 measured pairs pass across all three themes

---

## [0.4.1-beta.1] - 2026-05-23

### Fixed
- **Accessibility — touch targets**: calendar day cells now use the full grid cell as the tap target (≥48 dp on typical phones) instead of the inner 36 dp circle, matching Android's minimum
- **Accessibility — screen reader labels**: each calendar day now announces its full state to TalkBack, e.g. "May 23, today, period day" or "May 25, predicted period, ovulation window" — no longer relies on colour or shape alone
- **Accessibility — version row**: Settings → About version row now exposes itself as a button to TalkBack and shows a "Tap to see changelog" subtitle for sighted users
- **Accessibility — ovulation dot**: dot enlarged from 4 dp to 6 dp for improved visibility at small calendar cell sizes

### Changed
- `README.md`: documented rationale for API 26 minimum (NotificationChannel, introduced in Android 8.0, is required for the alarm-stream reminder channel)
- `template_requirements.md`: checked off all items that were implemented but still marked `[ ]` (README, CHANGELOG, LESSONS, CI workflows, build/signing, licences screen, notifications, authentication, privacy — one remaining open item: WCAG AA contrast ratio verification)

---

## [0.4.0-beta.1] - 2026-05-23

### Added
- Custom symptoms: tap the **+ Add** chip in the Symptoms section of Log Period to pick from your saved symptom library or type a new name; new names are saved to the library for reuse
- Custom symptom names are always stored and displayed in lowercase; the picker is case-insensitive (typing "Nausea" and "nausea" resolve to the same entry)
- Room DB migration 1 → 2: new `custom_symptoms` table for the user's symptom library

### Changed
- Settings → About: version label is now a regular tap (was long-press) to open the changelog dialog; the tap target is wider (full card width + vertical padding) for easier tapping
- Built-in symptom chips now display in lowercase ("cramps", "back pain", …) for visual consistency with custom symptoms

---

## [0.3.0-beta.3] - 2026-05-23

### Added
- **Data import** — Settings → Data → Import Data opens a file picker for a GoFlo JSON export; choose Merge (skips periods whose start date already exists) or Replace (clears all existing data first); designed for migrating to a new phone

---

## [0.3.0-beta.2] - 2026-05-23

### Added
- **Data export** — Settings → Data → Export Data serialises all period logs and symptoms to a JSON file and shares it via the Android share sheet; mandatory because cloud backup is excluded
- **Delete all data** — Settings → Data → Delete All Data permanently removes all stored records behind a two-step confirmation dialog
- **Data retention policy** — added to the privacy & medical disclaimer; data is kept indefinitely until the user deletes it or uninstalls the app
- **ProGuard keep rules** for Room DAO interfaces and generated implementations, DataStore protobuf internals, and the Biometric library — prevents release-build crashes caused by R8 stripping reflection-heavy code
- FileProvider declaration in `AndroidManifest.xml` and `res/xml/file_paths.xml` to support secure file URI sharing without broad storage permissions

---

## [0.3.0-beta.1] - 2026-05-23

### Added
- Long-press the version number in Settings → About to open a "What's New" dialog showing the last 5 changelog entries

---

## [0.2.1-beta.1] - 2026-05-22

### Added
- `README.md` covering project description, privacy summary, build instructions, and contributing links
- Unit tests for `PeriodRepository` cycle math (13 cases) and `PinManager` PIN hashing (7 cases)
- `lint-baseline.xml` — lint now runs in CI against a committed baseline; only new issues surface in PRs
- CI: `./gradlew test` and `./gradlew lintDebug` steps added to the PR build workflow

### Changed
- `PinManager` switched from `android.util.Base64` to `java.util.Base64` (identical encoding, now testable on JVM without Android SDK)
- `template_requirements.md`: added README, unit tests in CI, and lint baseline requirements

---

## [0.2.0-beta.1] - 2026-05-22

### Added
- Open Source Licences screen (accessible from Settings → About) listing all runtime dependencies and their copyright holders

---

## [0.1.1-beta.1] - 2026-05-22

### Fixed
- Kotlin compile errors: nullable `LocalDate` type mismatch in `HomeViewModel`; `SelectableChip` parameter order causing trailing lambda to resolve to `modifier` instead of `onClick`
- Theme flash on startup — app no longer briefly shows Coral before switching to the saved theme
- Settings screen now shows the live version from `BuildConfig.VERSION_NAME` instead of a hardcoded string

### Changed
- CI workflow now runs on pull requests (pre-merge build check) and `workflow_dispatch` only; no automatic builds on merge to main
- CI fails fast if `versionName` already has a GitHub Release, ensuring every PR carries a version bump
- Removed artifact uploads from CI to avoid filling storage quota; releases are created manually via `workflow_dispatch`
- Screenshots and recents thumbnails re-enabled (removed `FLAG_SECURE`)
- Adopted `MAJOR.MINOR.PATCH-beta.N` versioning scheme

### Added
- `gradle.properties` with `android.useAndroidX=true` (was missing, caused CI build failures)
- This changelog

---

## [0.1.0-beta] - 2026-05-22

Initial beta release.

### Added
- Monthly calendar home screen with period days (filled), predicted days (dashed outline), and ovulation indicator
- Log period form: start/end dates, flow level (Spotting/Light/Medium/Heavy), symptoms (FlowRow), notes (500 char limit)
- History screen: list of past periods with duration, flow, and symptoms; tap to edit
- Settings: three switchable light themes (Coral, Turquoise, Green), reminder controls, security section
- Cycle math: average cycle length (clamped 21–35 days, default 28), next period prediction, ovulation date
- Three reminder types via AlarmManager (USAGE_ALARM stream to bypass DND): pre-period alert, ovulation window, daily during-period log prompt
- Optional PIN lock (PBKDF2-HMAC-SHA256, 100k iterations, 16-byte salt, constant-time compare)
- Optional biometric unlock (fingerprint / face)
- Privacy & medical disclaimer shown on first install and on every app update; accessible from Settings at any time
- All data stored locally — no network requests, no accounts, no telemetry
- Cloud backup and device transfer explicitly excluded for all health data
