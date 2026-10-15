# Feature Specification: Intentional Mobile Use MVP

**Feature Branch**: `main` (no feature branch created)

**Created**: 2026-10-08

**Status**: Draft for owner review; Android-first target confirmed

**Input**: Recover the October 6 brainstorm for owner review. The owner has clarified
that DeAlghorithm is a mobile app project targeting Android first.

**Source**: [Shared project conversation](https://chatgpt.com/share/6ac6ec0a-29c4-83ec-9d83-c09d3c1f4a45).

## Product Direction and Scope

DeAlghorithm is a mobile app that helps people notice unplanned, repetitive use of
selected apps and choose what to do next. It protects the user's stated intention
through behavioral feedback, gentle interruptions, and user-selected alternatives.
Screen time alone is not proof of harmful use. Its Doomscroll Score is a tentative
behavioral heuristic, not a diagnosis or a measurement of someone's mental state.

The recovered brainstorm supplied the product direction: deterministic scoring,
escalating interventions, optional Intent Mode, replacement activities, and local
analytics. Android-first mobile development is now confirmed. The earlier desktop
browser and YouTube Shorts scope is superseded. Supported-app defaults, formula,
and timing limits still need owner review and physical-device validation.

**First-release platform**: Android. iOS is deferred.

Included: opt-in monitoring of a user-selected supported app, explainable scoring
when its required signals are available, Intent Mode, voluntary intervention choices,
a small replacement-activity list, and local seven-day summaries. The first
integration is limited to the Android TikTok app as a proposed test target. Monitor
the entire selected app rather than claiming to distinguish feeds from messages.
TikTok support must pass the physical-device gate before it is advertised.

Excluded: browser extensions, desktop clients, hard blocking, accounts, cloud sync,
machine learning, content analysis, automatic feed recommendations, and unsupported
claims about time reclaimed, intention, or completed productive activity.

The baseline uses opt-in app-usage observation and ordinary mobile notifications.
It MUST NOT require screen-reading access or permission to draw over other apps.
Scroll events, in-app feed recognition, overlays, and forced app launches are
outside the MVP scope. Platform support does not imply reliable delivery under
every device's background restrictions.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set and Protect an Intention (Priority: P1)

A person chooses a supported app, states a purpose, and sets a time budget. They
receive a reminder when that budget ends and decide whether to continue or switch
to something else. They can use a clearly labeled manual timer if automatic
monitoring is unavailable or they decline permissions.

**Why this priority**: A voluntary intention and reminder provide a useful first
slice without depending on unverified access to another app's scroll behavior.

**Independent Test**: Set a five-minute intention, reach the budget, extend it, and
finish. Repeat in manual mode with observation permissions denied.

**Acceptance Scenarios**:

1. **Given** no monitoring consent, **When** another app is used, **Then** no
   cross-app behavior is collected and monitoring remains disabled.
2. **Given** a 15-minute automatically measured budget, **When** 10 minutes of
   selected-app use have been observed, **Then** score prompts remain suppressed.
3. **Given** the budget expires and reminders are enabled, **When** a reminder is
   delivered through the permitted mobile channel, **Then** the user can finish,
   add five minutes, or open the replacement activities.
4. **Given** manual mode, **When** the user starts a five-minute timer, **Then** it
   measures elapsed time and explicitly states that other-app use is not observed.
5. **Given** automatic monitoring is unavailable, **When** a manual timer finishes,
   **Then** no measured app usage or Doomscroll Score is fabricated.

### User Story 2 - Notice a Repetitive Usage Pattern (Priority: P2)

A person enables supported behavioral monitoring and sees which measured signals
contribute to a tentative Doomscroll Score. They can decline reminders, turn off
monitoring, or declare that their current use is intentional.

**Why this priority**: This is the distinctive detection feature, but its usefulness
requires a demonstrated mobile monitoring capability and an evaluated heuristic.

**Independent Test**: Provide the defined usage and reopening observations on the
supported platform, verify the score, then revoke observation permission.

**Acceptance Scenarios**:

1. **Given** both required signals are available, **When** 10 foreground minutes
   and three repeat openings are observed, **Then** the proposed score is 50.
2. **Given** the reopening signal is unavailable, **When** a usage summary opens,
   **Then** the score says Unavailable rather than assuming zero reopenings.
3. **Given** a score of 85 and no intention or suppression period, **When** the
   first eligible score reminder occurs, **Then** it uses the awareness stage.
4. **Given** observation permission is revoked, **When** the app next evaluates
   monitoring, **Then** collection stops and the app explains how to restore access
   or use manual mode; the missing interval is not estimated as observed usage.
5. **Given** a reminder, **When** Continue for 5 minutes is selected, **Then**
   reminders are suppressed for the extension period, using the mode's time basis.

### User Story 3 - Choose an Alternative and Review Patterns (Priority: P3)

A person saves activities, selects one after a reminder, and reviews local daily
summaries. The app distinguishes measured usage, manual timers, and user choices.

**Why this priority**: A practical next action makes the interruption useful; an
honest summary supports reflection without claiming unobserved outcomes.

**Independent Test**: Save an activity, select it, inspect the daily counts, and
use Delete all data to return the app to the initial opt-in state.

**Acceptance Scenarios**:

1. **Given** Read two pages is saved, **When** it is selected, **Then** one selection
   is recorded, without claiming the reading was completed.
2. **Given** no saved activities, **When** alternatives open, **Then** Take a break
   and Add an activity remain available.
3. **Given** measured sessions and manual timers, **When** the summary opens,
   **Then** measured selected-app minutes and manual timer minutes are separate.
4. **Given** stored preferences and summaries, **When** Delete all data is confirmed,
   **Then** local records and preferences are removed and monitoring is disabled.

### Edge Cases

- Locking the device or using an unselected app adds no measured selected-app time.
- Split-screen and overlapping app states MUST NOT double-count elapsed time. The
  supported-platform plan must define which state qualifies as foreground use.
- Lack of scrolling does not establish inactivity; watching or reading may be
  intentional. The app must not infer intention from duration alone.
- Denied observation permissions leave manual intentions and saved activities usable.
  Denied reminder permission leaves status and overdue intentions visible in the app.
- Delayed monitoring data, background restrictions, or terminated processes are
  reported as gaps. Missed reminders are not replayed as a burst when the app returns.
- Notification dismissal does not mean a reminder was read or an activity completed.
- Returning from a reminder or from DeAlghorithm itself is not a repeat-opening signal.
- Permission loss clears transient scoring state; permission restoration starts
  a fresh observation window and does not import earlier browsing history.
- Ending a manual timer or choosing a break does not prove the user stopped another app.
- Device clock changes must not create negative durations or duplicate daily records.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Monitoring MUST require explicit opt-in with an explanation of the
  observed metadata, purpose, and local processing. Users MUST select the monitored
  app and be able to disable monitoring immediately.
- **FR-002**: Setup MUST disclose which features the platform and granted permissions
  support. Missing permissions MUST NOT block access to manual intentions or activities.
- **FR-003**: Automatic budgets MUST count only verified foreground use of the selected
  app while the device is unlocked. Unknown intervals MUST be excluded and labeled.
- **FR-004**: Manual mode MUST use an explicitly labeled elapsed-time timer with
  Start, Pause, Resume, and Finish. Manual elapsed time MUST NOT become measured usage.
- **FR-005**: Intent Mode MUST support Find something, Reply to someone, Entertainment,
  and Other, with 5-, 15-, or 30-minute budgets. Optional purpose text remains transient.
  An intent applies to one selected app or one manual timer.
- **FR-006**: An unexpired intent budget MUST suppress score reminders for that session.
  Expiry offers Finish, Add 5 minutes, and Choose an activity; it takes priority over
  a score reminder due at the same time.
- **FR-007**: Automatic scoring MUST follow the proposed rule below only when both
  required signals are verified as available. Each score MUST identify its observed
  inputs and explain that it cannot determine whether use is harmful or unintentional.
- **FR-008**: Reminder delivery MUST use a platform-permitted, user-enabled channel.
  It MUST NOT force another app to close, require overlay permission, or open
  DeAlghorithm without a user action. Scores update when new observations arrive;
  no sub-second or identical cross-platform delivery guarantee is made.
- **FR-009**: At most one unresolved reminder may exist, with at least 120 seconds
  between reminder requests. Delayed reminders MUST NOT accumulate for later replay.
- **FR-010**: Continue for 5 minutes MUST suppress reminders for five verified selected-
  app minutes in automatic mode or five elapsed timer minutes in manual mode.
  Take a break ends the current intention and suppresses reminders for five elapsed
  minutes. Neither action controls or verifies activity in another app.
- **FR-011**: Users MUST be able to create, rename, and delete up to 10 activities,
  each with a nonempty name of at most 80 characters. Selecting one records only
  the choice; it MUST NOT automatically navigate or claim completion.
- **FR-012**: Summaries MUST show measured selected-app minutes, manual timer minutes,
  reminder requests, breaks chosen, and activity selections separately. Retain
  today and the previous six local calendar days; expire older records on next access.
- **FR-013**: Behavioral processing and storage MUST remain on the device. DeAlghorithm
  MUST NOT transmit usage records, activity selections, purpose text, or telemetry.
- **FR-014**: Observation MUST be limited to selected-app identity, foreground duration,
  and supported reopening transitions. The app MUST NOT collect messages, typed text,
  searches, media, screenshots, feed contents, full URLs, or unrelated app history.
- **FR-015**: Raw transition timestamps, scoring windows, and optional intent text MUST
  be transient. Persist only preferences, saved activities, remaining intent budget
  needed for timer recovery, and daily aggregates. Any restored intent resumes paused
  until current monitoring status is established; missing time is not fabricated.
- **FR-016**: Delete all data MUST clear local records, cancel pending reminders,
  disable monitoring, and return to the opt-in state. Disable monitoring MUST stop
  collection and cancel automatic reminders while preserving prior summaries.
- **FR-017**: Controls MUST have screen-reader names, logical focus order, readable
  contrast, and usable touch targets. Reminders MUST be voluntary and use neutral,
  non-shaming language; reflection responses MUST be optional and neither stored nor judged.
- **FR-018**: Before claiming automatic monitoring support for a platform/app pair,
  verification MUST demonstrate the required observations, permission revocation,
  and permitted reminder delivery on a physical device. Unsupported capabilities
  MUST remain labeled unavailable rather than simulated as production behavior.

### Proposed Doomscroll Score

This mobile heuristic replaces the browser scroll formula. It is a draft hypothesis
for evaluation, not an established measure of doomscrolling.

- `T` = verified foreground minutes on the selected app since a two-minute absence.
- `R` = repeat openings of that app in the last 10 minutes. The first opening is not
  a repeat. A return qualifies only after at least 10 seconds in another app;
  device unlocks and returns from DeAlghorithm/reminders do not qualify.
- Duration component = `min(T / 20, 1)`.
- Reopening component = `min(R / 6, 1)`.
- Score = `100 * (0.6 * duration component + 0.4 * reopening component)`.
- Round to the nearest whole number, with halves rounded upward.
- Two minutes continuously outside the selected app resets `T`; `R` remains a rolling
  10-minute count. Ten minutes without a qualifying return ends the scoring session.
- If either signal or the current observation window is incomplete, show Unavailable.
  Manual mode never calculates this score.

Examples: `T=10, R=3` gives 50; `T=20, R=6` gives 100;
`T=20, R=0` gives 60. These examples cannot establish intention or harm.

### Intervention Rules

An unexpired user intention and any extension or break suppression take precedence.
For automatic scoring, the proposed eligibility bands are:

- **0-30**: No score reminder; this does not certify intentional use.
- **31-60**: Awareness, showing verified usage and repeat-opening observations.
- **61-80**: Intent check, asking whether use matches the person's intended purpose.
- **81-100**: Optional reflection; a subsequent eligible reminder offers replacement.

The first score reminder is Awareness. Later reminders advance by at most one stage
through Awareness, Intent check, Reflection, and Replacement when the current score
permits it. Lower scores return to the lower eligible stage. Replacement requires a
prior Reflection and a score of at least 81. Budget-expiry reminders do not advance
score stages. All stages respect the cooldown and offer Continue, Take a break,
Choose an activity, and access to Disable monitoring. No response is compulsory.

### Key Entities *(include if feature involves data)*

- **Preferences**: Selected supported app, monitoring opt-in, and reminder settings.
- **Observation Window**: Transient foreground duration, repeat-opening transitions,
  observation completeness, score, and reminder stage.
- **Intent**: Mode, selected app if applicable, purpose, remaining budget, and status.
- **Activity**: A user-defined plain-text replacement action.
- **Daily Summary**: Separate measured usage and manual-timer totals plus action counts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Scoring examples, rounding boundaries, session resets, budget expiry,
  and reminder suppression produce their defined outcomes in repeatable verification.
- **SC-002**: Disabled monitoring, locked-device time, and unselected-app use contribute
  zero measured selected-app time; split-screen intervals are never double-counted.
- **SC-003**: No reminder requests occur within 120 seconds of one another or during
  an active intent budget, extension, or break suppression period.
- **SC-004**: Core setup, intention, reminder response, activity selection, and deletion
  flows are usable with a screen reader and without a focus trap.
- **SC-005**: At least four of five usability participants can distinguish measured
  use from a manual timer and explain a sample reminder without assistance.
- **SC-006**: Verification finds zero behavioral-data transmissions and no stored
  content or raw transitions. Deletion leaves no records or pending reminders.
- **SC-007**: Permission-denied users can complete a manual intention and select an
  activity, while automatic measurement and scores remain labeled unavailable.
- **SC-008**: Physical-device verification establishes the supported platform/app
  observations and reminder channel before automatic monitoring is advertised.

## Assumptions

- DeAlghorithm is the canonical name; the earlier brainstorm's spellings refer to it.
- The audience voluntarily wants help with repetitive mobile-app use. This is neither
  a parental-control product nor a medical intervention.
- Android first is confirmed by the owner. TikTok is a proposed first supported
  app for physical-device validation; iOS and additional apps are deferred.
- Monitoring covers a selected app as a whole, not specifically its short-video feed.
  Intent Mode protects deliberate entertainment, reading, or long-form viewing.
- Duration and reopening observations require validation; scroll-based detection
  and in-app feed recognition are deferred rather than assumed available.
- Formula, thresholds, timing limits, activity limits, and retention are proposed
  defaults requiring owner review and false-positive evaluation.
- Android usage-observation access and user-enabled notifications are dependencies
  of automatic monitoring and reminders. No accessibility or overlay access is
  required by this baseline. Required observations still need device validation.
- Local means no app-operated cloud storage or synchronization. Implementation planning
  must also determine whether usage records must be excluded from device backups.
- Owner review precedes implementation planning; the plan must resolve foreground
  state rules and observation/delivery feasibility on supported Android versions.
  No implementation language, framework, or architecture is selected here.

### Platform Reference Material

These references establish dependencies, not guarantees of product feasibility.

- [Android usage observation](https://developer.android.com/reference/android/app/usage/UsageStatsManager).
- [Android app-state observation](https://developer.android.com/reference/android/app/usage/UsageEvents.Event).
