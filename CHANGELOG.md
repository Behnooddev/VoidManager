# Changelog

All notable changes are recorded here. The format follows Keep a Changelog and versions follow semantic versioning.

## [Unreleased]

## [0.3.1] - 2026-10-01

### Fixed

- BUG-002: the Android build script used `java.util.Properties`, which does not resolve inside a Gradle Kotlin script; it now imports `java.util.Properties`. This error stopped every Gradle step.
- BUG-003: `compileSdk` raised from 36 to 37, which Compose Multiplatform 1.12.1 requires. `targetSdk` remains 36.
- BUG-004: a test in `core:data` returned a nullable value from a query mapper and did not compile.

### Changed

- CI: all steps run after a failure and Gradle continues past failing tasks; the debug APK and the test and lint reports are uploaded as artifacts; the workflow can be started by hand.
- Added `docs/bug-log.md` and the rule that every fix is a patch release.

### Compatibility

- Database schema: 1.
- Key file format: 1.
- Backup format: none yet.
- Share protocol: none yet.

## [0.3.0] - 2026-09-30

### Added

- `core:crypto`: Argon2id key derivation, HKDF, AES-256-GCM, the key file format, vault creation, unlock, lock, password change, and an optional device-key unlock slot. Checked against RFC 9106, RFC 5869 and published AES-GCM vectors.
- `core:security`: password policy with strength estimate, auto-lock controller, re-authentication window, secret reveal timer.
- Value encryption for sensitivity levels 2 and 3 through a session-bound `ValueCipher`.

### Changed

- Tink is no longer a planned dependency for the key hierarchy (ADR-0003 amendment).

### Compatibility

- Database schema: 1.
- Key file format: 1.
- Backup format: none yet.
- Share protocol: none yet.

## [0.2.0] - 2026-09-29

### Added

- `core:model`: entities, field values, field registry with sensitivity levels, relationship types, UUIDv7 ids.
- `core:database`: schema version 1 (SQLDelight), encrypted drivers for Android (SQLCipher) and desktop (SQLCipher-compatible JDBC fork), vault opener with migration planning and a check that the file is encrypted.
- `core:data`: text and phone normalization, sensitivity and sharing rules, repositories for people, field values, relationships, trash and search, built-in registry seeding.
- Decision and spike documentation for desktop database encryption.

### Fixed

- CI: the Gradle wrapper is made executable in the workflow; `.gitattributes` added for line endings.

### Compatibility

- Database schema: 1.
- Backup format: none yet.
- Share protocol: none yet.

## [0.1.0] - 2026-09-29

### Added

- Phase 0 documentation: architecture, data model, security design, threat model outline, backup format, share protocol draft, roadmap and architecture decision records.
- Phase 1 foundation: Gradle build with Kotlin Multiplatform, Android application module, desktop application module, shared UI module, logging facade, design system tokens and base components, navigation shell, CI workflow, MIT license.

### Compatibility

- Database schema: none yet.
- Backup format: none yet.
- Share protocol: none yet.
