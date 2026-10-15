# Activities and seven-day summaries verification

Date: 2026-10-09 (Asia/Manila). Device: API 36 emulator, Android 16.

T024–T029 are complete for the independent manual-based US3 increment. Automatic
observation, scoring and physical background gates remain incomplete. No dependencies
or schema changes were added; schema 1 already contained activities and daily aggregates.

## Behavior delivered

- Add, rename, delete and reorder up to ten activities. Names are trimmed and contain
  1–80 characters. Stable local IDs survive renaming and ordering. Invalid edits or
  an eleventh activity preserve existing records and do not trigger a storage error.
- One explicit choice increments one activity-selection counter. Choosing stays on
  Activities; it does not finish a timer, open external content or claim completion.
  Empty lists offer Add an activity and Take a break. Selection feedback has a polite
  accessibility live region; actions have descriptive names and 48dp minimum targets.
- Summary shows today and six previous local dates, with manual duration, recorded
  measured duration, reminder requests, breaks chosen and selections separately.
  Dates without observations show measured use unavailable, not an invented measured
  zero. Durations display whole minutes and seconds; stored milliseconds are unchanged.
- Disable monitoring resets opt-in/mode and preserves activities, summaries and manual
  timers. Automatic checkpoints are paused and automatic reminders canceled if present.
  No actual observer/service exists yet; real collection-stop reliability belongs to
  the US2 device gates and must be integrated when observation is implemented.
- Confirmed deletion invalidates generation under the runtime mutex before cancellation
  and clearing. It clears activities, summaries, intent, preferences and transient
  purpose/selection feedback. Stale activity edits, selections and timer callbacks
  cannot recreate deleted records. Captured timer work is canceled on deletion errors.

All activity transformations reuse the existing transactional StateStore update;
no stale snapshot is written. The existing noBackupFilesDir location and backup
exclusions remain in effect.

## Checks and outcomes

The initial activity journey failed at the absent `Add an activity` control, before
implementation. This demonstrates the acceptance test detected missing behavior.

Final build command (the ignored init script uses the previously staged official
Maven cache for this machine's dependency-download workaround):

```powershell
.\gradlew.bat --offline -I .setup-cache/local-official.init.gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain
```

Result: exit 0, `BUILD SUCCESSFUL in 1m 53s`, 80 tasks. Six JVM tests passed.
Lint reports zero errors and the existing missing-launcher-icon warning.

Both APKs were installed with `adb -s emulator-5554 install --no-streaming -r -t`.
Notification grant/revoke commands ran outside instrumentation because permission
changes can terminate the instrumented process.

- Initial focused StateStoreTest + ActivityFlowTest run: `OK (8 tests)`, 56.217 seconds.
- Final complete native suite with notifications granted: `OK (15 tests)`, 83.702 seconds.
  Fourteen cases executed successfully; the denied-notification case was intentionally
  skipped by its permission assumption and then exercised in the denied matrix.
- Final ActivityFlowTest + denied-notification timer case, notifications revoked:
  `OK (6 tests)`, 38.803 seconds, no skips. These include all five activity journeys.

Together, all fifteen distinct native cases executed successfully across the matrix.
The suite includes existing manual timer, native notification actions, transient text,
rotation and stale-generation regression checks.

Three StateStore cases cover retention and separate totals, invalid/unknown schemas,
activity bounds, 80-character names, serialized user ordering and calendar changes.
An injected date advances one day, pruning the seventh prior date; a backward change
removes future dates. Rejected edits leave the prior snapshot intact. This checks the
retention policy without changing the device clock.

Five ActivityFlow cases exercise empty-list fallback, add/rename/delete/reorder,
trimmed names and disabled Save for empty/81-character input; single selection without
navigation/completion; seven dated summary rows and unavailable measurements; ten-item
limits and stale/removed IDs; cancel/confirm deletion, canceled PendingIntent identities
and no surviving notifications; and summaries/manual checkpoints preserved on disable.
Stale callback checks live in ActivityFlowTest/IntentFlowTest alongside the runtime,
while storage constraints remain in StateStoreTest.

The first combined denied run found a test isolation failure: the timer case assumed
Timer was open after Activities tests left Settings open. IntentFlowTest now explicitly
opens Timer in its setup. The final combined run passed.

Summary and empty activity views were visually inspected on the emulator. A System UI
wait dialog initially obstructed the summary; choosing Wait cleared it, and the next
capture showed the app unobstructed. This was a system window, not an app error claim.
Full TalkBack, enlarged-text, keyboard and five-person acceptance remain T032 work.

## Evidence locations and limits

Ignored local logs: `build/activities-baseline-test.log`,
`build/activities-verification-gate.log`, `build/activities-integrated.log` and
`build/activities-denied-final.log`. Inspected local screenshots:
`build/activities-summary-preview.png` and `build/activities-preview.png`.

The measured summary rendering test seeds aggregate fixtures. It verifies separate
labels and counters, not actual cross-app observation. No physical phone, supported
TikTok package/OS pair, service eligibility, measured battery behavior or usability
participant outcome is inferred from these emulator checks. T014–T023 and T030–T033
remain unchecked; this result does not declare the full MVP complete.
Subsequent milestone (2026-10-09): T014-T016 now pass their pure-rule and diagnostic
trial checks; see [observation trial evidence](observation-trial.md). The statements
above describe the earlier US3 verification state. Physical T017 and later automatic
gates remain unchecked.
