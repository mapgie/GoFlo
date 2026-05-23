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

## [0.8.2-beta.1] - 2026-05-23

### Changed
- **Swipe-to-delete UX** — replaced the confirmation dialog with a Snackbar +
  Undo pattern; swiping right-to-left now removes the card immediately and shows
  a 10-second "Period deleted · Undo" snackbar; tapping Undo restores the period
  with no DB write; the DB deletion is committed only after the snackbar times out

### Technical
- `HistoryViewModel`: replaced single `deletePeriod()` with three-stage lifecycle
  — `stageDeletion()` (hide from list), `undoDeletion()` (restore), `commitDeletion()`
  (DB write); the `periods` StateFlow now combines with `_pendingDeleteIds` to
  filter out staged entries; `symptomTrends` continues to use the raw repository
  flow so trends are unaffected by transient pending-delete state
- `HistoryScreen`: `SwipeToDismissBox.confirmValueChange` returns `true` for
  `EndToStart` (card slides off); `LaunchedEffect(state.currentValue)` calls
  `onDelete` when settled; snackbar coroutine launched on screen-level
  `rememberCoroutineScope` so it survives card composable disposal;
  `Modifier.animateItem()` on each card for smooth list collapse on removal

---

## [0.8.1-beta.1] - 2026-05-23

### Fixed
- **Security — widget PIN bypass**: the home screen widget now shows a neutral
  placeholder ("GoFlo — tap to open") instead of cycle data when PIN lock is
  enabled; sensitive health data is no longer visible on the home screen without
  authentication (regression introduced in 0.8.0-beta.1)
- **Security — CSV formula injection**: `exportAsCsv()` now prefixes any
  free-text field (notes, custom symptoms) whose first character is `=`, `+`,
  `-`, `@`, `\t`, or `\r` with a tab so spreadsheet apps never interpret the
  content as a formula (DDE/CSV injection defence)
- **Widget — custom cycle length ignored**: the widget now reads
  `AppPreferencesStore.preferredCycleLength` and uses the user-set override
  instead of always falling back to the auto-calculated average
- **"Set end date" button**: tapping "Set end date" in the no-end-date
  confirmation dialog now immediately opens the end-date picker; previously it
  only dismissed the dialog, leaving the user to manually find the picker
- **Cycle slider — DataStore write on every drag frame**: the cycle-length
  slider now uses a local `Float` state while dragging and writes to DataStore
  only in `onValueChangeFinished`; eliminates dozens of disk writes per second
  during drag
- **Unmanaged CoroutineScope in widget**: `GoFloWidget.updateAllWidgets()` now
  uses a module-level `CoroutineScope(SupervisorJob() + Dispatchers.IO)` instead
  of creating a new orphaned scope on every call
- **No validation guard on `setPreferredCycleLength`**: the DataStore setter now
  `require`s that the value is either 0 (auto) or within 21–45, throwing
  `IllegalArgumentException` on out-of-range input to prevent silent prediction
  corruption

---

## [0.8.0-beta.1] - 2026-05-23

### Added
- **Home screen widget** — a 2×1 cell AppWidget showing cycle status at a glance:
  - While a period is active: "Period · day N" + "Avg cycle: N days"
  - Otherwise: "Period in N days" / "Period due today" / "Period due tomorrow" +
    "Day N of ~N"
  - No data logged yet: "Tap to get started"
  - Tapping the widget opens the app
  - Updated every 30 minutes by the OS (the system minimum); data is read from
    Room on `Dispatchers.IO` via `goAsync()` so the main thread is never blocked
  - Registered in AndroidManifest as `.widget.GoFloWidget` with
    `@xml/widget_info` (minWidth 180 dp, targetCellWidth 2, minSdk 26 compat)
  - Background: dark semi-transparent rounded rectangle (`widget_background.xml`)
    visible on both dark and light launcher wallpapers

---

## [0.7.0-beta.1] - 2026-05-23

### Added
- **CSV export** — Settings → Data → Export Data (CSV) serialises all period logs
  to a standard CSV file (RFC 4180) with columns: start_date, end_date,
  duration_days, flow_level, symptoms (semicolon-separated), notes; shared via the
  Android share sheet using the existing FileProvider; compatible with spreadsheet
  apps and data analysis tools
- **Swipe-to-delete in History** — swipe any period card right-to-left to reveal a
  red trash background; releasing past the threshold shows a confirmation dialog
  ("Delete / Cancel"); the card always snaps back so no accidental deletes occur
- **Symptom trends** — a "Symptom Trends" card appears at the top of the History
  screen once ≥3 periods are logged; shows up to 5 most-common symptoms with their
  occurrence count, percentage-of-periods, and a thin progress bar for quick
  visual comparison

---

## [0.6.0-beta.1] - 2026-05-23

### Added
- **Cycle length personalisation** — Settings → Cycle section with a toggle to
  switch between "Auto" (calculated from logged history) and a custom fixed length
  (21–45 days, controlled by a slider); preference is persisted in DataStore and
  feeds HomeViewModel via a combined flow so the calendar and all cycle predictions
  update instantly without restart
- **Ovulation window (±2 days)** — the calendar now marks the two days before and
  after the peak ovulation day with a softer 4 dp, 50%-alpha dot; the home screen
  Cycle Info card now shows the full five-day range (e.g. "May 20 – May 24")
  instead of a single date; TalkBack announces surrounding days as "fertility
  window" and the peak day as "ovulation day"

---

## [0.5.1-beta.1] - 2026-05-23

### Fixed
- **Accessibility**: bottom navigation bar icons now have content descriptions
  ("Home", "History", "Settings") so TalkBack announces them correctly
- **End-date warning**: tapping Save on the Log Period screen without setting an
  end date now shows a confirmation dialog ("Save as ongoing / Set end date")
  explaining that ongoing entries are excluded from average cycle calculations

### Added
- **Privacy Policy** button in Settings → About navigates to the full privacy &
  medical disclaimer — previously the disclaimer was only shown on install/update

---

## [0.5.0-beta.1] - 2026-05-23

### Added
- **10 themes** — Settings → Appearance now shows a grouped theme picker with a
  colour-swatch dot on each chip so you can preview the hue before selecting:
  - **Light** — Coral, Teal (was "Turquoise"), Sage (was "Green")
  - **Dark** — Coral, Teal, Sage; each is a Material3 dark colour scheme with
    light primary tones on deep backgrounds; status-bar icons automatically flip
    to light when a dark theme is active
  - **Follow system** — adopts the Teal palette in light or dark based on your
    device's system-wide dark-mode preference
  - **High Contrast** — Light (near-black on pure white) and Dark (pure white on
    pure black); every contrast pair exceeds 15:1
  - **Blue & Orange** — deuteranopia- and protanopia-safe palette; uses blue as
    the primary colour instead of red, safe for the ~9 % of users with red-green
    colour vision deficiency; period days render as blue circles
- Existing "Turquoise" and "Green" preferences stored in DataStore continue to
  resolve correctly — no data migration needed

### Changed
- All 10 themes (110 measured colour pairs) verified against WCAG AA before
  shipping; three dark-theme outline colours bumped by 2 RGB points to clear
  the 3.0:1 UI-component threshold on dark surfaceVariant backgrounds

---

## [0.4.2-beta.1] - 2026-05-23

### Fixed
- **WCAG AA contrast — Coral theme**: primary colour darkened from `#D9604A` to `#C15542`
  to fix three failing contrast pairs:
  - White day-number text on period-filled circles: was 3.7:1, now **4.5:1** (threshold 4.5:1)
  - Primary on `surfaceVariant` (chip borders, ovulation dot): was 2.9:1, now **3.5:1** (threshold 3.0:1)
  - Primary on `primaryContainer` (focused outlines): was 2.8:1, now **3.5:1** (threshold 3.0:1)
  - Turquoise and Green themes were already fully compliant; no changes needed
- `template_requirements.md`: WCAG AA checkbox now checked — all 33 measured pairs
  pass across all three themes

---

## [0.4.1-beta.1] - 2026-05-23

### Fixed
- **Accessibility — touch targets**: calendar day cells now use the full grid cell as the tap target
  (≥48 dp on typical phones) instead of the inner 36 dp circle, matching Android's minimum
- **Accessibility — screen reader labels**: each calendar day now announces its full state to
  TalkBack, e.g. "May 23, today, period day" or "May 25, predicted period, ovulation window" —
  no longer relies on colour or shape alone
- **Accessibility — version row**: Settings → About version row now exposes itself as a button to
  TalkBack and shows a "Tap to see changelog" subtitle for sighted users
- **Accessibility — ovulation dot**: dot enlarged from 4 dp to 6 dp for improved visibility at
  small calendar cell sizes

### Changed
- `README.md`: documented rationale for API 26 minimum (NotificationChannel, introduced in
  Android 8.0, is required for the alarm-stream reminder channel)
- `template_requirements.md`: checked off all items that were implemented but still marked `[ ]`
  (README, CHANGELOG, LESSONS, CI workflows, build/signing, licences screen, notifications,
  authentication, privacy — one remaining open item: WCAG AA contrast ratio verification)

---

## [0.4.0-beta.1] - 2026-05-23

### Added
- Custom symptoms: tap the **+ Add** chip in the Symptoms section of Log Period to pick from your
  saved symptom library or type a new name; new names are saved to the library for reuse
- Custom symptom names are always stored and displayed in lowercase; the picker is case-insensitive
  (typing "Nausea" and "nausea" resolve to the same entry)
- Room DB migration 1 → 2: new `custom_symptoms` table for the user's symptom library

### Changed
- Settings → About: version label is now a regular tap (was long-press) to open the changelog
  dialog; the tap target is wider (full card width + vertical padding) for easier tapping
- Built-in symptom chips now display in lowercase ("cramps", "back pain", …) for visual
  consistency with custom symptoms

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
