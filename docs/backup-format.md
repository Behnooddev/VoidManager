# Encrypted backup format

Status: Proposed. Backup format version 1 is a draft. The container, the encryption and the payload described below are implemented and tested in `core:backup` (0.9.0). Reading and writing the database, the screens and the file picker are not yet; see the implementation notes.

## File extension

Working name: `.vmbk`. A single web search for that extension found no registered file type; the search was not exhaustive, and several similar extensions (`.vbk`, `.vbm`, `.vmb`, `.vmba`) belong to other software. The extension is not fixed until a broader check is done and the result is recorded here. Restore identifies files by magic bytes, not by extension.

## Container

```
magic            4 bytes  "VMBK"
format_version   uint16
header_length    uint32
header           canonical JSON, UTF-8
payload          chunked authenticated encryption stream
```

Header fields:

- `format_version`, `schema_version`, `app_version`
- `kdf`: `argon2id`, `memory_kib`, `iterations`, `parallelism`, `salt`
- `cipher`: `aes-256-gcm-hkdf-streaming`, chunk size
- `created_at`
- `payload_kind`: `logical-v1`

The header is bound to the payload as associated data, so changing any header field makes decryption fail. The header does not contain names, counts or any personal information.

Truncation is detected: the streaming construction marks the final segment, and a missing final segment is an error.

## Payload (`logical-v1`)

The payload is compressed, then encrypted. It is a logical export, not a copy of the SQLite file:

- `manifest.json`: schema version, table list, row counts, per-entry SHA-256
- one file of records per table in a stable, versioned serialization
- media files as separate entries

A logical export was chosen over copying the database file because it can be validated, migrated between schema versions, and restored into a newer schema without carrying engine-specific state.

## Keys

The backup key is derived from a backup password with Argon2id. It is independent from the master password and from the vault key. The user can choose the same password, but the app does not assume it. Level 2 and 3 values are exported in decrypted form inside the backup payload and protected only by the backup key; they are never written in the clear outside it.

## Implementation notes (0.9.0)

- `BackupService.create` and `open` work on byte arrays in memory. A backup is limited to 256 MiB, and the payload to 128 MiB per file and 256 MiB in total after decompression. Streaming to and from files is a later change, needed when media is added.
- Header: canonical JSON with sorted keys. The format version is also in the fixed prefix so that a newer file is recognized before the header is parsed. `check` is a small AES-GCM value sealed under a key derived from the backup key; it lets the reader say "wrong password or the header was changed" separately from "damaged file".
- Keys: Argon2id over the NFKC-normalized backup password gives a 32-byte key. HKDF-SHA256 with a random 32-byte salt from the header derives three independent values: the check key, the stream key and a 7-byte nonce prefix. The salt is unique per file, so a nonce is never reused under a key.
- Chunks are 64 KiB. Each chunk's nonce is the prefix, a 32-bit counter and a last-chunk flag, and the whole header bytes are associated data of every chunk. Cutting chunks off, adding, repeating or reordering them, and changing any header byte all fail authentication.
- The key cost in the header is checked against the same limits as the vault key file (19 to 512 MiB, 2 to 10 iterations) before any derivation, so a hostile file cannot make the app allocate gigabytes.
- Payload: a ZIP archive with `manifest.json` and four JSON Lines tables (`definitions`, `entities`, `values`, `relationships`). The manifest holds each table's row count and SHA-256. The reader accepts exactly those five files, rejects names with a path or dots, and stops at the size and file-count limits while decompressing.
- Records are validated on read (types, protection levels, entity kinds, duplicate parts); a file that decrypts but fails validation is reported as invalid and nothing is restored.
- Not in this version: tags, groups and media (the app has no screens for them yet; the manifest and `payload_version` allow adding tables), restore into a vault that already has data (replace and merge), and a safety snapshot before a restore.
- The file extension is still a working name; see above.

## Restore

1. Read magic and header. Reject unknown `format_version` with a clear message.
2. Ask for the backup password. A wrong password and a corrupted file produce distinct messages where the construction allows it.
3. Decrypt and validate in a dry run: manifest, hashes, schema version, record validation. Nothing is written.
4. Show a summary: counts, backup date, app version, schema version.
5. Ask for the restore mode: restore into an empty vault, replace the current vault, or merge. Replace and merge require explicit confirmation.
6. Before replace or merge, create a safety snapshot of the current vault.
7. Apply in one transaction, migrating records from the backup's schema version to the current one.
8. Verify counts and report success or failure. On failure the previous vault is left as it was.

Existing data is never overwritten without confirmation, and an incompatible backup is never silently converted.

## Compatibility

Every released `format_version` remains readable. A fixture backup per format version, built from synthetic data, is kept in the test resources and restored in CI. Reader code migrates old payloads step by step to the current schema.

## Ordinary exports

CSV, XLSX and PDF exports are separate features and separate formats. They are plaintext, exclude level 3 by default, and let the user choose fields. Cells that begin with `=`, `+`, `-` or `@` are neutralized in CSV and XLSX.
