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


## L02: Deterministic sample repository

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Added a singleton in-memory `SampleEarthquakeRepository` bound to the domain
  repository contract through Hilt.
- Added three stable sample events with times based on the injected Clock when the
  repository is created; reset restores that same immutable sample set.
- Added controllable empty-list, held-refresh, and one-shot failure scenarios.
  A failed refresh preserves the observed list, and a later retry succeeds.
- Added fixed-clock unit tests covering stable newest-first content, empty/reset,
  failure/retry, and a refresh held until explicitly released.
- Updated architecture, data contract, and roadmap status to reflect the
  implemented sample slice and still-pending USGS source.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` | Passed; Gradle reported 8s, 59 tasks (27 executed, 32 up-to-date) |
| JVM unit tests | 13 passed, 0 failures/errors/skips (4 sample repository, 9 prior) |
| Hilt code generation and debug APK assembly | Passed |
| Android lint | Passed |
| `git diff --check` | Passed |

No device test was needed because this step adds a repository and its JVM tests,
not a screen or Android platform behavior. Total development time was not
measured; the Gradle duration is build execution time only.

### Commit

Message: `feat: add deterministic sample repository`.
Scope is limited to the sample repository, Hilt binding, repository unit tests,
and the corresponding English documentation/records.


## L03: Earthquake list state and ViewModel

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Added immutable `EarthquakesUiState` and a Hilt `EarthquakesViewModel` with
  initial loading, manual refresh, retry, typed error state, and last-success time.
- Combined the repository list stream with refresh state using
  `SharingStarted.WhileSubscribed`; refresh work runs in `viewModelScope` and
  cancellation is rethrown rather than converted to a regular failure.
- Added lifecycle ViewModel and coroutine test dependencies plus a Main dispatcher
  rule for deterministic coroutine tests.
- Added fake-repository tests for content, empty results, initial failure and
  retry, failed refresh retaining existing content, and a refresh held in progress.
  Tests actively collect the WhileSubscribed state stream.
- Updated architecture and roadmap records; the Compose screen remains L04.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain` | Passed on retry; Gradle reported 3s, 59 tasks (10 executed, 49 up-to-date) |
| JVM unit tests | 18 passed, 0 failures/errors/skips (5 ViewModel, 13 prior) |
| Debug APK assembly and Hilt/KSP generation | Passed |
| Android lint | Passed |
| `git diff --check` | Passed |

The first build attempt failed compiling the test dispatcher rule because its
`setMain`/`resetMain` extension imports were missing. Added the imports; the
subsequent full check passed. The build also reported the existing local NDK
`source.properties` warning and packaged a native library without symbol
stripping. No device test was run; this step adds ViewModel logic and JVM tests,
not a rendered screen. Total development time was not measured.

### Commit

Message: `feat: add earthquake list ViewModel state`.
Scope is limited to the list UI state/ViewModel, required lifecycle/coroutine test
dependencies, focused JVM tests, and the corresponding English documentation.


## L04: Earthquake list screen

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Replaced the starter greeting with the Turkish earthquake-list route and screen.
  The route obtains its Hilt ViewModel and collects state with
  `collectAsStateWithLifecycle`; reusable screen content takes state and callbacks.
- Added magnitude, place, occurrence time, depth, last-update display, refresh and
  retry controls, plus explicit initial-loading, refreshing, empty, and error
  presentations. Cached content remains visible while an initial refresh runs;
  labels communicate magnitude and errors in text, not color alone.
- Added Turkish resources, locale-aware magnitude/time formatting, Hilt Compose
  ViewModel integration, and seven Compose instrumentation tests.
- Updated the Hilt Activity smoke test and project records.

### Verification

| Check | Result |
| --- | --- |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | Passed; Gradle reported 3s, 85 tasks (12 executed, 73 up-to-date) |
| JVM unit tests | 18 passed, 0 failures/errors/skips |
| `./gradlew :app:lintDebug --console=plain` | Passed; Gradle reported 2s |
| `./gradlew :app:connectedDebugAndroidTest --console=plain` | Passed on Android 10 / API 29; 9 tests, 0 failures/errors/skips (7 screen, 2 existing) |
| Manual launch and UI inspection | Passed; sample list, time/magnitude labels, and font scale 1.3 inspected on emulator |
| Font setting restoration | Restored to 1.0 after large-font check |
| `git diff --check` | Passed |

An initial combined lint/build run hit a lint analyzer error while reading a
missing KSP-generated backup source; a standalone lint run passed. The first
instrumentation attempt could not install over an app signed with a different
debug key; installing the current APK and rerunning produced 9 passing tests.
The compile also reported the existing incomplete local NDK warning and native
symbol stripping fallback. Device tooling emitted its existing JVM 25
`sun.misc.Unsafe` warning. No total development-time interval was measured.

The user later reported Android Studio could not delete generated Dex/Hilt output.
Inspection found 2,466 root-owned entries under `app/build`, created by Gradle
commands run from the root-owned assistant shell. Stopped that root Gradle daemon
and changed ownership of only those generated entries to `mlhysrszn:staff`; no
build files were deleted or source files changed. The user confirmed Android
Studio builds successfully afterward.

### Commit

Message: `feat: build earthquake list screen`.
Scope is limited to the Compose list route/content, localized strings, Hilt
ViewModel route integration, screen instrumentation tests, and updated records.


## D01: USGS GeoJSON client and mapper

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Added a Retrofit 3.0.0 service for the USGS all-day GeoJSON feed and a singleton
  Hilt Retrofit/JSON setup. Added the Android INTERNET permission. The service is
  injectable; this task does not yet fetch or persist live data in the repository.
- Added Kotlin serialization DTOs and mapper. Unknown keys are ignored; accepted
  earthquakes map to domain fields, sort newest first, and retain source-generated
  time separately.
- Defined parsing behavior: malformed JSON/root/feed structure throws
  `MalformedUsgsFeedException`; missing/blank identity, missing/unconvertible
  occurrence time, and non-earthquake features are skipped individually.
  Invalid coordinate pairs are omitted without dropping otherwise valid events;
  missing optional fields and negative finite magnitudes are preserved.
- Added a recorded summary JSON fixture and five focused JVM tests for mapping,
  optional/invalid fields, coordinate order, unknown keys, filtering, and malformed
  feed handling. Hilt graph smoke test now resolves the service without a network call.
- Updated the roadmap, architecture, and data contract to show that live
  repository synchronization and persistence remain future tasks.

### Versions and verification

| Item/check | Result |
| --- | --- |
| Retrofit | 3.0.0 |
| kotlinx.serialization JSON | 1.11.0; compiler plugin aligned to Kotlin 2.3.21 |
| `sudo -u mlhysrszn -H ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | Passed; Gradle reported 43s, 85 tasks executed |
| JVM unit tests | 23 passed, 0 failures/errors/skips (5 USGS mapper, 18 prior) |
| `sudo -u mlhysrszn -H ./gradlew :app:lintDebug --console=plain` | Passed; Gradle reported 9s |
| `sudo -u mlhysrszn -H ./gradlew :app:connectedDebugAndroidTest --console=plain` | Passed on Android 10 / API 29; 9 tests, 0 failures/errors/skips; injected Retrofit service resolved |
| `git diff --check` | Passed |

No live USGS request was made; network fetch, HTTP failure handling, and
repository synchronization are not claimed complete. Build output remained
owned by `mlhysrszn` because verification ran as the project user. The existing
incomplete local NDK warning and native-symbol stripping fallback remain;
device tooling emitted the known protobuf/JVM 25 `sun.misc.Unsafe` warning.
Total development time was not measured.

### Commit

Message: `feat: add USGS GeoJSON mapping`.
Scope is limited to DTOs, mapper and fixture/tests, Retrofit service/Hilt setup,
INTERNET permission, and corresponding English documentation/records.


## D02: Room event store and synchronization metadata

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope and policy

- Added Room 2.8.5 runtime/ktx/compiler/testing dependencies, KSP schema export,
  and the version-1 database schema under `app/schemas`.
- Added event and singleton synchronization-metadata entities, epoch-millisecond
  mappings, indexed occurrence time, DAO, Room database, and singleton Hilt
  providers. Hilt graph test resolves the DAO.
- Defined retention/list window as the inclusive interval `[now - 24 hours, now]`;
  future-dated and older records are excluded. `observeRecent` uses this window.
- A fresh, structurally valid feed atomically replaces the event snapshot within
  that interval, and a valid empty snapshot clears it. A source generation older
  than the last applied generation is ignored. If the stored generation is known,
  an incoming snapshot without a generation is also ignored; events and source
  generation remain intact while local successful-fetch time advances. An initial
  unversioned snapshot is allowed when no source generation is known.
- Added entity mapping JVM tests and five Room instrumentation tests for upsert,
  observable ordering/window bounds, atomic snapshot plus metadata updates, stale
  and unversioned snapshots, valid empty feeds, and persistence after reopening.
- Repository/network coordination remains D03; no live network fetch was added.

### Verification

| Check | Result |
| --- | --- |
| `sudo -u mlhysrszn -H ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | Passed; Gradle reported 7s, 85 tasks (22 executed, 63 up-to-date) |
| JVM unit tests | 25 passed, 0 failures/errors/skips (2 entity mapping, 23 prior) |
| `sudo -u mlhysrszn -H ./gradlew :app:lintDebug --console=plain` | Passed; Gradle reported 4s |
| `sudo -u mlhysrszn -H ./gradlew :app:connectedDebugAndroidTest --console=plain` | Passed on Android 10 / API 29; 14 tests, 0 failures/errors/skips (5 Room) |
| `git diff --check` | Passed |
| Generated Room schema | Created at `app/schemas/com.mlhysrszn.earthquake.data.local.room.EarthquakeDatabase/1.json` |

Existing local NDK source-properties/native-symbol warnings and the device
protobuf/JVM 25 `sun.misc.Unsafe` warning remain. Gradle checks ran as the project
user; no root-owned files were created under `app/build`. Total development time
was not measured.

### Commit

Message: `feat: add Room earthquake event store`.
Scope is limited to Room dependencies/configuration/schema, entities/DAO/mappings,
database Hilt providers, focused tests, and corresponding English records.


## D03: USGS synchronization through the repository

Date: 2026-09-28 (Europe/Istanbul).
Status: committed after user approval.

### Scope

- Added singleton `UsgsEarthquakeRepository` as the production
  `EarthquakeRepository` binding. List observations read the rolling Room window;
  refresh fetches the USGS service, maps DTOs, and atomically applies the snapshot.
- Serialized overlapping refreshes with a `Mutex`. Cancellation is rethrown.
  Network/HTTP errors map to `NETWORK`, malformed feeds to `INVALID_RESPONSE`,
  Room SQL errors to `STORAGE`, and unexpected exceptions to `UNKNOWN`; failures
  preserve cached events.
- Added an Android test `@TestInstallIn` replacement that binds the sample
  repository, so instrumentation is deterministic and makes no live HTTP calls.
- Added six repository integration tests using a fake service and in-memory Room:
  fresh feed storage, network and malformed-response cache preservation, stale
  snapshot behavior, serialized overlapping requests, and cancellation.
- Manually launched the production APK on the emulator and confirmed current USGS
  events appeared in the Room-backed list.
- Updated architecture, data contract, and roadmap records. The ViewModel contract
  is unchanged; D02's retention and stale-snapshot policies remain in force.

### Verification

| Check | Result |
| --- | --- |
| `sudo -u mlhysrszn -H ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | Passed; Gradle reported 5s, 85 tasks (26 executed, 59 up-to-date) |
| JVM unit tests | 25 passed, 0 failures/errors/skips |
| `sudo -u mlhysrszn -H ./gradlew :app:lintDebug --console=plain` | Passed; Gradle reported 4s |
| `sudo -u mlhysrszn -H ./gradlew :app:connectedDebugAndroidTest --console=plain` | Passed on Android 10 / API 29; 20 tests, 0 failures/errors/skips (6 repository integration) |
| Manual production launch | Passed; live USGS magnitudes, places, and times appeared in the list |
| Generated build-file ownership | No root-owned entries under `app/build` |
| `git diff --check` | Passed |

The first compile attempt found a missing `toDomain` extension import; the import
was added and subsequent build/tests passed. Instrumentation used the test sample
binding and did not call the network; the manual production launch used the public
USGS summary feed. Existing NDK/source-properties and protobuf/JVM 25 warnings
remain. Total development time was not measured.

### Commit

Message: `feat: connect USGS feed to Room repository`.
Scope is limited to the production repository, Hilt binding, deterministic test
replacement, integration tests, and corresponding English documentation/records.
