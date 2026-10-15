# Manual intention verification

Date: 2026-10-09 (Asia/Manila). Device: read-only API 36 emulator, Android 16.

T007-T013 are complete. Manual timers support the four purpose categories,
5/15/30-minute budgets, pause/resume, finish, five-minute extension and break
suppression. Manual duration never becomes measured selected-app time or a score.
The activity list and automatic US2 capabilities are not complete.

## Checks and outcomes

- Three SessionRules JVM tests passed, alongside three model tests. They cover
  budgets, expiry, elapsed extensions, paused suppression, five-minute breaks,
  the 120-second cooldown, unresolved requests and clock/boot changes.
- Build, JVM tests, lint and both APK assemblies passed: exit 0,
  `BUILD SUCCESSFUL in 1m 43s`, 80 tasks.
- The manual UI journey passed with reminders disabled, including pause/resume,
  overdue controls, extension and finish; measured time and reminder requests stayed zero.
- A separate native permission-denied test passed with reminder preferences enabled:
  overdue status and extension remained usable, with zero requests.
- Real Android notification actions passed: Add extends once; old revision tokens
  cannot extend again; Dismiss resolves the request without finishing; Finish ends
  the intent; Choose opens the empty activity view only after sending its PendingIntent.
  Request counts describe posting requests, not notices read or delivered.
- Purpose text was absent from the durable file and cleared on finish.
- Activity recreation preserved the live running timer and revision.
- After instrumentation stopped the app process, a cold launch restored its running
  checkpoint PAUSED. Remaining budget and daily totals matched the pre-launch bytes;
  no process-gap time or subsequent paused time was added.
- Old generation alarm/action callbacks and a stale purpose-editor callback after
  deletion could not recreate an intent, totals or optional text.

Seven distinct IntentFlow instrumented cases passed across the permission matrix,
including the running-checkpoint fixture used for the separate cold-launch check.
The three latest rotation/denial/deletion cases reported `OK (3 tests)`.
Ignored logs are `build/manual-final-gate.log`, `build/manual-notification-test.log`,
`build/manual-permission-test.log` and `build/manual-recovery-privacy.log`.

The initial rule check failed for the absent implementation. The initial UI baseline
then failed for missing controls after the Android 16 Espresso compatibility fix.
Espresso 3.7.0, test runner 1.7.0 and AndroidX test JUnit 1.3.0 are now pinned; the
[official release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)
describe the input-injection fix. Permission revocation runs outside instrumentation
because the emulator terminated the instrumented process when its permission changed.

## Runtime and delivery limits

One process-scoped runtime and mutex serialize timer actions, deletion, callbacks
and notification posting. Checkpoints use elapsedRealtime for active manual time.
A wall/monotonic discrepancy over five seconds or a boot-context change pauses
conservatively. Every process restoration pauses a running checkpoint and resets
existing cooldown context conservatively. Optional purpose text has no durable field.

Expiry alarms use ordinary inexact AlarmManager scheduling, not exact-alarm access.
Receivers are not exported. Immutable explicit PendingIntent identities include
generation, session and revision in their data URI, not only extras. Canceled,
stale and suppressed callbacks cannot recreate records or replay the same expiry.

The emulator action tests use an explicitly expired test checkpoint to test UI and
callback routing. They do not establish exact five-minute background delivery.
Doze, OEM restrictions and process termination can delay or prevent reminders.
A callback starting a new process restores the timer paused instead of inventing
missed time. Denied permission or a disabled channel leaves overdue status visible.

No physical Android background-delivery, TikTok observation, score, activity-list,
TalkBack or five-person usability acceptance is claimed here. Their tasks remain
unchecked. No app was published.
