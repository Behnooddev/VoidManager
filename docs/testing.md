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

## Tests in the repository now

| Module | Tests | Ran where |
| --- | --- | --- |
| `core:common` | logging facade | Authoring environment and CI (passed) |
| `core:model` | id generation, field registry, relationship types, masked text | Authoring environment and CI (passed) |
| `core:data` (logic) | text and phone normalization, sensitivity and sharing rules | Authoring environment |
| `core:database` (pure) | migration planning, key formatting, encryption check | Authoring environment |
| `core:crypto` | Argon2id, HKDF and AES-GCM against published vectors; key file; unlock, lock, password change, device unlock; tampering | Authoring environment (39 tests) and CI (passed) |
| `core:security` | password policy, auto-lock, re-authentication window, reveal timer | Authoring environment (20 tests) and CI (passed) |
| `core:crypto` integration | Key hierarchy with the encrypted database and repositories | CI (passed; it runs inside `:core:crypto:test`) |
| `core:database` (JVM) | encrypted vault create, reopen, wrong key, newer schema | CI (passed) |
| `core:data` (JVM) | repositories against an encrypted vault | CI: did not compile in 0.3.0 (BUG-004); passes since 0.3.1 |
| `app:shared` lock flow | state machine, throttle, setup form, controller | Authoring environment (40 tests) and CI (passed) |
| `app:shared` people | routes, field validation, quick add, profile building, model | Authoring environment (41 tests); CI passed up to 0.5.0 |
| SQL schema | constraints, cascades, all queries | Ran against SQLite in the authoring environment, outside Gradle |

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

## QR branding tests (Phase 8)

- The encoder matrix is identical with and without a logo; only the rendered image differs.
- A branded QR decodes to the exact payload at full size, half size, quarter size and with a light blur, in both presentations.
- The logo never covers finder patterns, separators, timing patterns or format and version information.
- Oversized logos are reduced step by step, and a plain QR is used when the floor size still fails.
- A QR that fails validation is never displayed.
- The QR payload parses to the bootstrap fields only.

## Things that cannot be verified in every environment

Keystore-backed behavior, biometrics, deep link dispatch and clipboard behavior require an Android emulator or device. Each phase report states which tests ran where.
