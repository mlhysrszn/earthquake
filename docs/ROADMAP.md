# Android Earthquake App Roadmap

Status: F02/F01 were committed in `f224494`; the single app module correction
(F03) was committed in `c46ce1b`; Hilt setup (F06) was committed in `2b9789e`.
L01, L02, L03, L04, D01, D02, D03, D04, N01, N02, N03, and N04 are committed.

## Goal

Deliver an Android app in which a user can view recent USGS earthquakes, set a
magnitude threshold, and receive a notification for an eligible new event. The
code should be easy to explain and modify during the case-study interview.

## Agreed direction

- Native Android with Kotlin and Jetpack Compose.
- USGS worldwide data without an additional geographic filter.
- A list covering the past 24 hours, event details, and notification settings.
- One Gradle module: app. UI, domain, data, DI, and platform responsibilities use
  packages inside app; all unit/device tests also remain in app.
- MVVM with lifecycle-aware state collection and Hilt constructor injection.
- A single Activity with Navigation 3 and ViewModels scoped to screen entries.
- Room for event/cache storage, DataStore for preferences, and WorkManager for
  periodic background checks.
- A deterministic demo using the same business rules as real data.
- Local analytics events, meaningful tests, and an English delivery README.
- All project documentation is written and maintained in English.

See [Architecture](ARCHITECTURE.md) and [USGS Data Contract](DATA_CONTRACT.md).
The [Android guideline review](ANDROID_GUIDELINE_REVIEW.md) records reviewed
choices and project-specific tradeoffs. Library versions are validated during
setup, including Hilt/KSP/toolchain compatibility.

## Scope boundaries

No account system, payments, embedded map, location permission, custom backend,
multiple earthquake providers, or store release in the initial delivery.
Periodic checks do not carry an instant-delivery guarantee. The deadline remains
unspecified, so this roadmap defines dependencies and completion criteria rather
than dates or invented time estimates.

## How we will work

1. Work on one small task at a time, in the order below unless a dependency says
   otherwise. Keep the app buildable after each code task.
2. Read the task's output and acceptance criteria before changing code.
3. Run the relevant build/tests or manual checks and record the actual result.
4. Mark a task complete only after its acceptance criteria pass. Record blockers
   and unverified behavior explicitly.
5. Update affected documents in the same step as a changed decision.
6. Record time and AI assistance in [Work Log](WORK_LOG.md) and
   [AI Usage Report](AI_USAGE_REPORT.md).
7. At each small task/batch checkpoint, report the changes and test results, then
   wait for explicit user approval before committing. After approval, commit only
   the reviewed scope. Do not start the next batch while commit approval is pending.

Checkboxes track verified work, not whether it has been committed. The progress
log records commit status separately. The user's requested commit approval is
mandatory; permission to work or run tests does not authorize a commit.

## Phase 1: Verify and establish the architecture

### F02 - Establish repository and work records

- [x] Check Git state, initialize a repository if still absent, verify ignore
  rules, and create a work log plus an AI usage report template.
- Done when: local SDK paths, credentials, and generated build files are excluded;
  time and AI work can be recorded without inventing measurements.
- Order: moved before F01 at the user's request. Remote publication is part of
  delivery, not this task. This initial checkpoint groups F02 and F01 for review.
- Result: local `main` initialized; ignore rules verified; working agreement and
  records created. Initial commit: `f224494`. No remote has been created.

### F01 - Verify the starter project

- [x] Inspect the JDK, Android SDK, Gradle, AGP, and Kotlin setup; build and launch
  the existing app.
- Done when: the starter builds, baseline tests are run, and the app opens on an
  emulator/device. A documented environment blocker would keep the task open.
- Depends on: F02.
- Result: debug build, 1 template unit test, and 1 template device test passed.
  Lint reported 0 errors and 16 warnings. Cold launch and the starter greeting
  were verified on Android 10 / API 29. See [Work Log](WORK_LOG.md) for limits.

### F03 - Confirm the single app module structure

- [x] Keep settings limited to `:app`; remove the uncommitted domain module and
  its Kotlin/JVM plugin configuration. Document package responsibilities in app.
- Done when: Gradle lists only app, the starter build/unit test/lint checks pass,
  and active documentation consistently describes package-based organization.
- Depends on: F01.
- Scope: no new business classes or empty placeholder packages. Create actual
  packages as their features are implemented.
- Result: Gradle lists only app; debug build and the existing unit test passed.
  Lint reports 0 errors and the original 16 warnings.

### F06 - Configure Hilt in app

- [x] Validate Hilt/KSP/toolchain compatibility and aligned JVM targets. Add the
  Hilt Application, Activity entry point, and binding/provider setup described
  in Architecture section 3.1, all inside app. Keep domain rules free of Android
  runtime APIs and use standard constructor injection.
- Done when: the generated graph compiles, the starter launches through the Hilt
  entry point, and a graph smoke check resolves an injected dependency. Scopes
  match ownership and long-lived bindings use application context.
- Depends on: F03.
- Result: debug/release builds, the existing unit test, lint, and 2 device tests
  passed. Hilt resolves a shared UTC Clock; the starter Activity renders under
  HiltTestApplication and cold-starts under EarthquakeApplication. See Work Log
  for the tested versions and compatibility limits.

**Checkpoint:** a runnable app with Hilt and documented package responsibilities.

## Phase 2: Deliver the first list using sample data

### L01 - Define the initial domain contract

- [x] Add Earthquake, the list observation/refresh contract, and meaningful refresh
  outcomes. Define nullable fields and model invariants.
- Done when: presentation can consume the contract without any API or Room types;
  model validation rules have focused JVM tests.
- Depends on: F06.
- Result: pure Kotlin model and repository contract added; 8 focused domain tests
  pass, along with the existing starter test. Debug assembly and lint pass.

### L02 - Add a deterministic sample repository

- [x] Implement the list contract with fixed sample events and controllable empty,
  loading, and failure scenarios.
- Done when: samples can be reset and do not depend on a live earthquake occurring.
- Depends on: L01.
- Result: singleton sample repository is bound through Hilt; fixed-clock tests cover
  content, empty/reset, pending refresh, and one-shot failure/retry scenarios.

### L03 - Implement list state and ViewModel

- [x] Add a Hilt ViewModel with immutable UI state, initial load, manual refresh,
  and retry behavior. Handle results as state and use viewModelScope for actions.
- Done when: tests with fakes and coroutine test dispatchers cover content, empty
  results, initial failure, and failed refresh retaining existing content. Tests
  account for any WhileSubscribed streams.
- Depends on: L02.
- Result: Hilt ViewModel and lifecycle-aware state flow added; five coroutine-test
  cases cover content, empty, initial failure/retry, retained content on failure,
  and pending refresh. Tests actively collect the WhileSubscribed stream.

### L04 - Build the list screen

- [x] Render magnitude, source place text, occurrence time, and last update;
  provide refresh/retry and consistent loading/empty/error states. Collect with
  collectAsStateWithLifecycle; reusable content accepts state and callbacks.
- Done when: sample scenarios work on-device, labels remain legible with larger
  font settings, and information does not depend on color alone.
- Depends on: L03.
- Result: Turkish Compose list screen uses lifecycle-aware state collection and
  state/callback content; 7 screen tests and the Hilt Activity test pass. Emulator
  launch and 1.3 font-scale readability were manually checked.

**Checkpoint:** a visible, testable list driven through the full architecture.

## Phase 3: Integrate USGS and persistence

### D01 - Implement and test USGS mapping

- [x] Add the network client, summary DTOs, and DTO-to-domain mapping with recorded
  or synthetic fixtures. Define malformed-payload versus invalid-record behavior.
- Done when: mapping tests cover missing fields, invalid identities/times,
  coordinate order, negative magnitudes, unknown fields, and non-earthquake events.
- Depends on: L01; perform after the sample list checkpoint.
- Result: Retrofit service and Hilt network providers added; five fixture-backed
  mapping tests verify optional/invalid fields, coordinate order, negative values,
  ignored unknown keys/non-earthquake records, and malformed-feed boundaries.

### D02 - Add the local event store

- [x] Add Room entities, DAOs, mappings, and synchronization metadata. Define
  retention, the rolling 24-hour list window, and stale-snapshot handling.
- Done when: database tests verify insert/update behavior, ordering, observable
  reads, transactions, and persistence after reopening the database.
- Depends on: D01.
- Result: Room 2.8.5 entities/DAO, Hilt database provider, epoch-millisecond
  mappings, and schema export added. Five device database tests cover upsert,
  ordering/window boundaries, atomic snapshots, stale snapshots, and reopening.

### D03 - Connect synchronization to the repository

- [x] Fetch, validate, and store USGS records; expose Room as the UI's event source.
  Coordinate overlapping refreshes and preserve coroutine cancellation.
- Done when: real data appears without changing the ViewModel contract; failed
  refreshes preserve cached content; fetch and source-generation times stay distinct.
- Depends on: D02, L04.
- Result: production Hilt binding now uses the USGS repository, which fetches and
  maps the all-day feed into atomic Room snapshots. Six integration tests cover
  success, cache-preserving failures, stale data, overlap, and cancellation; a
  live feed was manually displayed on the emulator.

### D04 - Add event details and navigation

- [x] Add typed Navigation 3 destinations, a saved back stack, and entry-scoped
  Hilt ViewModels. Pass event IDs using an explicit supported argument mechanism.
  Display event details and fetch by ID only when missing locally.
- Done when: cached details work offline and unavailable events have a clear state.
  Test the single-event response, list-to-detail flow, Back, recreation, and saved
  navigation state; verify arguments reach the correct ViewModel.
- Depends on: D03.
- Result: typed serializable routes, saved Navigation 3 back stack, entry-scoped
  Hilt ViewModels, and assisted event-ID delivery added. Detail first uses Room,
  fetches only when missing, and shows an unavailable state for removed events.
  Tests cover single-event mapping, detail fetching, list/detail/Back, and restore.

**Checkpoint:** live list and details, with usable cached data when offline.

## Phase 4: Add notification preferences and delivery

### N01 - Write the notification decision table

- [x] Define eligibility for below/equal/above threshold, first synchronization,
  duplicate events, threshold changes, late records, revisions, long offline
  periods, re-enabling notifications, permission denial, and event-ID aliases.
- Done when: each case has a deterministic expected result, including the
  baseline/cutoff and retention policy needed to implement it.
- Depends on: D03.
- Result: [Notification Decision Table](NOTIFICATION_DECISION_TABLE.md) defines
  baseline/cursor behavior, candidate eligibility, alias deduplication,
  permission/preferences suppression, retry/crash outcomes, and retention.

### N02 - Persist notification preferences

- [x] Add the preferences contract and DataStore implementation. Specify default
  enabled state, threshold, selectable range, and step size.
- Done when: valid preferences survive restart and invalid input is handled.
- Depends on: N01.
- Result: default-disabled preferences and the validated 0.0–9.5 magnitude
  threshold (0.5 steps, default 4.0) persist with DataStore; invalid stored values
  fall back safely and invalid writes are rejected.

### N03 - Build notification settings

- [x] Add enabled/disabled controls, threshold selection, and actual OS permission
  status. Request permission when the user enables notifications.
- Done when: granted, denied, and system-disabled states are understandable;
  denied permission does not erase the user's threshold preference.
- Depends on: N02.
- Result: Compose settings route exposes persisted controls, checks Android runtime
  permission and app-level notification status, requests permission on opt-in, and
  links to OS settings when blocked.

### N04 - Implement domain eligibility rules

- [x] Implement the decision table with injected time and explicit inputs.
- Done when: JVM tests cover every decision-table case without Android APIs.
- Depends on: N01.
- Result: pure Kotlin eligibility policy uses injected `Clock` and explicit
  snapshot, identity, preference, permission, event, and magnitude inputs. Seven
  JVM tests cover the documented branches and rolling-window boundaries.

### N05 - Add persistent notification processing state

- [ ] Store processed event identities, baselines, and notification outcomes;
  define retry/crash behavior and coordinate concurrent processors.
- Done when: integration tests cover restart and concurrent processing without
  reselecting already handled events contrary to the policy.
- Depends on: N04, D02.

### N06 - Implement the Android notification adapter

- [ ] Add the notification channel, permission-aware delivery result, stable
  notification identity, and event-ID navigation when a notification is tapped.
- Done when: a deterministic eligible event posts a notification and opens the
  correct detail screen, including a cold app start.
- Depends on: N03, N05, D04.

### N07 - Schedule and connect background checks

- [ ] Add unique periodic work with network constraints, bounded retries, and
  scheduling/cancellation tied to preferences. Reuse the same synchronization
  and notification rules for relevant foreground and background triggers. Use
  @HiltWorker/@AssistedInject and the injected HiltWorkerFactory through the
  Application's Configuration.Provider. Remove only the required default
  WorkManager initializer and verify the merged manifest.
- Done when: Worker checks verify success/retry/failure, disabled notifications,
  first-run behavior, and repeat processing; timing limitations are documented.
  Cold-start execution uses fresh Workers and dependencies available from
  SingletonComponent; no Activity/ViewModel scope leaks into the worker graph.
- Depends on: N06.

**Checkpoint:** saved preferences lead to eligible notifications and detail navigation.

## Phase 5: Make the result measurable and demonstrable

### Q01 - Add local product events

- [ ] Implement local event storage and wire list/detail views, preference saves,
  permission outcomes, notification posting/opening, and refresh failures.
- Done when: the agreed events can be inspected with timestamps and minimal useful
  properties. Demo events are identifiable; recomposition does not duplicate views.
- Metrics: setup completion, notification-to-detail opening, and refresh failures.
  Define event denominators; posting does not prove that a user saw a notification.
- Depends on: N07. Instrument earlier flows here without changing their behavior.

### Q02 - Add an isolated demo scenario

- [ ] Provide a clearly labeled demo that injects events through the real domain
  rules and delivery adapter. Select sources through build-variant Hilt bindings
  with separate storage and work configuration.
- Done when: the demo can show an event below threshold, one above threshold, and
  a duplicate; it resets predictably and cannot mix with live records.
- Depends on: Q01.

### Q03 - Run the acceptance walkthrough

- [ ] Verify list -> details, preferences -> notification -> details, offline
  startup, refresh failure, denied permission, restart, and larger font settings.
- Done when: relevant unit/integration/UI tests pass, on-device checks are logged,
  and remaining limitations are explicitly recorded. Hilt integration tests use
  isolated storage and test binding replacements; screen state and navigation
  restore correctly after recreation.
- Depends on: Q02.

**Checkpoint:** the core journey works, can be measured, and can be demonstrated on demand.

## Phase 6: Prepare the case-study handoff

### H01 - Finish the README and usage report

- [ ] Document setup, scope, architecture choices, notification limits, testing,
  demo steps, actual time spent, AI tools/models, delegated work, verification,
  and the next improvement.
- Done when: the README links to the roadmap, architecture, data contract, and
  usage report. Usage figures identify their source and denominator; estimates
  are labeled and unavailable data is not fabricated.
- Depends on: Q03 and the ongoing records started in F02.

### H02 - Verify reproducibility and prepare the interview

- [ ] Run the documented setup from a clean checkout, repeat the demo, review
  tracked files, and prepare a small practice change to a rule or UI behavior.
- Done when: documented commands work, the repository contains the deliverable,
  the development environment is ready, and the architecture can be explained
  using the actual code. Provide a repository URL once a destination is available.
- Depends on: H01. An absent remote destination blocks publication, not local work.

## Completion checklist

- [ ] Recent USGS events and details work with explicit loading/error/empty states.
- [ ] Preferences persist and notification behavior matches the decision table.
- [ ] Duplicate handling and initial synchronization are verified.
- [ ] Cached content, permission denial, and missing data behave predictably.
- [ ] Demo, local events, tests, README, and AI usage report are ready.
- [ ] The repository is reproducible and ready for the interview.

## Progress log

| Task | Status | Verification / blocker |
| --- | --- | --- |
| Planning documents | Done | Roadmap created; architecture and data contract written in English |
| Android guideline review | Done | Hilt selected; architecture tasks remain pending |
| F02 | Committed: f224494 | Local main initialized; ignore rules and work records verified |
| F01 | Committed: f224494 | Debug build, unit/device tests, lint, and emulator launch checked; see Work Log |
| F03 | Committed: c46ce1b | Only app remains; debug build/unit test/lint passed; package-based plan updated |
| F06 | Committed: 2b9789e | Debug/release builds, unit/device tests, lint, and production cold launch passed |
| L01 | Committed | Domain model and repository contract; 9 JVM tests passed, debug build and lint passed |
| L02 | Committed | Sample repository and Hilt binding; 13 JVM tests passed, debug build and lint passed |
| L03 | Committed | Hilt list ViewModel/state; 18 JVM tests passed, debug build and lint passed |
| L04 | Committed | Compose list screen; 9 device tests, 18 JVM tests, lint, and emulator/font-scale checks passed |
| D01 | Committed | USGS Retrofit service/GeoJSON mapper; 23 JVM tests and 9 device tests passed, debug build and lint passed |
| D02 | Committed | Room event store/sync metadata; 25 JVM tests and 14 device tests passed, debug build and lint passed |
| D03 | Committed | USGS-to-Room repository; 25 JVM tests and 20 device tests, lint, and live-feed check passed |
| D04 | Committed | Typed detail navigation; 30 JVM tests, 28 device tests, lint, and live detail/Back check passed |
| N01 | Committed | Notification policy documented, including baseline, aliases, suppression, late arrivals, retries, and retention |
| N02 | Committed | DataStore preferences; 36 JVM tests, 28 device tests, debug build and lint passed |
| N03 | Committed | Notification settings/permission UI; 39 JVM tests, 32 device tests, debug build and lint passed |
| N04 | Committed | Pure eligibility policy; 46 JVM tests passed, including seven policy cases |

Add a row for each task as work starts. Record actual commands/results or manual
checks, and keep task checkboxes consistent with this log.
