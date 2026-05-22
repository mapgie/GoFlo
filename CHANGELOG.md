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
