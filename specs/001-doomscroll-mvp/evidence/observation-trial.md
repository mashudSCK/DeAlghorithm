# Observation trial and pure rule verification

Date: 2026-10-09 (Asia/Manila). T014-T016 are complete; T017 remains unchecked.
This is a consent-based diagnostic trial, not verified automatic monitoring.

## Delivered behavior

Settings offers two explicit TikTok package candidates and a disclosure before opt-in.
Cancel preserves opt-out. Agree persists only the selected package and opt-in preference;
it also initializes a fresh transient trial if access and the selected app are already
available. No query occurs until an explicit check. Manual mode remains independent. Opening Android Usage Access settings requires a
separate action. A grant alone never starts a trial or imports historical usage.
Starting a trial requires consent, a current grant, the installed selected package,
an interactive screen and hidden keyguard. Permission restoration and process recovery
require a fresh explicit start. No background service or polling loop was added.

The native adapter reads only recent post-start lifecycle/screen/keyguard events on
explicit checks. The query window is bounded to 60 seconds and 2,000 events; a two-second
settling tail and 20-second overlap allow conservative duplicate handling. These are
trial limits, not an Android event-delivery guarantee. Late unseen events, missing data,
clock changes, query errors, permission loss and ambiguous focus invalidate coverage
or stop the trial. Checks while the device is locked stop the trial; a completed
lock/unlock sequence can establish a fresh baseline through its returned events.

Package/activity names are classified in memory as SELECTED, DEALGORITHM, OTHER or
UNKNOWN. The resumed-activity set uses session-salted digests, never unrelated raw
identities. Events, snapshots, digests and salt are transient and are cleared on Stop,
disable, deletion, storage failure or process loss. The diagnostic trial creates no
measured summary, automatic budget consumption, production score or score reminder.
Disable preserves existing manual timers, activities and summaries. Deletion rejects
old-generation setup/start/check callbacks before any new data can be published.

The reducer excludes same-package activity changes, first openings, own-app/reminder
paths, lock intervals and ambiguous multi-resume from qualifying returns. A qualifying
return requires at least ten seconds in OTHER following an earlier selected opening.
Missing or incomplete ten-minute coverage exposes repeat openings as unavailable,
not zero. Verified selected duration resets after two minutes continuously away.

Interpretation: ten minutes continuously away ends the return session. Continuous
selected use does not expire merely because no reopening occurs; otherwise the stated
T=20/R=0 score example would be impossible. This follows the research's away-from-app
service stop rule and is an explicit implementation interpretation pending device review.

Pure ScoreRules uses exact integer upward half rounding, duration/return caps and the
specified 50/100/60 examples. It checks reminder bands, first Awareness, at most one
stage advance, Reflection before Replacement, suppression/cooldown/unresolved state
and budget-expiry priority. It is not wired to production observation or reminders.

## Verification

Before implementation, all eleven new JVM cases failed with NotImplementedError.
The initial native consent case failed because the setup control was absent.
Both baselines therefore detected missing behavior before wiring.

Final command (the ignored init script uses this machine's staged official Maven cache):

```powershell
.\gradlew.bat --offline -I .setup-cache/local-official.init.gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain
```

Result: exit 0, BUILD SUCCESSFUL in 2m 19s, 80 tasks. All 17 JVM tests passed:
three state, three manual session, seven reducer and four score cases. Lint reports
zero errors and one existing missing-launcher-icon warning. No dependency/schema change.
The manifest adds PACKAGE_USAGE_STATS and only two named package visibility queries;
no Internet, accessibility, overlay, exact-alarm or service permission was added.

Native checks ran on the disposable workspace AVD DeAlghorithm_Test_API36, Android 16,
SDK 36, serial emulator-5554. Both current APKs installed successfully with
`adb install --no-streaming -r -t`. Permission changes were outside instrumentation.

- Usage Access denied, notifications granted: complete suite OK (18 tests), 141.9 seconds.
  Seventeen executed successfully; the denied-notification case intentionally skipped.
- Usage Access allowed, notifications denied: ObservationFlowTest plus the denied
  notification timer case OK (4 tests), 37.724 seconds. Three executed successfully;
  the denied-Usage-Access case intentionally skipped.

All eighteen distinct native cases executed successfully across the two runs. The three
new cases cover explicit consent/cancel, the native Usage Access route, denied access,
missing selected apps, no pre-consent queries, no permission-only history import,
generation invalidation, disable preservation and transient observation storage.
The existing manual, notification-action, activity, summary and storage cases also pass.
The Usage Access activity route was intercepted by the native intent test; this does
not prove the physical device's settings screen or consent behavior.

Settings and the consent dialog were visually inspected. Full TalkBack/enlarged-text,
keyboard navigation and five-person usability acceptance remain T032.

## Environment recovery and limits

The previous read-only task emulator failed current-app installation with
INSTALL_FAILED_INSUFFICIENT_STORAGE. The following stale-APK/crashed instrumentation
run is not counted. A fresh AVD was created entirely under ignored .setup-cache with
6.7 GB free, then both current APKs installed. The original user AVD and unrelated
application data were preserved. This is a local verification workaround.

Neither candidate TikTok package is installed on the test emulator. Consequently,
actual native queryEvents reads and real selected-app transitions were not exercised.
Synthetic reducer tests and permission checks cannot pass T017. A physical phone with
an installed selected package must establish package/OS support, same-app changes,
returns, lock/reminder paths and split-screen reliability in evidence/observation.md.
T018-T023 remain gated; no service eligibility, battery, background delivery, automatic
score/budget or complete MVP claim follows from these results. T030-T033 remain pending.

Ignored logs: build/observation-baseline.log, observation-ui-baseline.log,
observation-final-gate.log, observation-isolated-native.log and
observation-granted-matrix.log. Inspected screenshots: build/observation-settings.png
and build/observation-consent.png. No raw other-app history is recorded in evidence.

API references checked during implementation: [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event),
[UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager),
[AppOpsManager](https://developer.android.com/reference/android/app/AppOpsManager) and
[Usage Access settings](https://developer.android.com/reference/android/provider/Settings#ACTION_USAGE_ACCESS_SETTINGS).
