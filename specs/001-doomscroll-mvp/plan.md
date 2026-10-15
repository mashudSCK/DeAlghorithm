# Implementation Plan: Android Intentional Use MVP

**Branch**: `main` | **Date**: 2026-10-08 | **Spec**: [spec.md](spec.md)

**Input**: `specs/001-doomscroll-mvp/spec.md`

The setup script reports the feature-directory identifier `001-doomscroll-mvp` as
BRANCH; the actual Git branch is `main`. No branch was created by this workflow.

## Summary

Build one native Android app with optional intentions, manual elapsed timers,
replacement activities, local summaries, and voluntary notifications. Investigate
opt-in usage observation for the proposed TikTok integration before enabling
automatic budgets or Doomscroll Scores. The score remains the specification's
unvalidated duration/reopening heuristic. Do not implement feed/content inspection.

## Technical Context

**Language/Version**: Kotlin, with the compatible compiler version supplied by the
installed Android Studio scaffold; JDK 17. Pin actual compiler/plugin versions in
the generated catalog before the first successful build.

**Primary Dependencies**: Android framework usage, alarm, notification and service
APIs; Compose/Material 3 and lifecycle ViewModel from the Empty Activity template;
Preferences DataStore 1.2.1. Use the template's compatible Compose/compiler versions.
No dependency injection framework, backend, analytics SDK, or navigation framework.

**Storage**: One bounded Preferences DataStore in noBackupFilesDir. JSON-encoded
activity and daily-summary collections. Seven-day retention; transient observations.

**Testing**: Focused JVM tests for scoring, interval reduction, budget/suppression,
and retention; Android instrumentation for storage and UI/action integration;
physical-device monitoring and background-delivery checks.

**Target Platform**: Android 10/API 29 minimum; compile/target API 36 initially.
Verify distribution requirements at release. Android 14+ typed-service behavior is
part of the monitoring gate. TikTok is a proposed physical-device integration target.

**Project Type**: Native mobile app, one application module at repository root.

**Performance Goals**: Responsive UI with no disk/query work on the main thread;
initial five-second polling during enabled automatic sessions, with missed intervals
excluded and delivery latency reported. No guaranteed notification delivery deadline.

**Constraints**: Opt-in only, no content collection, no INTERNET permission,
accessibility/overlay access, exact-alarm permission, or automatic app launches.
Automatic support is conditional on device evidence and service eligibility.

**Scale/Scope**: One user/device, one selected app, ten saved activities, seven daily
summaries, four small views: setup/home, intention, activities, and summary/settings.

## Constitution Check

Pre-research and post-design assessment: PASS for the proposed design.

- **Specification first**: User outcomes and acceptance criteria exist in spec.md.
  Platform is confirmed. Owner review remains visible; planning does not approve
  the heuristic or substitute for implementation evidence.
- **Smallest complete solution**: One app module, framework APIs, bounded storage,
  direct object construction. No speculative abstractions or remote infrastructure.
- **Functional accessible UI**: Apply Uncodixfy to plain mobile controls; use Android
  accessibility semantics, readable contrast, and minimum 48dp touch targets.
- **Evidence before completion**: A mandatory physical-device gate precedes automatic
  support claims. Synthetic reducer tests cannot satisfy that gate.
- **Durable documentation**: Decisions, contracts, validation and limitations live
  beside the specification; persisted artifacts use normal prose.
- **Privacy/compatibility**: Omit network permission; exclude backups; validate storage
  version; fail safely on corruption. No existing application data needs migration.

No constitution exceptions are requested. Design gates pass; implementation and
release gates described below have not passed yet.

## Project Structure

### Documentation (this feature)

```text
specs/001-doomscroll-mvp/
  spec.md
  plan.md
  research.md
  data-model.md
  quickstart.md
  contracts/mobile-behavior.md
  checklists/requirements.md
  tasks.md                       # next workflow; not created by this plan
```

### Source Code (planned repository-root layout)

```text
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/java/com/mashudsck/dealghorithm/
    MainActivity.kt
    AppViewModel.kt
    AppState.kt
    StateStore.kt
    SessionRules.kt
    UsageObserver.kt
    MonitoringService.kt
    ReminderReceiver.kt
    ReminderScheduler.kt
  src/main/res/xml/backup_rules.xml
  src/main/res/xml/data_extraction_rules.xml
  src/test/java/com/mashudsck/dealghorithm/
  src/androidTest/java/com/mashudsck/dealghorithm/
build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml
gradle/wrapper/
gradlew
gradlew.bat
```

**Structure Decision**: A single module and small concrete classes. This is a planned
layout, not existing code. Keep screens in the activity until splitting improves
readability; do not create layers or interfaces merely to match the tree.

## Delivery Sequence and Gates

1. Scaffold the minimal app with compatible locked tooling; prove it builds.
2. Implement the independent manual intention/activity slice, local state, ordinary
   reminders, accessible controls, and deletion. Preserve distinctions between
   manual timer minutes and measured usage.
3. Build a small observation trial on an authorized physical device. Verify Usage
   Access grant/revocation, same-app transitions, screen lock, ten-second returns,
   reminder-return exclusion, and multi-window ambiguity. Start after consent;
   reduce unrelated identities immediately to classifications without recording them.
4. Investigate user-started foreground monitoring. On Android 14+, specialUse is a
   candidate, not an approval. Record the actual subtype justification, service
   stop behavior, supported OS/device matrix, background gaps, and battery evidence.
   No dataSync/health label or automatic self-restart is allowed.
5. Only after observation works, integrate automatic intentions, the score, prompt
   stages, and measured summaries. Show unavailable during incomplete ten-minute
   observation windows, ambiguous app state, or interrupted coverage.
6. Run focused unit/instrumented checks plus physical-device scenarios; conduct the
   five-person usability evaluation before claiming its success criterion passed.

If observation or service eligibility fails, report automatic requirements as
incomplete. Manual mode is a useful fallback, not completion of the entire MVP.
Record any agreed release-scope change rather than silently removing requirements.

## Monitoring and Reminder Design

Count only unambiguous observed selected-app intervals while interactive and keyguard
hidden. Multiple resumed packages or uncertain lifecycle state suspend measurement.
Use one logical observer and one reminder ID; deduplicate overlapping query results.
Reopenings follow the specification, not activity-resume counts. A fresh permission
window needs complete coverage before treating absence of returns as a zero count.

Automatic service start requires an explicit action in the visible app and an ongoing
notification with Stop. Stop after ten minutes away from the selected app; do not
restart after process loss without another user action. Manual mode works independently.

Manual timers use elapsedRealtime during a live process; state checkpoints permit
conservative paused recovery. Inexact alarms carry session/generation identifiers,
recheck current state, and never replay obsolete reminders. All reminders respect
budget priority, one unresolved request, cooldown, and user suppression. Notification
permission/channel denial changes delivery status, not measured data or user choices.

## Complexity Tracking

No justified constitution violations. The candidate foreground service is required
only for the automatic observation experiment; it cannot expand into a generic
background framework. See research.md for alternatives and unresolved evidence gates.
