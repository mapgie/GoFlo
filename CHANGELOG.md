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
## [0.38.0-beta.1] - 2026-06-05

### Added
- New "Alarms & Notifications" screen that gathers custom alarms, period reminders, and their system permissions in one place. The permission rows (Notifications, Exact alarms, Do Not Disturb access) show their live grant status as "Allowed" or "Tap to allow" and refresh when you return from system settings. Status is shown with both an icon and a label, never colour alone.
- Enabling "Override Do Not Disturb" on a custom alarm now prompts to grant Do Not Disturb access. The bypass relies on this permission, so previously the toggle could be silently ineffective until the access was granted elsewhere.

### Changed
- Manage screen: alarms, reminders, and the three permission rows are now reached through a single "Alarms & Notifications" entry instead of being spread across separate rows and a standalone "Permissions" section.

---
## [0.37.0-beta.1] - 2026-06-04

### Changed
- Manage screen: the two system permission rows ("Notification permissions" and "Alarm permission") are now grouped under a "Permissions" subheader, separating them from the tracking settings above.
- Manage screen: removed the inconsistent divider between the tracking rows so the list reads as one continuous group, matching Material 3 list guidance.
- Beta feedback banner: the "Feedback is encouraged" link now uses the modern annotated-link API, so it is announced with the correct link role to accessibility services and exposes a proper touch region.

---
## [0.36.0-beta.3] - 2026-06-04

### Changed
- Weight category (Weight, Pregnancy, and PCOS modes) now uses a slider instead of a free-entry field, with a default range of 0-200 kg. The range is fully adjustable in category settings.

---
## [0.36.0-beta.2] - 2026-06-04

### Changed
- Active tracking mode cards now show a filled heart icon instead of an outline heart, replacing the previous check-circle tick. The card background colour change on activation is unchanged.

---
## [0.36.0-beta.1] - 2026-06-04

### Added
- Silent delivery mode for period reminders (pre-period, ovulation, daily). Reminders are now delivered to the notification shade with no sound or vibration. Selectable via the three-way Notification / Silent / Alarm picker in Manage > Reminders.
- "Do Not Disturb access" shortcut in the Manage screen (below Alarm permission). Opens the system settings where users can grant GoFlo DND access, required for the Override Do Not Disturb feature in custom alarms.

### Changed
- The delivery mode description text in Manage > Reminders now updates for all three modes instead of just showing Notification vs Alarm.

---
## [0.35.0-beta.1] - 2026-06-04

### Changed
- New slider categories now default to 5 values (1-5) instead of 11 (0-10), making the most common use case a 5-point scale out of the box.
- BBT Temperature category (Fertility mode) now uses a slider input instead of a free-entry field. The Celsius/Fahrenheit toggle already present in the activation sheet controls the slider range.
- Pregnancy mode activation now pre-fills the "Last period" date from the most recent period already logged in the app. A note is shown when this auto-detection is active. Users can still change the date or switch to entering a due date instead.

---
## [0.34.0-beta.1] - 2026-06-04

### Changed
- Category edit screen redesigned to align with Material 3 principles: pencil (rename) and palette (edit appearance) icons are now direct action buttons in the top app bar trailing edge, replacing the inline pencil next to the title and the "Edit appearance" overflow menu item.
- Category values are now displayed as a chip row (like the Period Log view) instead of a scrollable list with a floating action button. Each chip has a trailing edit icon; tapping it opens the rename dialog. Non-system values show a Delete button in the rename dialog.
- Removed the divider line at the bottom of the values section.
- Section headings ("Values in this category", "Slider scale settings", etc.) now use Title Small typography for improved visual hierarchy.
- Aligned section heading typography across all edit screens: SettingsScreen (Mode, Colour, Custom colours, Export scope, Date range, What to include, Format, GoFlo Status, Quick Log, Discreet icons, Your own icon) and ModesScreen activation sheet (This mode enables, Temperature unit, Your pregnancy date, Categories to add) all now use Title Small, matching the category edit screen standard.

---
## [0.33.0-beta.1] - 2026-06-04

### Changed
- Edit category (slider): value labels are now hidden by default. A "Label values" toggle reveals the labelling options, keeping the screen less busy.

---
## [0.32.2-beta.1] - 2026-06-04

### Fixed
- Tapping Home in the bottom navigation bar while on the Settings screen now returns to Home. The Home tab also stays highlighted while Settings is open, making it clear which section you are in.

---
## [0.32.1-beta.1] - 2026-06-04

### Fixed
- Pregnancy setup: date input now uses DD/MM/YYYY order with a numeric keyboard; slashes are inserted automatically so no separators need to be typed.

---
## [0.32.0-beta.1] - 2026-06-04

### Added
- Custom alarms: create multiple alarms at custom times, each with its own delivery mode (notification, alarm, or silent full-screen), optional Do Not Disturb override, and configurable snooze duration (5, 10, 15, 30, or 60 minutes).
- Alarm schedule types: every day, during period only, outside period only, N days before predicted period, N days after period start, or on the Nth day of period.
- Category reminders: link an alarm to one or more tracking categories. Notifications include Log and Snooze quick actions. Tapping Log opens the category logging screen.
- Alarms page: manage all custom alarms from Manage > Alarms.
- Category alarms section: manage alarms for a specific category directly from that category's edit screen.
- Silent alarms: delivered as a full-screen interrupt with no sound or vibration, requiring a dismiss or snooze action.
- Full backup now includes app settings (theme, reminder schedule, cycle preferences, dashboard state) and custom alarms. Restoring a full backup on a new phone recreates alarms and reschedules them automatically.

---
## [0.31.1-beta.1] - 2026-06-04

### Fixed
- Full backup now truly includes everything: all app settings (theme, reminders, display preferences, tracking modes, pregnancy date, temperature unit, custom hues, dashboard state) are exported and restored on import. Previously only pinned stats were included.
- Category `modeKey` is now exported in full backup. Without this, restoring a backup and re-activating a Tracking Mode would duplicate mode-seeded categories instead of recognising them as already present.

---
## [0.31.0-beta.1] - 2026-06-04

### Added
- Tracking Modes: a new screen in Manage lets you activate preset configurations for Fertility, Pregnancy, Weight, Endometriosis, PCOS, HRT, Perimenopause, and Hormone Tracking. Each mode suggests relevant categories with opt-in/opt-out per item. Multiple modes can be active at the same time, and cross-mode categories (e.g. Weight appearing in PCOS and Pregnancy) are deduplicated automatically.
- Pregnancy mode: when active with a due date entered, a week counter card appears on the home screen showing the current week, trimester, due date, and days remaining. Both expected due date (EDD) and last menstrual period (LMP) are supported.
- Fertility mode: seeds BBT Temperature and Cervical Fluid categories. Temperature unit (Celsius or Fahrenheit) is chosen at activation. The BBT chart will follow in a future update.
- DB migration to v17 adding the modeKey column that links mode-seeded categories to their preset for deduplication.

---
## [0.30.2-beta.1] - 2026-06-04

### Fixed
- Calendar and stats charts now start the week on Monday (ISO 8601 / UK convention). Previously the calendar grid showed Sunday as the first column and week groupings in Stats could vary by device locale.

---
## [0.30.1-beta.1] - 2026-06-04

### Changed
- Tracking Categories: explicit "Reorder" mode (toolbar toggle) replaces always-visible drag handles. Handles appear only when reorder mode is active; swipe actions are suspended during reorder.
- Tracking Categories: category name upgraded to `titleMedium` for stronger visual hierarchy over the subtitle.
- Tracking Categories: list cards use `surfaceContainerLow` instead of `surfaceVariant` for a lighter, less heavy appearance.
- Tracking Categories: palette icon removed from list rows. Appearance editing (icon and colour) is now in the three-dot overflow menu on each category's detail screen.
- Tracking Categories: FAB replaced with an Extended FAB labelled "Add Category" for clearer affordance.

---
## [0.30.0-beta.1] - 2026-06-03

### Added
- Categories: built-in categories (Flow, Symptoms, Ovulation Test) can now be hidden. Swipe left on any built-in category in Manage Categories to hide it, or restore it from the Archived section. A confirmation is always shown before hiding.
- Settings: new "Reset Category Settings" option in Data & Backup. Deletes all custom categories and restores any hidden built-in categories. Period logs and tracking history are not affected.

---
## [0.29.0-beta.1] - 2026-06-03

### Added
- Manage: help dialog explaining category types, value ordering, drag-to-reorder, archive, and delete. Accessible two ways: the info icon in the Tracking Categories toolbar, and a "Help" item in the three-dot menu on each category's values screen.

---
## [0.28.0-beta.1] - 2026-06-03

### Added
- Stats: Pie chart slices and Trends progress bars now use ordered shades of the category's own colour for default-type categories. Lower-order values (e.g. Spotting) appear as a lighter tint; higher-order values (e.g. Heavy) appear as the full colour. Colour still varies by hue for numeric and non-default categories.

---

## [0.27.0-beta.1] - 2026-06-03

### Added
- Custom theme: tap the dotted "Custom" slot at the end of the colour grid in Appearance to reveal hue sliders for primary, secondary, and tertiary colours. The app builds a full Material 3 colour scheme from your choices.
- Theme palette picker is now a neat 4-column grid. The custom slot sits at the natural end of the last row.

### Fixed
- History delete loop: swiping a period, cancelling, then tapping into it no longer re-triggers the delete dialog on return.
- History swipe lock: after cancelling a delete, the card is immediately swipeable again with no delay.
- History card highlight: the red swipe-to-delete background now clips to the card's rounded corners instead of showing a square behind them.

---
## [0.26.0-beta.1] - 2026-06-03

### Added
- Cycle phase label on the home screen: the Cycle info card now shows which phase you are in (Menstrual, Follicular, Ovulatory, or Luteal) with the current day number. Tapping the row opens an info sheet explaining each phase.
- Cycle length per entry in the History screen: each past period card now shows how many days that cycle lasted (days from that period start to the next).
- Ovulation Test system category (database migration v16): seeded with Positive, Negative, and Faint options. Appears in the Log menu alongside Flow and Symptoms and shows up on the calendar dot indicator.
- Cycle Phase Summary chart type in Stats: select any non-numeric category and choose "By Phase" to see your logged values broken down by Menstrual, Follicular, Ovulatory, and Luteal phases with the top recorded values per phase.
- Export for Doctor Visit: Settings > Data and Backup now includes a plain-text report covering the last 12 months of periods, symptom frequencies, and custom category data, formatted for sharing with a healthcare provider.

---
## [0.25.0-beta.1] - 2026-06-03

### Added
- Full-screen disruptive alarm mode: when delivery mode is set to Alarm, the notification now launches a lock-screen alarm activity with a large icon and the user's custom alarm name.
- User-nameable alarms: set a custom label in Manage > Reminders that appears on the alarm screen when it fires.
- Year navigation in Stats chart area: CalendarYear and YTD modes now show prev/next year arrows directly above the chart, consistent with Month view navigation.
- Alarm and notification permission shortcuts in Manage screen for quick system-settings access.

### Changed
- Stats: Time range picker moved below the chart so the chart is immediately visible when a category is selected.
- Stats: Plus One (increment) categories now correctly sum the logged count per day in Time Series charts instead of showing 1 for every day logged.
- Stats: Removing a pin from the Dashboard now immediately clears the Unpin button in Stats.
- Settings: Tracking and Notifications sections removed; these are now exclusively in Manage.
- Manage: Items now show trailing chevron arrows, matching the Settings screen style.
- Manage > Reminders: Delivery mode description updated to say "Full-screen alarm" instead of "Plays alarm sound".
- Category management: Removed redundant Save button from the values list screen; changes save automatically.

---
## [0.24.1-beta.1] - 2026-06-02

### Changed
- Custom cycle length slider now goes up to 90 days (was 45), supporting longer cycles common with PCOS and other irregular cycle patterns.

---
## [0.24.0-beta.1] - 2026-06-02

### Added
- Symptoms and Flow are now fully user-extensible: rename, add, and delete options via Settings → Tracking Categories → Symptoms / Flow.
- The "Add" chip on the Log Period screen creates new symptom options inline and selects them immediately.
- Database migration (v15) converts stored enum names to display labels and migrates all custom symptoms into the unified `tracking_values` table, so every symptom option is managed in one place.
- Export/import remains backward-compatible: old enum-name exports ("CRAMPS", "MEDIUM") are recognised and mapped to display labels on import.

## [0.23.0-beta.1] - 2026-06-02

### Added
- Manage tab is now the rightmost item in the bottom navigation bar (after Stats).
- Bottom navigation bar now remains visible when navigating into the Settings screen.
- Manage screen now includes a beta feedback banner and three new items matching the Settings page: Cycle, One-Tap Quick Log, and What You Track (with the Tune icon).
- New standalone Manage. Cycle screen and Manage. One-Tap Quick Log screen accessible from the Manage tab.

### Fixed
- Dashboard Distribution chart (pie chart) legend was clipped by a fixed 200dp height container. The container now wraps its content so the full legend is visible.
- Dashboard ALL TIME charts were showing only one data point because the range started at year 2000, producing hundreds of empty monthly buckets. The start date now uses the earliest actual log date, matching the Stats screen behaviour.
- Fresh installs incorrectly allowed deletion of system tracking-category values (Flow and Symptoms options). The seed INSERT statements now mark those values as system-seeded so they cannot be deleted by the user.
- Privacy policy link now points to the correct GitHub page URL instead of the raw file URL.
- Changelog dialog now includes a "View full changelog" button that opens the full CHANGELOG.md on GitHub.

## [0.22.5-beta.1] - 2026-06-02

### Fixed
- Period prediction window now shows calendar markers and "Period expected" status when today falls within the predicted window but the predicted start date has already passed. Previously the prediction was only displayed when the predicted start date was today or in the future, so days 2-5 of a 5-day expected window showed nothing.
- Changed the status from "Cycle day X" to "Period expected" when the user is inside the predicted period window without an active logged period.
- Fixed em-dash style violations in the cycle info card ("Period active, day X" and "X to Y" for ovulation window).

## [0.22.4-beta.1] - 2026-06-02

### Fixed
- Additional accessibility role fixes missed in the initial audit: archive-dialog "Don't show this again" row (`Role.Checkbox`), alarm-permission list item in reminder settings (`Role.Button`), export-scope and export-format radio rows (`Role.RadioButton` with `RadioButton(onClick = null)`), theme-mode selector (`Role.RadioButton`), palette picker (`Role.RadioButton`), app-icon picker (`Role.RadioButton`), calendar day cells (`role = Role.Button` inside `clearAndSetSemantics`), and speed-dial scrim dismiss overlay (`Role.Button` + contentDescription).

### Added
- `a11y_check.py`: Python CI script that scans all Compose source files and fails if any `.clickable` or `.combinedClickable` modifier chain is missing a `.semantics { role = Role.* }` declaration. Runs as an early fast-fail step on every PR.
- CI workflow (`build.yml`) now runs the accessibility role check before the build step so violations are caught before a full Gradle build.
- Accessibility coding rules added to `.claude/CLAUDE.md` so future AI-assisted changes follow the same patterns automatically.

## [0.22.3-beta.1] - 2026-06-02

### Fixed
- Content descriptions added to speed-dial FABs (category and period log buttons) so TalkBack announces the button's action rather than reading nothing.
- Keyboard and switch-access role semantics (`Role.Button`, `Role.RadioButton`) applied to all custom-clickable elements: Manage screen list items, history period cards, stats chart-type selector, year picker, archive section header, category icon picker, colour swatches, export format rows, alarm permission banner, and settings navigation items.
- Archive and stats-warning expand/collapse controls now report their current state (`stateDescription = "Expanded"/"Collapsed"`) so TalkBack announces the post-tap state.
- PIN entry screens (lock and setup) now announce digit count changes via a polite live region on the dot indicator, and announce PIN errors immediately via an assertive live region.
- Delete key (⌫) on PIN keypads now has `contentDescription = "Delete"` so TalkBack reads the action rather than the raw Unicode symbol.
- Export format radio rows set `RadioButton(onClick = null)` so the wrapping row is the single focusable unit, preventing duplicate TalkBack announcements.
- Accessibility section in `template_requirements.md` expanded with five explicit principles covering content descriptions, keyboard/switch roles, dynamic text scaling, focus order, and live-region announcements.

## [0.22.2-beta.1] - 2026-06-02

### Changed
- Upgraded Android Gradle Plugin from 8.4.0 to 8.13.2 and Gradle wrapper from 8.6 to 8.11.1.

## [0.22.1-beta.1] - 2026-06-02

### Changed
- Compose BOM upgraded from `2024.12.01` to `2025.05.01` (Compose UI 1.8.x, Material3 1.4.x). No user-visible changes; enables use of stable Compose 1.8 APIs internally.

## [0.22.0-beta.1] - 2026-06-02

### Added
- **Drag to reorder categories** — long-press the drag handle on any active tracking category to reorder it. The new order is persisted immediately. Works alongside existing swipe-to-archive and swipe-to-delete gestures.

## [0.21.2-beta.1] - 2026-06-02

### Changed
- Licences screen maintainer note expanded to document all excluded dependencies (`junit`, `room-compiler`, `ui-tooling`).

## [0.21.1-beta.1] - 2026-06-02

### Changed
- Compose BOM upgraded from `2024.06.00` to `2024.12.01` (Compose UI 1.7.6, Material3 1.3.1). No user-visible changes; enables use of stable Compose 1.7 APIs internally.

## [0.21.0-beta.1] - 2026-06-01

### Added
- **User-extensible Symptoms** — the Symptoms (and Flow) system categories now show an "Add value" button so users can add their own options alongside the built-in ones (Cramps, Headache, etc.). Built-in values are protected and cannot be deleted; user-added values can be removed normally. DB migration 13 to 14 marks all existing system category values as seeded.

## [0.20.0-beta.1] - 2026-06-01

### Added
- **Archived categories section** — archived tracking categories are now hidden in a collapsible "Archived (N)" section at the bottom of the Manage Categories screen, keeping the active list clean.
- **Archive warning: don't show again** — the archive confirmation dialog now has a "Don't show this again" checkbox. Once checked, future archives skip the dialog.
- **Bleeding (non-period) symptom** — added as a seeded value in the Symptoms system category for all users (via DB migration 12 to 13).
- **Stats: pin/unpin toggle** — the pin button on the Stats screen now shows "Unpin from Dashboard" when the current view is already pinned, and removes the pin on tap.
- **Stats: relative month pinning** — pinning a monthly view now always tracks the current calendar month instead of pinning the specific month at pin time.
- **Reminders: delivery mode** — new Notification vs Alarm toggle in Reminders settings. "Notification" uses a standard notification (no special permission); "Alarm" uses an exact alarm with alarm sound.

### Fixed
- **Reminders: broken alarms** — when the Alarms and reminders permission is not granted, reminders now fall back to an inexact alarm instead of being silently skipped. A warning banner appears in Reminders settings linking directly to the permission screen.
- **Stats: archived categories hidden from picker** — archived tracking categories no longer appear in the Stats category selector.
- **Stats: Compare bar widths** — in dual-category Compare mode, both bars in each time bucket are now equal width at all zoom levels.
- **Slider explanation text** — the flow slider helper text no longer says "1-4"; it now reads "a numeric slider" to reflect user-configurable ranges.

## [0.19.0-beta.1] - 2026-06-01

### Added
- **Manage tab** — new bottom navigation tab combining "What You Track" (category management) and Reminders, so tracking setup is one tap from anywhere.
- **Onboarding banner** — first-time users see a dismissable hint on the Home screen explaining how to start logging.
- **About: personal note from Margarida** — the About screen now opens with a personal statement about GoFlo's privacy principles and the use of LLM tooling during development.
- **Privacy Policy** — the in-app Privacy Policy is now a dedicated screen reading from `PRIVACY_POLICY.md`, with a link to the hosted version on GitHub. Medical Disclaimer and Privacy Policy are now two separate buttons in Settings > About.
- **Changelog footer** — a subtle tappable version label at the bottom of the Home screen opens the "What's New" dialog.
- **Changelog pill button** — the version entry in Settings > About is now a pill-shaped "v... What's New" button.

### Changed
- "Privacy Policy & Medical Disclaimer" merged button removed from main Settings list. Both items are accessible individually from Settings > About.
- Privacy Policy link added to the bottom of the Medical Disclaimer screen.

## [0.18.2-beta.1] - 2026-06-01

### Fixed
- **Period prediction** — predicted next period no longer displays on past dates when a period is overdue. Predictions are only shown when the calculated start date is today or in the future.
- **History undo** — fixed a race condition where tapping "Undo" very quickly after swiping could fail silently because symptom data had not yet been cached. Symptom data is now always cached before the delete is staged.
- **History delete** — added a confirmation dialog before a period entry is permanently removed, so accidental swipes no longer result in data loss without warning.
- **Beta banner** — the heart symbol (♥) now sits outside the hyperlink and no longer opens the browser when tapped.
- **Screen state** — set `launchMode="singleTop"` on MainActivity so returning to the app via the recents screen no longer recreates the activity and resets navigation to the home view.

## [0.18.1beta.1] - 2026-06-01

### Changed 
- **Stats** — Small fixes.

## [0.18.0-beta.1] - 2026-05-31

### Added
- **Stats — selectable category slots** — two labelled slot chips now appear in the category picker. Tap a slot to make it active, then tap any category to fill it. This lets you browse different categories in one slot without losing the other.
- **Stats — category swap button** — when two categories are selected, a swap icon appears next to the slot chips to instantly reverse which is in slot 1 and slot 2 (flips X/Y axes on Scatter plots).
- **Stats — month navigation moved above chart** — the `< Month Year >` row now lives at the top of the chart card so it sits directly above the data, rather than inside the time range picker.

### Changed
- **Stats — Distribution charts now consistent** — numeric Distribution renders as a donut/ring to match the categorical Distribution view; both use the DonutLarge icon.
- **Stats — Average icon corrected** — "Average" chart type now uses a bar-chart icon to match the bar chart it renders.
- **Stats — single-category views hidden for two-category selections** — "Trends" and "Over Time" no longer appear in the chart type selector when two categories are selected.
- **Stats — Scatter plot baseline** — X and Y axes now always start at 0.
- **Stats — chart colour cycling** — colour order changed to primary/tertiary/error/secondary so that themes with similar primary and secondary colours are less likely to assign indistinguishable colours to two series.
- **Stats — dual-series charts use category colours** — the Compare chart uses each category's own colour token for its bars instead of fixed theme colours.
- **Settings — back button on main list** — the main Settings screen now shows a back-arrow in the toolbar so users have a clear, visible way to return.
- **Settings — Full backup export** — a new "Export scope" toggle lets users choose between "Data only" (existing behaviour) and "Full backup", which also includes all category names, values, colors, archive status, and dashboard pins. Useful for transferring everything to a new phone.
- **Stats — Year before YTD in time range selector** — the segment order is now All Time, Year, YTD, Month.
- **Stats — YTD default time range** — the Stats screen now opens on Year-to-Date instead of All Time.
- **Stats — selections remembered** — the Stats screen now remembers the last time range, category selections, chart type, and zoom level across navigation and app restarts.
- **Stats — month zoom control** — when the Month time range is selected, two zoom buttons let users compress or expand the bar width, so all 31 days can fit on screen at once.
- **Stats — landscape auto-hide** — in landscape orientation the top bar collapses on scroll, the beta banner is hidden, and the bottom navigation bar is hidden to maximise chart space.

### Fixed
- **Security — biometric lock crash** — enabling biometric unlock no longer causes a crash if the biometric prompt is shown while the activity is not yet fully active.
- **Flow — slider mode** — the Flow system category can now be switched to a 1-4 slider (Spotting/Light/Medium/Heavy) via Settings > Tracking Categories > Flow. When slider mode is on, stats treat each logged flow value as a number, enabling Numeric Average, Time Scatter, and Distribution charts. The toggle lives alongside the existing category settings and can be switched back to chip mode at any time.
- **Dashboard — duplicate pin prevention** — pinning a chart view that is already on the dashboard now shows "Already pinned to dashboard" instead of silently adding a duplicate. Each combination of category, chart type, and time range gets a deterministic hash ID so existing duplicates are also blocked on re-pin.

---

## [0.17.0-beta.1] - 2026-05-31

### Added
- **Settings — Export Data full-page screen** — "Export Data" now opens a dedicated scrollable full-page screen (replacing the cropped `AlertDialog`). All options — date range presets, include/exclude toggles, JSON/CSV format selection — are fully accessible with a sticky Export button at the bottom.
- **Widgets — Quick Log category picker** — users can now choose which specific categories appear in the Quick Log (4×2) widget (up to 4). If none are selected the first four active categories are shown automatically, preserving previous behaviour.
- **Widgets — Status widget privacy opt-in** — the "Show data when PIN is set" toggle is now always visible in the Widgets settings sub-screen, with clear explanatory copy. It is disabled (with a hint) when no PIN is set, so users know the option exists before they set one.

### Changed
- **Settings — section header spacing** — top padding on section headers reduced from 20 dp to 12 dp for a slightly denser layout; touch-target sizes are unaffected (accessibility-safe).

---

## [0.16.1-beta.1] - 2026-05-31

### Changed
- **Stats category picker — context-aware labels** — X/Y axis labels in the summary bar and chip prefixes now only appear when the Scatter chart type is active (where X and Y map to real axes). All other chart types show category names without axis terminology.
- **Stats selection colour unified** — both the first and second selected category use the same `primaryContainer` fill, eliminating the arbitrary primary/secondary hue clash (pink vs teal). The summary bar now uses `primary` for both names rather than `primary` + `secondary`.

---

## [0.16.0-beta.1] - 2026-05-31

### Changed
- **Navigation — Settings moved to top app bar** — the Settings tab has been removed from the bottom navigation bar. A gear icon button in the top-right of the Home screen app bar now opens Settings, keeping the bottom bar focused on the core destinations: Home, History, (Dashboard,) and Stats.
- **Settings — flat Material list layout** — the expandable accordion card layout has been replaced with a flat Material `ListItem` layout. Each section entry is a dense list row; tapping it opens a dedicated sub-screen with its own top app bar and back arrow, rather than expanding inline.
- **Settings — section headers** — headers (TRACKING, NOTIFICATIONS, etc.) are now `SemiBold`, rendered in the primary colour, with increased top padding (20 dp) to clearly separate grouped items.
- **Settings — navigation items** — items that open a sub-screen show a trailing Chevron Right icon. Items that control a binary setting show a trailing Material Switch that toggles immediately without navigating.
- **Settings — section dividers** — `HorizontalDivider` separates the major setting groups (Tracking, Notifications, Personalisation, Privacy & Data, Widgets, Help & Feedback, About).
- **Settings — icon alignment** — leading icons are constrained to 24×24 dp bounding boxes for consistent vertical alignment across all list items.
- **Settings — standardised background** — list items use the `surface` container colour, removing the "boxed" `surfaceVariant` card look of the old layout.

---

## [0.15.0-beta.1] - 2026-05-31

### Added
- **Dashboard screen** — a new optional tab (Home | History | **Dashboard** | Stats | Settings) that displays pinned stats views as cards. Enable it via the new toggle in the Stats screen; pin any category/range/chart combination with "Pin this view".
- **Stats — Trends chart** — shows logged value frequency as a labelled progress-bar chart. Available for any trackable category. Replaces the old Symptom Trends section that was previously in the History screen.
- **Stats — Time Scatter chart** — plots log values against date (time on X, value on Y). This is now the default chart type when a single numeric category is selected.

### Changed
- **Beta feedback banner** — text updated to "Feedback is encouraged ♥".
- **Stats month picker** — replaced the dialog with an inline `← MMMM yyyy →` arrow navigator inside the time-range card.
- **Stats over-time granularity** — specific-month range now defaults to day-by-day view instead of weekly buckets.
- **Stats scatter axes** — X and Y axes always start at 0 for specific-year and specific-month ranges (AllTime / YTD retain auto-scaling).
- **Stats chart memory** — switching categories now restores the last explicitly chosen chart type when it is still valid for the new selection.
- **Settings — Track Against Time** — the toggle is now visible for all category types, including the system Flow and Symptoms categories.
- **Settings — category values screen** — the "Add a value" bottom button is replaced by a floating action button (FAB); the bottom button is now always "Save".

### Fixed
- **History** — Symptom Trends section removed from the History screen (superseded by the Stats Trends chart).

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
