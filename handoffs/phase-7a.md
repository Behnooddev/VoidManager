# Handoff: Phase 7a (encrypted backup format)

Date: 2026-10-12. Version 0.9.0.

## Scope

The backup file: container, key derivation, encryption and payload, as a module of its own. Reading the vault into a backup, restoring one, the screens and the file picker are 7b. Export to CSV, XLSX and PDF and import are 7c.

The phase was split this way on purpose. The format and its cryptography can be tested completely without the database or Android, and a mistake in them is a security mistake, so they were built and tested first and apart from the data layer.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `core:backup` module (`MiniJson`, HKDF, chunked AES-GCM, `BackupService`, `LogicalPayload`, snapshot types) | Compiled and tested outside Gradle | 47 tests passed |
| Module wiring (`settings.gradle.kts`, build file, CI test and assemble steps, Windows and macOS job) | Written, not run | |
| `Argon2idBackupKdf` and `BackupWithArgon2idTest` (`core:crypto`) | Written, not built | Needs Bouncy Castle, which is not available in the authoring environment |
| `docs/backup-format.md` implementation notes, roadmap split | Written | |

## Decisions taken

- A backup is a logical export (ZIP of JSON Lines), not a copy of the database file, as `docs/backup-format.md` already said. JSON was used for the records because it needs no new dependency and can be validated line by line; the small reader in `MiniJson` accepts only what the format uses.
- The password check value is stored in the header so the app can say "wrong password" separately from "damaged file". It does not weaken the file: it is sealed under a key derived from the password hash, so an attacker gains nothing over trying a password on the payload.
- The header is not secret and holds no personal data; it is authenticated with every chunk instead of being encrypted, so a reader can show the date, app version and format before asking for a password.
- The key cost is read from the header, so the limits are checked before deriving anything.
- Whole files are handled as byte arrays, with a 256 MiB limit. This keeps the API simple for this phase; streaming is needed only when media is added.
- The backup password is independent of the master password. NFKC normalization matches the master password rules, so the same words typed on another keyboard or system still match.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 47 `core:backup` tests (JSON subset, RFC 5869 vector, chunk cutting, reordering and bit flips, header tampering, hostile costs, compression bomb, no plaintext in the file) | Kotlin 2.4.20 compiler and the JDK, outside Gradle, with PBKDF2 as a fast stand-in for Argon2id | 47 passed |
| ktlint 1.5.0 on every file | Authoring environment | Passed |

## Not verified and known risks

1. Gradle has not built the new module, and the Argon2id adapter and its test have not been compiled. The adapter calls `Argon2idKdf.derive(password, salt, KdfParams)` as the key calibrator does; a mismatch would be a compile error in CI.
2. 0.6.0, 0.7.0, 0.8.0 and this release have not been seen by CI. Errors from any of them may appear in the first run; the file name says which.
3. The tests use PBKDF2 as the password hash. The format code does not depend on which hash it is, and `BackupWithArgon2idTest` covers the real one in CI, but a bug that appears only with Argon2id output would be found there first.
4. The 256 MiB limit and the in-memory handling mean a very large vault would fail with "too large" rather than stream. There is no media in the vault yet.
5. There is no safety snapshot, replace or merge: 7b restores into an empty vault only. The user cannot yet get data back from a backup through the app; until 7b, a backup file cannot be created from the app either.

## Next

Push the releases and send the CI log. Then 7b: read the vault into a snapshot (definitions with overrides, entities including trashed ones, values with parts, relationships) and restore into an empty vault in one transaction, preserving ids and timestamps; backup and restore screens; the Android file picker. That needs new repository methods that write rows with given ids, which is why it waits for the data layer to have been through CI.
