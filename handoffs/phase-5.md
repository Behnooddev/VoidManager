# Handoff: Phase 5 (secrets: reveal, re-authentication, clipboard, timing settings)

Date: 2026-10-10. Version 0.7.0.

## Scope

The rules in `docs/security/password-manager.md` for showing, copying and editing protected values, and the settings that control their timing. A password generator, autofill and one-time codes are not planned (`docs/open-decisions.md`). Sharing and export rules for level 3 values belong to the sharing phases.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `SecretAccess` (reveal time, one value at a time, re-authentication window) | Tested | Uses the existing `ReAuthWindow` and `RevealController` |
| `SecretsModel` (reveal, copy, pending action after re-authentication) | Tested | Against a stand-in for Compose state |
| `AppSettings`, `AppSettingsHolder`, `SettingsStore` | Tested | Damaged values fall back to defaults |
| `VaultGateway.verifyPassword`, `VaultFlow` and `VaultController` re-authentication | Tested; service test written, not run | `VaultServiceTest` runs in CI |
| Editor keeps level 3 text off screen (`CompositeInput.merged`) | Tested | |
| `FieldValueInput.toString` prints no text; `LoggingTest` | Tested | |
| Profile Show, Hide and Copy; re-authentication prompt; Settings sections | Written, not built | Compose |
| `AndroidSecretClipboard`, `SharedPreferencesSettingsStore`, desktop equivalents | Written, not built, not run | |

## Decisions taken

- Level 3 means the part or value sensitivity, not the sensitivity of the surrounding field: a card is level 2 but its CVV2 and PIN are level 3.
- Replacing a level 3 value needs no re-authentication, because the editor never shows the old text; an empty level 3 field keeps the stored text. The cost is that a single level 3 part cannot be cleared without deleting the value.
- Wrong re-authentication passwords share the throttle with unlock, so the prompt cannot be used to guess the password faster than the lock screen allows.
- Copy of a level 3 value needs re-authentication like showing it does.
- Settings are stored outside the vault (private preferences on Android, a properties file on desktop) and every value must be one of a fixed set of choices. A lock timer must work before unlock, and a missing or edited value must not turn a protection off.
- Clipboard clearing compares a unique label, not the text, because the clipboard text cannot be read from the background on Android 10 and later. It runs only while the process is alive; the settings screen states this. A copy that is not clearable on exit was preferred over clearing when the vault locks, because the app locks as soon as it goes to the background, which would erase the text before it could be pasted into another app.
- The reveal state, the re-authentication window and any waiting action live in a `SecretsModel` that is created with the unlocked vault and cleared when it locks.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 116 `app:shared` and `core:model` tests together (35 new) | Kotlin 2.4.20 compiler, outside Gradle, with a stand-in for Compose state | 116 passed |
| Every string used is defined and imported; no unused strings | Script over the sources | No gaps |
| ktlint 1.5.0 on every file | Authoring environment | Passed |

## Not verified and known risks

1. The Compose screens, the Android clipboard and the settings stores have not been compiled; Compose and Android code cannot be built outside Gradle. 0.6.0 is also still waiting for its CI run, so a first error could come from either release.
2. The clipboard behavior is untested on any device. Check on API 33 and above: copy a card number and confirm the clipboard preview hides it; wait for the delay and confirm that pasting gives nothing; copy a second value before the delay and confirm the second one is not cleared early. Check on a device below API 33 if one is available.
3. Android may freeze a cached process before the delay runs. Confirm what happens when the app is left in the background for the whole delay.
4. `ClipboardManager.getPrimaryClipDescription` is expected to be readable in the background; if the label cannot be read, the clipboard is not cleared. Confirm on a device.
5. Biometric re-authentication uses the same prompt as device unlock and proves presence only; it does not bind the revealed value to the prompt.
6. The re-authentication window and reveal time are not shown on screen while they run.

## Next

Push 0.7.0 (together with 0.6.0 if it has not been pushed) and send the CI log. Then the clipboard and re-authentication checks on a device, then phase 6 (search, relationships, smart actions) or phase 4b.
