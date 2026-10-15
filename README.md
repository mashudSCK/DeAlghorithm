# DeAlghorithm

DeAlghorithm is an open-source, Android-first mobile app project that helps people
notice repetitive app use, protect their intentions, and choose meaningful alternatives.
The native Kotlin/Compose app now supports manual timers, voluntary reminders,
replacement activities and seven-day local summaries. Settings also offers an explicit,
consent-based observation trial for device testing. Automatic monitoring and scoring
remain unavailable until physical-device gates pass. The full MVP is still in development.

Review the [MVP draft](specs/001-doomscroll-mvp/spec.md) for optional intentions,
proposed behavioral scoring, voluntary reminders, activities, and local summaries.

## Getting started

```sh
git clone https://github.com/mashudSCK/DeAlghorithm.git
cd DeAlghorithm
```

Read [the project constitution](.specify/memory/constitution.md) and
[AGENTS.md](AGENTS.md) before contributing. Use JDK 17 and Android SDK 36, then run:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

See [quickstart](specs/001-doomscroll-mvp/quickstart.md) for installation,
instrumentation and current verification limits. Activities and summaries work
without Usage Access; all behavioral records remain local. The diagnostic trial keeps
its events transient and creates no measured summaries or production score. See
[trial verification and limits](specs/001-doomscroll-mvp/evidence/observation-trial.md).
The first [physical observation gate](specs/001-doomscroll-mvp/evidence/observation.md)
ran on Android 10. Short native trials now verify selected duration and a qualifying
return, but OS navigation can invalidate lifecycle coverage. The full observation
gate remains failed; no automatic app/OS pair is supported yet.

For development with Codex, open this directory as your workspace. Project skills
are stored in `.agents/skills/`. The Spec Kit workflow uses `$speckit-specify`,
`$speckit-plan`, `$speckit-tasks`, and `$speckit-implement`. Shared scripts use
PowerShell, so install PowerShell if you need to run them on another platform.

## Contributing

Contributions are welcome. Open an issue to discuss features or report problems.
For a contribution, fork the repository, create a branch, and submit a pull request.
Describe the problem, the resulting behavior, and how you verified the change.
Keep changes focused and preserve the project's simplicity and accessibility rules.

New features need a specification and acceptance criteria before implementation.
Small fixes can use a concise problem statement and a focused regression check.
Do not commit credentials, private data, or local setup caches.

## Development tools

- [Spec Kit](https://github.com/github/spec-kit): specifications, plans, and tasks.
- [Uncodixfy](https://github.com/cyxzdev/Uncodixfy): functional UI design guidance.
- [Caveman](https://github.com/JuliusBrussee/caveman): concise development conversations.
- [Ponytail](https://github.com/DietrichGebert/ponytail): minimal, complete implementations.

These tools provide development instructions. They are not application dependencies.

## License

Original project contributions are available under the [MIT License](LICENSE).
Bundled third-party materials retain their upstream licenses and copyright notices:
[Spec Kit](.specify/LICENSE), [Uncodixfy](.agents/skills/uncodixfy/LICENSE),
[Caveman](.agents/skills/caveman/LICENSE), and [Ponytail](.agents/skills/ponytail/LICENSE).
