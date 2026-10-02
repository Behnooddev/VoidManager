# Handoff: Phase 1 (repository foundation, build system, CI, design system)

Date: 2026-09-29

## Scope

The Gradle build, the module structure, an empty application shell for Android and desktop, a logging facade, design system tokens and base components, CI, repository policy files, and the brand asset specification.

## Verification update (2026-10-01, version 0.3.1)

The first CI run of version 0.3.0 built these modules with Gradle for the first time. Results are in `docs/bug-log.md`. Items in the sections below that say the build or the generated code was unverified are superseded by that run, except where the bug log lists them as still open.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| Gradle build, version catalog, wrapper | Scaffolded | Never run. Wrapper files come from the Gradle repository tag for 9.5.0; CI validates the wrapper jar |
| `core:common` logging facade (`Logger`, `LogSink`, `Redacted`) | Tested | Compiled with the Kotlin 2.4.20 compiler and its 5 tests executed outside Gradle |
| `core:designsystem` tokens, theme, base components, icons | Scaffolded | Not compiled. Compose libraries were not available in the authoring environment |
| `app:shared` navigation shell with four destinations | Scaffolded | Not compiled. Screens are honest empty states; no feature exists |
| `app:android` entry point | Scaffolded | Manifest, theme, activity. No launcher icon yet |
| `app:desktop` entry point | Scaffolded | Window and installer configuration |
| CI workflow, Dependabot | Scaffolded | Never run |
| License, security policy, contributing guide, changelog | Implemented | |
| Version consistency script | Implemented | Reads `version.properties` and checks `CHANGELOG.md` |
| Documentation updates (QR branding, design system, setup, ADR-0007) | Designed | |

## Decisions taken

- AGP 9 module structure (ADR-0007).
- Pinned versions: Kotlin 2.4.20, Android Gradle plugin 9.3.0, Gradle 9.5.0, Compose Multiplatform 1.12.1, compileSdk and targetSdk 36, minSdk 26. Sources: the Kotlin compatibility table and the AGP 9.3 release notes. `androidx.activity` 1.10.1 and `kotlinx-coroutines` 1.10.2 are older releases chosen because they are known to exist; update them on the first build.
- The design system uses Compose foundation only, no Material components.
- No detekt yet. No convention plugins yet. Backups disabled in the manifest (`allowBackup="false"`).
- Desktop installer version is fixed at 1.0.0 until the app reaches 1.0.0.
- Compose plugin aliases (`compose.runtime`, `compose.foundation`, `compose.ui`, `compose.components.resources`) are used. They are deprecated in favor of direct library references but still documented in the current Kotlin guides.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| `core:common` compiled and its tests run | Authoring environment, Kotlin 2.4.20 compiler, JDK 21 | Passed |
| ktlint 1.5.0 on all Kotlin files | Authoring environment | Passed after auto-format |
| Syntax-level scan of Compose sources | Authoring environment, compiler without Compose libraries | No syntax errors; type errors expected because libraries were missing |

## Not verified and known risks

1. Nothing has been built with Gradle. The first `./gradlew :app:desktop:run` and `:app:android:assembleDebug` are the first real verification.
2. The Android-KMP library DSL block name (`androidLibrary { }`) follows the Kotlin guide of July 2026. If Gradle sync rejects it, use the block name from the Android documentation for the plugin version in use.
3. `kotlin { target { ... } }` in `app:android` follows the AGP 9 migration guide. Built-in Kotlin means the Kotlin Android plugin is not applied.
4. Compose Multiplatform resources need the generated `Res` class in package `io.github.behnooddev.voidmanager.shared.resources`. If the import in `App.kt` fails, run the resource generation task or check the `compose.resources` block.
5. Google Maven must be reachable for AGP and AndroidX artifacts.
6. Contrast ratios in the design tokens were chosen by hand and not measured.
7. Lint may report issues in the Android module that could not be seen here.
8. `gradle/verification-metadata.xml` does not exist yet.

## How to continue

1. Commit the tree to `main`, push, and read the first CI run.
2. Fix build errors, keeping the changes minimal, and update this document with what was found.
3. Add the brand assets under `assets/brand` (see `assets/brand/README.md`), then wire the launcher icons and desktop icons (Phase 1.1).
4. Generate dependency verification metadata after the first successful build.
5. Start Phase 2 (database and data model) when the build is green.

## Open items

`docs/open-decisions.md`, items 13 to 21.

## Entry criteria for Phase 2

- CI is green on `main`.
- The desktop app starts and shows the shell.
- The Android debug APK installs and shows the shell.
- Brand assets integration (Phase 1.1) is done or explicitly postponed.

## File map

- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `version.properties`
- `core/common`: logging facade and tests
- `core/designsystem`: theme, tokens, components, icons
- `app/shared`: shell, destinations, string resources
- `app/android`: manifest, theme, activity, build script
- `app/desktop`: main function, installer configuration
- `.github/workflows/ci.yml`, `.github/dependabot.yml`
- `scripts/check-version-consistency.sh`
- `docs/`, `handoffs/`, `assets/brand/README.md`
