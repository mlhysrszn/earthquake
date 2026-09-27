# Architecture

Status: implementation plan. Only the original `:app` module exists today; the
module structure below has not been implemented.

## Purpose

Build an Android app that displays recent earthquakes, saves a notification
threshold, and notifies users about eligible new events. The design should make
business rules easy to test and implementation decisions easy to explain.

Use the [roadmap](ROADMAP.md) for task order and progress, and the
[data contract](DATA_CONTRACT.md) for USGS mappings and repository responsibilities.
All project documentation is maintained in English. The
[Android guideline review](ANDROID_GUIDELINE_REVIEW.md) records which decisions
changed, which remain, and the official sources behind them.

## 1. Module boundaries

Use four Gradle modules. Arrows represent compile-time dependencies:

```text
:app ----------> :presentation ----------> :domain
  |                                        ^
  +------------> :data --------------------+
  +---------------------------------------> :domain
```

| Module | Responsibility | Allowed project dependencies |
| --- | --- | --- |
| `:app` | Application/Activity, dependency wiring, WorkManager worker, Android notification adapter, external entry points | presentation, data, domain |
| `:presentation` | Compose screens, ViewModels, UI state, theme, user interactions, navigation graph | domain |
| `:domain` | Kotlin models, repository interfaces, platform ports, notification rules, necessary use cases | None |
| `:data` | USGS client, DTO mapping, Room, DataStore, repository implementations, local event storage | domain |

Domain is a Kotlin/JVM module with no Android, Compose, Room, or Retrofit
references. Platform-independent libraries such as Coroutines/Flow are allowed.
Presentation cannot depend on data, so screens cannot access API clients or DAOs.

Organize presentation by feature: `earthquakes/list`, `earthquakes/detail`,
`settings`, `navigation`, and `designsystem`. Keep these as packages initially.
App hosts the Hilt application component that connects the modules.

Tradeoff: four modules require more Gradle setup than one module. In return,
business rules can run in JVM tests, and the UI/data boundary is enforced by
module dependencies. Additional feature modules require a concrete need.

Android does not mandate four modules or a separate domain module. We retain
these boundaries to isolate the shared notification policy and make dependency
rules enforceable. Our domain module also owns repository contracts as a
project-specific dependency-inversion choice. This is not the exact module graph
shown in Android's domain-layer guide. Repository implementations still own
source coordination, caching, and persistence; use cases handle shared policy or
orchestration. Simple reads can use repository interfaces directly.

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

#### Module responsibilities

| Module | Hilt responsibility |
| --- | --- |
| `:app` | `@HiltAndroidApp` Application, `@AndroidEntryPoint` Activity, Android adapters, cross-module/domain providers, WorkManager configuration |
| `:data` | Repository constructor injection, interface bindings, network/storage providers |
| `:presentation` | `@HiltViewModel` classes with injected constructors; `hiltViewModel()` at destination entry points |
| `:domain` | Android-free models, contracts, and rules; plain constructors supplied by Hilt `@Provides` methods outside this module |

Keeping domain constructors plain is a local choice to keep the JVM module free
of Android DI tooling. Those objects still belong to the Hilt graph; providers
receive dependencies as parameters. There is no parallel manual graph.

App must have all Hilt-contributing modules on its transitive dependency path.
Configure Hilt and KSP only in the modules that need their code generation.
Validate compatible Hilt, AndroidX Hilt, KSP, Kotlin, AGP, and JDK versions during
F06; align JVM compilation targets and do not copy versions from guide examples
without checking compatibility. Gradle modules and Hilt binding modules are
different concepts and do not need a one-to-one mapping.

#### Ownership and lifetimes

| Dependency | Planned lifetime |
| --- | --- |
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
- Data owns persistent event, notification, and synchronization records.
- App implements a domain notification port using Android APIs. Domain does not
  reference NotificationManager or Context.
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
  when creating modules. The original starter passed F01 verification; the
  planned multi-module/Hilt configuration has not yet been implemented or tested.
  See [Work Log](WORK_LOG.md) for the baseline results.

## References

- [Android modularization](https://developer.android.com/topic/modularization/patterns)
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer)
- [Offline data access](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Periodic work](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)
- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt and Jetpack](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt across modules](https://developer.android.com/training/dependency-injection/hilt-multi-module)
- [Hilt testing](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Navigation state](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)
- [Coroutines](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Custom WorkManager configuration](https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration)

These are general guidelines. The four-module structure is a project decision
based on the scope and the importance of explicit architecture boundaries.
