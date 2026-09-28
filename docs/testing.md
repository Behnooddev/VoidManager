# Testing strategy

Status: Proposed.

## Layers

| Layer | Scope | Tools |
| --- | --- | --- |
| Unit | Pure logic in shared modules: normalization, field rules, relationship engine, merge engine, policy checks | kotlin.test, Turbine for Flow |
| Property-based | Merge, normalization, migration round trips | Kotest property testing |
| Repository / database | Repositories against a real SQLite (encrypted) database on the JVM | kotlin.test, SQLDelight JVM driver |
| Crypto | Known-answer tests (RFC 9106 Argon2id vectors, published AEAD vectors), tamper and truncation tests, wrong-key tests | kotlin.test |
| Migration | Every schema step from every released version, using fixture databases | SQLDelight migration verification, fixtures |
| Format compatibility | Backup and share fixtures per format version restored/parsed in CI | fixtures under `core/testing` |
| UI | Compose UI tests for key flows and accessibility semantics | Compose test APIs |
| Instrumented (Android) | Keystore, biometric wrapper, deep link intake, clipboard, secure flag | AndroidX test on emulator |
| Desktop | Window, keyboard and installer smoke tests | JVM tests, manual checklist per release |

## Required behavior coverage

Person creation and editing, quick add, detailed profile, custom fields, multiple values, relationships and reciprocals, search including normalization, favorites, tags, trash, restore, permanent deletion, export, import, encryption, backup, restore, password handling, app lock, share package creation, share validation, incoming share, conflict handling, migrations.

## Security-focused tests

- Ciphertext moved to another row or part fails authentication.
- Truncated or modified backups and share packages are rejected.
- Oversized, malformed and decompression-bomb inputs are rejected without exhausting memory.
- Level 3 values are absent from search results, list models, logs, share packages and default exports.
- After lock, no key material is reachable through the session object, and repositories refuse level 2 and 3 reads.
- Formula-injection strings are neutralized in CSV and XLSX output.

## Data rules

All test data is synthetic. Phone numbers use reserved fictional ranges, card numbers use published test numbers, no real names, addresses or documents.

## Things that cannot be verified in every environment

Keystore-backed behavior, biometrics, deep link dispatch and clipboard behavior require an Android emulator or device. Each phase report states which tests ran where.
