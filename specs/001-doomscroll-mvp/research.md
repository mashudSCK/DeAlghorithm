# Research: Android Intentional Use MVP

**Date**: 2026-10-08

Research resolves design choices, not physical-device feasibility. No APK, device
test, battery measurement, or store approval exists yet.

## 1. Native app and build tooling

**Decision**: One Android app module using Kotlin, Jetpack Compose, a ViewModel,
and direct construction of a small state store. Use the installed Android Studio
Empty Activity template and commit its compatible Gradle wrapper/version catalog.
Use Java 17, minimum Android 10/API 29, and compile/target API 36 as the initial
build baseline; recheck distribution target requirements before release.

**Rationale**: Android is confirmed. Native observation and notification integration
are central; a cross-platform bridge brings no benefit to the first platform.
This workspace already has Java 17, Android Studio, SDK platforms 33-36, and adb.
No authorized device is currently attached.

**Alternatives**: Flutter/React Native, multiple feature modules, dependency injection
frameworks, and a backend are unnecessary for this scope. Do not upgrade the IDE
solely to match a documentation version. If AGP 9 is selected, use built-in Kotlin
and match the Compose compiler plugin to the resolved Kotlin compiler; pin the
actual compatible versions during scaffolding.

[Project creation](https://developer.android.com/studio/projects/create-project),
[AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes),
[built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin),
[Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler).

## 2. Usage observation

**Decision**: Read recent usage transitions after opt-in and a separate Usage Access
Settings grant. Start at consent time, rather than importing prior history. Reduce
nonselected identities immediately to OTHER or DEALGORITHM; retain no unrelated
package names or raw history. Same-package activity switches are not reopenings.

**Rationale**: Usage totals cannot establish repeat-opening sequences. Observation
must reconstruct a conservative state from events, not claim to measure attention.
Permission restoration starts a fresh observation window.

**Alternatives**: Accessibility, screen scraping, overlays, and content inspection
are excluded. Synthetic inputs may test reducers but never establish production support.

[Usage observation](https://developer.android.com/reference/android/app/usage/UsageStatsManager),
[event reference](https://developer.android.com/reference/android/app/usage/UsageEvents.Event).

## 3. Foreground ambiguity and locked devices

**Decision**: Count time only in an observed, continuous, selected-app interval with
an interactive screen, hidden keyguard, and no overlapping resumed-app ambiguity.
Uncertain intervals are excluded; scores remain unavailable until a complete fresh
window is established. Detecting multi-window ambiguity is a device-test gate.

**Rationale**: Android multi-resume can leave several activities resumed. Selecting
the latest resumed package does not prove exclusive focus. Storage-unlocked status
is also different from current screen-lock status.

**Alternatives**: Guessing focus or treating missing observations as zero would
contradict the specification. Full-window coverage is required for a zero reopening count.

[Multi-resume](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode#multi-resume),
[keyguard](https://developer.android.com/reference/android/app/KeyguardManager#isKeyguardLocked()),
[screen interactivity](https://developer.android.com/reference/android/os/PowerManager#isInteractive()),
[storage unlock](https://developer.android.com/reference/android/os/UserManager#isUserUnlocked()).

## 4. Background monitoring is conditional

**Decision**: Investigate an explicitly user-started, stoppable foreground monitoring
service. On Android 14+, specialUse is the candidate type; its actual suitability,
required declaration, and distribution eligibility remain release gates. Poll recent
observations on a modest initial five-second cadence only during an enabled session.
Suspend querying while locked and stop after ten minutes away from the selected app.
Do not restart monitoring automatically after process termination.

**Rationale**: Timely cross-app feedback needs ongoing observation. A service type
must describe the real work; this app must not masquerade as health or data sync.
Successful sideloading would not establish Google Play eligibility.

**Alternatives**: Periodic WorkManager has a 15-minute minimum and cannot support
the intended monitoring cadence. Unbounded ordinary background loops are unreliable.
If the gate fails, manual mode remains usable, but automatic requirements remain
incomplete; changing release scope needs an explicit recorded decision.

[Service types](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use),
[Play service eligibility](https://support.google.com/googleplay/android-developer/answer/16559646),
[service declarations](https://support.google.com/googleplay/android-developer/answer/13392821),
[background start limits](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start),
[user stopping](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping),
[periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work#schedule_periodic_work).

## 5. Timers and notifications

**Decision**: Use monotonic elapsed time for running manual timers and inexact alarms
for background expiry notices; no exact-alarm permission. Automatic budgets consume
only verified observed duration. Ordinary notification channels and user-action
PendingIntents carry reminders. Check notification permission/channel state.

**Rationale**: In-app remaining time can be accurate without promising exact
background delivery. Delayed callbacks recheck the session and suppression state,
then post at most one relevant reminder. Record requests, never reads or delivery.
Restored intentions remain paused; process gaps are never fabricated as observed use.

**Alternatives**: Exact alarms, forced launches, overlay prompts, and periodic workers
for timer precision are unnecessary. Requesting notification access is contextual.

[Elapsed clock](https://developer.android.com/reference/android/os/SystemClock),
[boot count](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT),
[alarm guidance](https://developer.android.com/develop/background-work/services/alarms),
[notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## 6. Local storage and verification

**Decision**: Preferences DataStore 1.2.1, using one store in noBackupFilesDir. Encode
bounded activities and seven daily summaries with platform JSON; no database yet.
Use transactional writes. Exclude behavioral state from backup/device transfer,
disable backup, omit INTERNET, and avoid analytics or crash-reporting SDKs.

**Rationale**: Small bounded state does not need Room. Backup exclusion supports the
local privacy promise; manifest backup disablement alone is insufficient on all devices.
Deletion must invalidate stale callbacks before clearing state.

**Alternatives**: Room becomes useful if history/query scope grows. Plain ad hoc file
writes require more concurrency handling. Cloud persistence is outside scope.

Verify pure scoring/state logic with focused JVM tests; storage, notification actions,
and UI with instrumentation; background behavior with physical Android 10 and a
modern typed-service device. These checks are planned, not performed.

[DataStore](https://developer.android.com/topic/libraries/architecture/datastore),
[custom storage factory](https://developer.android.com/reference/androidx/datastore/preferences/core/PreferenceDataStoreFactory),
[backup rules](https://developer.android.com/identity/data/autobackup),
[local tests](https://developer.android.com/training/testing/local-tests),
[instrumentation](https://developer.android.com/training/testing/instrumented-tests).

## 7. Diagnostic implementation notes (2026-10-09)

T014-T016 now implement the pure reducer/score candidates and the user-triggered native
trial described in [trial evidence](evidence/observation-trial.md). Earlier research
entries describe design decisions; implementation and verification status belongs to
that dated evidence and tasks.md. No physical observation/background gate has passed.

The native reducer starts without a known resumed-app baseline. It imports no
pre-consent events; a fresh lock/screen boundary clears prior resumed identities.
Until known lifecycle coverage is established, focus and repeat openings remain
unavailable. Multi-resume is excluded rather than resolved by latest-resume guessing.
Overlapping query events are deduplicated; late unseen events invalidate coverage.

Ten minutes continuously away ends the return session, consistent with the proposed
service stop rule. Continuous selected use remains eligible for T=20/R=0; interpreting
ten minutes without a reopening as an expiry during continuous selected use would
contradict that explicit score example. This implementation interpretation is recorded
for review during T017 and does not change the unvalidated heuristic into a clinical claim.

Explicit checks use a 60-second/2,000-event bound. The initial two-second settling
tail was increased to five seconds after a physical probe observed two unique events
up to 392 ms beyond the previous cursor. This is a trial margin, not a delivery
promise; unforeseen late events still invalidate coverage. The native read branch
now runs on the Huawei Android 10 phone; no target TikTok package is installed on the
emulator. No service, automatic score publication or measured-summary writes exist.

## 8. Physical reconciliation fixes (2026-10-09)

Two failing recovery regressions identified discarded fresh boundaries and repeated
rejection of the same late event, plus a pre-consent activity cleanup pause after
unlock. Recovery now processes new boundaries after invalidating old coverage,
deduplicates rejected events and accepts cleanup only in the proven empty post-lock
baseline before its first new resume. Ordinary unmatched pauses remain invalid.

Two additional failing handoff regressions showed that matched pause/resume timestamp
gaps erased verified duration and OTHER-return eligibility. The complete empty
lifecycle set now counts no foreground time, preserves previously verified inputs
and suspends published focus/counts until a candidate resumes. Actual OTHER time
alone qualifies the ten-second return. Missing or ambiguous observations still
exclude measurement and cannot qualify a return. See the observation contract.

All 21 JVM tests and eighteen distinct ordinary native cases passed after these
changes. A four-minute real probe established 163,391 ms of selected time, but this
partial result does not establish complete-window return reliability. Current
physical gate findings and failures are in [observation evidence](evidence/observation.md).

## 9. Native replay comparison (2026-10-12)

A bounded chronological replay now reuses the adapter's actual classifier and exact
post-start/cursor bounds. In a passing short trial it matched 163,279 selected
milliseconds and one candidate return. A warm trial reproduced an unmatched OTHER
pause and failed identically in both replay and incremental reads, with no late
events. This rules out incremental delivery as the explanation for that reproduced
trace, but does not distinguish missing resume, extra pause or instance ambiguity.
Keep conservative invalidation; do not infer completeness from current visible app.
See [physical evidence](evidence/observation.md) for actual limits and next checks.
