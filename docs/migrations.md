# Migrations

Status: Proposed.

## Database

- Migrations are explicit SQL steps in SQLDelight `.sqm` files, one per schema version. There are no implicit or destructive auto-migrations.
- Startup sequence when the stored schema is older than the app:
  1. Validate the database (`PRAGMA integrity_check`, foreign key check).
  2. Copy the database file to a snapshot in private storage.
  3. Run the migration steps in a transaction.
  4. Verify: schema version, row counts of core tables compared with the pre-migration counts where they should match.
  5. Remove the snapshot after a successful verification and a successful next open; keep it on failure.
- On failure the app does not open the vault, keeps the snapshot, and shows an error stating that no data was changed and what the user can do.
- If the stored schema is newer than the app, the app refuses to open it and asks the user to update. There are no downgrade migrations.
- Data migrations that transform encrypted values (for example changing the associated-data layout) run only while the vault is unlocked and re-encrypt each value in the same transaction.

## Backup and share formats

- Readers convert older payloads step by step (`v1 -> v2 -> v3`), each step a pure function with tests.
- Fixture files for every released version stay in the test resources.
- A file from an unknown newer version is rejected with a clear message and is never partially imported.

## Testing

- Every migration has a test that starts from a fixture database of the previous version with synthetic data and checks the result.
- A chain test migrates from the oldest fixture to the current version.
- Migration failure paths (interrupted, corrupted, insufficient space) are tested for snapshot retention and unchanged data.
