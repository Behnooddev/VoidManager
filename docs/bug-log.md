# Bug log

Every defect found in the repository is recorded here with its cause, its fix and what prevents a repeat. Each fix is released as a new patch version with a changelog entry (see `versioning.md`).

| ID | Found in | Fixed in | Summary |
| --- | --- | --- | --- |
| BUG-001 | 0.1.0 | 0.2.0 | CI could not run `gradlew` (permission denied) |
| BUG-002 | 0.3.0 | 0.3.1 | Android build script failed to compile: `java.util` unresolved |
| BUG-003 | 0.3.0 | 0.3.1 | Android modules compiled against SDK 36, Compose 1.12.1 needs 37 |
| BUG-004 | 0.3.0 | 0.3.1 | Repository test did not compile: nullable value in a query mapper |

## BUG-001: `gradlew` not executable in CI

- Symptom: `./gradlew: Permission denied`, exit code 126, in every Gradle step.
- Cause: the executable bit of `gradlew` is not kept when the files are copied through an archive or committed from Windows, so Git stored the script as a plain file.
- Fix: the workflow runs `chmod +x ./gradlew` before the first Gradle call; `.gitattributes` keeps line endings stable (LF for scripts, CRLF for `gradlew.bat`). The bit can also be stored with `git update-index --chmod=+x gradlew`.
- Prevention: the workflow step makes the build independent of the stored bit.

## BUG-002: `java.util.Properties` unresolved in the Android build script

- Symptom: `Unresolved reference 'util'` at `app/android/build.gradle.kts` line 10. The error occurs while Gradle configures the project, so every step that starts Gradle failed, including the formatting check.
- Cause: inside a Gradle Kotlin script the name `java` refers to the project's `java` extension, not to the `java` package. The fully qualified name `java.util.Properties` therefore did not resolve.
- Fix: `import java.util.Properties` at the top of the script and use `Properties()`.
- Prevention: build scripts do not use fully qualified `java.` names. Build scripts cannot be compiled outside Gradle, and the formatter only checks layout, so this class of error is found only by a CI run.

## BUG-003: compileSdk 36 is below what Compose 1.12.1 requires

- Symptom: `checkDebugAarMetadata` failed in `app:android`, and `checkAndroidMainAarMetadata` failed in `app:shared`, `core:designsystem` and other Android-target modules. Eight Compose artifacts (animation, foundation, ui, runtime-saveable) reported that dependents must compile against Android API 37 or later.
- Cause: `compileSdk` was set to 36 when the versions were pinned in Phase 1. The Compose Multiplatform 1.12.1 release depends on AndroidX Compose libraries that require API 37 at compile time. The requirement was not checked when the version was chosen.
- Fix: `android-compileSdk` is 37 in `gradle/libs.versions.toml`. `targetSdk` stays 36, because compile level and target level are independent: the compile level only decides which APIs the code may use, and the target level decides which runtime behavior changes apply.
- Prevention: when a library version is pinned or updated, its minimum compile SDK is checked in the release notes. The CI Android step reports this class of error at once.
- Note: the SDK platform for API 37 must be available to the build. It is already on the runner image: the 0.4.0 build did not download it, and the explicit install step that existed in 0.3.1 only logged a broken pipe, so it was removed in 0.4.1.

## BUG-004: nullable value returned from a SQLDelight query mapper

- Symptom: the test sources of `core:data` did not compile (`Cannot infer type for type parameter 'T'`, `Return type mismatch: expected 'Any', actual 'ByteArray?'`). Because of this, the repository tests did not run.
- Cause: a query mapper lambda returned a nullable column (`value_cipher`). The type of a query's rows cannot be nullable.
- Fix: the lambda returns `checkNotNull(cipher)`, which also makes the test fail clearly when the value is absent.
- Prevention: none beyond CI. The generated query classes do not exist outside Gradle, so test code that uses them cannot be compiled in the authoring environment.

## BUG-005: `Char` passed to `ByteArray.fill`

- Symptom: `app:shared` did not compile (`Argument type mismatch: actual type is 'Char', but 'Byte' was expected`, `VaultController.kt` lines 54 and 80). Everything that depends on `app:shared` failed with it: its tests, the Android debug build and lint, and the desktop compile. The `core` modules and their tests, including the new calibrator and device-unlock tests, passed.
- Cause: the overwrite constant in `VaultController` was a `Char` (right for the password `CharArray`) and was reused for the device key, which is a `ByteArray`.
- Fix: the device key is overwritten with `fill(0)`.
- Prevention: `VaultController` is now compiled and tested outside Gradle too, against a minimal stand-in for Compose's `mutableStateOf` (12 tests in `VaultControllerTest`, which also check that passwords and device keys are overwritten). The screens, the design system and the Android classes still cannot be compiled outside Gradle, so CI remains the first check for those.
- Not yet seen: the compile of `app:android` (including `AndroidDeviceKeyProvider`) never ran in the 0.4.1 build because `app:shared` failed first.

## Warnings that are not defects

These appear in the build output. They do not fail the build and are tracked here so they are not mistaken for new problems.

| Warning | Meaning | Plan |
| --- | --- | --- |
| `androidLibrary` block is deprecated, use `android` | The Android-KMP plugin renamed its block | Rename in all library modules in a separate change, with a build to confirm |
| `compose.runtime`, `compose.foundation`, `compose.ui`, `compose.components.resources` are deprecated | The plugin accessors are replaced by direct library references | Replace with catalog entries when the stable library versions are confirmed |
| `commonTest` exists but Android host tests are not enabled | Tests in `commonTest` run on the JVM target only, not as Android host tests | Acceptable for now: the shared logic does not depend on the platform. Enable `withHostTest` if Android-specific test coverage is needed |
| Configuration cache is not enabled | A build speed suggestion | Optional |

## What the CI run of 0.3.0 confirmed

Before this run the modules had never been built by Gradle. The run showed that these work: Gradle configuration of all modules, the formatting check, the SQLDelight code generation, compilation and tests of `core:common`, `core:model`, `core:security`, `core:database` (including the encrypted database tests) and `core:crypto` (including the integration test with the encrypted database), compilation of `core:data`, assembly of all shared modules for Android, compilation of the design system, the shared UI and the desktop app, and the secret scan. Not yet confirmed: the Android debug build, Android lint, and the tests of `core:data`.
