# Threat model (outline)

Status: Proposed. To be reviewed before the security foundation is implemented (Phase 5 begins only after this review).

## Assets

- Personal data of the user and the people they store (contact details, addresses, notes, photos, attachments).
- Level 2 data: national ID, financial information, private documents.
- Level 3 data: passwords, PINs, CVV2, recovery secrets.
- Key material: master password, vault key, device key, backup passwords.

## Adversaries and scenarios

| Scenario | Protection | Limits |
| --- | --- | --- |
| Stolen device, locked | Full-database encryption, Keystore-bound unlock, per-value encryption for levels 2 and 3 | Depends on the OS lock screen and hardware keystore quality |
| Stolen device, unlocked or shared with another person | Auto-lock, re-authentication for level 3 reveal, secure window flag | An unlocked session exposes what the user can see |
| Malware or root on the device | Not defended | Compromised OS can read memory, screen and keystrokes |
| Copied database file | SQLCipher plus per-value encryption | Strength of the master password governs offline attack cost |
| Backup file obtained by an attacker | Argon2id-derived key, authenticated encryption, no plaintext metadata beyond format header | Weak backup passwords can be attacked offline; the setup flow enforces a minimum and shows an estimate |
| Malicious import file (CSV, XLSX, backup) | Size limits, streaming parse, schema validation, no execution, decompression limits | Parser dependencies remain an attack surface and are kept updated |
| Malicious share package | AEAD integrity, protocol and schema validation, size and count limits, staged in memory, nothing imported without confirmation, no automatic opening of links | Sender identity is self-asserted unless signing is adopted |
| Interception of a share QR or deep link | QR holds only ephemeral public material, a single-use token and a transport descriptor; sender approves the connecting device; short expiry | Another app registered for the same custom scheme can see the QR contents; approval and confirmation code limit the impact |
| Clipboard exposure | Explicit copy, sensitive flag, timed clear | Other apps or clipboard managers can read the clipboard on older Android versions |
| Screenshot or screen recording | Secure window flag on sensitive screens | The flag does not stop a camera pointed at the screen |
| Logs and crash reports | Masked types, no telemetry, no crash upload, lint rule against direct logging | Developer builds may log more; they are not distributed |
| Formula injection through exported cells | Cells starting with `=`, `+`, `-`, `@` are neutralized in CSV and XLSX | Behavior in third-party spreadsheet software varies |
| Compromised dependency | Pinned versions, checksum verification, dependency audit in CI, minimal dependency set | Cannot eliminate supply-chain risk |
| Modified key file (lower cost parameters, huge memory request) | Cost parameters are bound into the wrap as associated data and range-checked before any derivation runs | An attacker who can rewrite the file can still delete it; the vault is then unreachable without a backup |
| Weak master password | Minimum length and strength estimate | The user can still choose a poor password above the minimum |
| Coercion or shoulder surfing | Not defended beyond masking | Out of scope |

## Not protected

- A compromised or rooted operating system.
- Screen capture by hardware, or by accessibility services granted by the user.
- Data the user exports to plaintext files.
- Loss of all copies of the master password when no backup exists.

## Depends on the operating system

- Keystore and StrongBox behavior, biometric quality, screen lock strength.
- Clipboard behavior and clearing support.
- App sandboxing and file permissions.
- Deep link dispatch.

## Review items

- Choice and validation of the desktop database encryption approach (ADR-0002).
- HPKE and token construction for share sessions (ADR-0004).
- Argon2id parameter floor and the master password policy.
- Behavior of the app when the Keystore key is invalidated.
