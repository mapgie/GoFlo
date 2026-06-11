# GoFlo — Claude Code Instructions

When fixing a bug or solving a non-obvious problem, check `LESSONS.md` for prior art. If the fix produces a transferable lesson, add it to `LESSONS.md` in the same commit.

## Working in the web/remote environment

- This container has no Android SDK and no Gradle wrapper jar, so the app cannot be compiled here. Do not attempt Gradle builds, and do not report build failures caused by the missing toolchain. CI is the build check.
- Do not include "I couldn't compile, so I verified by inspection instead" style disclaimers in chat replies or PR descriptions. The reader already knows this environment can't build. Just make the change and state what it does.

## Versioning and changelog

Every PR that touches app code **must** add a changelog fragment. No exceptions.

### Scheme: `MAJOR.MINOR.PATCH[-prerelease]`

Version numbers communicate **compatibility risk**, not effort or importance.

| Bump | When to use |
|---|---|
| MAJOR | Breaking change: removes or changes behaviour users depend on, destructive DB migration (data loss risk), incompatible export/backup format change |
| MINOR | Backward-compatible addition: new feature, new screen, new setting, deprecation of existing behaviour |
| PATCH | Backward-compatible fix: bug fix, copy change, performance improvement, internal refactor with no user-visible impact |

When in doubt between MINOR and MAJOR, ask: can a user who doesn't update continue using
their exported data without loss? If yes → MINOR.

Pre-release suffix: `-beta.N`. Current status: **beta** — all versions carry `-beta.N`
until explicitly promoted.

### How to record a change (every PR)

Do **not** edit `CHANGELOG.md` or `app/build.gradle.kts`'s `versionCode`/`versionName`
directly — these are owned by the release automation and editing them in a feature PR is
the main source of merge conflicts. Instead, add **one** fragment file at
`changelog/unreleased/<short-slug>.json`:

```json
{
  "bump": "patch",
  "added": ["..."],
  "changed": ["..."],
  "fixed": ["..."]
}
```

`bump` is required (`patch`/`minor`/`major`); include only the `added`/`changed`/`fixed`
sections that apply, each a list of one-line user-facing descriptions. CI
(`changelog-check.yml`, via `check_changelog_fragment.py`) fails the PR if no valid
fragment is added, and also fails any non-release PR that edits
`versionCode`/`versionName`. See `changelog/unreleased/README.md` for details.

### Cutting a release

The "Prepare release" GitHub Actions workflow (`workflow_dispatch`,
`.github/workflows/prepare-release.yml`) runs `consolidate_changelog.py`, which:
- re-validates and gathers all fragments in `changelog/unreleased/`
- computes the overall bump as the highest severity among them
- bumps `versionCode` (+1) and `versionName` in `app/build.gradle.kts` — a `patch`-level
  release increments `beta.N`; `minor`/`major` reset to `beta.1`
- writes one consolidated entry at the top of `CHANGELOG.md`
- deletes the consumed fragments
- opens a `Release vX.Y.Z` PR for review (after failing fast if the computed version
  already has a git tag)

**Merging the Release PR publishes the release automatically.** The publish-release
workflow (`.github/workflows/publish-release.yml`) detects the version change on `main`,
creates the `vX.Y.Z` tag, builds the APK, and creates the GitHub release. The tag is the
duplicate gate — tag creation is atomic at the remote, so a version can only ever be
published once. There is no manual publish dispatch; do not re-add one.

Promoting out of beta (dropping the `-beta.N` suffix) remains a manual edit, done via a
`release/*` branch (CI blocks version edits on any other branch).

### Changelog immutability rules — NO EXCEPTIONS

- **Never edit an existing entry.** Once a changelog entry is committed, its version string and change list are frozen. Treat them like a released tag.
- **Never reuse a version string.** Released versions are immutable — never re-tag, amend, or reuse a version string.
- **Never delete an entry.** Even if a feature was reverted, keep the original entry and add a new entry at the top describing the revert.
- **The "What's New" dialog shows only the 5 most recent entries.** The full list in `CHANGELOG.md` is the permanent record; users see a summary.

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
