# Work Log

## Working rules

All entries are in English. Record observed results and distinguish successful
checks, warnings, and unverified behavior. Before every commit, present the scope
and test results and wait for explicit user approval, as recorded in
[AGENTS.md](../AGENTS.md). Task checkboxes indicate verified work; commit status
is recorded separately.

## Initial checkpoint: F02 and F01

Date: 2026-09-27 (Europe/Istanbul).
Status: verified; awaiting user approval for the first commit.
Branch: `main`. Commit: none. Remote: none.

### Scope

- Initialized the existing project as a local Git repository.
- Preserved the Android starter source and its existing build configuration.
- Added ignore rules for future module build directories, Kotlin cache, local
  environment files, and signing material. Existing local/IDE exclusions remain.
- Added the project working agreement and English work/AI usage records.
- Updated roadmap order: Git setup precedes baseline verification, as requested.
- Prepared the existing planning documents for the initial repository snapshot.

### Environment observed

| Item | Observed value |
| --- | --- |
| Host | macOS arm64 |
| Gradle wrapper | 9.6.0 |
| Gradle launcher JVM | Zulu 17.0.12 |
| Gradle daemon JVM | Eclipse Adoptium 25.0.3, selected by the existing daemon criteria |
| Android Gradle Plugin | 9.4.1 |
| Compose compiler plugin | 2.2.10 |
| Compile / target SDK | 37 / 37 |
| Minimum SDK | 29 |
| Device | Existing flutter_emulator AVD, Android 10 / API 29, arm64 |

The JVM bundled inside Gradle is distinct from the project's Kotlin/Compose
plugin version. Hilt/KSP compatibility remains a separate F06 task.

### Verification results

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` | Passed; 1m 11s reported by Gradle |
| Unit tests | 1 passed; 0 failures/errors/skips |
| Lint | 0 errors; 16 warnings |
| `./gradlew :app:connectedDebugAndroidTest :app:installDebug --console=plain` | Passed; 14s reported by Gradle |
| Device tests | 1 passed; 0 failures/errors/skips on API 29 |
| APK install and cold Activity launch | Passed after reinstalling the APK following device tests |
| UI inspection | UI hierarchy and screenshot show `Hello Android!` in MainActivity |
| Git exclusions | Local properties, IDE workspace, Gradle/Kotlin caches, module builds, and signing/environment files excluded |
| Documentation checks | Task dependencies, English text, local links, and completed/pending statuses checked |

The existing unit test checks arithmetic and the device test checks the package
context. They verify the starter test setup, not future earthquake behavior.
No new application behavior was added in this checkpoint.

### Warnings and limits

- Lint: 8 dependency/plugin update notices, 7 unused color resources, and 1
  redundant Activity label. No lint baseline or suppression was added.
- The local NDK 28.2.13676358 installation lacks source.properties. Debug assembly
  succeeded, packaging libandroidx.graphics.path.so without symbol stripping.
  The shared SDK installation was not modified.
- Device test tooling emitted a sun.misc.Unsafe deprecation warning on JVM 25.
  Tests passed. Reassess toolchain compatibility during F06.
- Device coverage is API 29 only. Current-target-device and notification-permission
  verification belong to later Android feature checks.
- Device-test cleanup left the app unavailable to the first launch attempt.
  Reinstalling the built APK restored a successful cold launch; no source fix
  was required.
- Remote publication, Hilt setup, new modules, and feature implementation are pending.

### Evidence

Generated reports remain excluded from Git and can be regenerated:

- Unit results: `app/build/test-results/testDebugUnitTest/`.
- Device results: `app/build/outputs/androidTest-results/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Device report: `app/build/reports/androidTests/connected/debug/index.html`.

Temporary UI evidence was inspected locally; it is not part of the initial commit.
The emulator is left running with the starter app open.

### Time accounting

An observed verification interval ran from 23:08:33 to 23:16:39 Europe/Istanbul
(8m 06s). It includes tool permission waits and is not a full development-time
estimate. Earlier planning and later documentation time were not measured.

### Commit approval

Proposed message: `chore: initialize project and verify Android baseline`.
Scope: Android starter, Gradle wrapper/configuration, ignore rules, working
agreement, and English project documents. Approval is pending; nothing is committed.
