# Project Requirements Template

Standards every project must meet before shipping. Work through each section top-to-bottom; tick off items as they are implemented.

---

## Universal — all projects

### Documentation

- [ ] **`CHANGELOG.md`** at the repo root, following the versioning policy below
- [ ] **`LESSONS.md`** — principle-focused notes on non-obvious decisions and hard-won fixes; entries ordered by risk to a new project if forgotten
- [ ] **Developer contact** — defined in `README.md` or repo description:
  - Public repo → GitHub Issues enabled; link in README
  - Private repo → determined per-project (Slack channel, email, internal tracker); documented in README
- [ ] **Minimum supported OS / runtime version** — documented in `README.md` with a rationale (hardware coverage target, API dependency, etc.); any decision to raise the minimum is a MINOR version bump and requires a changelog entry

### Versioning policy

Format: `MAJOR.MINOR.PATCH[-beta.N]`

| Bump | When |
|---|---|
| MAJOR | Breaking change, destructive DB migration (data-loss risk), incompatible backup format |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation |
| PATCH | Backward-compatible fix: bug fix, copy change, perf improvement, internal refactor |

Rules:
- MINOR bump resets PATCH to 0 (`1.4.2 → 1.5.0`); MAJOR resets both (`1.4.2 → 2.0.0`)
- `versionCode` (or equivalent build integer) increments by 1 with every PR — no exceptions
- Increment pre-release N for each beta on the same base version (`1.4.2-beta.1 → 1.4.2-beta.2`)
- Released versions are immutable — never re-tag, never amend, never delete a changelog entry
- Merge conflicts in CHANGELOG must preserve both sides; if two branches used the same version string, renumber the lower-priority one upward

### Changelog immutability rules
- Never edit an existing entry once committed
- Never reuse a version string
- Never delete an entry — even for reverts, add a new entry describing the revert
- Each PR adds its changelog entry in the same commit as the feature or fix

### Accessibility

- [ ] All interactive elements have a minimum touch / click target of **48×48dp** (Android) or **44×44pt** (iOS)
- [ ] Every icon-only control has a **content description** (or equivalent accessible label) — buttons, icon buttons, image-only elements
- [ ] Text and interactive elements meet **WCAG AA contrast ratios**: 4.5:1 for body text, 3:1 for large text and UI components
- [ ] The app is navigable without colour alone — selection state, errors, and status are also communicated via shape, label, or icon

### Error states

- [ ] Every screen that loads data has an **empty state** — a clear message when there is nothing to show (not a blank screen)
- [ ] Errors shown to users are **generic and actionable** — no raw exception messages, stack traces, or internal identifiers; errors include a suggested next step where possible
- [ ] Network or I/O failures offer a **retry affordance** where retrying is meaningful
- [ ] Destructive or irreversible actions require **explicit confirmation** (dialog or multi-step) before executing

---

## Android

### Build & signing

- [ ] **Debug keystore committed to the repo** (`debug.keystore`, standard `android`/`android` credentials)
  - Prevents CI from generating a fresh keystore per runner, which blocks OTA updates with a "conflicting package" error
  - Wire into `signingConfigs.debug` in `app/build.gradle.kts`
  - Add `*.jks` and `release.keystore` (not `debug.keystore`) to `.gitignore`
- [ ] **`gradle.properties`** committed with at minimum:
  ```
  android.useAndroidX=true
  android.nonTransitiveRClass=true
  org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
  ```
- [ ] **`proguard-rules.pro` reviewed** whenever a new library is added — Room, DataStore, Retrofit, and most reflection-heavy libraries require explicit keep rules; crashes that appear only in release builds and not debug builds are almost always a missing ProGuard rule

### CI workflows

- [ ] **PR build check** (`.github/workflows/build.yml` or equivalent):
  - Triggers on `pull_request` targeting the default branch
  - Triggers on `workflow_dispatch` for on-demand releases
  - No `push` trigger — avoids duplicate builds on merge
  - No `upload-artifact` step — avoids filling artifact storage quota; the build result (pass/fail) is the signal
  - **Version check step** (runs before the build, fast-fail): reads `versionName` from `build.gradle.kts` and fails if a GitHub Release for that tag already exists — enforces the version-bump-per-PR rule
  - Release creation step gated on `github.event_name == 'workflow_dispatch'`

- [ ] **Licence screen sync check** (`.github/workflows/license-sync.yml` or equivalent):
  - Triggers only when `gradle/libs.versions.toml` changes in a PR
  - Fails if the licences screen source file was not also modified in the same PR

### Open source licences

- [ ] **Licences screen** in the app (Settings → About → Open Source Licences):
  - Lists every runtime dependency (not build-only tools) with its licence and copyright holder
  - Grouped by licence type
  - A maintainer comment in the file reminding contributors to update it when `libs.versions.toml` changes
  - Enforced by the licence sync CI check above

### Notifications

- [ ] Any reminder or alert that must be heard uses the **alarm audio stream**:
  - Create the notification channel with `AudioAttributes(usage = USAGE_ALARM)` so it plays at alarm volume and bypasses Do Not Disturb
  - Channel properties are written once and are immutable — changing the stream type requires a new channel ID
  - Use `AlarmManager.setExactAndAllowWhileIdle` for time-critical triggers

### UI

- [ ] Use `FlowRow` (not `LazyRow`) for chip groups in forms — `LazyRow` clips silently and hides required input
- [ ] Selected chip state must pass the "readable in 100ms" bar — override `FilterChipDefaults` with a high-contrast fill; the default Material3 selected treatment (subtle border change) is not sufficient
- [ ] No colours hardcoded in `TextStyle` / typography — omit `color` and let the theme propagate it

### Pre-release smoke test

Run before every release build. No CI substitute — these require eyes on a real device or emulator.

- [ ] Fresh install (no prior data) — app launches without crash, empty states are shown correctly
- [ ] Data survives process kill — background the app, use `adb shell am kill <package>`, reopen; all saved data present
- [ ] Notifications fire at the correct time and respect the alarm audio stream
- [ ] Theme switching applies immediately with no flash or restart
- [ ] PIN lock engages when the app is backgrounded and re-opened
- [ ] Biometric unlock prompt appears when PIN + biometric are both enabled
- [ ] Disclaimer appears on first install; does not appear again after acknowledgement unless version code changes

---

## Personal data (local or remote)

Applies whenever the app stores information that could identify or characterise a person — health data, location history, messages, behavioural patterns, financial records, etc.

### Authentication

- [ ] **Optional PIN lock** with:
  - PBKDF2-HMAC-SHA256, minimum 100,000 iterations, 16-byte `SecureRandom` salt
  - Constant-time comparison (`MessageDigest.isEqual`) — never `==` or `String.equals`
  - PIN cleared from memory in a `finally` block after hashing
- [ ] **Optional biometric unlock** (fingerprint / face) — only available when a PIN is set; biometric is disabled automatically if the PIN is removed
- [ ] App locks when sent to background (`ProcessLifecycleOwner.onStop`) and re-evaluates lock state on `Activity.onStart`
- [ ] Auth state transitions guarded at the data layer, not only in the UI — biometric cannot be enabled without a PIN; removing a PIN clears biometric

### Privacy

- [ ] **Privacy & medical disclaimer** shown on first install and on every app update (keyed to `BuildConfig.VERSION_CODE`); must be acknowledged before the app is usable; also accessible from Settings at any time
- [ ] **Backup & transfer exclusions** in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`: exclude the database, WAL/SHM files, and all DataStore preference files from both cloud backup and device transfer
- [ ] No network requests, no third-party analytics, no crash reporting SDK that transmits data off-device — or, if any of these are present, explicitly disclosed in the privacy disclaimer and justified in the PR that introduces them

### Data lifecycle

- [ ] **Data export** — user can export all their data in a portable format (CSV or JSON); especially mandatory when system backup is excluded (see above); export via the platform share sheet so users can save to Files, email, or a third-party app
- [ ] **Data deletion** — explicit "Delete all data" action in Settings, behind a confirmation dialog; distinct from uninstall (users may not know uninstall deletes data)
- [ ] **Retention policy** documented in the privacy disclaimer:
  - Default: data kept indefinitely until the user explicitly deletes it or uninstalls the app
  - If automatic pruning is implemented (e.g. rolling 24-month window), the policy and threshold must be disclosed in the disclaimer and configurable or opt-out in Settings
