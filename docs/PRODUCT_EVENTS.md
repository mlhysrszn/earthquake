# Local Product Events

Q01 events are diagnostic/product-measurement records stored only in the app's
Room database. No event is uploaded to a server or third-party analytics SDK.
Each record has a UUID, UTC timestamp, event name, `LIVE`/`DEMO` environment, and
a small string-to-string properties object. Public USGS event IDs are used only
to join notification posting/opening records; there are no account, device, or
location identifiers.

The `product_events` Room table retains the latest 90 days, capped at 20,000
rows. `ProductEventRepository.observeRecentEvents()` and `getRecentEvents()`
expose the newest 200 records by default for tests and local inspection; Android
Studio's Database Inspector can inspect the same table.

## Event catalog

| Event | When recorded | Useful properties |
| --- | --- | --- |
| `SCREEN_VIEW` | List or detail route is composed | `screen=list/detail`; details also include `event_id` and `entry_source=list/notification/external` |
| `NOTIFICATION_SETUP_STARTED` | User enables the notification preference | `attempt_id` |
| `NOTIFICATION_SETUP_COMPLETED` | Enabling preference was saved successfully | Matching `attempt_id`; this means preference persistence, not OS permission approval |
| `NOTIFICATION_PREFERENCE_SAVED` | Enabled state or magnitude threshold was saved | `preference`, `value` |
| `PERMISSION_OUTCOME` | Runtime permission request returns, or system settings changes the observed status | `status`, `trigger=runtime_request/system_settings` |
| `NOTIFICATION_POSTED` | Android accepted a successful post/update request | `event_id` |
| `NOTIFICATION_OPENED` | Navigation entered the matching detail route from a notification | `event_id` |
| `NOTIFICATION_DELIVERY_SUPPRESSED` | OS permission or app notification setting prevents posting | `event_id`, `reason` |
| `NOTIFICATION_DELIVERY_FAILED` | Adapter returned a retryable posting failure | `event_id` |
| `REFRESH_STARTED` | Foreground or background refresh begins | `origin=foreground/background` |
| `REFRESH_SUCCEEDED` | A valid source snapshot was accepted | `origin`, `accepted_count` |
| `REFRESH_FAILED` | Source refresh returned a failure | `origin`, `reason` |
| `NOTIFICATION_PROCESSING_FAILED` | Refresh succeeded but pending notification processing threw | `origin` |

Local recorder/storage errors are logged and do not change refresh, preference,
or notification behavior. Coroutines still propagate cancellation. The `environment`
column is set explicitly and Q02 demo flows must use `DEMO` rather than `LIVE`.

## Metric definitions

Compute counts/rates from retained local rows; report the time range and environment
with every result.

- **Preference setup completion:** distinct `attempt_id`s with
  `NOTIFICATION_SETUP_COMPLETED` divided by distinct `attempt_id`s with
  `NOTIFICATION_SETUP_STARTED`. Completion means the enabled preference was
  persisted. OS permission is a separate outcome and does not redefine this
  numerator.
- **Notification-to-detail opening:** distinct `event_id`s with
  `NOTIFICATION_OPENED` within seven days of at least one matching
  `NOTIFICATION_POSTED`, divided by distinct successfully posted `event_id`s in
  the same reporting cohort. A successful post means the app submitted/updated
  an OS notification; it does **not** prove the user saw it.
- **Refresh failure rate:** `REFRESH_FAILED` count divided by `REFRESH_STARTED`
  count, grouped by `origin`. A worker retry is another started attempt. Failures
  in notification processing after a successful feed refresh are recorded
  separately and are not included in the refresh-failure numerator.

`SCREEN_VIEW` is emitted from a route-scoped `LaunchedEffect`, so recomposition
does not create another view event. Returning to a route after navigating away is
a new composition/view. A notification opening is recorded only when its matching
detail route is entered.
