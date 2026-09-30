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

## Spike result (Phase 2, 2026-09-29)

The desktop risk was checked with a real database. The `io.github.willena:sqlite-jdbc` artifact (release 3.53.4.0, project `Willena/sqlite-jdbc-crypt`) is a fork of the stock SQLite JDBC driver with the SQLite3 Multiple Ciphers extension. It uses the same `org.sqlite` package, so it replaces the stock driver and works with the SQLDelight JDBC driver.

Checked on Linux, JDK 21, with connection properties `cipher=sqlcipher`, `legacy=4` and `key=x'<64 hex digits>'`:

- the database file does not start with the plain SQLite header and does not contain inserted text;
- reopening with a different key fails with `SQLITE_NOTADB`;
- reopening with the right key reads the data;
- `PRAGMA foreign_keys` set through the connection properties is enforced;
- FTS5 virtual tables work in an encrypted database.

Findings that shape the code:

- The `hexkey` parameter did not encrypt anything in the properties and URL forms that were tried; the database was created in plain form without an error. The `key=x'...'` form works. The application therefore checks the file header after every open (`EncryptionCheck`) and refuses to continue on a plain file.
- The stock `org.xerial:sqlite-jdbc` artifact is excluded from every configuration in the root build, because both artifacts contain the same classes.

Not checked: Windows and macOS. The jar bundles their native libraries, but they were not run. Android uses `net.zetetic:sqlcipher-android` and was not run either. The desktop gate in `platform-strategy.md` remains until the Windows and macOS builds are tested on those systems.

## Consequences

- Shared SQL schema and migrations across platforms.
- A dependency on SQLCipher builds and their license terms, to be recorded when added.
- Encrypted database performance is measured in Phase 9.
