# Physical TikTok observation gate (T017)

Date: 2026-10-09 (Asia/Manila).
Status: FAILED observation feasibility on this candidate. T017 remains unchecked; T018-T023 remain gated.

## Current finding

The authorized Huawei STK-L21 / Android 10/API 29 / TikTok
com.ss.android.ugc.trill 47.1.4 yields selected duration and qualifying returns in
bounded ordinary transition trials. On 2026-10-12, a four-minute native audit passed:
163,279 selected milliseconds and one qualifying return, exactly equal between
incremental reads and bounded chronological replay.

A subsequent warm-state/reminder trial reproduced failure during the OS-navigation
stage. It stopped early after 281.794 seconds: an unmatched OTHER pause invalidated
coverage. The paused identity had resumed earlier in the fresh window. Replay and
incremental reads failed identically, with zero late events. This narrows the blocker
to native lifecycle coverage/matching, rather than incremental delivery for that
trace. It does not distinguish a missing new resume, an extra pause or multiple
activity instances. The selected-app alias hypothesis is not established as the cause.

T017 remains unchecked; T018-T023 remain gated. The owner reported split-screen
unavailable through normal phone controls on 2026-10-12. Earlier forced split-screen
metadata is retained as history and does not prove correct ambiguity handling.
Dated results below distinguish passed short cases from the failed complete-window
gate. T014-T016 checks remain in [trial evidence](observation-trial.md).

## Device record

Fill these fields only from the authorized physical device. Do not save its serial,
account identifiers, content, unrelated package list or raw UsageEvents history.

- Device model: Huawei STK-L21
- Android release / API: Android 10 / API 29
- Selected TikTok package / version: com.ss.android.ugc.trill / 47.1.4 (versionCode 470104)
- DeAlghorithm APK version: 0.1.0 (versionCode 1)
- Usage Access consent/grant: owner confirmed consent; native grant verified as allowed. Grant restoration left the trial stopped until explicit Start.
- Notification permission/channel: enabled native intentions channel; real manual notice observed and its own tap PendingIntent sent during the long probe
- Aggregate duration error / query delay: reliable total-duration error not established; short-probe lateness and long-probe reminder observation timing recorded below

## Procedure

Install the existing debug APK without uninstalling or clearing user data. Use its
Settings disclosure and the phone's native Usage Access screen. Consent stays with
the user; do not substitute adb app-op commands for the physical consent journey.
Initialize a fresh trial only after consent and grant. Lock/unlock once, then check
from DeAlghorithm while unlocked within one minute. The adapter never imports
pre-start history; the current five-second settling tail means the latest transition may be
absent until the next explicit check.

Record each scenario as pass, fail or not run with aggregate intervals/counts and
unavailability reasons. Start a fresh trial between independent cases. Do not make
an unavailable return count into zero. Repeat-opening checks require ten minutes of
continuous known coverage, with explicit checks less than one minute apart.
If ordinary phone transitions cannot establish that window, record the failure and
fix the reducer/adapter before declaring support. Partial snapshots alone do not
prove reopening reliability.

| Scenario | Required observation | Actual result |
|---|---|---|
| Consent canceled / permission denied | No queries or imported history; manual timer remains usable | Not run |
| Consent, native grant, explicit fresh trial | Only post-start events; honest initializing/unknown state until baseline | Native consent/grant and fresh start exercised; short probe recovered selected duration, longer probe lost coverage |
| Selected-app duration | Candidate selected intervals agree with a timed script; report error rather than invent a tolerance | Partial: 163,391 ms in short probe; long probe froze at 41,603 ms despite later selected-app holds |
| Same-package activity changes | No duplicate duration or extra reopening | Warm same-package launches exercised with candidate count 0 during early valid samples; distinct activity-switch reliability not established |
| First selected opening | No qualifying return | Candidate 0 during first verified opening; published R unavailable until complete coverage |
| OTHER excursion under ten seconds | No qualifying return | Five-second Home hold exercised; uncertainty followed, so a reliable negative case was not proved |
| OTHER excursion at least ten seconds | Exactly one qualifying return after an earlier selected opening, when complete coverage exists | Failed: twelve-second Home holds exercised; final expected candidate 1 was 0 and the window remained incomplete |
| Own-app / reminder return | No qualifying return from DeAlghorithm or its reminder path | Actual own-app excursion and real manual notice/tap exercised; exclusion accuracy not established because coverage was already invalid |
| Lock / screen off | No locked duration or lock-triggered return; missing lifecycle coverage stays unavailable | Fresh lock/unlock boundary exercised; reads skipped while locked and initial selected duration recovered; full lock-return accuracy still unverified |
| Split-screen / multiple resumed packages | Ambiguity excludes selected duration; no latest-resume focus guess | Native split-screen resumed both selected and own app; coverage was already invalid, so causally correct ambiguity handling was not proved |
| Usage Access revoked / restored | Trial stops/clears; restoration alone imports nothing and requires fresh start | Not run |
| Gap over one minute / unexpected late events | Stop or invalidate coverage; no fabricated time or zero returns | Earlier delayed manual checks exceeded the gap limit; short probe counted two rejected late events, long probe counted none |
| Disable / process loss | Trial clears; existing manual records stay; no self-restart | Not run |
| Permitted reminder route | User-enabled channel can deliver a voluntary manual reminder; return does not count as a selected reopening | Not run |

The trial creates no automatic scores, budgets or measured summaries. This gate
establishes only the actual package/OS observation candidate. T018 must separately
justify service eligibility, and T020 must verify physical background/battery behavior
before T021 integrates automatic budgets/scoring. No store approval follows from
sideloading or this observation gate.
## Initial native probe finding

The opt-in 90-second PhysicalObservationTest ran on the physical phone without
deleting records. It failed its selected-interval assertion: 16 native queries,
16 unknown samples, zero selected samples and zero candidate selected milliseconds.
The post-start event-kind histogram contained activity resume/pause and all four
screen/keyguard event kinds; no lifecycle class was missing. The histogram was not
capped. This proves native reads occurred but does not establish usable observation.
No raw transitions, unrelated identities or timestamps were saved. The uncertain
baseline was then diagnosed using transient issue categories, described below.

## Diagnostic probe and implementation outcome

An explicitly invoked PhysicalObservationTest reuses the production runtime and
checks every five seconds for a bounded 30-660 seconds. It requires saved consent,
current native access and the installed selected target; it does not grant access,
delete records, replace activities or seed measured summaries. Locked checks are
skipped until the user unlocks normally. Only aggregate counters and issue categories
are reported. A bounded event-kind histogram is optional on current consent,
generation, selected package and grant; no identities or raw timestamps are printed.
Cleanup clears the observer even when the probe fails. Default test runs skip this
physical-only case unless physicalObservation=true is explicitly supplied.

UsageReducer snapshots now expose a transient ObservationIssue: missing baseline,
locked state, no resumed activity, multiple resumed apps, missing/late events,
invalid identity or unmatched pause. Existing reducer tests verify these meanings.
These diagnostics do not relax exclusions or change score/window eligibility.

The second probe requested 120 seconds and completed in 127.082 seconds. It failed:
22 queries and 22 unknown samples, zero selected samples/milliseconds, no complete
window and unavailable returns. Aggregate issue samples: BASELINE_MISSING 3,
LOCKED 1, NO_RESUMED_ACTIVITY 7, UNMATCHED_PAUSE 1, LATE_EVENT 10. The native histogram
again contained resume/pause and all screen/keyguard event kinds, with zero missing
lifecycle classes and no histogram cap. These are sample counts, not counts of unique
late events or a measured delivery-latency distribution. A reason can persist between
checks. No maximum lateness, device battery behavior or timing tolerance was measured.

The first probe completed in 95.309 seconds. Timed bench commands launched the actual
TikTok activity, held it for 20 seconds, returned to Home for 10-12 seconds and launched
TikTok again. Launches succeeded. Content, messages, searches and feed screens were
not inspected or acted on. Earlier manual check attempts exceeded the one-minute gap
limit and are not substitutes for the bounded probe findings.

The observed mechanism is conservative baseline invalidation: an unmatched pause or
unseen event older than the reducer cursor clears known coverage. Without a new
successful boundary and lifecycle sequence, subsequent samples remain uncertain.
The current lock-baseline/event-reconciliation design needs further device-backed
work before support is defensible. The results do not justify simply ignoring pauses,
accepting late data as complete, weakening ambiguity rules or publishing zero returns.

Regression verification after adding diagnostics: full assembly, all 17 JVM tests and
lint passed; BUILD SUCCESSFUL in 1m 55s, 80 tasks. Lint has zero errors and the existing
missing-launcher-icon warning. The isolated API 36 emulator suite reported OK (19 tests),
37.231 seconds: 17 executed successfully, with intentional skips for denied notifications
and the physical-only probe. The existing eighteen distinct ordinary cases were verified
across the earlier permission matrix; this run did not re-exercise notification denial.
No dependency, storage-schema, manifest or production UI changes were made in this step.

Commands for a future authorized physical bench (use the actual serial):

```powershell
adb -s <serial> install --no-streaming -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install --no-streaming -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w -r -e class com.mashudsck.dealghorithm.PhysicalObservationTest -e physicalObservation true -e windowSeconds 660 com.mashudsck.dealghorithm.test/androidx.test.runner.AndroidJUnitRunner
```

Read/agree to the app disclosure and grant access through native Settings first.
After READY, lock/unlock once and exercise the real transitions. A successful bench
assertion alone cannot complete T017: every required same-app, return, lock/reminder,
split-screen and permission-loss scenario must have actual aggregate findings.
The 660-second full-window bench was not run because the short probes established
no selected intervals. Native permission revocation, split-screen, same-app switches,
reminder return and complete ten-minute return accuracy remain unverified on this phone.

Local ignored logs: build/physical-probe-initial.log, physical-probe-diagnostic.log,
physical-diagnostic-gate.log and physical-probe-regression-native.log. Phone app records
were not cleared. The observer was stopped and the phone returned to DeAlghorithm.
No service/background/release eligibility or automatic capability follows from this gate.

Final harness-only cleanup build/lint also passed: BUILD SUCCESSFUL in 1m 2s,
57 tasks. Final default-probe plus denied-notification run on the isolated emulator
reported OK (2 tests), 4.007 seconds: the denied case passed and the opt-in physical
probe intentionally skipped. Together the ordinary eighteen native cases executed
successfully after the diagnostics change. The final cleanup guard compiled and the
default skip was exercised; the 120-second physical failure above preceded that last
cleanup-only edit. Phone UI was checked afterward: trial stopped, native access still
granted. The grant was left as the user selected it, with no active observer/query loop.

## Recovery fixes and full physical attempt (2026-10-09)

Four focused regressions failed before their fixes: two recovery cases and two
matched handoff cases. A pre-consent cleanup pause is now accepted only before the
first fresh resume in the proven empty post-lock baseline. Late rejection invalidates
old coverage, deduplicates the rejected event and still processes later new recovery
boundaries. Matched empty lifecycle handoffs preserve verified inputs without counting
selected or OTHER foreground time during the empty interval. Missing identity, true
uncertainty, unmatched pauses and multiple resumed packages remain excluded.
The [observation contract](../contracts/mobile-behavior.md) records these distinctions.

The first patched physical run requested 240 seconds and completed in 245.619 seconds:
44 native queries, 32 selected samples, nine unknown samples and 163,391 selected
milliseconds. The window was incomplete and published returns remained unavailable.
Two unique rejected late events were observed, with maximum lateness 392 ms beyond
the previous reducer cursor. This is cursor lateness, not a general delivery-latency
measurement. Sample reasons included NO_RESUMED_ACTIVITY 4 and LATE_EVENT 5.
That run preceded the matched-handoff fix and used the old two-second tail.

The trial tail was then increased to five seconds as a device-informed margin, not
a guarantee. The final production reducer/adapter assembled successfully, all 21 JVM
tests passed and lint reported zero errors plus the existing missing-icon warning
(BUILD SUCCESSFUL in 1m 58s, 80 tasks). The isolated API 36 emulator native suite was
OK (19 tests), 50.926 seconds: 17 ordinary cases passed, denied notifications and the
opt-in physical probe skipped. The separate denied/default-probe run was OK (2 tests),
6.332 seconds: one denied case passed and the probe skipped. All eighteen distinct
ordinary native cases therefore executed successfully after the production fixes.
An earlier mistyped denied-test selector executed only the default probe skip and
is excluded from the permission-matrix claim.

The full physical run requested 900 seconds and completed in 905.836 seconds:

- 171 native queries; eight selected samples and 163 unknown samples.
- 41,603 selected milliseconds; no complete window at any sample or at the end.
- Final/max candidate returns 0; published R unavailable. The final timed script
  required candidate 1; the actual assertion failed with expected 1, actual 0.
- Zero unique late events; maximum cursor lateness 0 ms with the five-second tail.
- Sample reasons: BASELINE_MISSING 3, LOCKED 1, NO_RESUMED_ACTIVITY 11,
  UNMATCHED_PAUSE 148. These are sampled reasons, not 148 distinct pause events.
- Post-start event-kind totals: resume 20, pause 21, screen-off/on 1 each and
  keyguard-shown/hidden 1 each. No lifecycle classes were missing; the histogram
  was not capped. No raw identities, transitions or timestamps were saved.

Timed actions included warm selected-package launches, five-second Home holds,
twelve-second Home holds, an own-app excursion, split-screen and a final short/
qualifying return pair. Native split-screen metadata confirmed both selected and
own activities resumed. Because invalid coverage preceded split-screen and reminder
paths, their lack of counted time/returns is not an independent passing exclusion
case. The owner restored normal fullscreen; no feed content was inspected or acted on.
Native window modes followed the [Android 10 source constants](https://android.googlesource.com/platform/frameworks/base/%2B/android10-release/core/java/android/app/WindowConfiguration.java).

The optional reminder bench started a real five-minute manual intention only after
checking no user intention was active. Its own intentions notification was observed
4,565 ms after budget expiry and the bench sent that notification's content tap
PendingIntent. This is sampled observation timing, not a notification delivery bound
or proof of human reading. Cleanup finished only the bench's own session and stopped
observation. Real manual-time/request records remain; existing activities and measured
summary totals were preserved. No measured usage or automatic score was seeded.

After the full failure, the bench gained per-check issue/focus/lateness output and a
strict non-null native-channel check. This harness-only edit compiled successfully
in 51 seconds. Its final isolated-emulator default run was OK (1 test),
4.489 seconds, with the physical test intentionally skipped. It has not yet run
as an opt-in physical probe. The long-run numbers
above refer to the preceding harness and the same final production reducer/adapter.

Next investigation: identify the classified source and lifecycle ordering of the
first unmatched pause with the new live diagnostics and a bounded native audit.
Compare a single bounded replay with incremental reads before changing matching or
coverage rules. Public class-based lifecycle identity may be a limitation; the
[framework instance-ID accessor](https://android.googlesource.com/platform/frameworks/base/%2B/refs/heads/main/core/java/android/app/usage/UsageEvents.java)
is a hidden SystemApi, not an available public-app fix. No hidden-API workaround,
latest-resume guessing or blanket unmatched-pause suppression was introduced.
Physical native revocation/restoration, reliable same-app/lock/reminder/split-screen
exclusions and process-loss cases still need completion after return coverage works.

Local ignored logs: build/reconciliation-baseline.log, reconciliation-gate.log,
reconciliation-physical.log, handoff-baseline.log, handoff-gate.log,
handoff-native.log, handoff-native-denied.log, full-physical-window.log and
probe-live-reasons-build.log and probe-live-reasons-default.log. Final own-app UI showed the trial stopped and native
Usage Access still granted. T017 remains unchecked; tracker remains 22/33. No
service, automatic integration, release, commit or push was performed.

The owner requested a 5:00 PM Manila stop on 2026-10-09. No further phone probe
was started near that deadline. Resume with the unmatched-pause audit above.

## Classified native replay audit (2026-10-12)

The phone and selected package versions were rechecked and unchanged. Native Usage
Access remained granted. The diagnostic harness now queries from the adapter's exact
fresh trial start to its last observed cursor, reuses the same classifier and keeps
only ephemeral salted keys. A bounded chronological replay reports aggregate results
and classified unmatched-pause onsets, never raw package/class keys or timestamps.
The optional stopOnUnmatched argument stops promptly on the first reproduced fault;
it still fails the required return/full-window assertions. This avoids waiting out a
known failed run. No lifecycle matching, production UI, storage, schema, permission,
service or automatic-score behavior was changed.

Current build initially failed before compilation with a JVM local connection error:
Unable to establish loopback connection / UnixDomainSockets Invalid argument: connect.
A process-local short workspace JVM temporary/socket path allowed the build to run:
JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=<workspace>/.setup-cache/java-tmp
-Djava.io.tmpdir=<workspace>/.setup-cache/java-tmp. No firewall, system setting, global
Java installation or dependency was changed. This is a verified workaround; the
underlying host cause was not established. The audit gate then passed in 1m 41s,
80 tasks: both APKs, all 21 JVM tests and lint with zero errors and the existing
missing-icon warning. The subsequent harness-only early-stop build passed in 41s.

The 240-second physical request completed in 245.643 seconds, OK (1 test):
45 queries, 32 selected samples, nine unknown samples, 163,279 selected milliseconds,
final/max candidate returns 1 and no late events or unmatched onsets. The scripted
five-second Home interval added no return; the twelve-second Home interval added one.
Chronological replay exactly matched selected milliseconds and candidate returns.
The short window remained incomplete and published R remained unavailable. This run
started with a cold selected-app launch, observed through native launch metadata.

The warm 900-second request used a real five-minute manual intention and early-stop
mode. Before failure it exercised warm same-package launches, short/qualifying Home
holds and an own-app excursion; candidate count reached 1 and stayed 1 through the
own-app path. The owner then attempted normal split-screen setup and reported it
unavailable. The fault arose during that OS-navigation stage; the exact user gesture
and its causal relation were not inspected or proved.

The warm run failed after 281.794 seconds, before reminder expiry:

- 53 queries; 28 selected samples, 18 unknown samples; 147,315 selected milliseconds.
- Final candidate 0, max candidate 1; no complete window and published R unavailable.
- One classified unmatched onset: pause app OTHER, an earlier matching resume seen,
  one previously resumed class for that package since the latest screen boundary.
- Chronological replay also returned 147,315 selected milliseconds, candidate 0,
  incomplete coverage and UNMATCHED_PAUSE. No delivery/deduplication discrepancy
  was observed between the replay and incremental snapshots.
- Zero late events and maximum cursor lateness 0; all lifecycle classes present,
  histogram uncapped. Aggregate kinds: resume 9, pause 10 and one each of screen
  off/on and keyguard shown/hidden.
- Reason samples: BASELINE_MISSING 10, LOCKED 1, NO_RESUMED_ACTIVITY 6,
  UNMATCHED_PAUSE 1. Initialization/lock waiting is not failed selected measurement.

Native metadata confirmed the selected launcher activity is an alias for another
selected activity. Android exposes that mapping through
[ActivityInfo.targetActivity](https://developer.android.com/reference/android/content/pm/ActivityInfo#targetActivity).
However, this reproduced unmatched pause was OTHER, not SELECTED, and a prior matching
resume existed. No selected-alias normalization or hidden instance-ID workaround is
justified by these findings. The next discriminating check is a minimal native
Home/Recents journey with per-identity resume/pause aggregate balance; preserve
missing-data invalidation until a credible safe reconciliation mechanism is proved.

The early-stop cleanup finished only its own manual test intention and stopped
observation. Existing activities and measured totals passed preservation assertions
before the expected-return assertion failed; real partial manual-time records remain.
No reminder was seen or tapped in this early-ended warm run. A real reminder was
observed in the historical 2026-10-09 run, which did not prove exclusion accuracy.
The phone was returned to DeAlghorithm; UI showed trial stopped and access granted.

Logs are ignored local files: build/native-audit-gate-2026-10-12.log,
native-audit-stop-build-2026-10-12.log, native-audit-physical-2026-10-12.log and
native-audit-warm-2026-10-12.log. No emulator or destructive full native suite ran on
the user's phone. No additional task, service or automatic capability is complete;
tracker remains 22/33. Permission-loss, complete-window, reliable lock/reminder/
split-screen exclusions and process-loss checks remain pending. No commit, push or
release occurred.
