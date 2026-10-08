# DeAlghorithm Constitution

## Core Principles

### I. Specification Before Implementation

New features MUST have a specification with user outcomes, scope, and verifiable
acceptance criteria before implementation. Use Spec Kit to record specifications,
plans, and tasks under `specs/`. Plans MUST identify material assumptions and
dependencies. Small fixes MAY use a concise problem statement and regression check
instead of a full feature workflow. This keeps implementation tied to actual needs.

### II. Smallest Complete Solution

Coding work MUST follow Ponytail at full intensity: inspect the affected flow,
reuse existing code, then prefer standard libraries, native platform features,
and existing dependencies before adding code or dependencies. New abstractions and
dependencies MUST address a present requirement and have a recorded justification.
Speculative scaffolding MUST NOT be added. Simplicity MUST NOT remove requested
behavior, input validation, security, accessibility, or protection against data loss.

### III. Functional, Accessible UI

UI work MUST apply the project Uncodixfy skill and prioritize clear navigation,
readable typography, consistent spacing, and functional controls. Decorative
elements MUST NOT obscure tasks or replace useful information. Controls MUST have
accessible names, keyboard access, and visible focus indicators. User-supplied
design requirements take precedence over default aesthetic rules, while
accessibility requirements remain in force.

### IV. Evidence Before Completion

Every change MUST be checked against its acceptance criteria before being reported
complete. Nontrivial behavior MUST have a runnable check appropriate to its risk;
bug fixes MUST include a check that detects the reported failure. Verification MAY
use focused assertions, existing tests, or documented manual checks for visual
changes. Checks MUST exercise outcomes rather than merely mirror implementation.
Unrun checks, failures, and material limitations MUST be disclosed.

### V. Clear Communication and Durable Documentation

Chat responses MUST apply Caveman at full intensity while preserving exact technical
meaning, constraints, and necessary explanation. Security explanations and
multi-step instructions MUST remain unambiguous. Project documents, code comments,
and commit messages MUST use normal prose. Decisions affecting behavior, scope,
dependencies, or compatibility MUST be recorded in the relevant feature artifacts.

## Project Constraints

- `AGENTS.md` defines runtime workflow guidance; project skills live in `.agents/skills/`.
- Shared principles live in this constitution; feature artifacts live in `specs/`.
- The technology stack MUST be selected from concrete feature requirements and recorded
  in the implementation plan. This constitution does not mandate a stack.
- Credentials and secrets MUST NOT be committed. Untrusted inputs MUST be validated
  at system boundaries, and errors MUST NOT expose secrets.
- Changes affecting stored data or public interfaces MUST document compatibility
  effects and a recovery or migration approach before implementation.

## Development Workflow

1. Define feature outcomes and acceptance criteria with `$speckit-specify`.
2. Resolve material ambiguities; use `$speckit-clarify` when needed.
3. Record the approach with `$speckit-plan`, including a constitution check.
4. Generate actionable tasks with `$speckit-tasks` and execute scoped implementation.
5. Run relevant checks and review the change against scope and applicable principles.
6. Report delivered behavior, verification results, and remaining limitations.

Small fixes use the abbreviated workflow permitted by Principle I. A failed check
MUST be resolved or explicitly reported as incomplete. Scope expansion MUST be
recorded before implementation; unrelated cleanup MUST be deferred.

## Governance

This constitution governs project specifications, plans, implementation, and reviews.
Explicit user instructions and applicable higher-priority agent instructions take
precedence. Conflicts MUST be surfaced and resulting project deviations recorded.
`AGENTS.md` and project skills supply operational guidance consistent with these rules.

Amendments MUST state the reason, affected principles, and impact on existing work.
The project owner authorizes amendments through explicit instructions or an invoked
constitution update. Each amendment MUST include a compliance review and update the
version and last-amended date. Affected feature plans MUST be identified for follow-up;
template sources MUST NOT be edited by the constitution workflow.

Versioning follows semantic versioning: MAJOR for incompatible principle removals
or redefinitions, MINOR for new principles or materially expanded guidance, and
PATCH for clarifications without governance changes. Reviews MUST check applicable
principles and record justified exceptions with their scope and follow-up conditions.

**Version**: 1.0.0 | **Ratified**: 2026-10-08 | **Last Amended**: 2026-10-08

