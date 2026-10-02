# Development setup

## Requirements

- JDK 17 or newer. The build targets Java 17 bytecode. CI uses JDK 21.
- Android SDK with platform 37 for the Android app (`compileSdk` is 37; the Android Gradle plugin installs the build tools it needs).
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
| Unit tests (shared logic) | `./gradlew :core:common:jvmTest :core:model:jvmTest :core:data:jvmTest :core:database:jvmTest :core:security:jvmTest :core:crypto:test` |
| Check formatting | `./gradlew spotlessCheck` |
| Apply formatting | `./gradlew spotlessApply` |
| Version consistency | `bash scripts/check-version-consistency.sh` |

On Windows use `gradlew.bat` in place of `./gradlew`.

The debug APK is written under `app/android/build/outputs/apk/debug/`.

## Building without a local toolchain

The project does not require Android Studio. The CI workflow builds the Android debug APK, compiles the desktop app, runs the unit tests and checks formatting on every push to `main` and on every pull request.

- The APK is attached to each run as the `debug-apk` artifact (Actions tab, open the run, Artifacts section). It is signed with the debug key and can be installed on a device that allows installs from unknown sources.
- Test and lint reports are attached as the `reports` artifact.
- Every step after the first failure still runs, and Gradle continues after a failing task, so a single run lists most problems at once.
- The full log of a run can be downloaded from the run page (gear icon, "Download log archive").

## Versions

Tool and library versions are pinned in `gradle/libs.versions.toml`. The application version is in `version.properties` and must have a matching entry in `CHANGELOG.md`.

## Environment variables and secrets

None are needed for local builds. Release signing keys are supplied by CI secrets and are never stored in the repository.
