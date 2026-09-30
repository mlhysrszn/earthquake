# Earthquake

An Android app that shows recent USGS earthquakes worldwide, lets the user set a
magnitude threshold, and posts a notification for an eligible new event. It is a
case-study project built in small, reviewed steps.

## Scope

- Native Android (Kotlin, Jetpack Compose), `minSdk` 29, single Activity.
- A list of worldwide events from the past 24 hours, event details, and
  notification settings (enable switch, 0.0–9.5 threshold in 0.5 steps).
- Cached content stays usable offline; failed refreshes keep existing data.
- Not included: accounts, payments, maps, location permission, a custom backend,
  other earthquake providers, or a store release.

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
  [work log](docs/WORK_LOG.md). Current results: 53 JVM tests, 55 live and 57 demo
  device tests, lint with 0 errors, all on an API 29 emulator.

The Android 13+ permission prompt, denial, and grant were checked manually on an
API 34 emulator. Known gap: a real timed background run after process death was
not observed.

## Local product events

The app records a small event log (screen views, preference saves, permission
outcomes, notification posts/opens, refresh outcomes) in a local Room table with
LIVE/DEMO labels. Nothing is uploaded. See [product events](docs/PRODUCT_EVENTS.md).

## AI assistance and time

AI tools and models used, delegated work, verification, and their limits are in the
[AI usage report](docs/AI_USAGE_REPORT.md). Total development time and token usage
were not measured.

## Next improvement

Add a user-facing view of the local event log, and automate the Android 13+
permission flow in an instrumentation test.
