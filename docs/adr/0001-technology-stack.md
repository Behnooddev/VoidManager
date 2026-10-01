# ADR-0001: Technology stack

Status: Accepted (2026-09-29)

## Context

Requirements that drive the choice: Android quality first, offline operation, local encryption, secure key storage, a credible path to Windows, Linux and macOS, one shared implementation of security-critical logic, and long-term maintainability by a small team.

## Decision

- Language: Kotlin.
- UI: Compose Multiplatform for Android and desktop.
- Build: Gradle with Kotlin DSL, a version catalog, and convention plugins in `build-logic`.
- Shared code in Kotlin Multiplatform modules; platform code behind `expect`/`actual` or interfaces.
- Libraries proposed, each confirmed when first introduced:
  - kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime
  - SQLDelight for typed SQL and migrations (ADR-0002)
  - Bouncy Castle for Argon2id and HKDF, platform AES-GCM (ADR-0003)
  - Koin for dependency injection
  - Coil for image loading
  - ZXing core for QR generation

Versions are pinned in `gradle/libs.versions.toml` in Phase 1 to the latest stable releases at that time.

## Alternatives considered

- Flutter (Dart). One codebase for all platforms and ready-made packages for encrypted SQLite and secure storage. Rejected because Android integration is less direct for Keystore, biometrics and deep links, and the security-relevant ecosystem is smaller.
- Native Android plus a separate desktop application. Best per-platform fit but duplicates the security and data logic. Rejected.
- Web technologies in a shell (Tauri, Electron). Rejected because of the weaker Android story and larger attack surface for a security-focused product.

## Consequences

- One implementation of the data model, crypto orchestration, merge and export logic.
- Desktop secure storage and encrypted database need extra platform work (see platform-strategy.md and ADR-0002).
- Contributors need Kotlin and Gradle multiplatform knowledge. Build times for multiplatform projects are higher than for Android-only projects.
- The Phase 0 authoring environment cannot compile Kotlin. Compilation is verified in CI and on developer machines.
