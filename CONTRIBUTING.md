# Contributing

Repository: https://github.com/Behnooddev/VoidManager

## Before starting

Read `docs/README.md`, `docs/architecture.md` and `docs/open-decisions.md`. Changes that alter scope, the data model, the security design, or a file format need an entry in `docs/adr/` and a discussion before implementation.

## Setup

See [docs/development-setup.md](docs/development-setup.md).

## Rules

- Format with `./gradlew spotlessApply` before committing. CI runs `spotlessCheck`.
- Add or update tests with every behavior change. Security-sensitive modules (`core:crypto`, `core:security`, `core:sharing`, `core:backup`) need tests for failure paths, not only the success path.
- Update the documentation and `CHANGELOG.md` in the same pull request as the change.
- Test data is synthetic. Do not commit real names, phone numbers, addresses, documents, credentials or keys.
- Do not log secrets. Use the logging facade in `core:common`.
- Do not add a dependency without recording its license and reason in the pull request.
- Keep pull requests focused on one change.

## Vulnerabilities

Do not report vulnerabilities in issues. See [SECURITY.md](SECURITY.md).

## License

Contributions are licensed under the MIT license of this repository.
