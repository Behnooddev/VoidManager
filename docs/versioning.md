# Versioning

Status: Proposed.

Each compatibility-sensitive artifact has its own version. One number is never used to hide an incompatible change.

| Artifact | Scheme | Source of truth |
| --- | --- | --- |
| Application | `MAJOR.MINOR.PATCH` (semantic versioning) | `version.properties` |
| Android version code | `MAJOR * 1000000 + MINOR * 1000 + PATCH` | derived in Gradle |
| Database schema | integer, incremented by every migration | SQLDelight migration files |
| Backup format | integer `format_version` in the header | `core:backup` constant |
| Share protocol | integer, sent in the QR and package | `core:sharing` constant |
| Export formats | identifier and integer per format (`csv-1`, `xlsx-1`, `pdf-1`) | `core:export` constants |
| Design system | internal integer, changed on token or component breaking changes | `core:designsystem` |

## Compatibility matrix

The matrix lives in `CHANGELOG.md` per release and lists: app version, schema version, backup format version, share protocol version, oldest backup format still readable, oldest share protocol still accepted.

## Rules

- A change to the database schema increments the schema version and adds a migration.
- A change to the backup payload that older readers cannot handle increments `format_version`. New readers keep reading old versions.
- A change to the share package or session that older apps cannot handle increments the protocol version. An app rejects unknown newer versions with a message asking the sender or recipient to update.
- A consistency check in CI fails when versions disagree across files or the changelog lacks an entry for the current version.
- Breaking changes are never released without release notes.

## Fix releases

Every change that fixes a defect, however small, is a patch release: the patch number in `version.properties` is raised, `CHANGELOG.md` gets an entry under Fixed, the defect is recorded in `bug-log.md` with cause, fix and prevention, and the handoff of the affected phase is updated. A fix is never merged silently into the previous version.
