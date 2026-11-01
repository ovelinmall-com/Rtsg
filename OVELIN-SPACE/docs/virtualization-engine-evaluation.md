# Virtualization engine evaluation

**Reviewed:** 2026-11-01

## Update 2026-11-01

BlackBox (Blacks-BlackBox fork) is now embedded as module `:Bcore`, with the changes listed in the README: remote log upload and `inject.so` loading removed, device-identity spoofing kept as upstream (the owner decided to keep it), hidden-API access retained because the engine requires it. Requirements 3–5 below still need physical-device verification; none of it has been run yet.

## Original decision (superseded)

No third-party virtualization engine is included in the first build. The reviewed candidates do not yet establish a sufficiently safe and independently verified fit for this product. In particular, the project rules exclude device-identity spoofing and attempts to bypass app security or authentication. The app therefore reports that virtual instances are unavailable and does not create placeholder instances.

## Candidates reviewed

### Blacks-BlackBox

- Source: https://github.com/Black00Z/Blacks-BlackBox
- Repository metadata at review: Apache-2.0, not archived, last pushed 2026-10-07; approximately 1.1 MB of source.
- The checked-in build declares compile SDK 35, target SDK 28, and minimum SDK 21.
- The README claims newer Android support, but this was not verified on physical Android devices.
- Source inspection found hidden-API bypass dependencies and device/Android-ID spoofing and anti-detection code paths. These are not suitable for this project’s stated security boundary.
- **Decision:** do not embed or enable it. A future review would need to remove those paths, audit the full dependency tree, and verify ordinary app compatibility on supported devices.

### PrismSpace

- Source: https://github.com/mhmdwaelanwr/PrismSpace
- Repository metadata at review: Apache-2.0, created 2026-08-22, last pushed 2026-09-16.
- Its own project description calls the engine experimental and distinguishes successful compilation/hook loading from proof that arbitrary apps run correctly.
- **Decision:** not mature enough for the APK without device-level compatibility and isolation tests.

### VirtualXposed

- Source: https://github.com/android-hacker/VirtualXposed
- Repository metadata at review: GPL-3.0, not archived, last pushed 2024-03-08.
- The repository is older and based on app hooking. Its current Android compatibility and the project's licensing obligations have not been established for this product.
- **Decision:** not selected.

### VirtualApp

- Source checked: https://github.com/asLody/VirtualApp
- The repository endpoint was unavailable during review, so its current source and license could not be verified.
- **Decision:** do not use unverified code.

## Supported alternative

Android's managed Work Profile is the system-supported route for separate app data on devices that allow it. Provisioning requires the Android-managed setup flow and user/admin consent; device and OEM rules determine availability. It is not a way to make an unlimited number of copies, and OVELIN SPACE does not provision or manage a Work Profile in this release.

## Requirements before adopting an engine

1. Verify the complete source and dependency licenses and preserve required notices.
2. Confirm no device-identity spoofing, integrity-check evasion, credential interception, or other security-bypass feature is shipped.
3. Test installation, launch, data separation, permissions, notifications, updates, uninstall, and cleanup on physical devices across the declared Android versions and CPU architectures.
4. Test apps only where the user is authorized to run them; make incompatibilities explicit instead of silently bypassing app protections.
5. Make clone count resource-bound and report storage/memory limits rather than promising unlimited instances.
