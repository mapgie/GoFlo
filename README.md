# GoFlo

A private, local-only period tracking app for Android.

## Privacy

All data is stored on-device. GoFlo makes no network requests, has no accounts, and includes no analytics or crash-reporting SDKs. Cloud backup and device transfer are explicitly disabled.

## Requirements

- Android 8.0 (API 26) or higher — minimum is set at API 26 because
  `NotificationChannel` (introduced in Android 8.0) is required to create the
  alarm-stream notification channel that lets period reminders bypass Do Not
  Disturb. Raising this minimum further is a MINOR version bump and needs a
  changelog entry.
- JDK 17 for building

## Building

```bash
# Clone and open in Android Studio, or build from the command line:
git clone https://github.com/mapgie/GoFlo.git
cd GoFlo

echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties  # or set path manually

./gradlew assembleDebug        # build APK
./gradlew test                 # run unit tests
./gradlew lintDebug            # run lint
```

The debug APK is signed with the committed `debug.keystore` so all builds (local and CI) share a consistent signature. OTA updates work without reinstalling.

## Releasing

Releases are created manually via GitHub Actions → **Build & Release APK** → **Run workflow**. Every PR must bump `versionCode` and `versionName` in `app/build.gradle.kts` and add a `CHANGELOG.md` entry. CI will reject PRs where the version already has a published release.

## Contributing

See [`template_requirements.md`](template_requirements.md) for the full project standards checklist and [`LESSONS.md`](LESSONS.md) for hard-won implementation notes.

## Contact

Issues and feature requests: [GitHub Issues](https://github.com/mapgie/GoFlo/issues)
