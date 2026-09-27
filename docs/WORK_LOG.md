# Work Log

## Working rules

All entries are in English. Record observed results and distinguish successful
checks, warnings, and unverified behavior. Before every commit, present the scope
and test results and wait for explicit user approval, as recorded in
[AGENTS.md](../AGENTS.md). Task checkboxes indicate verified work; commit status
is recorded separately.

## Initial checkpoint: F02 and F01

Date: 2026-09-27 (Europe/Istanbul).
Status: committed after the user approved the proposed scope with "devam".
Branch: `main`. Commit: `f224494`. Remote: none.

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
- At this checkpoint, remote publication, Hilt setup, and feature implementation
  were pending. Current scope keeps all application code in app.

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
agreement, and English project documents. Approved and committed as `f224494`.
The initial snapshot preserves the starter's Windows wrapper line endings.


## Superseded F03 attempt: separate Kotlin/JVM domain module

Date: 2026-09-27 (Europe/Istanbul).
Status: superseded by the user's single app module instruction. The separate
module was never approved or committed and has now been removed. Results below
are historical checks for that discarded setup, not current build instructions.

### Changes

- Registered `:domain` and added the shared Kotlin/JVM plugin alias.
- Configured a JDK 17 toolchain with matching Java/Kotlin bytecode target 11,
  consistent with the current app target. Gradle still uses its existing daemon JDK.
- Configured JUnit 4 using the existing catalog entry.
- Documented the module's purpose, source layout, and verification commands.
- Excluded the IDE metadata directory after Android Studio generated additional
  project files. No IDE files were removed from disk.
- Updated progress and recorded the approved baseline commit.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :domain:build :domain:dependencies --configuration testRuntimeClasspath :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` | Passed; 32s reported by Gradle |
| `./gradlew :domain:build :domain:dependencies --configuration compileClasspath --console=plain` | Passed independently of app tasks; 1s reported by Gradle |
| Domain compile classpath | Kotlin stdlib 2.2.10 and JetBrains annotations only |
| Domain test runtime | Kotlin stdlib, JUnit 4.13.2, Hamcrest, and annotations; no Android dependencies |
| Domain compile/test tasks | NO-SOURCE; the module has no production or test classes yet |
| Existing app unit tests | 1 passed; 0 failures/errors/skips |
| Android lint | 0 errors; 17 warnings |
| Repository/documents | Diff whitespace, local links, task order, and Git exclusions checked |

No placeholder domain tests were added. A configured test runtime is not a passed
behavioral test. L01 introduces real models and their tests. Device tests were
not repeated because this step changes build configuration and documentation;
the app source is unchanged and its debug assembly/unit checks passed.

### Limits and follow-up

- The added lint warning is a newer-version notice for the new Kotlin/JVM plugin
  alias. The previous 16 baseline warnings remain.
- The existing NDK symbol-stripping warning remains; debug packaging succeeded.
- The shared Kotlin 2.2.10 version is retained. Its published fully supported
  Gradle range does not cover the existing 9.6.0 wrapper. Successful local checks
  do not establish full toolchain compatibility; F06 must review and align the
  Kotlin/AGP/Gradle/Hilt/KSP combination before completing integration.
- See [Kotlin Gradle configuration](https://kotlinlang.org/docs/gradle-configure-project.html)
  for toolchains and the support matrix. App targets and daemon JVM requirements
  are separate settings and should not be changed merely to match a version number.

### Time accounting

Observed interval: 23:46:07 to 23:49:08 Europe/Istanbul (3m 01s). Includes tool
permission waits and excludes unmeasured planning, final review, and documentation.

### Commit approval

The proposed separate-module commit was not approved and was not created.
The current F03 correction below supersedes that proposed scope.


## F03 correction: keep all code in app

Verification recorded: 2026-09-28, 00:00:36 (Europe/Istanbul).
Status: committed as `c46ce1b` after the user approved with "go".

### Scope

- Removed the uncommitted domain build file, README, and generated build outputs.
- Restored settings, root build plugins, and version catalog to the approved
  baseline, which includes only app. No committed module was deleted.
- Documented UI/domain/data/DI/background/notification packages inside app.
- Kept Hilt as the DI decision and moved all planned unit/device tests under app.
- Removed the two roadmap tasks for separate data and presentation modules;
  retained task IDs for the remaining 24 tasks.
- Recorded the single-module constraint in AGENTS.md.
- Retained the pending IDE metadata exclusion and baseline commit records.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew projects :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` | Passed; 10s reported by Gradle |
| Gradle project hierarchy | Only `:app` |
| App unit tests | 1 passed; 0 failures/errors/skips |
| Lint | 0 errors; 16 baseline warnings |
| Gradle configuration | Byte-for-byte identical to approved baseline settings/root build/version catalog |
| Domain scaffold | Removed, including the generated JAR; no separate module remains |
| Documentation | 24 tasks with valid dependencies, local links, and consistent single-module decisions |

The existing local NDK stripping warning remains. No application source or
runtime behavior changed. Device tests were not repeated for this correction;
the earlier baseline device test and launch results remain historical evidence.
The latest build and unit test were rerun after removing the module.

### Commit approval

Proposed message: `docs: adopt single app module architecture`.
The committed scope relative to `f224494` contains English planning/progress
documents, AGENTS.md, and the IDE metadata exclusion. It contains no Gradle or app
source changes. The discarded domain module was never committed.

The user approved this correction with "go"; commit `c46ce1b` was created before
starting F06. F06 requires its own approval before commit.


## F06: Hilt foundation in app

Date: 2026-09-28 (Europe/Istanbul).
Status: committed as `2b9789e` after separate user approval.
Commit: `build: configure Hilt dependency injection`.

### Scope

- Added Hilt/KSP catalog entries, plugins, runtime/compiler dependencies, and
  device-test support. All production and test source remains in app.
- Added EarthquakeApplication with @HiltAndroidApp and registered it in the
  manifest; annotated MainActivity with @AndroidEntryPoint.
- Added di/TimeModule with a singleton system UTC Clock. This is the time source
  for future window/eligibility rules; no business behavior is implemented yet.
- Added HiltTestRunner and a graph/UI smoke test. It resolves Clock through Hilt,
  checks its UTC zone, shared instance and current time, and verifies the greeting.
- Retained the two starter tests; their coverage remains limited to the test setup.
- Updated the English architecture, roadmap, guideline review, and work records.

### Versions and compatibility

| Item | F06 configuration |
| --- | --- |
| Hilt plugin/runtime/compiler/testing | 2.60.1 |
| KSP | 2.3.12 |
| Kotlin Gradle / Compose compiler | 2.3.21 (previously 2.2.10) |
| AGP / Gradle wrapper | 9.4.1 / 9.6.0, unchanged |
| Java / Kotlin bytecode target | 17 / 17 (previously 11) |
| Launcher / daemon JVM | Zulu 17.0.12 / Temurin 25.0.3, unchanged |
| Compile / target / minimum SDK | 37 / 37 / 29, unchanged |

Hilt 2.59 introduced AGP 9 support; 2.60.1 includes fixes on that line. Its 2.60
release moved Kotlin dependencies to 2.3.21, so this setup aligns the explicit
Kotlin Gradle and Compose compiler versions to 2.3.21. AGP's built-in Kotlin
remains enabled. The root build uses the documented buildscript dependency to
override AGP's bundled compiler version; no kotlin-android or kapt plugin is added.
KSP performs code generation, including instrumentation-test components.

Compatibility evidence is the local build/test result, not a claim that every
combination is officially covered: Kotlin 2.3.21's standalone KGP support table
lists Gradle through 9.3 and AGP through 9.0, below this project's existing
AGP/Gradle pair. AGP 9 uses its own built-in Kotlin integration. No compatibility
opt-out, validation suppression, or framework upgrade beyond F06 dependencies was
needed. A clean checkout/second-machine reproduction remains H02. AndroidX Hilt
ViewModel/Worker integrations will be validated when introduced.

References checked:

- [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup.html)
- [Dagger/Hilt release notes](https://github.com/google/dagger/releases)
- [KSP release notes](https://github.com/google/ksp/releases)
- [AGP built-in Kotlin version override](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Built-in Kotlin targets](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Hilt instrumentation setup](https://dagger.dev/hilt/instrumentation-testing.html)

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain` | Passed; Gradle reported 1m 25s, 145 tasks executed |
| Debug/release Hilt generation and bytecode transformation | Passed; Application/Activity graph and Clock provider generated |
| Java/Kotlin class-file inspection | Both major version 61, confirming JVM 17 targets |
| Unit tests | 1 starter test passed; 0 failures/errors/skips |
| Lint | 0 errors, 16 warnings; 8 dependency/plugin notices, 7 unused colors, 1 redundant label |
| `./gradlew :app:connectedDebugAndroidTest --console=plain` | Passed; Gradle reported 11s |
| Device tests on Android 10 / API 29 | 2 passed: starter context and Hilt graph/UI smoke test; 0 failures/errors/skips |
| Production APK reinstallation and cold Activity launch | Passed; adb reported Status: ok, LaunchState: COLD |
| UI hierarchy after production launch | MainActivity shows Hello Android! |

The device-test runner substitutes HiltTestApplication; the separate normal APK
launch confirms the production EarthquakeApplication wiring. Release assembly
uses the existing unoptimized configuration and does not establish a signed
release or shrinker validation. No feature/domain coverage is claimed yet.

Existing local warnings persist: the incomplete NDK installation prevents native
symbol stripping, and device-test protobuf tooling emits an Unsafe deprecation
warning under the daemon JVM 25. Neither failed the checks; the shared SDK was not
modified. No full elapsed-work duration was measured; Gradle durations above are
command-reported execution times, not total development time.

F06 was committed as `2b9789e` after user approval. L01 followed as the next
roadmap task.


## L01: Initial domain contract

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Added the plain Kotlin `Earthquake` model with nullable source fields and
  constructor invariants for identity, finite measurements, and valid coordinate
  pairs/ranges. Negative finite magnitudes remain valid.
- Added `EarthquakeRepository` with a Flow for the rolling 24-hour list (newest
  first) and a suspend refresh operation.
- Added typed refresh success/failure outcomes, including accepted-record count
  and network, invalid-response, storage, and unknown failure categories.
- Added the direct kotlinx-coroutines-core dependency required by the public Flow
  contract and focused unit tests for model invariants and refresh outcomes.
- Updated the roadmap and data contract to distinguish the completed domain
  contract from the still-pending USGS adapter and concrete repository.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` | Passed on retry; Gradle reported 1m 27s, 59 tasks executed |
| JVM unit tests | 9 passed, 0 failures/errors/skips (5 Earthquake, 3 refresh result, 1 starter) |
| Debug APK assembly | Passed |
| Android lint | Passed |
| `git diff --check` | Passed |

The first Gradle invocation timed out after 120 seconds while downloading the
Gradle distribution; the retry completed successfully. The build still reports
the existing local NDK warning (`source.properties` missing; native library
packaged without symbol stripping). No Android device test was needed for this
pure domain/API contract change. No full development-time interval was measured.

### Commit

Message: `feat: define earthquake domain contract`.
Scope is limited to the domain model/repository contract, its focused tests, the
direct Flow dependency, and the corresponding English documentation/records.
