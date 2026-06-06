# GoFlo — Claude Code Instructions

When fixing a bug or solving a non-obvious problem, check `LESSONS.md` for prior art. If the fix produces a transferable lesson, add it to `LESSONS.md` in the same commit.

## Working in the web/remote environment

- This container has no Android SDK and no Gradle wrapper jar, so the app cannot be compiled here. Do not attempt Gradle builds, and do not report build failures caused by the missing toolchain. CI is the build check.
- Do not include "I couldn't compile, so I verified by inspection instead" style disclaimers in chat replies or PR descriptions. The reader already knows this environment can't build. Just make the change and state what it does.

## Versioning

Every PR **must** include a version bump and a changelog entry. No exceptions.

### Scheme: `MAJOR.MINOR.PATCH[-prerelease]`

Version numbers communicate **compatibility risk**, not effort or importance.

| Bump | When to use |
|---|---|
| MAJOR | Breaking change: removes or changes behaviour users depend on, destructive DB migration (data loss risk), incompatible export/backup format change |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation of existing behaviour |
| PATCH | Backward-compatible fix: bug fix, copy change, performance improvement, internal refactor with no user-visible impact |

Rules:
- Releasing a MINOR resets PATCH to 0 (`1.4.2 → 1.5.0`)
- Releasing a MAJOR resets MINOR and PATCH to 0 (`1.4.2 → 2.0.0`)
- Released versions are immutable — never re-tag or amend a released version
- When in doubt between MINOR and MAJOR, ask: can a user who doesn't update continue using their exported data without loss? If yes → MINOR.

Pre-release suffix: `-beta.N` (increment N for each beta on the same base version).

Current status: **beta** — all versions carry `-beta.N` until explicitly promoted.

### How to bump

1. Update `versionCode` (always increment by 1) and `versionName` in `app/build.gradle.kts`
2. Add a new entry at the top of `CHANGELOG.md` following the existing `## [version] - date` format
3. Include both changes in the same commit as the feature/fix

### Changelog immutability rules — NO EXCEPTIONS

- **Never edit an existing entry.** Once a changelog entry is committed, its version string and change list are frozen. Treat them like a released tag.
- **Never reuse a version string.** If a merge conflict tempts you to keep a version number that already exists in the file, bump to the next available number instead.
- **Never delete an entry.** Even if a feature was reverted, keep the original entry and add a new entry at the top describing the revert.
- **Merge conflicts in CHANGELOG.md must preserve both sides.** When resolving a conflict, keep all entries from both branches and order them by version number (newest at top). If two branches used the same version number, keep both entries and renumber the lower-priority one.
- **The "What's New" dialog shows only the 5 most recent entries.** The full list in `CHANGELOG.md` is the permanent record; users see a summary.

### Examples

```
Bug fix only           → 1.4.2-beta.1 → 1.4.3-beta.1   (versionCode +1)
New feature            → 1.4.2-beta.1 → 1.5.0-beta.1   (versionCode +1)
Breaking DB migration  → 1.4.2-beta.1 → 2.0.0-beta.1   (versionCode +1)
Second beta iteration  → 1.4.2-beta.1 → 1.4.2-beta.2   (versionCode +1)
Promote to release     → 1.4.2-beta.1 → 1.4.2           (versionCode +1)
```

## Architecture Notes

- **UI layer:** Jetpack Compose + Material 3, MVVM with ViewModels; navigation via Compose Navigation
- **Data layer:** Room (SQLite), migrations in `data/db/AppDatabase.kt` — always add a migration for schema changes, never use `fallbackToDestructiveMigration`
- **Theme:** Light, Dark, and system-following variants across Coral, Teal, and Sage palettes; High Contrast and Blue & Orange accessible variants also provided. Brand font: Comfortaa Bold (used for labels, FAB, and display text); body copy uses the system sans-serif.
- **No network:** The app makes zero network requests. Do not add internet permission or any networking dependency.
- **Permissions in use:** `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `USE_BIOMETRIC`, `USE_FINGERPRINT`. Do not add others without discussion.
- **Privacy invariant:** All health data stays on-device. Cloud backup and device transfer are explicitly disabled in the manifest — do not remove these exclusions.

## Key Rules

- Sensitive health data (cycle dates, symptoms, period history) must never appear in widget content when PIN lock is enabled — show a neutral placeholder only
- `MaterialTheme.colorScheme.error` is reserved for genuine errors and destructive confirmations — do not repurpose for general UI states
- All colour-coded states must also communicate via shape or label (not colour alone) — the Blue & Orange theme exists because ~9 % of users have red-green colour blindness
- Medical disclaimer and privacy policy surfaces must always use the body font — never the Comfortaa brand font
- Minimum tap target: 44×44dp
- Never use `fallbackToDestructiveMigration` in the Room database config
- **Never use en dashes (–) or em dashes (—) in user-facing text.** They read as robotic. Use a period, colon, or reword the sentence instead. Hyphens in genuine compound words ("in-app", "4-digit", "built-in", "30-day") are fine.

## Accessibility Rules (enforced by `a11y_check.py` in CI)

Every `.clickable {}` or `.combinedClickable {}` modifier **must** carry a matching `.semantics { role = Role.<Type> }` in the same modifier chain. Use the role that best describes the element:

| Role | Use for |
|---|---|
| `Role.Button` | Navigation, generic action, expand/collapse, dialog dismiss |
| `Role.RadioButton` | Mutually exclusive single-select (theme pickers, icon pickers, format selectors) |
| `Role.Checkbox` | Toggle with two named states where the element acts as a row wrapping a Checkbox |
| `Role.Switch` | Toggle with two named states; pair with `stateDescription` to announce current state |

Additional rules:
- Place `.semantics { role = }` **before** `.clickable {}` in the chain when the clickable lambda is longer than a few lines, so the CI window check can find it.
- When the parent Row/Box handles the click, set the inner `Checkbox` / `RadioButton` to `onClick = null` to prevent double-focus.
- `clearAndSetSemantics { }` must also include `role = Role.<Type>` — it replaces all child semantics, so the role must be re-declared there.
- Status text that appears or changes in response to user action needs `Modifier.semantics { liveRegion = LiveRegionMode.Assertive }` (errors) or `LiveRegionMode.Polite` (non-urgent feedback).
- Icon-only interactive controls (FABs, icon-only buttons outside of `IconButton`) need `Modifier.semantics { contentDescription = "<action label>" }` on the container itself.
- Inline text links inside body copy must use `LinkAnnotation.Url` / `withLink` (not the deprecated `ClickableText`), so the span carries the link role and a full-phrase touch region.
- Run `python3 a11y_check.py` locally before pushing to confirm no new violations.
