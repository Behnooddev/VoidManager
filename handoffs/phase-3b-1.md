# Handoff: Phase 3b-1 (setup, unlock and lock wiring)

Date: 2026-10-03. Version 0.4.0.

## Scope

Phase 3b is split in three. 3b-1 (this one) makes the vault usable from the app: create it, unlock it, lock it. 3b-1b adds Android Keystore device unlock with the biometric prompt and calibrates the Argon2id cost on a device. 3b-2 adds the People, Personal, Quick Add and trash screens.

Device unlock was moved out of 3b-1 on purpose. It needs `BiometricPrompt` with a crypto object, which cannot be exercised without a device and would mix a hard-to-verify change into the first CI run of the unlock flow.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `VaultGateway`, `OpenedVault`, `OpenOutcome` (`core:data`) | Written | Interface only; keeps keys out of the UI layer |
| `VaultService` (`core:crypto`) | Test written, not run | `VaultServiceTest` uses a real encrypted file: create, lock, unlock, wrong password, damaged key file, double lock |
| `VaultFlow`, `AttemptThrottle`, `SetupForm` (`app:shared`) | Tested | 23 tests passed outside Gradle |
| `VaultController` and the setup and unlock screens | Not built | Compose code; first compile is the CI run |
| `VmPasswordField`, secret support in `VmTextField` | Not built | |
| Android: `VoidManagerApp`, lifecycle locking, `FLAG_SECURE` | Not built | |
| Desktop: controller in `~/.voidmanager` | Not built | Development convenience only |

## Decisions taken

- The controller lives in the `Application`, not the activity, so rotation does not lock.
- Background locks immediately when the policy grace is zero (decided in `VaultFlow.onBackgrounded`), not on return, so the keys are not held in memory while the app is away.
- The wrong-password wait is in memory and documented as not a security boundary.
- `FLAG_SECURE` is always on.
- Password text uses plain `remember`, never `rememberSaveable`.
- Lint check `InvalidPackage` is disabled for the Android module as a precaution: Bouncy Castle contains classes for packages Android lacks (for example JNDI) that this code never loads. Remove the line if the first lint run shows it is not needed.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 23 tests: throttle, setup form, lock state machine | Kotlin 2.4.20 compiler, outside Gradle, with a test shim and a stub for `VaultRepositories` | 23 passed |
| ktlint 1.5.0 on every changed file | Authoring environment | Passed |

## Not verified and known risks

1. No Gradle build was possible in the authoring environment (Maven is not reachable). Everything that touches Compose, Android or SQLDelight compiles for the first time in CI. Expect the first run to find something; bug-log it as usual and ship a 0.4.1 patch.
2. `VaultServiceTest` has never run.
3. Argon2id at 64 MiB and 3 iterations runs in Bouncy Castle's pure Java code. Unlock time and memory pressure on a low-end phone are unmeasured; this is the calibration item in 3b-1b.
4. The first Android lint run with Bouncy Castle on the classpath is unseen (see decisions).
5. The idle timer is reset by touch and key events only. Soft keyboard typing does not generate them, so every editing screen must call `VaultController.onInteraction()` on text changes (3b-2).
6. Nothing was tried on a device or an emulator.

## Next

Push 0.4.0, send the CI log. After a green run: 3b-1b (Keystore and biometric, calibration), then 3b-2.
