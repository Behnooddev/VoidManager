# Handoff: Phase 0 (architecture and technology selection)

Date: 2026-09-29

## Scope

Analysis of the master specification and the design of everything that must be fixed before code: stack, architecture, data model, key management, backup format, share protocol, threat model, testing, CI/CD, versioning, migrations, roadmap.

## Delivered

| Item | Status | Location |
| --- | --- | --- |
| Architecture and module layout | Designed | `docs/architecture.md`, `docs/repository-layout.md` |
| Platform strategy | Designed | `docs/platform-strategy.md` |
| Data model and field system | Designed | `docs/database.md` |
| Key hierarchy, unlock, lock behavior | Designed | `docs/security/key-management.md` |
| Password manager design | Designed | `docs/security/password-manager.md` |
| Threat model outline | Designed | `docs/security/threat-model.md` |
| Share protocol v1 draft | Designed | `docs/sharing.md` |
| Backup format v1 draft | Designed | `docs/backup-format.md` |
| Photo and attachment storage | Designed | `docs/media-storage.md` |
| Testing, CI/CD, versioning, migrations | Designed | `docs/testing.md`, `docs/ci-cd.md`, `docs/versioning.md`, `docs/migrations.md` |
| Roadmap | Designed | `docs/roadmap.md` |
| Architecture decision records | Accepted or proposed | `docs/adr/` |

No code was written.

## Decisions taken

- Kotlin and Compose Multiplatform (accepted).
- SQLite through SQLDelight with SQLCipher, plus per-value encryption for sensitivity levels 2 and 3.
- Vault key wrapped by a password-derived key and an optional device-bound key. Argon2id through Bouncy Castle, AEAD and HPKE through Tink.
- Logical, chunked and authenticated backup format with a separate backup password.
- Share protocol in three layers (package, session, transport).
- Later confirmations on 2026-09-29: MIT license, Android `minSdk` 26, share methods QR code and Nearby, package id `io.github.behnooddev.voidmanager`. The full list is in `docs/open-decisions.md`.

## Verification performed

Documents were reviewed for consistency with the specification and for tone. No code, so no build or test.

## Not verified and known risks

- SQLCipher on the desktop JVM has no first-party driver. Desktop release depends on a spike in Phase 2.
- The `.vmbk` extension was checked with a single search only.
- Argon2id parameter floor and master password policy are not calibrated.
- HPKE usage with Tink for the share session is a design intent, not confirmed against the library.

## How to continue

Read `docs/README.md`, then `docs/open-decisions.md`. Phase 1 builds the repository and the design system.

## Open items

See the open table in `docs/open-decisions.md`.

## Entry criteria for Phase 1

Stack accepted (met). License chosen (met). Repository location known (met).
