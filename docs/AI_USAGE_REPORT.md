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
- Commit approval for this corrected scope is pending. Usage totals remain unavailable.
