# Architecture

Status: the project uses a single `:app` Gradle module. All planned application
code and tests belong in app. The Hilt foundation, initial domain contract,
deterministic sample repository, list ViewModel/state, and Compose list screen are
implemented. USGS and remaining product features are pending.

## Purpose

Build an Android app that displays recent earthquakes, saves a notification
threshold, and notifies users about eligible new events. The design should make
business rules easy to test and implementation decisions easy to explain.

Use the [roadmap](ROADMAP.md) for task order and progress, and the
[data contract](DATA_CONTRACT.md) for USGS mappings and repository responsibilities.
All project documentation is maintained in English. The
[Android guideline review](ANDROID_GUIDELINE_REVIEW.md) records which decisions
changed, which remain, and the official sources behind them.

## 1. Single-module structure and package boundaries

**Decision: keep all application code and tests in `:app`.** Layer responsibilities
are organized as packages, not separate Gradle modules. Create packages when they
contain real code; no empty scaffold or marker classes are needed.

Planned packages under `com.mlhysrszn.earthquake`:

```text
app/src/main/java/com/mlhysrszn/earthquake/
  MainActivity.kt
  EarthquakeApplication.kt
  ui/
    theme/
    earthquakes/list/
    earthquakes/detail/
    settings/
    navigation/
  domain/
    model/
    repository/
    usecase/
  data/
    remote/
    local/
    repository/
  di/
  background/
  notifications/
```

The starter Activity/theme, domain model and repository contract, sample
repository, list ViewModel/state, and Compose list screen exist today. Other
planned paths remain for future code.

| Package | Responsibility |
| --- | --- |
| `ui` | Compose screens, ViewModels, state, theme, and navigation |
| `domain` | Plain Kotlin models, repository contracts, and necessary shared rules/use cases |
| `data` | USGS integration, DTO mapping, Room, DataStore, repositories, and local event storage |
| `di` | Hilt bindings, providers, and qualifiers |
| `background` | WorkManager scheduling and Worker orchestration |
| `notifications` | Android notification delivery and permission integration |

UI depends on domain contracts; data implements those contracts. UI must not
access API clients, DAOs, or DataStore directly. Keep domain rules free of Android
runtime APIs so they can be tested locally. Shared notification policy justifies
use cases; simple reads can use repository interfaces directly.

These are package conventions checked in review and behavioral tests. A single
Gradle module does not enforce layer isolation at compile time, and Kotlin
`internal` visibility applies to the entire app module. Use private declarations
where practical; do not claim that packages provide module-level isolation.

Local unit tests, including domain rules, live in `app/src/test`; Android/Compose
integration tests live in `app/src/androidTest`. Additional Gradle modules require
an explicit change to the user's single-module decision.

## 2. UI and data flow

Use MVVM with unidirectional data flow:

```text
User action -> ViewModel -> use case / repository interface
Local data stream -> ViewModel -> StateFlow<UiState> -> Compose
```

- Composables render state and report user actions. Route composables obtain
  screen ViewModels; reusable content receives state and callbacks.
- Expose immutable `StateFlow<UiState>` and collect it with
  `collectAsStateWithLifecycle`. Keep mutable state private.
- For UI state derived from observed streams, use `stateIn` with an appropriate
  `WhileSubscribed` policy; background work must not depend on UI subscribers.
- Handle ViewModel-originated results as state updates. User-triggered navigation
  stays in UI callbacks; do not introduce a general one-shot event bus for errors
  or navigation. Pending user messages are state with explicit acknowledgement.
- Use a single Activity and Navigation 3 for the Compose destinations. Save the
  typed back stack and scope ViewModels to entries using the supported decorators.
  Pass an event ID, not an entire Earthquake object, between destinations.
- ViewModels manage screen state without knowing DTOs, DAOs, or notification APIs.
  They do not hold Context, Activity, Resources, or Application references.
- Add use cases for shared rules or behavior involving several steps. Do not add
  a wrapper for every repository call.
- Once persistence is implemented, observe the list from Room. Validate network
  responses and write them to storage before updating the UI.
- Distinguish initial loading from refreshing existing content. A refresh failure
  preserves cached content and exposes an actionable error.
- Show the last successful update time.
- Navigate to details using an event ID. Support fetching a missing local event
  and displaying an unavailable state.

## 3. Models, failures, and dependency injection

- Keep API DTOs and Room entities out of UI contracts. Their separate mappings
  serve validation and persistence needs. Reuse the domain model in UiState when
  suitable; add a separate UI model only for an actual presentation requirement.
- Handle missing fields during mapping. Missing magnitude is not zero. Events
  without valid IDs or occurrence times cannot enter the domain event list.
- Convert expected network/storage failures into meaningful results. Preserve
  coroutine cancellation instead of treating it as an ordinary failure.
- Store timestamps as UTC instants and display them in the device time zone.
  Inject a Clock for time-dependent rules.
- Make suspend APIs safe to call from the main thread. Use `viewModelScope` for
  screen actions and structured concurrency for other work. Inject dispatchers
  where code changes execution context; let Room/Retrofit manage their supported
  asynchronous operations. Avoid GlobalScope and preserve cancellation.
- Use internal/private visibility for implementation details where possible.

### 3.1 Dependency injection: Hilt

**Decision: use Hilt with constructor injection.** Hilt owns the dependency graph;
there is no application-managed dependency container or service locator.

Use `@Inject` constructors for supported owned classes, `@Binds` for interface
implementations, and `@Provides` for external types such as the HTTP client,
Room database, DataStore, and Clock. Install bindings in the appropriate Hilt
component. Qualify dispatchers or other bindings with the same underlying type.

#### Wiring within app

- Application uses `@HiltAndroidApp`; MainActivity uses `@AndroidEntryPoint`.
- `di` contains `@Module` bindings/providers for repositories, storage, network,
  Android adapters, Clock, and qualified dispatchers as needed.
- Repository implementations use constructor injection.
- Screen ViewModels use `@HiltViewModel`; destination entry points retrieve them
  through `hiltViewModel()`.
- Domain rules may use standard `javax.inject.Inject` constructor annotations
  when needed. They must not depend on Context, Android lifecycle types, or Hilt
  component APIs. Data classes and pure functions need no DI annotations.
- Background work uses HiltWorkerFactory as described below.

Hilt 2.60.1 and KSP 2.3.12 are configured in app with Kotlin/Compose 2.3.21,
AGP 9.4.1, and Gradle 9.6.0. Java targets 17; built-in Kotlin inherits that
target. The root build explicitly aligns AGP's Kotlin compiler with the Compose
compiler. Debug/release builds and device graph checks pass in the local
environment; see Work Log for support-matrix limits. AndroidX Hilt integrations
will be version-checked when ViewModels and Workers are added. Hilt binding modules are annotated classes inside app, not additional
Gradle modules. There is no separate JVM plugin or manual dependency container.

Implemented in F06: `EarthquakeApplication`, the MainActivity entry point, and
`di/TimeModule` providing one system UTC Clock per application graph. The Clock
needs no Android context and will support injectable time in subsequent rules.
`HiltTestRunner` selects HiltTestApplication for device tests; HiltGraphTest
checks the Clock binding and starter screen. Production cold launch is checked
separately because the test runner replaces the Application.

#### Ownership and lifetimes

| Dependency | Planned lifetime |
| --- | --- |
| Clock | `@Singleton` in SingletonComponent; a shared UTC time source |
| HTTP client and Room database | `@Singleton` in SingletonComponent |
| DataStore | One provided instance per backing file in the process |
| Shared repositories and synchronization coordination | `@Singleton` where shared state/resources require it |
| Stateless policies and use cases | Unscoped by default |
| ViewModel | Hilt-managed, owned by its screen/navigation entry |
| Worker | New instance for each execution, created through HiltWorkerFactory |

Apply scopes for a reason; do not mark every class singleton. Long-lived Android
bindings use `@ApplicationContext` and never retain an Activity or ViewModel.
Room/DataStore hold durable state because Hilt scopes do not survive process death.

#### ViewModels and navigation

Use `@HiltViewModel` and constructor injection. Retrieve each screen ViewModel
with `hiltViewModel()` under the correct Navigation 3 entry owner, using
`rememberViewModelStoreNavEntryDecorator` and the required saved-state setup.
Supply runtime event IDs through the supported saved-state or Hilt assisted
creation mechanism selected during D04; do not assume Navigation 3 automatically
copies a route key into SavedStateHandle. Verify restoration and Back behavior.
Reusable composables take state/callbacks rather than obtaining dependencies.

#### WorkManager

Use `@HiltWorker` with `@AssistedInject`; Context and WorkerParameters are assisted
parameters. Worker dependencies must be available from SingletonComponent, either
unscoped or singleton-scoped. Do not inject activity- or ViewModel-scoped objects.

The Application implements `Configuration.Provider` and supplies the injected
`HiltWorkerFactory`. Remove WorkManager's default initializer as required by the
custom configuration, preserving other App Startup initializers. Verify the
merged manifest and cold-start execution in N07. No handwritten WorkerFactory.

#### Tests and demo

Plain unit tests construct subjects with fakes directly. Android graph tests use
Hilt test tooling and prefer `@TestInstallIn` for shared binding replacements.
Use an isolated test database and verify graph creation. Demo/live implementations
are selected through build-variant Hilt bindings with separate storage/work setup;
never mutate a process-global dependency registry or bind both unqualified
implementations in one variant.

## 4. Notification responsibilities

- The Worker triggers orchestration and maps outcomes to WorkManager results.
- Threshold, new-event, and previously-notified rules belong to domain behavior.
- The data package owns persistent event, notification, and synchronization records.
- The notifications package implements a domain notification port using Android
  APIs. Domain rules do not reference NotificationManager or Context.
- Coordinate foreground refreshes and background work. An in-memory set of IDs
  is insufficient for deduplication across process restarts.
- Database writes and operating-system notifications cannot form one atomic
  transaction. Use stable notification identities and persistent processing
  states for retries; do not claim exactly-once delivery.
- Demo events pass through the same orchestration and eligibility rules, with
  separate storage/work configuration and visible demo labels.

Working rules: magnitude must be strictly greater than the threshold; initial
synchronization must not notify historical events; lowering a threshold must not
replay historical records. Before implementing notification delivery, define
late-arriving events, magnitude revisions, long offline periods, re-enabling
notifications, and retention behavior in a decision table.

## 5. Verification boundaries

| Area | Behavior to verify |
| --- | --- |
| Domain JVM tests | Below/equal/above threshold, initial baseline, duplicates, time rules |
| Data tests | DTO mapping, missing fields, cache updates, failed refreshes, persistent deduplication |
| ViewModel tests | Loading, content, empty results, refresh errors, preference updates |
| Android integration | Room transactions, Worker results, notification permission and routing |
| Compose tests | List-to-detail navigation and notification settings journeys |

Prefer fakes for collaborators and deterministic coroutine test dispatchers.
Include active collectors when testing streams that use WhileSubscribed.
Test DI wiring where Android components consume it, while keeping policy tests
independent of Hilt. Verify navigation state restoration and notification entry
points as part of the Android walkthrough.

Run checks relevant to each change. Update the roadmap with the result and any
unverified behavior. A planned check is not a passed check.

## 6. Data source and scope

Use USGS worldwide records without an additional geographic filter. This does
not imply that the catalog contains every earthquake that occurred.

The initial list covers the past 24 hours using the GeoJSON summary feed. If
custom date or region queries become necessary, add the catalog API within data.
The [data contract](DATA_CONTRACT.md) defines mappings and synchronization limits.

The first runnable slice uses deterministic sample data. The real USGS adapter
then replaces that source without changing the presentation contract.

## 7. Constraints and open decisions

- The delivery deadline is not yet specified. The roadmap gives task order rather
  than calendar commitments.
- WorkManager periodic work has a minimum interval of 15 minutes and does not
  guarantee exact execution times. The MVP provides periodic checks.
- If near-real-time delivery becomes a requirement, revisit the delivery design
  before presenting a latency promise.
- Validate library versions against the existing AGP, Kotlin, and Gradle setup
  when adding dependencies. F06 verifies the Hilt foundation locally; ViewModel,
  Worker, and storage integration remain later tasks. See [Work Log](WORK_LOG.md)
  for versions, verification, and compatibility limits.

## References

- [Android modularization](https://developer.android.com/topic/modularization/patterns)
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer)
- [Offline data access](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Periodic work](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)
- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt and Jetpack](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt testing](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Navigation state](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)
- [Coroutines](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Custom WorkManager configuration](https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration)

These are general guidelines. The single app module follows the user's chosen
project scope; package responsibilities provide the architectural organization.
