# Security policy

## Supported versions

There is no stable release yet. Until one exists, only the current state of the `main` branch is considered supported.

## Reporting a vulnerability

Send reports by email to shabehnood47@gmail.com. Do not open a public issue for a vulnerability.

Include:

- the affected version or commit
- the platform (Android version and device, or desktop operating system)
- steps to reproduce, or a proof of concept
- the impact you observed or expect
- whether the issue is already public

Reports are handled by a single maintainer on a best-effort basis. Please allow time for a fix before disclosing details publicly, and coordinate the disclosure date by email.

## Scope

In scope: the application code in this repository, the backup and share formats, and the build and release configuration.

Out of scope: vulnerabilities in the operating system, in third-party libraries that are already fixed upstream, and attacks that require a compromised or rooted device. The assumptions and known limits are described in [docs/security/threat-model.md](docs/security/threat-model.md).
