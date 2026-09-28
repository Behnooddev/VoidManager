# Password manager

Status: Proposed.

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

## Screens

Screens showing secrets set the secure window flag on Android so they are excluded from screenshots and the recent-apps preview.

## Sharing and export

- Level 3 values cannot be added to a share package. The package builder rejects them; this is enforced in `core:sharing`, not only in the UI.
- Ordinary exports exclude level 3 values by default. Including them requires an explicit choice with a confirmation. Encrypted backups include them, encrypted under the backup key.

## Logging

Passwords, PINs and CVV2 values are never logged. Logging types mask them; a test asserts that formatted log output for a `SecretValue` contains no secret.
