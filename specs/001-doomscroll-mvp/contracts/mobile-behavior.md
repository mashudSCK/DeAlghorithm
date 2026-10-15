# Mobile Behavior Contract

The app has user-facing controls and Android callbacks, not a public network API.
This contract defines observable actions without introducing a reusable service layer.

## User Actions

| Action | Required state | Result |
| --- | --- | --- |
| Enable monitoring | Disclosure accepted; supported app selected | Open Usage Access settings if needed; no collection before grant/opt-in |
| Start automatic session | Usage Access; validated app/OS; service eligible; visible-app user action | Start stoppable observation; show ongoing status; score initializing until complete |
| Start manual intent | No monitoring permission required | Start labeled elapsed timer; never create measured app minutes or a score |
| Pause/Resume | Active or paused intent | Stop/restart mode-appropriate consumption; do not count unknown automatic intervals |
| Continue five minutes | Current unresolved reminder/session | Resolve reminder; add mode-appropriate suppression; use current session only |
| Take a break/Finish | Current session | End intention; five-minute elapsed suppression; no forced app closure |
| Choose activity | Existing saved activity | Record selection once; never claim completion or open external content automatically |
| Dismiss reminder | Current request | Resolve request and retain cooldown; no implied five-minute extension |
| Stop/Disable monitoring | Any observation status | Stop collection and pending automatic reminders immediately; retain prior summaries |
| Delete all data | Explicit confirmation | Cancel all work/reminders, invalidate callbacks, clear all state, opt-in=false |

## Observation Contract

Input carries a transient timestamp and classified lifecycle/screen/keyguard event.
Reduce selected-package activity switches without counting them as app reopenings.
An OTHER interval must be verified for at least ten seconds before a selected-app
return can increment R. Unknown, lock/unlock, or DeAlghorithm/reminder return paths
cannot qualify. Query overlap is deduplicated in memory.

Output carries observed selected-app duration, valid return count, and completeness.
Only a complete window produces a score; unavailable is a distinct state, not zero.
Multiple resumed app candidates suspend measurement rather than guessing focus.

A matched pause leaving the complete resumed set empty proves only an interval with
no resumed candidate. It contributes no selected or OTHER time and publishes no
focus or reopening count while empty. A subsequent matched handoff preserves earlier
verified inputs; only actual OTHER foreground milliseconds can meet the ten-second
threshold. Missing identities, unmatched pauses outside the initial empty post-lock
cleanup, missing queries, late events and multiple resumed packages still invalidate
coverage or eligibility. A rejected late event is deduplicated, never replayed into
previously counted time; a later fresh screen boundary may establish a new baseline.

## Android Callback Contract

Use explicit immutable PendingIntents with session ID, generation, and action kind;
receivers are not exported for arbitrary external invocation. Validate identifiers,
current durable state and allowed actions before modifying state or posting a notice.
One reminder notification ID prevents concurrent prompts. A stale, canceled, deleted,
permission-denied, or suppressed callback does nothing and cannot resurrect records.

No full-screen launch, exact-alarm permission, accessibility service, overlay,
INTERNET permission, or broad installed-app inventory belongs in the baseline manifest.
Only a narrow query for the supported installed app is justified. Typed foreground
service permissions/declarations appear only if the observation trial establishes
that the candidate service is appropriate.

## Visible States

Always distinguish Manual timer, Measured app use, Monitoring stopped, Permission
needed, Initializing observation, Score unavailable, and Reminders disabled.
Notification posting is a request, not evidence the user saw or read it. If delivery
is unavailable, show overdue intent status in the app. Present activity text as data,
not HTML or navigation. Keep all controls accessible and reminders non-shaming.
