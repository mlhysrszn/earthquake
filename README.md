# Earthquake

An Android app that shows recent USGS earthquakes worldwide, lets the user set a
magnitude threshold, and posts a notification for an eligible new event. It is a
case-study project built in small, reviewed steps.

## Target user and problem

Turkish-speaking people who want to know when a significant earthquake happens
without watching a news feed or a raw seismic list all day. They care about large
events (for example near family, in neighbouring countries, or anywhere in the
world), but small, frequent tremors are noise to them.

The app answers two questions: "what happened recently?" (the list and details)
and "tell me only when it matters to me" (a personal magnitude threshold with a
notification for each new event above it). It is not an early-warning system; it
reports events after the seismic network has published them.

## Key decisions

| Decision | Why |
| --- | --- |
| USGS as the only source | Free, keyless, stable GeoJSON with documented fields; AFAD/Kandilli have better coverage of small Turkish events but no equally documented public API. Weaker coverage of small local events is an accepted limitation. |
| USGS summary feed (`summary/all_day.geojson`) instead of `fdsnws/event/1/query` | USGS recommends summary feeds for automated clients, and the feed is updated every minute. The catalog query is only needed for custom date, place, or magnitude filters, which are out of scope. |
| Worldwide events, past 24 hours, no region filter | No location permission is needed and the threshold already controls noise; a region filter is a possible next step. |
| The threshold filters notifications, not the list | The list stays a complete picture of recent activity; the threshold expresses what is worth an interruption. |
| First synchronization never notifies | Installing the app must not post a burst of notifications for events that already happened. |
| Each event is notified at most once | Events are tracked by ID (and USGS aliases), so repeated feeds do not re-notify. An event first suppressed as below threshold is checked again when USGS revises it, so an upward magnitude revision still notifies once. |
| The threshold is inclusive | With 5.0 selected, a magnitude 5.0 event is notified; USGS magnitudes often land exactly on such values. |
| Periodic WorkManager checks (15 min minimum) | Reliable without a backend or push service; the tradeoff is that notifications are not instant. |
| Local-first (Room cache) | The list stays usable offline, and failed refreshes never erase data. |
| A separate `demo` flavor | Notification behavior can be shown on demand with the same business rules, without waiting for a real earthquake. |
| Local product events instead of an analytics SDK | The case allows local logging; nothing leaves the device. |

## Scope

- Native Android (Kotlin, Jetpack Compose), `minSdk` 29, single Activity.
- A list of worldwide events from the past 24 hours with severity-colored
  magnitudes, relative ages ("12 dk önce"), a magnitude filter (all, 3.0+, 4.0+,
  5.0+), and pull-to-refresh. The list refreshes on return when data is older
  than five minutes.
- Event details with coordinates, an "open in map" action (any installed map
  app), and a link to the USGS event page.
- Notification settings: enable switch and a 0.0–9.5 threshold in 0.5 steps.
- Cached content stays usable offline; failed refreshes keep existing data and
  the last successful update time.
- Place names come from USGS in English (for example "10 km SW of ..."); they
  are shown as received and not translated.
- Not included: accounts, payments, an embedded map, location permission, a
  custom backend, other earthquake providers, or a store release.

## Architecture in brief

One Gradle module (`:app`) with packages for UI, domain, data, and DI. MVVM with
lifecycle-aware state collection, Hilt constructor injection, Navigation 3, Room
for cached events and notification state, DataStore for preferences, and
WorkManager for periodic checks. A shared refresher runs the same synchronization
and notification rules for foreground and background triggers.

Details: [Architecture](docs/ARCHITECTURE.md), [USGS data contract](docs/DATA_CONTRACT.md),
[notification decision table](docs/NOTIFICATION_DECISION_TABLE.md),
[product events](docs/PRODUCT_EVENTS.md), [Android guideline review](docs/ANDROID_GUIDELINE_REVIEW.md).
Progress and decisions: [Roadmap](docs/ROADMAP.md), [Work log](docs/WORK_LOG.md),
[AI usage report](docs/AI_USAGE_REPORT.md).

## Build and run

Requirements: JDK 17 (the toolchain used here) and an Android SDK; Android Studio
creates `local.properties` with `sdk.dir` (it is not committed).

There are two product flavors:

| Flavor | Application ID | Data source |
| --- | --- | --- |
| `live` | `com.mlhysrszn.earthquake` | Real USGS feed |
| `demo` | `com.mlhysrszn.earthquake.demo` | Locally injected scenario events |

```
./gradlew :app:installLiveDebug      # live app
./gradlew :app:installDemoDebug      # demo app
./gradlew :app:testLiveDebugUnitTest # JVM tests
./gradlew :app:connectedLiveDebugAndroidTest :app:connectedDemoDebugAndroidTest  # device tests
./gradlew :app:lintLiveDebug :app:lintDemoDebug
```

`live` is the default variant in Android Studio; pick `demoDebug` in Build Variants
to run the demo.

The demo uses its own database, preferences file, and WorkManager job name, so it
never mixes with live data.

## Demo steps

1. Install and open the demo app (label "Deprem Takip · DEMO").
2. Open **Bildirimler** and turn on notifications (on Android 13+ grant the
   permission when asked). Leave the threshold at 4.0.
3. Tap **Demoyu sıfırla** to start from an empty baseline.
4. Tap **Eşik altı olay ekle**: a magnitude 3.5 event appears, no notification.
5. Tap **Eşik üstü olay ekle**: a magnitude 5.5 event appears and one
   notification is posted. Tapping it opens that event's detail screen.
6. Tap **Son olayı tekrar gönder**: the same event is delivered again and no
   second notification is posted.

## Notification limits

Background checks use a unique periodic WorkManager job with a connected-network
constraint. WorkManager's minimum period is 15 minutes and the OS decides the
actual run time, so delivery is not instant. The first synchronization only sets a
baseline; events already present then are never notified. See the decision table
for duplicates, late records, revisions, and permission states.

## Testing

- JVM unit tests cover domain rules, mapping, policies, ViewModels, and refresh
  orchestration.
- Device tests cover Room (including migrations), repositories, notification
  processing and delivery, WorkManager scheduling and workers, Compose screens,
  navigation, and the demo scenario.
- Recorded results, warnings, and manual checks per task are in the
  [work log](docs/WORK_LOG.md). Current results: 62 JVM tests, 62 live and 64 demo
  device tests, lint with 0 errors, all on an API 29 emulator.

The Android 13+ permission prompt, denial, and grant were checked manually on an
API 34 emulator. Known gap: a real timed background run after process death was
not observed.

## Local product events

The app records a small event log (screen views, preference saves, permission
outcomes, notification posts/opens, refresh outcomes) in a local Room table with
LIVE/DEMO labels. Nothing is uploaded. See [product events](docs/PRODUCT_EVENTS.md).

How we would know the product works:

- **Setup completion:** share of users who start enabling notifications and
  successfully save the preference.
- **Notification-to-detail opening:** share of posted notifications whose event
  detail is opened, a sign that the alerts are relevant rather than noise.
- **Refresh failure rate:** share of foreground/background refreshes that fail,
  a sign of whether the data stays fresh.

## Time spent

Estimate: about **10.5 hours** of active work over 2026-09-27 to 2026-09-30. It is
the sum of the spans from the first to the last commit in each of six working
sessions (2h53m, 4h05m, 1h01m, 1h45m, 22m, and about 30m from 17:39 to the
post-review fix commit on 2026-09-30). This is a lower bound: work done before
each session's first commit (planning, reading documentation) is not included.
Time was not tracked with a timer.

## AI assistance

AI coding assistants wrote most of the code and documents under step-by-step
direction; the human set scope and decisions and approved every commit. Every step
was verified with builds, JVM and device tests, lint, and manual emulator checks
before approval.

| Assistant / model | Work | Commits | Share |
| --- | --- | --- | --- |
| OpenAI Codex (exact model ID not captured) | Planning documents, project setup, Hilt | 3 | 9% |
| OpenCode, GPT-6 Luna (`opencode-go/gpt-6-luna`) | Domain, list, USGS/Room data, details, notifications, background work, product events | 16 | 50% |
| Claude Code, Claude Sonnet 5.5 (`claude-sonnet-5-5`) | Demo flavor, acceptance walkthrough, README, permission check, inset fix | 7 | 22% |
| Claude Code, Claude Opus 5.5 (`claude-opus-5-5`) | Case-brief review, README, publication, code review, post-review fixes, processor restructuring, record updates | 6 | 19% |

The share is the number of commits per model divided by all 32 commits, counting
the commit that updates this table. The table is updated with every commit so it
always matches Git history. Commits are not weighted by size; the post-review fix
batch alone changed about 40 files. Tokens
were not exported, so token-based shares are unavailable. Details per task are in
the [AI usage report](docs/AI_USAGE_REPORT.md).

## Next improvement

Add an optional region filter (for example "near Türkiye") on top of the magnitude
threshold, show a user-facing view of the local event log, and automate the
Android 13+ permission flow in an instrumentation test.
