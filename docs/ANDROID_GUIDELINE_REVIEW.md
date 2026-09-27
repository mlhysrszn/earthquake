# Android Guideline Review

Status: planning review updated for a single app module; Hilt remains pending.
Git setup and starter verification are recorded separately in [Work Log](WORK_LOG.md).
Reviewed on: 2026-09-27. Single-module decision updated on: 2026-09-28.

## Outcome

Use Hilt from the first dependency-injection setup. The earlier manual container
and handwritten ViewModel/Worker factory plan is superseded. The app has multiple
screen ViewModels and WorkManager, which are explicit reasons to choose Hilt in
Android's [architecture recommendations](https://developer.android.com/topic/architecture/recommendations).

The review also made lifecycle collection, navigation, coroutine ownership,
testing, and component scopes explicit. These changes are incorporated in
[Architecture](ARCHITECTURE.md), [Data Contract](DATA_CONTRACT.md), and
[Roadmap](ROADMAP.md). The roadmap contains 24 active tasks after removing separate data/UI module setup.

## Decisions reviewed

| Area | Outcome | Project decision / rationale | Delivery tasks |
| --- | --- | --- | --- |
| Dependency injection | Change | Use Hilt's graph and constructor injection; replace manual wiring | F06 |
| Object lifetimes | Clarify | Scope shared stores/coordination; leave cheap stateless rules unscoped | F06, D02, N05 |
| ViewModel creation | Change | Use Hilt ViewModels at screen entries; reusable UI receives state and callbacks | L03, L04, D04 |
| Background injection | Change | Use HiltWorkerFactory and assisted Worker construction; check startup configuration | N07 |
| UI state | Clarify | Immutable StateFlow, lifecycle-aware collection, and private mutable state | L03, L04 |
| UI actions/results | Clarify | Process results into state; keep direct navigation actions in UI callbacks | L03, D04 |
| Navigation | Specify | Navigation 3, typed event-ID destinations, saved back stack, and entry-owned ViewModels | D04, N06 |
| Coroutines | Clarify | Main-safe operations, scoped work, injected dispatchers where needed, cancellation preserved | L03, D03 |
| Storage and repositories | Keep | Room-backed event reads; DataStore preferences; repository source coordination | D02, D03, N02 |
| Domain layer | Keep with rationale | Shared eligibility policy serves foreground/background processing; use cases only for useful shared behavior | L01, N04 |
| Gradle structure | Change | Keep all code and tests in app; separate responsibilities through packages at the user's request | F03, F06 |
| Model separation | Refine | Separate transport/storage concerns; avoid a mandatory extra UI model or one-to-one wrapper use cases | L01, D01, D02 |
| Tests | Clarify | Prefer fakes and coroutine test dispatchers; use Hilt test bindings for graph integration | L03, F06, Q03 |
| Demo wiring | Clarify | Use variant-specific Hilt bindings with isolated storage rather than mutable global overrides | Q02 |
| WorkManager schedule | Keep | Durable periodic checks fit the MVP; no exact-time or instant-delivery promise | N07 |

## Guidance and project choices

Android's domain layer is optional. This project keeps shared policies and
repository contracts in an app-local domain package. Data implementations own
caching and source coordination. The package conventions are not compiler-enforced
module boundaries. Domain behavior can still run in local JVM unit tests under
`app/src/test` when it avoids Android runtime APIs. See the
[domain guide](https://developer.android.com/topic/architecture/domain-layer) and
[modularization patterns](https://developer.android.com/topic/modularization/patterns).

The user explicitly chose one app module. Separate data/UI module tasks were
removed rather than replaced with empty scaffolding. Hilt binding modules remain
ordinary annotated classes in the app source tree.

Room and DataStore remain appropriate for the distinct event and preference
storage needs. Keeping reads local also preserves useful content during network
failures. See [offline data access](https://developer.android.com/topic/architecture/data-layer/offline-first)
and [DataStore](https://developer.android.com/topic/libraries/architecture/datastore).

No new framework is needed merely to label the architecture Clean Architecture,
MVVM, or MVI. Class responsibilities, data flow, and testable behavior are the
criteria. Retrofit/serialization choices are integration decisions, not a claim
that Android mandates a particular HTTP client.

## Integration checks before implementation is considered complete

- F06: confirm a compatible Hilt/KSP/Kotlin/AGP/Gradle/JDK combination and successful
  Hilt graph generation in app. The Hilt implementation has not yet been tested.
- D04: verify Navigation 3 saved state and ViewModel ownership. Do not assume an
  event ID in a route is automatically present in SavedStateHandle.
- N07: verify HiltWorkerFactory, component-compatible dependencies, the merged
  manifest, and execution when the UI has never opened in the current process.
- Q03: test dependency replacement with isolated storage and verify observable
  behavior rather than only checking annotations or class structure.

## Supporting references

- [Hilt setup and components](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt ViewModel, navigation, and Worker integration](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt testing](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Navigation state and ViewModel ownership](https://developer.android.com/guide/navigation/navigation-3/save-state)
- [UI event handling](https://developer.android.com/topic/architecture/ui-layer/events)
- [Coroutine practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Periodic work constraints](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)
