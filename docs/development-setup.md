# Development setup

## Requirements

- JDK 17 or newer. The build targets Java 17 bytecode. CI uses JDK 21.
- Android SDK with platform 36 and build tools 36.0.0 for the Android app.
- An IDE that supports the Android Gradle plugin version in `gradle/libs.versions.toml`: a current Android Studio, or IntelliJ IDEA 2026.1.2 or newer with the Android plugin.
- Git.

The Gradle wrapper downloads Gradle 9.5.0 on first use. The Android SDK location is read from `local.properties` (`sdk.dir=...`) or from `ANDROID_HOME`. `local.properties` is not committed.

If dependency resolution fails because a repository is unreachable from your network, configure a mirror for Google Maven and Maven Central in `settings.gradle.kts`. Do not commit credentials.

## Commands

| Task | Command |
| --- | --- |
| Run the desktop app | `./gradlew :app:desktop:run` |
| Build the Android debug APK | `./gradlew :app:android:assembleDebug` |
| Android lint | `./gradlew :app:android:lintDebug` |
| Unit tests (shared logic) | `./gradlew :core:common:jvmTest :core:model:jvmTest :core:data:jvmTest :core:database:jvmTest` |
| Check formatting | `./gradlew spotlessCheck` |
| Apply formatting | `./gradlew spotlessApply` |
| Version consistency | `bash scripts/check-version-consistency.sh` |

On Windows use `gradlew.bat` in place of `./gradlew`.

The debug APK is written under `app/android/build/outputs/apk/debug/`.

## Versions

Tool and library versions are pinned in `gradle/libs.versions.toml`. The application version is in `version.properties` and must have a matching entry in `CHANGELOG.md`.

## Environment variables and secrets

None are needed for local builds. Release signing keys are supplied by CI secrets and are never stored in the repository.
