# Changelog

All notable changes are recorded here. The format follows Keep a Changelog and versions follow semantic versioning.

## [Unreleased]

## [0.5.0] - 2026-10-08

Phase 3b-2: People, Personal, Quick Add, profiles and Trash.

### Added

- People tab: list with search, a Favorites section, Quick Add and a link to Trash.
- Quick Add: a name, and optionally a phone number, an email and a note. Filled fields are validated; the phone and email become the primary values.
- Profile: values grouped by section, with the primary value first. Values at protection level 2 and 3 stay masked until Show is pressed. Add, edit and delete a value; rename; favorite; move to Trash.
- Personal tab: the Personal profile (created on first use) with the same editing, and no Trash action.
- Trash: restore, or delete forever after a confirmation.
- Field editor for single-line and multi-line text, number, date (`YYYY-MM-DD`), web address, phone and email values, with a label, a note and per-type validation. Phone numbers accept Persian and Arabic-Indic digits.
- Back handling: the Android back action returns to the previous screen inside a tab before it leaves the app. The screen position survives a rotation.
- `PeopleModel`, `PeopleRoute`, `QuickAdd`, `FieldInput` and `ProfileBuilder` in `app:shared`; `VmListItem` in the design system; `BackDispatcher`.
- 29 tests for the new logic (routes, validation, quick add, profile building and the model against in-memory repositories).

### Changed

- Typing in any text field counts as activity, so the idle timer does not lock the vault in the middle of an edit.
- A stale search result can no longer replace the result of what was typed afterwards.

### Not included

- Editing composite values (address, card, account, work, education); they are shown read-only with the protected parts left out.
- Relationships, photos, tags, custom field definitions, and a way to raise the protection level of a value.
- Permanent deletion of single values has no Trash; deleting a value is immediate after a second confirming tap.

### Compatibility

- Database schema: 1.
- Key file format: 1.
- Backup format: none yet.
- Share protocol: none yet.

## [0.4.2] - 2026-10-04

### Fixed

- BUG-005: `VaultController` passed a `Char` to `ByteArray.fill` when overwriting the device key, so `app:shared` did not compile in 0.4.1. The key is now overwritten with `fill(0)`.

### Added

- `VaultControllerTest` (12 tests): unlock, wrong password, device unlock, cancelled and invalidated prompts, enabling and disabling, and that the password and the device key are overwritten after use.

### Changed

- `MainActivity` creates its biometric key provider once instead of on every recomposition.

### Compatibility

- Database schema: 1.
- Key file format: 1.
- Backup format: none yet.
- Share protocol: none yet.

## [0.4.1] - 2026-10-03

Phase 3b-1b: biometric unlock and a key derivation cost fitted to the device.

### Added

- Biometric unlock on Android. A 256-bit AES key lives in the Android Keystore (StrongBox when present), usable only after a strong-biometric prompt for each operation, and invalidated when biometrics change. The vault's device key is 32 random bytes stored encrypted by that key; the key file keeps a second wrapping of the vault key under it. The password always keeps working.
- Settings screen: turn biometric unlock on or off.
- Unlock screen: "Unlock with biometrics" button when it is on and available. If the system key is invalidated, the slot is removed and the screen says why.
- `KdfCalibrator`: at vault creation one timed Argon2id derivation sets the iteration count (2 to 10) for about 700 ms on this device; memory is lowered only when even two iterations take over 3 seconds. The cost is stored in the key file, so the vault opens on any device. Existing vaults are unchanged.
- `VaultGateway.deviceUnlockEnabled`, `unlockWithDeviceKey`, `disableDeviceUnlock` and `OpenedVault.enableDeviceUnlock`; `VaultManager.deviceUnlockEnabled` and a cost parameter on `create`.
- Dependencies: `androidx.biometric` 1.1.0 and `androidx.fragment` 1.8.6. `MainActivity` is now a `FragmentActivity`.

### Changed

- CI: the "Install Android platform 37" step is removed. Its log showed only a broken pipe, and the platform is already on the runner (the 0.4.0 build did not download it). The 0.4.0 note that this was fixed was wrong.

### Compatibility

- Database schema: 1.
- Key file format: 1 (the device-key slot already existed).
- Backup format: none yet.
- Share protocol: none yet.

## [0.4.0] - 2026-10-03

Phase 3b-1: the vault can be created and unlocked from the app.

### Added

- Setup screen: master password with confirmation, strength estimate and the password policy, and an acknowledgement that a forgotten password cannot be recovered.
- Unlock screen: wrong-password message, a growing wait after repeated failures (in memory only; see `docs/security/key-management.md`), and distinct messages for a damaged key file, a newer vault version and storage failures.
- `VaultGateway` and `OpenedVault` (`core:data`) and `VaultService` (`core:crypto`): password in, open encrypted vault out, with every failure named.
- `VaultFlow`: the lock state machine (setup, locked, unlocked), idle and background auto-lock, and a Lock button in the top bar.
- Android: the vault controller lives in the `Application`, so a screen rotation does not lock it; the app locks when it goes to the background and on the idle timeout; window contents are hidden from screenshots and the recent apps overview (`FLAG_SECURE`).
- `VmPasswordField` and secret-field support in `VmTextField` (masked input, show or hide, no autocorrect, password semantics).
- Desktop: the vault is stored in `~/.voidmanager`; idle timeout only. This is a development convenience.

### Changed

- `App` now takes a `VaultController`.
- The placeholder text no longer says the app contains the foundation only.
- CI: the `app:shared` tests run in the unit test step; the platform 37 install step no longer reports a broken pipe.

### Not included

- Android Keystore and biometric device unlock (planned as 0.4.1), calibration of the Argon2id cost on the device, and the People, Personal and trash screens (3b-2, 0.5.0).

### Compatibility

- Database schema: 1.
- Key file format: 1.
- Backup format: none yet.
- Share protocol: none yet.

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
