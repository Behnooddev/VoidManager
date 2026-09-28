# ADR-0005: Backup format

Status: Proposed

## Decision

Versioned container with a small authenticated header and a chunked, authenticated, encrypted payload. Payload is a logical export validated by a manifest. The backup key comes from a separate backup password through Argon2id. Details in `docs/backup-format.md`.

## Alternatives

- Encrypted copy of the SQLite file: simple, but ties backups to the storage engine and schema, and makes restore into a newer schema harder to validate. Rejected.
- Encrypting a ZIP with password-based ZIP encryption: weak schemes and inconsistent support. Rejected.

## Consequences

- Restore validates before writing anything.
- Every released format version needs fixtures and a reader.
- The file extension is not fixed yet.
