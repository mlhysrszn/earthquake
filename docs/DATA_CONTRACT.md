# USGS Data Contract

Status: the initial domain model, list repository contract, deterministic sample
repository, USGS feed client/mapper, and Room event store are implemented.
Repository synchronization and the production list source remain pending.
See the [roadmap](ROADMAP.md) for delivery order and the
[architecture](ARCHITECTURE.md) for package responsibilities inside app.

## 1. Source and scope

- Source: USGS worldwide records, without an additional geographic filter.
- Initial window: the past 24 hours.
- List endpoint: `https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson`
- Parse the FeatureCollection, retain events with `properties.type == "earthquake"`,
  and sort by occurrence time, newest first.
- The notification threshold does not filter the visible list.
- USGS recommends summary feeds for automated applications. Source updates every
  minute do not imply notification delivery every minute on a device.
- The catalog API supports date, location, and magnitude queries if required
  later. These controls are outside the initial UI scope.

## 2. Earthquake domain model

| Domain field | USGS field | Mapping decision |
| --- | --- | --- |
| id | feature.id | Reject an event with a missing or blank ID |
| magnitude | properties.mag | Nullable; missing magnitude cannot trigger a notification |
| magnitudeType | properties.magType | Nullable; preserve the source magnitude type |
| place | properties.place | Nullable; the UI labels missing location text |
| occurredAt | properties.time | Epoch milliseconds to Instant; reject an invalid/missing value |
| updatedAt | properties.updated | Nullable Instant for source revisions |
| longitude / latitude | geometry.coordinates[0 / 1] | Use only a valid coordinate pair |
| depthKm | geometry.coordinates[2] | Nullable depth in kilometers |
| sourceUrl | properties.url | User-facing source page |

Decode DTOs defensively when fields are missing or null. Non-finite measurements
are invalid. Do not replace missing measurements with zero or reject a magnitude
solely because it is negative. Coordinate order is longitude, latitude, depth.
Preserve source text in its original language. The planned UI labels remain
Turkish; English documentation does not change the app's language.

The app-local `Earthquake` domain model uses nullable values for magnitude,
magnitude type, place, update time, coordinates, depth, and source URL. Identity
must not be blank; occurrence time is required. A present magnitude or depth must
be finite, and coordinates must be absent together or form a finite longitude/
latitude pair within their geographic ranges. Negative finite magnitudes remain
valid. Mapping is responsible for rejecting invalid source records before they
reach the list.

The USGS mapper ignores unknown JSON keys. A malformed JSON document, a root that
is not a FeatureCollection, or a FeatureCollection without a features array fails
as `MalformedUsgsFeedException`; it is never reported as a valid empty feed.
Within a structurally valid collection, non-Feature entries and features with a
missing properties object, missing/blank ID, or missing/unconvertible occurrence
time are skipped. Records whose `properties.type` is not `earthquake` are also
filtered without rejecting other features. Missing optional values remain null.
An incomplete or out-of-range longitude/latitude pair is omitted while the
otherwise valid earthquake is retained. Negative finite magnitudes are preserved.
Valid events are ordered newest first. `metadata.generated` is exposed separately
as source generation time, not as the device's successful-fetch time. JSON values
with incompatible field types make the feed malformed rather than silently
coercing an individual record.

The summary event already contains the fields needed by the planned detail
screen. Avoid one detail request per list item. For an event missing locally,
use the catalog query `query?format=geojson&eventid=...` when needed. Handle its
single-event response separately from a collection. Missing or removed events
have an explicit unavailable state.

## 3. Domain contracts

- `EarthquakeRepository`: observe the local list and individual events, refresh
  the list, and fetch missing details. Return domain models rather than DTOs or
  Room entities.
- `NotificationPreferencesRepository`: observe and save the enabled preference
  and magnitude threshold. Keep OS permission status separate from preference.
- `NotificationHistoryRepository`: persist event identities and notification
  processing states. Data implements storage and atomic deduplication operations.
- `EarthquakeNotificationSender`: send domain notification requests through the
  Android adapter in app.

Introduce contracts only when their delivery step needs them. Notification
contracts are completed in the notification phase. Data implements the contracts;
Hilt bindings select implementations without adding Android dependencies to
these domain interfaces. All implementations and contracts live in app packages.
One-shot operations use main-safe suspend functions;
observable data uses Flow. Keep mutable streams inside implementations.

The initial `EarthquakeRepository` contract exposes a Flow of the rolling
24-hour list, newest first, and a suspend refresh operation. Refresh outcomes are
typed as success (with the count of accepted records) or failure classified as
network, invalid response, storage, or unknown. Infrastructure exception types
do not cross the domain boundary. Individual-event observation and missing-detail
fetching will be added when the detail feature needs them.

Room retains only events in the inclusive interval from `now - 24 hours` through
`now`; future-dated and older events are excluded. A valid, fresh summary feed is
an authoritative snapshot for this window and atomically replaces the stored
event set, including clearing it for a valid empty feed. Source generation times
older than the last applied snapshot are ignored. If a previous source generation
is known, an incoming snapshot without its own generation time is also ignored;
the local successful-fetch time may advance, but cached events and the applied
source generation remain unchanged. When no source generation is known yet, an
otherwise valid unversioned initial snapshot may be applied. Source-generation
time and local successful-fetch time are stored separately.

## 4. Synchronization behavior

Validate successful responses and write them to Room. The UI observes Room.
Network failures preserve cached data. Repository implementations coordinate
sources and storage; domain use cases hold shared policy or orchestration. Track local successful-fetch time
separately from the source's `metadata.generated` timestamp.

The initial synchronization establishes a baseline without posting notifications.
A 24-hour feed is not a complete historical archive. After a long offline period,
the MVP does not guarantee retrospective notifications for events outside that
window. Define late events, revisions, and retention before implementing delivery.

Do not silently treat an invalid payload as a successful empty feed. Define
partial-record failures, stale source snapshots, and cache reconciliation during
the corresponding implementation tasks.

## 5. First runnable slice: acceptance criteria

1. Deterministic sample events reach the UI through the domain repository interface.
2. UI uses domain contracts; it does not directly access network clients or DAOs.
3. The list supports loading, content, empty, and error states.
4. Replacing the sample source with USGS does not change the ViewModel contract.

During USGS integration, additionally verify DTO mapping for missing magnitude,
missing place, malformed coordinates, invalid IDs/timestamps, and single-event
versus collection responses.

## References

- [GeoJSON summary](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php)
- [GeoJSON detail](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson_detail.php)
- [Catalog API](https://earthquake.usgs.gov/fdsnws/event/1/)
- [Event field definitions](https://earthquake.usgs.gov/data/comcat/data-eventterms.php)
