# ADR-0003: Key management and cryptographic libraries

Status: Proposed

## Context

The app needs password-based key derivation, authenticated encryption for values and files, key derivation from a vault key, biometric-gated unlock, and key agreement for sharing, without custom cryptography.

## Decision

- A random vault key wrapped by a password-derived key and, optionally, by a device-bound key.
- Argon2id through Bouncy Castle behind a `KeyDerivation` interface; AEAD, streaming AEAD, HKDF and HPKE through Tink.
- Per-value encryption with associated data binding ciphertext to its table, row and part.
- Details in `docs/security/key-management.md`.

## Alternatives

- Deriving the data key directly from the master password: changing the password would require re-encrypting all data. Rejected.
- libsodium bindings for everything: strong primitives, but multiplatform and Android packaging adds native complexity. Kept as an alternative for Argon2id.
- Platform crypto APIs only: Argon2id is not available in standard Android or JDK APIs.

## Consequences

- Password change is cheap; vault key rotation is a separate operation.
- Two crypto libraries to keep updated, both maintained.
- Bouncy Castle's Argon2 is a pure-JVM implementation; performance on low-end devices is measured before the parameter floor is fixed.
