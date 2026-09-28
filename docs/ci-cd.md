# CI/CD

Status: Proposed. Workflows are written in Phase 1 and extended per phase.

## Pull request pipeline

1. Gradle wrapper validation.
2. Dependency verification against `gradle/verification-metadata.xml`.
3. Formatting check (ktlint through Spotless).
4. Static analysis (detekt, Android lint) with the project rule set, including the ban on direct logging.
5. Unit and repository tests on the JVM.
6. Android build (debug) and instrumented tests on an emulator for modules that need them.
7. Desktop build on Linux for every pull request; Windows and macOS builds on release branches.
8. Dependency vulnerability scan.
9. Secret scan.
10. Consistency check: version files, changelog entry, schema and format version files agree (see `versioning.md`).

## Main branch

Same as pull requests, plus generation of debug artifacts for inspection.

## Release pipeline

Triggered by a version tag.

1. Full pipeline on the tagged commit.
2. Release APK and AAB signed with a key held in encrypted repository secrets.
3. Desktop installers for the platforms supported by that release.
4. SHA-256 checksums file.
5. Release notes taken from the changelog, with compatibility and migration notes.
6. Artifacts attached to the release.

Secrets are never printed in logs. Signing keys are never committed. Whether builds are reproducible is checked once release builds exist; reproducibility is a goal and is not claimed until verified.

## Branch and review rules

Main is protected. Changes go through pull requests with passing checks. Security-sensitive modules (`core:crypto`, `core:security`, `core:sharing`, `core:backup`) require review.

## Dependency updates

Automated update proposals through Dependabot, reviewed manually. Updates that change a cryptographic library require the crypto test suite to pass and a changelog entry.
