# Password manager

Status: Proposed. The reveal, re-authentication, clipboard and secure-window rules below are implemented as of 0.7.0, except where a section says otherwise. Sharing and export rules belong to later phases.

## Scope

The password manager is the `account` field type plus the secret-handling rules below. It stores service, website, username, password and notes. Autofill, password generation and one-time-password codes are not in the specification and are not planned. See `open-decisions.md`.

## Storage

- `account` is a composite field value. Its `password` and recovery parts are level 3 and stored only as `value_cipher`.
- Username and password are separate parts. Username is level 1 and searchable; password is never indexed.
- Ciphertext is bound to its row and part through associated data.

## Read path

- Repositories return level 3 parts as a `SecretValue`, an opaque wrapper. It has no readable `toString`, is not serializable, and is not part of list or search models.
- Lists, search results, notifications and previews carry masked placeholders only.
- Revealing requires an unlocked session. For level 3 it also requires a recent authentication (biometric or master password) within a configurable window.
- The revealed value is held in UI state for a short, configurable time and is cleared when the screen loses focus, the app goes to background, or the app locks.

## Clipboard

- Copy happens only on explicit user action.
- Android: sensitive flag on API 33 and above; clear after timeout on API 28 and above; the user sees a message that a secret was copied and when it will be cleared.
- Desktop: clear after timeout when the clipboard content is still the copied value.
- Clipboard content is never logged.

## Implementation notes (0.7.0)

- Showing and copying go through `SecretAccess` (`app:shared`): level 2 needs an unlocked vault, level 3 also needs `ReAuthWindow` to be fresh. The window, the reveal time and the clipboard delay are settings (`AppSettings`).
- Re-authentication is the master password, checked with `VaultGateway.verifyPassword` (same cost as an unlock, wrong attempts share the unlock wait), or the biometric prompt when device unlock is on.
- The editor never displays level 3 text. A level 3 field opens empty and an empty field keeps the stored text, so no re-authentication is needed to replace a value and none is skipped to see one.
- Android: the sensitive flag is set on API 33 and above. After the delay the clipboard is cleared only if its label is the one this copy set, because the text cannot be read from the background. Clearing runs only while the process is alive; if the app is closed first, the text stays. The settings screen says so. API levels below 28 clear by overwriting the clipboard with an empty clip.
- Desktop: after the delay the clipboard is cleared if it still holds the copied text.
- Lists, search results and previews carry no level 3 text; `FieldText` prints a mask and `FieldValueInput` prints no text, and `LoggingTest` asserts both.

## Screens

Screens showing secrets set the secure window flag on Android so they are excluded from screenshots and the recent-apps preview.

## Sharing and export

- Level 3 values cannot be added to a share package. The package builder rejects them; this is enforced in `core:sharing`, not only in the UI.
- Ordinary exports exclude level 3 values by default. Including them requires an explicit choice with a confirmation. Encrypted backups include them, encrypted under the backup key.

## Logging

Passwords, PINs and CVV2 values are never logged. Logging types mask them; a test asserts that formatted log output for a `SecretValue` contains no secret.
