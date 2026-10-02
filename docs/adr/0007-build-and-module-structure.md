# ADR-0007: Build and module structure

Status: Accepted (2026-09-29)

## Context

Android Gradle plugin 9 is not compatible with the Kotlin Multiplatform plugin when the same module is also an Android application or a classic Android library. The Kotlin documentation requires the Android entry point to live in its own module and shared modules to use the Android-KMP library plugin (`com.android.kotlin.multiplatform.library`).

## Decision

- `app:android` is a plain Android application module (entry point, manifest, resources).
- `app:desktop` is a JVM application module (entry point, installer configuration).
- `app:shared` is a Kotlin Multiplatform library with the shared UI shell and Compose resources.
- `core:*` modules are Kotlin Multiplatform libraries with an Android target (Android-KMP plugin) and a JVM target. The JVM target serves the desktop app.
- No iOS target yet.
- Version catalog in `gradle/libs.versions.toml`. No convention plugins yet.
- Kotlin 2.4.20, Android Gradle plugin 9.3.0, Gradle 9.5.0, Compose Multiplatform 1.12.1. These follow the compatibility tables in the Kotlin and Android documentation at the time of writing.

## Alternatives

- A single `composeApp` module holding entry points and shared code. Rejected: not supported with AGP 9.
- Staying on AGP 8. Rejected: it would need to be migrated soon and the legacy compatibility flag is removed in AGP 10.

## Consequences

- Module boundaries match the platform split from the start.
- Android build type variants are not available in shared modules.
- Every version in the catalog needs a check against the compatibility tables when updated.

## Amendment (2026-10-01)

`compileSdk` is 37, because Compose Multiplatform 1.12.1 depends on AndroidX libraries that require it. `targetSdk` remains 36. See `docs/bug-log.md`, BUG-003.
