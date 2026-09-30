# Notification Decision Table

Status: N01 policy is implemented by the N04 pure eligibility policy and N05
persistent processor. N06 implements Android delivery; N07 implements
preference-driven WorkManager scheduling and shared foreground/background
orchestration. This document is the source of truth for event eligibility,
baselines, deduplication, and suppression.

## Inputs and evaluation boundary

- Evaluate only after a structurally valid, non-stale USGS snapshot has been
  accepted by synchronization. Network, HTTP, malformed-feed, storage, and stale
  snapshot outcomes do not create notification decisions or advance the cursor.
- Use the saved threshold and enabled preference at evaluation time. OS
  notification permission is a separate input from the user's preference.
- A candidate must be in the current inclusive 24-hour event window and have a
  previously unseen canonical event identity. Eligibility is strict:
  `magnitude > threshold`. Missing magnitude is ineligible.
- The effective feed cursor is `metadata.generated`; when the initial valid feed
  has no source generation time, use its local successful-fetch time. Later feeds
  must advance that cursor to be evaluated.

## Decisions

| Case | Notification decision | Persistent processing decision |
| --- | --- | --- |
| First successful synchronization | Never notify historical records, regardless of magnitude or preferences | Establish the baseline/cursor and mark all identities in that snapshot as baselined |
| Snapshot cursor is equal to or older than the processed cursor | Do not evaluate it | Do not change the cursor or per-event outcomes |
| New event magnitude is below threshold | Do not post | Record a terminal `BELOW_THRESHOLD` outcome |
| New event magnitude equals or exceeds threshold; enabled and permission granted | Attempt to post | Record `PENDING`, then `POSTED` when the adapter succeeds. The threshold is inclusive: with 5.0 selected, a 5.0 event is notified |
| New event has no magnitude | Do not post | Record a terminal `MISSING_MAGNITUDE` outcome |
| Same canonical identity appears in a later feed | Do not post again | Update cached event data and last-seen time; retain its existing outcome |
| Threshold increases | Does not cancel already-posted notifications | Apply the new threshold only to unprocessed candidates |
| Threshold decreases | Do not replay prior below-threshold events | Keep terminal outcomes; apply the new threshold only to unprocessed candidates and to later source revisions |
| Source revision (`updatedAt` after the last decision) of an identity previously suppressed as below threshold or missing magnitude | Evaluate it again like a new event with the current preferences; post once if it now qualifies | Replace the suppressed outcome with the new decision and decision time. Legacy `AT_THRESHOLD` rows are re-evaluated the same way |
| Revision of any other processed identity (posted, baselined, disabled, permission-suppressed, ambiguous) | Do not post a second notification | Update cached event data and last-seen time; retain the processing outcome |
| Late-arriving event occurred before the feed cursor but is still within the current 24-hour event window | Treat it as a new candidate and apply the current threshold/preferences | Process by its canonical identity; `updatedAt` does not make an old event eligible outside the 24-hour window |
| Event is older than the current 24-hour window | Do not notify or queue for later | Do not create a candidate; the event-store retention policy is independent |
| Long offline period | Consider only unseen events present in the current feed and still inside the 24-hour window; events outside it are not backfilled | Advance the cursor only after processing the accepted snapshot |
| Preference disabled when a candidate is evaluated | Do not post | Record terminal `SUPPRESSED_DISABLED`; enabling later does not replay it |
| OS notification permission denied | Do not post | Record terminal `SUPPRESSED_PERMISSION`; granting permission later does not replay it |
| Notification adapter has a retryable failure | Do not mark as posted | Keep `PENDING`/`RETRYABLE` only while the event is within the 24-hour window and notifications remain enabled/permitted |
| Event ID already matches a known alias | Treat as the same event; do not post again | Merge the alias into the existing canonical identity |
| An alias set conflicts with more than one known canonical identity | Do not post | Record/quarantine as `AMBIGUOUS_IDENTITY`; do not reassign existing aliases |

After handling every event in an accepted snapshot, persist the processed feed
cursor. The first baseline uses the source generation time when present and local
successful-fetch time otherwise. A failed or stale snapshot never advances it.

When multiple suppression conditions apply to one record, use this deterministic
precedence: initial baseline, stale snapshot, ambiguous alias, already processed,
outside event window, disabled preference, denied permission, missing magnitude,
below threshold. Only a record passing every check is eligible.

## Identity and alias rules

- `feature.id` is the canonical identity for a new event.
- Treat non-empty, trimmed IDs in USGS `properties.ids` as aliases of that
  canonical identity. Ignore empty tokens and duplicate aliases.
- If a new canonical ID or any alias resolves to an already-known identity, it is
  a duplicate/revision of that event. If aliases resolve to multiple identities,
  do not notify or merge them automatically.
- If no IDs/aliases overlap, different `feature.id` values are distinct events.
  Do not infer identity from place, coordinates, magnitude, or time.

## Delivery, crash, and retention policy

- Persist the canonical identity and `PENDING` outcome before calling the Android
  notification adapter. Persist the adapter result afterward.
- Use a stable OS notification ID derived from the canonical identity. If the
  process dies after posting but before persisting `POSTED`, a retry may update the
  same OS notification. Database state and OS posting are not atomic, so exactly-once
  delivery is not promised.
- A retryable pending outcome expires when the event leaves the rolling 24-hour
  window. Permission-denied and user-disabled outcomes are terminal, not retryable.
- Retain processed identity/alias mappings and terminal outcomes for 30 days after
  last seeing the event. Retain the baseline/cursor independently. D02 continues
  to retain earthquake rows only for the rolling 24-hour window.
