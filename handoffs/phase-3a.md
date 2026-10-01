# Handoff: Phase 3a (security foundation)

Date: 2026-09-30

## Scope

Phase 3 of the roadmap is split. Phase 3a covers the key hierarchy, encryption of stored values, the key file, vault lifecycle (create, unlock, lock, change password), the lock and re-authentication logic, and the password policy. Phase 3b covers everything that needs screens or a device: setup and unlock screens, Android Keystore device unlock, calibration of the cost parameters, and the People, Personal, Quick Add, profile and trash screens.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| Argon2id, HKDF, AES-256-GCM wrappers | Tested | Against RFC 9106, RFC 5869 and published AES-GCM vectors |
| Key file format v1 (parse, serialize, tamper checks) | Tested | Cost parameters and salt are bound into the wrap; range checks before derivation |
| `VaultManager`: create, unlock, change password, device-key slot | Tested | 39 tests in `core:crypto` ran outside Gradle |
| `VaultSession`: derived keys, lock that overwrites keys, `ValueCipher` for repositories | Tested | |
| File-based atomic key file store | Tested | |
| `core:security`: password policy, auto-lock controller, re-authentication window, reveal timer | Tested | 20 tests ran outside Gradle |
| Integration test: key hierarchy with encrypted database and repositories | Scaffolded | Written, never run (needs the SQLDelight-generated code) |
| Wiring into the application | Not started | No screen uses these classes yet |
| Android Keystore device key, biometric prompt | Not started | Interface slot exists in the key file and `VaultManager` |
| Calibration of the Argon2id cost on the device | Not started | Default 64 MiB, 3 iterations |

## Decisions taken

- AES-GCM from the platform provider; Bouncy Castle for Argon2id and HKDF; Tink removed from the plan for now (ADR-0003 amendment).
- Key file stored beside the database, atomic write, versioned.
- Passwords are NFKC-normalized before encoding.
- Change of password requires the current password and keeps the vault key.
- Cost parameter range accepted from a key file: 19 to 512 MiB, 2 to 10 iterations, parallelism 1 to 8.
- Password policy: minimum 12, never configurable below 8, small list of very common passwords, heuristic strength estimate.
- Lock defaults: 60 seconds idle, lock when the app goes to the background.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 39 crypto tests, including the three published vector sets | Kotlin 2.4.20 compiler, JDK 21, Bouncy Castle built from the r1rv86 source tag, outside Gradle | 39 passed |
| 20 security logic tests | Kotlin 2.4.20 compiler, outside Gradle | 20 passed |
| ktlint 1.5.0 | Authoring environment | Passed |

## Not verified and known risks

1. Gradle has not built this module. The Bouncy Castle coordinate `org.bouncycastle:bcprov-jdk18on:1.86` was derived from the upstream release tag and was not looked up on Maven Central; correct it on the first resolution error.
2. The tests ran against Bouncy Castle compiled from source and the JDK 21 provider, not against the Android runtime. Behavior on Android (AES-GCM through the platform provider, Bouncy Castle classes bundled in the APK) is expected to match but is not checked.
3. The JVM cannot guarantee that no copy of key material stays in memory. Overwriting reduces exposure only.
4. NFKC normalization creates a temporary string of the password that cannot be overwritten.
5. The Argon2id default of 64 MiB has not been timed on a low-end phone. A wrong default can make unlock slow or fail on devices with little memory.
6. The heuristic password strength estimate can overrate passwords built from dictionary words and cannot detect breached passwords.
7. A lost master password has no recovery. This is a design decision (`docs/open-decisions.md`, item 6) and the screens must say so.
8. `core:crypto` is a plain Kotlin JVM module. The Android application and the desktop application both depend on it; the dependency from `app:android` is not declared yet and is part of the wiring.

## How to continue

1. Push, read the CI run, fix compile errors in the order `core:model`, `core:database`, `core:data`, `core:security`, `core:crypto`.
2. Run `./gradlew :core:crypto:test` and `:core:security:jvmTest` locally. The integration test in `core:crypto` also needs the SQLDelight generated code.
3. Phase 3b: wire a `VaultManager` and `AutoLockController` into the application; build setup and unlock screens; add the Android Keystore adapter; calibrate the cost; then the People and Personal screens.

## Open items

`docs/open-decisions.md`, items 13 to 24.

## Entry criteria for Phase 3b

- CI green on `main`, including `:core:crypto:test`.
- The decision on the Android Keystore adapter details (item 23) is taken or deferred explicitly.

## File map

- `core/crypto`: key derivation, encryption, key file, vault manager, session
- `core/security`: lock logic, password policy, reveal timer
- `docs/security/key-management.md`, `docs/adr/0003-key-management.md`
