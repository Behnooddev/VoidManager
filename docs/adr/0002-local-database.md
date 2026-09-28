# ADR-0002: Local database

Status: Proposed

## Context

The database must be encrypted at rest, support search over normalized text, migrations that can be tested, and run on Android and the desktop JVM from shared code.

## Decision

SQLite accessed through SQLDelight. Full-database encryption with SQLCipher. Values of sensitivity level 2 and 3 are additionally encrypted per value (ADR-0003), so they stay protected even if database-level encryption is bypassed or misconfigured.

FTS5 provides search over level 0 and 1 normalized text only.

## Alternatives

- Room: Android-only until recently, less suited to shared desktop code. Rejected for the multiplatform goal.
- Realm and similar object databases: less transparent for migrations and search, larger dependency. Rejected.
- Plain SQLite with per-field encryption only: cannot protect the search index and level 0 and 1 data at rest. Rejected as the sole mechanism.

## Open risk

SQLCipher has an Android artifact. For the desktop JVM there is no first-party driver; options include a community JDBC build of SQLCipher or bundling native libraries. A spike in Phase 2 must produce a working, testable, maintainable approach on Windows, Linux and macOS. If none is found, options are to encrypt the database file at rest with an application-level scheme, or to change the search index design so no plaintext index is stored on desktop. Desktop release is blocked until this is resolved.

## Consequences

- Shared SQL schema and migrations across platforms.
- A dependency on SQLCipher builds and their license terms, to be recorded when added.
- Encrypted database performance is measured in Phase 9.
