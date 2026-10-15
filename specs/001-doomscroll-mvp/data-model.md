# Data Model: Android Intentional Use MVP

## Durable State

Use one versioned snapshot and transactional DataStore updates. No content, unrelated
package identity, raw usage transitions, reflection answers, or purpose text is durable.

| Entity | Fields and constraints |
| --- | --- |
| Preferences | schemaVersion=1; monitoringOptIn=false initially; selected supported package or null; remindersEnabled; selected mode |
| Activity | stable local ID; trimmed name, 1-80 characters; at most 10 activities; user-selected ordering |
| IntentCheckpoint | session ID; MANUAL/AUTOMATIC; selected package only for automatic; purpose category; remaining milliseconds; RUNNING/PAUSED/FINISHED/EXPIRED; generation |
| DailySummary | local calendar date; measuredMillis; manualMillis; reminderRequests; breaksChosen; activitySelections; nonnegative totals; retain today plus six prior dates |
| ReminderCheckpoint | current session/generation; unresolved reminder kind; last request time and clock/boot context needed for conservative cooldown recovery; suppression kind and remaining duration |

Clock and recovery metadata belong to timer/reminder state, not raw behavioral
history. Store no free-text purpose. Reopening counts and score history are not saved.
A selected package is an explicitly consented preference, not an installed-app inventory.

## Transient State

- ObservationWindow: consent start, current query cursor/deduplication keys, transient
  lifecycle state reduced to SELECTED/DEALGORITHM/OTHER/UNKNOWN, screen/keyguard state,
  completeness, measured duration since break, rolling qualifying return timestamps.
- IntentRuntime: monotonic anchors and optional Other purpose text, discarded on finish.
- ReminderRuntime: current stage, cooldown/suppression anchors, pending request context.
- CapabilityState: usage access, notification/channel enablement, supported-app
  availability, service eligibility, observation completeness, and monitoring status.

Keep transient package/class identifiers only as needed to reduce a query; immediately
forget unrelated identities and never log them. Do not persist a list of other apps.

## State Transitions

**Monitoring**: DISABLED -> NEEDS_PERMISSION/READY -> RUNNING -> PAUSED_UNCERTAIN or
STOPPED. A user starts RUNNING. Permission loss, service stop or process recovery
clears observation windows; restoring permission alone does not fabricate past data.

**Intent**: Start creates RUNNING with a 5/15/30-minute budget. Pause stops consumption;
Resume requires current capabilities. Finish is terminal. Expiry creates at most one
reminder; Add five minutes resumes with an extension. Restored checkpoints become
PAUSED until the user/capability state permits resumption. Save observed remaining
time before suspension; never reconstruct automatic usage across an unknown gap.

**Reminder**: Candidate -> SUPPRESSED or REQUESTED. Only REQUESTED increments the
counter and advances a score stage. Dismiss starts cooldown. Continue suppresses
by mode's time basis; Break finishes the intention and suppresses for five elapsed
minutes. Budget expiry takes priority over a score candidate.

**Deletion**: Disable observation, invalidate generation, cancel pending alarm and
notification, clear transient state, then atomically replace durable state with
initial defaults. A stale callback with an older generation performs no action.
A concurrently queried observation cannot recreate deleted state.

## Aggregation and Validation

- Merge intervals without overlap; partition observed time at local day boundaries.
- Only verified selected-app time becomes measuredMillis; manual timers use manualMillis.
- For ambiguous/unknown time, do not increment either category unless a separate
  explicitly running manual timer exists. Unknown observations do not become zeros.
- Use monotonic time for live duration, wall clock/time zone only for calendar labels.
  Detect clock/boot changes; conservatively pause or suppress rather than creating
  negative intervals or duplicate reminder requests.
- Enforce activity limits and reject empty names before storage. Treat names as text.
- Prune on every read/write access; combine writes atomically and perform I/O off main thread.
- Future schemas need an explicit migration; unknown/corrupt snapshots stop monitoring
  and show a recovery error. Do not silently overwrite records or upload diagnostic data.
- Store beneath noBackupFilesDir and disable/exclude cloud and device-transfer backup.
