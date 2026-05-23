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

## [0.3.0-beta.1] - 2026-05-23

### Added
- **Data export** — Settings → Data → Export Data serialises all period logs and symptoms to a JSON file and shares it via the Android share sheet; mandatory because cloud backup is excluded
- **Delete all data** — Settings → Data → Delete All Data permanently removes all stored records behind a two-step confirmation dialog
- **Data retention policy** — added to the privacy & medical disclaimer; data is kept indefinitely until the user deletes it or uninstalls the app
- **ProGuard keep rules** for Room DAO interfaces and generated implementations, DataStore protobuf internals, and the Biometric library — prevents release-build crashes caused by R8 stripping reflection-heavy code
- FileProvider declaration in `AndroidManifest.xml` and `res/xml/file_paths.xml` to support secure file URI sharing without broad storage permissions

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
