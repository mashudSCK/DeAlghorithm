# Quickstart and Validation: Android MVP

## Current Status

The Android app includes manual intentions, voluntary reminders, replacement activities,
seven-day local summaries and confirmed deletion. See [manual evidence](evidence/manual-intent.md)
and [activities/summary evidence](evidence/activities-summary.md) for checks and limits.
T014-T016 now add pure observation/score rules and an explicit diagnostic trial; see
[trial evidence](evidence/observation-trial.md). The task tracker is 22/33.
Automatic monitoring, measured budgets and scoring remain unavailable pending physical
observation and background gates. These manual features do not complete the full MVP.

Java 17, Android Studio, adb, and SDK platforms 33-36 were found in this workspace.
An API 36.1 emulator is available for instrumentation. A Huawei STK-L21 / Android 10
phone was connected for T017. Reconciliation fixes established selected duration in
a short probe, but complete-window return reliability remains unverified; see
[physical gate findings](evidence/observation.md). This package/OS candidate is not supported.
Physical phones are required for the
cross-app observation/reliability gate; emulators alone do not establish support.

## Build and Run

The app uses one Kotlin/Compose module, minimum API29 and compile/target API36.
The wrapper/catalog pin compatible versions. Keep SDK-path local.properties, build
outputs, and signing material out of Git.

From the repository root in PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb devices -l
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Set JAVA_HOME to JDK17 and configure the local SDK path if the build cannot discover
these installations. Do not install global Gradle or commit a machine-specific path.
Build/test/lint must pass; install/instrumentation require an authorized device.

## Focused Validation Scenarios

1. **Manual baseline**: Deny Usage Access. Start a five-minute manual intention,
   pause/resume, finish/extend, and select an activity. Summaries show only manual
   minutes. Automatic score remains unavailable; no browsing data is collected.
2. **Scoring rules**: JVM inputs T=10/R=3 produce 50; T=20/R=6 produce 100; T=20/R=0
   produce 60. Check rounding, 30/31, 60/61, 80/81 boundaries, break reset, complete
   windows, duplicate events, and no zero substitution for missing observations.
3. **Budget/reminder precedence**: Active intentions suppress score reminders;
   budget expiry wins a simultaneous score event. Continue and Break suppress
   reminders by the specified clocks. No requests are less than 120 seconds apart.
4. **Observation trial**: On the selected installed TikTok package, verify regular
   transitions, same-package activity switches, ten-second OTHER returns, lock/unlock,
   own-app/reminder return, split-screen, and clock changes. Ambiguity suspends
   measurement. Compare observed intervals to a timed script; never persist raw traces.
5. **Background/device gate**: Test Android10 and a modern Android14+ device with
   foreground-service typing. Evaluate stop action, battery saver, delayed queries,
   system/user process stop, restart, notification denial, and disabled channels.
   Report duration/delivery gaps and measured battery impact before support claims.
6. **Local privacy/deletion**: Verify no INTERNET permission or behavioral SDK traffic,
   no durable content/raw transitions/unrelated packages, backup exclusions, seven-day
   pruning, and complete deletion. Deliver stale alarm/action callbacks after deletion;
   no records or reminders may return.
7. **Recovery/accessibility**: Kill/restart/reboot with an intention; recovery is paused
   and unknown time is not measured. Complete core journeys with TalkBack, increased
   text size and keyboard navigation; confirm labels, focus order and 48dp targets.
8. **Usability**: Run the five-person review from SC-005. At least four must distinguish
   measured use from timers and explain a sample reminder without assistance.

Use data-model.md and contracts/mobile-behavior.md as expected behavior references.
Store only aggregate pass/fail/timing findings in evidence, never raw other-app history.

## Diagnostic Observation Trial

This developer trial is available in Settings. It does not start automatic monitoring,
consume an automatic budget, save measured summaries or display a production score.

1. Use an authorized physical phone with one of the displayed TikTok packages installed.
   Record the package and OS version for T017; do not call either package supported yet.
2. In Settings, select the installed package and choose Set up observation trial.
   Read the disclosure. Cancel leaves observation disabled. Agree records consent and
   initializes a transient trial if access and the installed target are already available;
   it does not query events.
3. Choose Open Usage Access settings and grant access explicitly in Android settings.
   Return to DeAlghorithm. Missing access/app states leave manual features usable.
4. If the trial is stopped, choose Start observation trial when unlocked. Access restored after interruption does
   not restart collection; every new trial begins at the current time without history.
5. Lock and unlock once, then exercise a short selected-app transition script. Return
   and choose Check recent observation within one minute. Do not invoke a check while
   locked. A completed lock/unlock sequence supplies the fresh baseline; a missing or
   ambiguous sequence keeps observation unavailable. Returning through DeAlghorithm
   is excluded from qualifying returns.
6. Check same-app activity changes, at least ten-second OTHER excursions, own-app/reminder
   paths and split-screen separately. Compare candidate intervals with a timed script.
   The trial has a five-second settling tail, a 60-second query-gap limit and a 2,000-event
   cap. Delays/gaps stop or invalidate the window rather than fabricate coverage.
7. Repeat openings stay unavailable until ten minutes of continuous known coverage.
   Disable monitoring clears transient trial data and preserves existing manual records.
   Delete all data also invalidates prior setup/start/check callbacks.

Save only aggregate outcomes in evidence/observation.md. Native reads run on the
Huawei Android 10 phone. Reconciliation fixes established selected duration in a
short probe, but subsequent return transitions remain unreliable. Neither target
package is installed on the emulator. T017 and later background gates remain
unchecked. The opt-in bench command and findings are in
[physical observation evidence](evidence/observation.md). The optional physical
reminder bench creates and finishes its own real five-minute manual intention;
manual time and reminder-request records remain. It does not seed measured usage,
delete existing records or replace an active user intention.

## Activities and Summaries Walkthrough

1. Open Activities. With an empty list, both Take a break and Add an activity are available.
2. Add `Read two pages`, rename it, and add another activity. Move up/down changes the saved order.
   Names are trimmed and must contain 1–80 characters; ten saved activities is the limit.
3. Choose an activity. Stay on Activities; one selection is recorded per explicit choice.
   Selection does not finish a timer, navigate to another app or claim completion.
4. Open Summary. The view shows today and the previous six local dates. Manual timer
   duration, recorded measured duration, reminder requests, breaks and selections are separate.
   Without observations, measured use is unavailable; it is not displayed as a measured zero.
5. Open Settings and Disable monitoring. Existing activities, summaries and manual timers stay.
   This also clears a running diagnostic trial. No background service is implemented;
   this does not establish the physical service-stop gate.
6. Choose Delete all data. Cancel preserves records; confirm clears timer, activities,
   summaries and preferences, invalidates prior callbacks and resets opt-in.

Focused emulator instrumentation (after assembling both APKs and installing them) uses:

```powershell
adb -s emulator-5554 install --no-streaming -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install --no-streaming -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e class com.mashudsck.dealghorithm.StateStoreTest,com.mashudsck.dealghorithm.ActivityFlowTest com.mashudsck.dealghorithm.test/androidx.test.runner.AndroidJUnitRunner
```

Use the actual authorized device serial. Change notification permission outside
instrumentation; Android may terminate the instrumented app when its permission changes.
All activity journeys can run with notifications denied. Native reminder-action tests
require granted notifications; run the denied-permission timer case in a separate run.

## Completion Limits

A successful build or synthetic test is not proof of cross-app feasibility or store
eligibility. Record failing automatic requirements as incomplete; do not replace
physical evidence with screenshots of simulated scores. Recheck current target-SDK
and foreground-service distribution requirements before a public app release.
