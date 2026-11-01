# OVELIN SPACE

Native Android app for locally importing APKs, recording app metadata, and managing files in a private workspace.

## What works in this build

- Kotlin, Jetpack Compose, and a local Room/SQLite database.
- Arabic RTL and English LTR interface.
- Lists installed apps and imports their available base/split APK files into app-private storage.
- Imports a selected APK through Android's document picker, reads its package/version metadata, and records the signer's SHA-256 certificate fingerprint.
- File manager for the private workspace: search, folders, rename, copy, move-to-root, details, and delete.
- Storage and RAM summaries.
- Local import logs and schema for app, instance, settings, and storage records.
- Virtual app instances via the embedded BlackBox engine (create, open, stop, delete).

## Virtual app cloning

Cloning uses the embedded **BlackBox** virtualization engine (Apache-2.0, see `Bcore/LICENSE-BlackBox-Apache-2.0`). Each clone is a separate virtual user that holds one installed copy of the app with its own data. Use *Apps → Clone*, then manage copies in the *Instances* tab (open, stop, delete).

- **The number of clones is bound by free storage and RAM**, not unlimited. The Instances tab shows an estimate of how many more fit.
- **Compatibility varies per app.** Banking apps and anything that checks device integrity (Play Integrity, root/emulator detection) may refuse to run. This project does not try to defeat those checks.
- **Device identity:** by default every clone presents itself as a Google Pixel 6 with its own Android ID; you can set a different manufacturer/model per clone when creating it. Some apps restrict accounts when they detect identity changes.
- The host targets API 28 because the engine needs pre-API-29 behaviour. The APK is for sideloading and is not eligible for Google Play.
- **Not tested here.** The engine integration was written without an Android SDK or device available; build it with the GitHub workflow and verify on a physical device (ARM64) before relying on it.

### Changes made to the upstream engine

- Removed `LogSender` (it uploaded logcat and device info to a third-party server) and `closecode/Entry` (loaded an `inject.so` from app data). `sendLogs` is now a no-op.
- Device spoofing is unchanged from upstream (Pixel 6 default for every virtual user).
- Removed the native `AntiDetection` module (it auto-installed hooks hiding root and virtual-environment traces from apps).
- The engine still uses hidden-API access (FreeReflection). It cannot run without it.

## Storage and privacy

- Imported APKs and app icons remain under this app's private files directory.
- APK selection uses Android's Storage Access Framework; the app does not request broad shared-storage access.
- Installed-app discovery uses `QUERY_ALL_PACKAGES`. Distribution through Google Play would require meeting Google's package-visibility policy; this build is delivered as a sideloadable test APK.
- The signer fingerprint identifies the certificate presented by Package Manager. It is not a malware scan, trust verdict, or guarantee that an APK is safe.
- Uninstalling OVELIN SPACE removes its private workspace under Android's normal app-data rules.

## Build

Open this directory as a native Android Gradle project, or push it to the `main` branch of the linked GitHub repository. The GitHub Actions workflow (JDK 21, NDK 27.0.12077973) builds `app-debug.apk` and attaches it to a versioned Release.

The automatically produced APK is debug-signed for testing. It is not a production/Play Store signing artifact.
