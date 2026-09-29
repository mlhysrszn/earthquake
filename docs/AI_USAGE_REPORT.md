# AI Usage Report

Status: initial record; update at each implementation checkpoint.

## Tools and model information

- Assistant: OpenAI Codex.
- Model: GPT-6 family as identified in the session instructions; the exact
  deployment/model identifier and per-turn token totals were not captured.
- Tools used for this checkpoint: local shell, Gradle, Git, Android emulator/ADB,
  image inspection, and document editing.
- Earlier planning used the provided PDF and official Android/USGS documentation.
- No delegated subagents were used in this checkpoint.

## Initial checkpoint: F02 and F01

| Work | AI contribution | Verification / human role |
| --- | --- | --- |
| Product and architecture planning | Drafted and revised English project documents | User chose scope, Hilt, and the commit approval workflow |
| Repository setup | Initialized Git and refined ignore rules | Git status and exclusion checks; user authorized initialization |
| Environment verification | Ran build, starter tests, lint, install, and launch checks | Parsed reports and inspected UI hierarchy/screenshot |
| Implementation | No new application behavior; starter code retained | Existing template tests passed with limits documented in Work Log |
| Commit | Created the reviewed baseline snapshot | User approved; commit f224494 |

## Usage measurements

| Measurement | Value | Basis |
| --- | --- | --- |
| Exact tokens by model | Unavailable | No session token export captured |
| Model usage percentages | Unavailable | No measured token/request denominator captured |
| Measured AI share of code | Not measured | No generated feature implementation in this checkpoint |
| Observed verification interval | 8m 06s | Tool timestamps; includes permission waits, excludes unmeasured work |

Do not treat account-wide remaining quota as this project's usage. When a usage
export becomes available, record exact model identifiers and either token or
request counts, specify the denominator, and compute percentages from those
counts. Label any future estimate explicitly; do not backfill guessed totals.

See [Work Log](WORK_LOG.md) for actual checks, warnings, and current commit status.


## Superseded F03 attempt

- AI work: configured the Kotlin/JVM module, updated the version catalog and
  settings, documented its boundaries, and ran dependency/build checks.
- Sources: official Kotlin Gradle configuration and JVM testing documentation.
- Validation: standalone module build, resolved classpath inspection, existing
  app build/unit tests/lint, and repository/document consistency checks.
- Domain test status: NO-SOURCE; no domain test pass or feature implementation is claimed.
- Model identity/token measurement limits remain as recorded above; no subagents used.
- Observed interval: 3m 01s, including permission waits and excluding unmeasured work.
- This separate-module attempt was not approved or committed. It was removed
  after the user chose to keep all code and tests in app.


## F03 correction: single app module

- User decision: keep all application code in app, with package organization.
- AI work: removed the uncommitted separate module, restored baseline Gradle
  configuration, and updated the architecture, roadmap, and working agreement.
- Validation: app-only project list, debug build, 1 passing existing unit test,
  0 lint errors/16 baseline warnings, and document consistency checks.
- Gradle reported 10s for the combined verification run. No full elapsed-work
  interval was measured for this correction; no duration estimate is asserted.
- The user approved this corrected scope with "go"; committed as `c46ce1b`.
  Usage totals remain unavailable.


## F06: Hilt foundation

- AI work: researched primary Hilt/KSP/Android/Kotlin documentation, configured
  dependencies and compiler targets, added Application/Activity wiring, the UTC
  Clock provider, and a device graph/UI test; updated English project records.
- Validation: debug/release APK builds, test APK build, 1 existing unit test,
  2 device tests, lint (0 errors/16 warnings), bytecode target inspection, and
  a normal production-Application cold launch with UI hierarchy inspection.
- Gradle reported 1m 25s for the build/unit/lint batch and 11s for device tests.
  These are command durations; total work time was not measured.
- Human role: approved the prior F03 correction with "go" (commit c46ce1b) and
  authorized continuing. F06 was separately approved and committed as `2b9789e`.
- No delegated subagents were used. Exact model/token totals remain unavailable.
- Compatibility limits and existing local environment warnings are recorded in
  Work Log; passing local checks does not establish every toolchain combination.


## L01: Initial domain contract

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the Earthquake domain model and invariants, initial list
  repository/refresh contracts, focused JVM tests, and corresponding documentation.
- Verification: 9 JVM tests passed; debug APK assembly and lint passed. The first
  Gradle invocation timed out during distribution download; the retry passed.
- Human role: authorized the F06 commit and continuation into L01, then approved
  the L01 commit.
- Total elapsed development time was not measured; the successful Gradle run
  reported 1m 27s, which is build execution time only.


## L02: Deterministic sample repository

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the Hilt-bound sample repository, controllable list and
  refresh scenarios, fixed-clock JVM tests, and updated project records.
- Verification: 13 JVM tests passed; Hilt code generation, debug APK assembly,
  and lint passed. Gradle reported 8s for 59 tasks, 27 executed and 32 up-to-date.
- Human role: authorized continuing after the L01 checkpoint and approved the
  L02 commit.
- Total elapsed development time was not measured.


## L03: Earthquake list state and ViewModel

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the Hilt ViewModel and immutable state, added lifecycle
  and coroutine test dependencies, wrote fake-repository tests, and updated docs.
- Verification: 18 JVM tests passed; debug APK assembly, Hilt/KSP generation, and
  lint passed. The first test compilation failed from missing dispatcher-rule
  extension imports; the corrected rerun passed.
- Human role: authorized continuing after the L02 checkpoint and approved the
  L03 commit.
- Total elapsed development time was not measured; the successful Gradle run
  reported 3s, which is build execution time only.


## L04: Earthquake list screen

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the Compose route/list and state presentations, localized
  strings and formatting, Hilt ViewModel route wiring, UI tests, and records.
- Verification: 18 JVM tests and 9 Android tests passed; debug/test APK assembly
  and standalone lint passed. Manually checked launch and text at font scale 1.3,
  then restored the emulator to 1.0.
- Human role: authorized continuing after the L03 checkpoint and approved the
  L04 commit.
- Total elapsed development time was not measured. The initial emulator install
  required a fresh APK because the installed package had a different debug key.
- Follow-up: Gradle commands run by the assistant shell as root created root-owned
  generated outputs under `app/build`; ownership was corrected without deleting
  build files, and the user confirmed Android Studio builds successfully.


## D01: USGS GeoJSON client and mapper

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added Retrofit/serialization configuration, the injectable USGS
  service, defensive GeoJSON DTO mapping, a recorded fixture, tests, and docs.
- Verification: 23 JVM tests and 9 Android tests passed; debug and test APKs
  assembled and lint passed. The Hilt device test resolved the service without
  issuing a live request.
- Human role: authorized continuing after the L04 checkpoint and approved the
  D01 commit.
- Total elapsed development time was not measured. USGS network fetch behavior
  remains unverified and is not claimed implemented.


## D02: Room event store and synchronization metadata

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added Room schema/entities/DAO and Hilt providers, UTC timestamp
  mappings, snapshot freshness/retention policy, database tests, and docs.
- Verification: 25 JVM tests and 14 Android tests passed; debug/test APK
  assembly and lint passed. Room schema version 1 was generated. No live feed was
  fetched.
- Human role: authorized continuing after the D01 checkpoint and approved the
  D02 commit.
- Total elapsed development time was not measured.


## D03: USGS synchronization through the repository

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: connected Retrofit/GeoJSON mapping to Room, added refresh/error and
  concurrency handling, replaced the production Hilt binding, and added tests.
- Verification: 25 JVM tests and 20 device tests passed; debug/test APK builds and
  lint passed. The production app was manually launched and displayed live USGS
  events; instrumentation used a sample binding and made no network requests.
- Human role: authorized continuing after the D02 checkpoint and approved the
  D03 commit.
- Total elapsed development time was not measured.


## D04: Event details and Navigation 3

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added typed Navigation 3 routes and state restoration, assisted Hilt
  detail ViewModel, local-first/detail fallback behavior, and UI/repository tests.
- Verification: 30 JVM tests and 28 Android tests passed; debug/test APK assembly
  and lint passed. Manually opened a live Room-cached event and returned to the
  list with Back. Instrumentation used the sample binding; detail HTTP tests used
  fakes.
- Human role: authorized continuing after the D03 checkpoint and approved the
  D04 commit.
- Total elapsed development time was not measured.


## N01: Notification decision table

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: drafted the explicit notification policy matrix and synchronized the
  roadmap, architecture, data contract, work log, and usage report.
- Verification: reviewed scenarios/references and `git diff --check` passed. No
  application code changed, so app tests/build were not rerun.
- Human role: authorized continuing after the D04 checkpoint and approved the
  N01 commit.
- Total elapsed development time was not measured.


## N02: Persist notification preferences

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added the validated preference model/contract, DataStore persistence
  and Hilt providers, restart/validation tests, and updated project records.
- Verification: 36 JVM tests and 28 Android tests passed; debug/test APK assembly
  and lint passed. Hilt graph integration checked default preferences.
- Human role: authorized continuing after the N01 checkpoint and approved the
  N02 commit.
- Total elapsed development time was not measured.


## N03: Notification settings and OS permission state

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added the notification settings route/UI, preference ViewModel,
  runtime permission status/request handling, navigation, tests, and records.
- Verification: 39 JVM tests and 32 Android tests passed; debug/test APK assembly
  and lint passed. The API 29 emulator displayed the not-required runtime status;
  API 33+ permission prompting remains unverified on a device.
- Human role: authorized continuing after the N02 checkpoint and approved the
  N03 commit.
- Total elapsed development time was not measured.


## N04: Notification eligibility policy

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the pure notification decision policy and tests, clarified
  suppression precedence in the decision table, and synchronized project docs.
- Verification: 46 JVM tests passed; debug build and lint passed. No Android
  behavior was added or claimed in this policy step.
- Human role: authorized continuing after the N03 checkpoint and approved the
  N04 commit.
- Total elapsed development time was not measured.


## N05: Persistent notification processing state

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added alias persistence and Room migration, baseline/cursor/outcome
  storage, a concurrency-coordinated processor, restart/retry tests, and records.
- Verification: 46 JVM tests and 39 Android tests passed; debug build, test APK
  assembly, schema migration, and lint passed. No OS notification was sent.
- Human role: authorized continuing after the N04 checkpoint and approved the
  N05 commit.
- Total elapsed development time was not measured.


## N06: Android notification delivery

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: implemented the Android sender/channel and permission-aware results,
  stable event identity/deep-link PendingIntent, and pending-work dispatcher.
- Verification: 46 JVM tests and 41 Android tests passed; debug/test APK builds
  and lint passed. Instrumentation posted a deterministic event and opened its
  matching detail from a notification tap.
- Human role: authorized continuing after the N05 checkpoint and approved the
  N06 commit.
- Limitation: Android 10/API 29 was the available device, so the Android 13+
  runtime permission dialog was not manually exercised. N07 scheduling remains.


## N07: Preference-driven background synchronization

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added Hilt WorkManager setup, a unique network-constrained periodic
  worker reconciled with saved preferences, bounded retry mapping, and a shared
  foreground/background refresh-and-notification path.
- Verification: 50 JVM tests and 50 Android tests passed; debug and test APKs,
  lint, merged-manifest checks, and generated Hilt worker-factory construction
  passed. One combined test/lint invocation hit an ADB property timeout; the
  connected suite passed when rerun separately.
- Human role: authorized continuing after the N06 checkpoint and approved the
  N07 commit.
- Limitations: WorkManager's 15-minute minimum is inexact and subject to OS delay;
  a real timed periodic run after production process death was not observed.
  Android 13+ notification permission UI was not available on the API 29 device.


## Q01: Local product events

- Assistant: OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: added local Room event history and migration, wired screen, preference,
  permission, notification, and refresh outcomes, and documented metric
  denominators without adding external analytics or uploads.
- Verification: 53 JVM tests and 54 Android tests passed; debug/test APKs and
  lint passed. Instrumentation verified event persistence, migration, matching
  notification-to-detail events, and no duplicate list view on recomposition.
- Human role: authorized continuing after the N07 checkpoint and approved the
  Q01 commit.
- Limitations: Android 13+ permission UI was unavailable; there is no user-facing
  event dashboard, and total development time was not measured.


## Q02: Isolated demo scenario

- Assistant: Claude Code, Claude Sonnet 5.5 (`claude-sonnet-5-5`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: completed the partially started flavor/DI/demo-storage work, added the
  demo controller, scenario UI and tests, fixed affected tests, and updated records.
- Verification: 53 JVM tests, 55 live and 57 demo Android tests, and lint on both
  variants passed; demo app launched manually.
- Human role: asked to continue, and fixed file ownership with `sudo chown`
  after root-owned files blocked edits. Commit approval is pending.
- Limitations: see the Q02 work log entry; development time was not measured.


## Q03: Acceptance walkthrough

- Assistant: Claude Code, Claude Sonnet 5.5 (`claude-sonnet-5-5`). No delegated
  subagents were used; exact token totals are unavailable.
- AI work: drove the emulator through the acceptance flows with adb, diagnosed and
  fixed the emulator's DNS, and recorded the results.
- Verification: see the Q03 work log entry; no application code changed.
- Human role: approved continuing after the Q02 commit.
- Limitations: Android 13+ permission denial was not exercised; development time
  was not measured.
