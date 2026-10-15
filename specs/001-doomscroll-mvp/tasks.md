# Tasks: Android Intentional Use MVP

**Input**: Design documents in `specs/001-doomscroll-mvp/`.

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [mobile contract](contracts/mobile-behavior.md),
and [quickstart.md](quickstart.md).

**Tests**: Focused checks implement SC-001 through SC-008 and FR-018. Write the listed
behavior tests before their implementation; verify that they detect missing behavior.
No framework beyond the Android scaffold's normal test dependencies is required.

**Organization**: Tasks are grouped by user story. All checkboxes are initially
unchecked; generating this file does not complete implementation or device validation.

## Format: `[ID] [P?] [Story] Description`

`[P]` identifies tasks that can run concurrently after their listed prerequisites,
in different files. `[US1]`, `[US2]`, and `[US3]` map to specification stories.

## Path Conventions

One repository-root Android `app/` module. Kotlin paths below use the application
package `com.mashudsck.dealghorithm`. Checkboxes below track implemented and verified work. Evidence files
contain aggregate outcomes and limitations, never raw usage or unrelated app history.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create a minimal, reproducible Android build.

- [X] T001 Scaffold a single Kotlin/Compose Empty Activity app with package com.mashudsck.dealghorithm, JDK17, minSdk29 and compile/targetSdk36; commit compatible pinned tooling in app/build.gradle.kts, build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml and gradle/wrapper/gradle-wrapper.properties plus gradlew/gradlew.bat.
- [X] T002 Add local.properties, .gradle/, build outputs and signing-material exclusions to .gitignore; configure the app/src/main/AndroidManifest.xml baseline without INTERNET, accessibility, overlay, exact-alarm or broad app-inventory permissions.
- [X] T003 Run assembleDebug, testDebugUnitTest and lintDebug through gradlew.bat; record actual locked versions, command results and any SDK setup in specs/001-doomscroll-mvp/evidence/build.md before proceeding.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared bounded state and privacy before story implementation.

- [X] T004 Define shared durable/transient models in app/src/main/java/com/mashudsck/dealghorithm/AppState.kt using data-model.md constraints verbatim: "schemaVersion=1", "monitoringOptIn=false initially", "selected supported package or null", "selected package only for automatic", "RUNNING/PAUSED/FINISHED/EXPIRED", and "nonnegative totals"; keep free-text purpose and raw observation windows transient.
- [X] T005 [P] After T004, implement one transactional Preferences DataStore 1.2.1 under noBackupFilesDir in app/src/main/java/com/mashudsck/dealghorithm/StateStore.kt; enforce schema/corruption error handling without silent replacement, partition nonoverlapping daily totals, prune "retain today plus six prior dates", and expose manual/observed totals separately.
- [X] T006 [P] After T004, disable backup and explicitly exclude behavioral state from cloud/device transfer in app/src/main/AndroidManifest.xml, app/src/main/res/xml/backup_rules.xml and app/src/main/res/xml/data_extraction_rules.xml; verify that the chosen DataStore path is excluded.

**Checkpoint**: T001-T006 complete. Story work can begin; no automatic capability is
advertised before its device gate.

## Phase 3: User Story 1 - Set and Protect an Intention (Priority: P1)

**Goal**: Deliver a usable manual intention with voluntary reminders even when
cross-app permissions are denied. Automatic budget consumption is integrated in US2.

**Independent Test**: Deny Usage Access; start, pause/resume, extend and finish a
five-minute timer. Only manual minutes accrue; score stays unavailable. Denied
notification access leaves overdue status visible without trapping the user.

### Tests

- [X] T007 [P] [US1] Add focused budget/cooldown/recovery cases in app/src/test/java/com/mashudsck/dealghorithm/SessionRulesTest.kt for 5/15/30-minute budgets, five-minute elapsed extensions/break suppression, 120-second cooldown, paused recovery and clock/boot changes.
- [X] T008 [P] [US1] Define manual permission-denied and notification-action journeys in app/src/androidTest/java/com/mashudsck/dealghorithm/IntentFlowTest.kt; verify labels, pause/resume, finish, extension and overdue state against contracts/mobile-behavior.md.

### Implementation

- [X] T009 [US1] Implement manual elapsedRealtime-based intention and reminder decisions in app/src/main/java/com/mashudsck/dealghorithm/SessionRules.kt; preserve "remaining milliseconds", session/generation identity and transient purpose text, count no automatic usage, and enforce one unresolved reminder plus the specified suppression clocks.
- [X] T010 [US1] Implement ordinary notification channels, contextual notification permission, inexact expiry alarms and explicit immutable action PendingIntents in app/src/main/java/com/mashudsck/dealghorithm/ReminderScheduler.kt and ReminderReceiver.kt; reject stale session/generation callbacks, count requests only and cancel obsolete work.
- [X] T011 [US1] Wire setup and manual intention controls in app/src/main/java/com/mashudsck/dealghorithm/AppViewModel.kt and MainActivity.kt; support the four purpose categories, 5/15/30-minute budgets, Start/Pause/Resume/Finish and Add five minutes; show monitoring/score unavailable without granted observation access.
- [X] T012 [US1] Implement atomic timer/reminder checkpoints and conservative PAUSED restoration in app/src/main/java/com/mashudsck/dealghorithm/StateStore.kt and AppViewModel.kt; clear optional text at session end and never reconstruct unknown automatic time after interruption.
- [X] T013 [US1] Run T007/T008 and the manual quickstart scenarios; record pass/fail results and background-delivery limitations in specs/001-doomscroll-mvp/evidence/manual-intent.md without claiming US2 or activity-list functionality is complete.

**Checkpoint**: A manual intention demonstration is available. This is the first
useful increment, not fulfillment of every US1 automatic scenario or the full MVP.

## Phase 4: User Story 2 - Notice a Repetitive Usage Pattern (Priority: P2)

**Goal**: Prove observation feasibility before integrating automatic budgets and the
unvalidated duration/reopening score. TikTok is the proposed first supported app.

**Independent Test**: On an authorized supported device, exercise real selected-app
transitions and permission loss. Verify score examples, intent suppression, incomplete
windows and notification actions. Synthetic traces alone cannot pass this checkpoint.

### Tests

- [X] T014 [P] [US2] Add lifecycle-reducer cases in app/src/test/java/com/mashudsck/dealghorithm/UsageObserverTest.kt covering same-package activity changes, ten-second OTHER returns, own-app/reminder and lock exclusions, query deduplication, ambiguous multi-resume and incomplete ten-minute windows.
- [X] T015 [P] [US2] Add score/stage cases in app/src/test/java/com/mashudsck/dealghorithm/ScoreRulesTest.kt for T=10/R=3 -> 50, T=20/R=6 -> 100 and T=20/R=0 -> 60, upward half rounding, 30/31-60/61-80/81 boundaries, two-minute duration reset and budget-expiry priority.

### Implementation and Feasibility Gates

- [X] T016 [US2] Implement consent-time Usage Access checks and recent transition reduction in app/src/main/java/com/mashudsck/dealghorithm/UsageObserver.kt, with setup wiring in AppViewModel.kt and MainActivity.kt; classify identities transiently as SELECTED/DEALGORITHM/OTHER/UNKNOWN, verify keyguard/interactivity, exclude ambiguity and keep missing observations distinct from zero.
**Trial checkpoint**: T014-T016 verified on 2026-10-09; see [observation trial evidence](evidence/observation-trial.md). The diagnostic adapter and pure rules do not establish actual TikTok or background support. Current total: 22/33 tasks; T017 remains the next physical gate.

- [ ] T017 [US2] Validate T016 against actual installed TikTok transitions on a physical device; record package/OS version and aggregate results for same-app switches, returns, locking, reminder paths and split-screen in specs/001-doomscroll-mvp/evidence/observation.md; leave this task unchecked if no device is available or observations are unreliable.
**Physical gate result (2026-10-09, updated)**: Recovery and matched-handoff fixes passed 21 JVM tests and all eighteen ordinary native cases. A short Huawei STK-L21 / Android 10 / TikTok com.ss.android.ugc.trill 47.1.4 probe established 163,391 ms of selected time. The subsequent 900-second physical run failed: an unmatched pause invalidated coverage, selected time froze at 41,603 ms and final expected return 1 was 0; no late events occurred with the five-second tail. A real manual reminder and split-screen were exercised, but their exclusion accuracy remains unverified because coverage was already invalid. See [physical observation findings](evidence/observation.md). T017 stays unchecked, T018-T023 remain gated and total remains 22/33.

**Classified audit update (2026-10-12)**: The four-minute native audit passed with 163,279 selected milliseconds and candidate R=1; replay and incremental reads matched exactly. A warm full-window/reminder attempt stopped early at 281.794 seconds on an unmatched OTHER pause whose identity had resumed earlier; replay reproduced the same invalidation with no late events. The owner reported normal split-screen unavailable. T017 remains unchecked and total remains 22/33; the next check is a minimal OS-navigation lifecycle audit, not weaker missing-data rules. See [current findings](evidence/observation.md).

- [ ] T018 [US2] After T017, document the actual user-started foreground-service use case, specialUse suitability on Android14+, required subtype/declarations, stop behavior and outstanding distribution eligibility in specs/001-doomscroll-mvp/evidence/service-eligibility.md; do not label the service health/dataSync or claim store approval.
- [ ] T019 [US2] After a defensible T018 design, implement the candidate five-second observation session in app/src/main/java/com/mashudsck/dealghorithm/MonitoringService.kt and its narrowly required declarations in app/src/main/AndroidManifest.xml; start only from visible-app user action, provide ongoing Stop, suspend locked observations, stop after ten minutes away and never self-restart after process loss.
- [ ] T020 [US2] Run the physical background gate on Android10 and a modern Android14+ device; record battery-saver/query gaps, notification denial/channel disablement, Stop/system termination, recovery and measured battery behavior in specs/001-doomscroll-mvp/evidence/background-monitoring.md; failure blocks claims of automatic support.
- [ ] T021 [US2] After T017/T020 establish supported observation, integrate the exact spec score, rolling returns, verified-duration budget consumption and automatic five-minute suppression in app/src/main/java/com/mashudsck/dealghorithm/SessionRules.kt and AppViewModel.kt; never compute scores in manual/incomplete/ambiguous states.
- [ ] T022 [US2] Integrate Awareness/Intent check/Reflection/Replacement eligibility, budget priority, cooldown, permission-loss cancellation and neutral visible states in app/src/main/java/com/mashudsck/dealghorithm/ReminderScheduler.kt, ReminderReceiver.kt and MainActivity.kt; activity choice uses the US3 list when available and otherwise offers the empty-list path.
- [ ] T023 [US2] Run score/reducer tests and real automatic-budget/permission-revocation scenarios; record results in specs/001-doomscroll-mvp/evidence/automatic-intent.md and keep failing automatic requirements incomplete rather than substituting simulated scores.

**Checkpoint**: Verified supported observation, score and automatic US1 budgets are
integrated. Store eligibility remains a release prerequisite; successful sideloading
is not store approval. If a gate fails, stop the dependent work and record the unmet
requirements; proceed with independent US3 work only if it does not hide the failure.

## Phase 5: User Story 3 - Choose an Alternative and Review Patterns (Priority: P3)

**Goal**: Manage replacement activities, view honest local summaries, and delete
all data. This story can be verified using manual sessions without US2 support.

**Independent Test**: Save/select Read two pages, inspect separate manual/measured
counts, prune expired records, delete all data and deliver stale callbacks; no
activity completion, records or reminders may be fabricated or recreated.

### Tests

- [X] T024 [P] [US3] Add bounded storage/deletion tests in app/src/androidTest/java/com/mashudsck/dealghorithm/StateStoreTest.kt for "trimmed name, 1-80 characters", "at most 10 activities", "user-selected ordering", seven local dates, separate totals and stale callbacks after deletion.
- [X] T025 [P] [US3] Add activity/summary/deletion journeys in app/src/androidTest/java/com/mashudsck/dealghorithm/ActivityFlowTest.kt, including no-activities fallback, no claimed completion and permission-denied manual use.

### Implementation

- [X] T026 [US3] Implement create/rename/delete/reorder/select actions in app/src/main/java/com/mashudsck/dealghorithm/AppViewModel.kt and StateStore.kt; enforce "stable local ID", "trimmed name, 1-80 characters" and "at most 10 activities" before transactional writes, and count each selection without automatic navigation.
- [X] T027 [US3] Add plain accessible activities and seven-day summary views in app/src/main/java/com/mashudsck/dealghorithm/MainActivity.kt; show measuredMillis/manualMillis and request/break/selection counts separately, wire reminder activity choice, and retain Take a break/Add an activity for the empty list.
- [X] T028 [US3] Implement confirmed Delete all data and disable-monitoring coordination in app/src/main/java/com/mashudsck/dealghorithm/AppViewModel.kt, StateStore.kt and ReminderScheduler.kt; invalidate generation before cancellation/clearing, stop observation, reset opt-in and prevent concurrent queries or callbacks from restoring deleted records.
- [X] T029 [US3] Run T024/T025 and summary/deletion quickstart scenarios; record outcomes in specs/001-doomscroll-mvp/evidence/activities-summary.md, including seven-day pruning, clock changes and preserved summaries when only monitoring is disabled.

**US3 verification (2026-10-09)**: T024–T029 passed; see [activities and summary evidence](evidence/activities-summary.md). This independent manual-based increment does not pass the US2 physical gates.

**Checkpoint**: All three stories can be exercised without conflating manual time,
measured use, reminder requests or completed activity. Unpassed US2 gates stay visible.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verify the integrated product against the specification, not add scope.

- [ ] T030 Run the complete build/unit/instrumented/lint and recovery suite; update specs/001-doomscroll-mvp/quickstart.md and README.md with actual runnable commands, supported-device evidence and remaining limitations, and record outcomes in specs/001-doomscroll-mvp/evidence/acceptance.md.
- [ ] T031 Review the final app/src/main/AndroidManifest.xml, backup-rule files, StateStore.kt and packaged dependencies for no behavioral transmissions/content/raw histories, excluded backups, callback validation and deletion races; record static/device privacy evidence in specs/001-doomscroll-mvp/evidence/privacy.md.
- [ ] T032 Validate MainActivity.kt controls with TalkBack, enlarged text, keyboard/focus and 48dp targets; run the five-person SC-005 review and record actual aggregate results in specs/001-doomscroll-mvp/evidence/usability.md, leaving this task incomplete if participants are unavailable.
- [ ] T033 Reconcile every FR-001 through FR-018 and SC-001 through SC-008 with recorded evidence in specs/001-doomscroll-mvp/evidence/acceptance.md; recheck release SDK/service requirements, mark unsupported automatic capabilities incomplete, and update README.md support claims without publishing an app or declaring unverified criteria passed.

## Dependencies & Execution Order

### Phase Dependencies

Setup T001-T003 -> foundation T004-T006 -> manual US1 T007-T013.
US2 T014-T023 then extends the manual infrastructure. US3 T024-T029 can follow US1
without US2; it must not edit shared files concurrently with US2 integrations.
Final T030-T033 depends on integrated stories and their evidence.

### User Story Completion Graph

```text
Setup -> Foundation -> US1 manual increment -> US2 -> complete automatic US1
                                |
                                +-> US3 manual-based activities/summaries
US2 + US3 + automatic US1 -> integrated acceptance
```

Automatic US1 criteria depend on US2's observation gate. A manual increment alone
does not complete those criteria. US3 is independent of automatic observation.

### Within Each Story

Write focused tests before implementing the behavior; detect behavioral failures,
not just compilation errors. Models/shared infrastructure precede integrations.
Run the listed acceptance checks before marking tasks complete. T017 and T020 are
hard evidence gates; T021 cannot proceed on synthetic evidence alone. No device or
participant availability is inferred from time elapsed.

### Parallel Opportunities

After T004, T005 storage and T006 backup configuration touch separate files.
Within stories, the independent test-file pairs T007/T008, T014/T015 and T024/T025
may be prepared concurrently. This is an execution option, not an instruction to
spawn agents. Shared MainActivity/AppViewModel/StateStore edits stay sequential.

## Parallel Example: User Story 1

After foundation: prepare SessionRulesTest.kt (T007) and IntentFlowTest.kt (T008)
concurrently; then implement T009-T012 and validate T013 in sequence.

## Parallel Example: User Story 2

After US1: prepare UsageObserverTest.kt (T014) and ScoreRulesTest.kt (T015)
concurrently. Device gates and service integration remain strictly sequential.

## Parallel Example: User Story 3

After US1: prepare StateStoreTest.kt (T024) and ActivityFlowTest.kt (T025)
concurrently. Serialize T026-T028 because they share state/UI files.

## Implementation Strategy

### MVP First

Complete setup, foundation and the manual US1 increment; demonstrate an intention
without cross-app permissions. It is a useful milestone, not a silent reduction
of the full specification. Keep activity-choice fallback clear until US3 is built.

### Incremental Delivery

Add observation evidence and US2, then US3 (or do US3 first while device work waits).
Validate each increment independently, preserving prior manual behavior. Only
mark automatic requirements complete after genuine device evidence. If service or
observation gates fail, record an explicit scope decision before changing the release.

### Stop Condition

This workflow only generates tasks. Do not scaffold, implement, deploy, publish,
or mark any execution checkbox complete while generating this document.
