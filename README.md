# DeAlghorithm

DeAlghorithm is an open-source project in its initial setup phase. This repository
currently contains development guidance and tooling; application features and the
technology stack have not yet been defined.

## Getting started

```sh
git clone https://github.com/mashudSCK/DeAlghorithm.git
cd DeAlghorithm
```

Read [the project constitution](.specify/memory/constitution.md) and
[AGENTS.md](AGENTS.md) before contributing. There is no application to run yet.

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
