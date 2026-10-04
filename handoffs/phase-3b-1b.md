# Handoff: Phase 3b-1b (biometric unlock and cost calibration)

Date: 2026-10-03. Version 0.4.1.

## Scope

Biometric unlock on Android through the Keystore, a Settings screen to turn it on and off, and Argon2id cost fitted to the device at vault creation. The People, Personal and trash screens are still 3b-2 (0.5.0).

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `KdfCalibrator` (`core:crypto`) | Tested | 7 tests passed outside Gradle against a stand-in for the hash |
| `VaultManager.create(password, params)`, `deviceUnlockEnabled()` | Written | Covered by the new service tests, not yet run |
| `VaultService`: device unlock, disable, calibrated create | Tests written, not run | 4 new tests in `VaultServiceTest` on a real encrypted file |
| `VaultFlow` / `VaultController` device paths | Flow tested | 28 flow tests passed outside Gradle; the controller (Compose state) is untested |
| `DeviceKeyProvider` interface (`app:shared`) | Written | |
| Unlock button and Settings screen | Not built | Compose; first compile is CI |
| `AndroidDeviceKeyProvider` (Keystore + `BiometricPrompt`) | Not built, not run on a device | |

## Decisions taken

- The Keystore key never leaves the Keystore. The vault's device key is separate random bytes stored encrypted by it (`device.key`), because the existing key file format wraps the vault key under raw 32-byte key material.
- Strong biometrics only, authentication for every operation, key invalidated on biometric enrollment changes. No screen-lock fallback.
- A failed or rejected device key removes the slot instead of leaving a dead button. Biometric failures do not feed the password throttle.
- Calibration changes iterations only (memory is lowered only when two iterations exceed 3 seconds). Target 700 ms and ceiling 3 s are provisional.
- `MainActivity` extends `FragmentActivity` because `BiometricPrompt` needs one.
- Library versions: `androidx.biometric` 1.1.0 (stable; the repository's development line is at 1.4.0-beta01) and `androidx.fragment` 1.8.6. Neither coordinate was looked up on Google's Maven (not reachable from the authoring environment); correct on the first resolution error.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 7 calibrator tests | Kotlin 2.4.20 compiler, outside Gradle | 7 passed |
| 28 lock-flow tests (23 earlier plus 5 device-unlock) | Same | 28 passed |
| ktlint 1.5.0 on every file | Authoring environment | Passed |
| Biometric and Keystore API names and constants | Read against the androidx source in the authoring environment | Match |

## Not verified and known risks

1. No Gradle build here. The Compose and Android code compiles for the first time in CI. The 0.4.0 run was green on its first attempt, which lowers the risk but does not remove it.
2. Nothing has run on a device. Keystore key generation, StrongBox fallback, the prompt, invalidation after a new fingerprint, and the unlock button have never been exercised. Check these before trusting the feature: turn it on, lock, unlock with a fingerprint; add a fingerprint in system settings and confirm the app falls back to the password with the explanation.
3. `fragment` 1.8.6 with the `activity` 1.10.1 pulled in by Compose is expected to be compatible but is unseen.
4. The calibration constants are guesses until measured. Create a vault on at least one low-end and one recent phone and note the unlock time.
5. The idle timer can lock the vault while the biometric prompt for turning the feature on is open; the enable then fails and the user retries.
6. Changing the password does not recalibrate. There is no password-change screen yet.

## Next

Push 0.4.1 and send the CI log. After a green run, and ideally a quick device check, start 3b-2 (People, Personal, Quick Add, trash) as 0.5.0.
