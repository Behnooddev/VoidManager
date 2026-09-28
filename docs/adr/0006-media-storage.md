# ADR-0006: Photo and attachment storage

Status: Proposed

## Decision

Encrypted files in app-private storage, one file per asset, metadata in the database, decryption to memory for display, thumbnails generated at import. Details in `docs/media-storage.md`.

## Alternatives

- Storing images as database blobs: bloats the database, slows backups and migrations. Rejected.
- Plain files in app-private storage relying on OS sandboxing only: leaves photos readable after a device compromise or file extraction. Rejected.
- Storing in the system gallery: exposes private photos to other apps. Rejected.

## Consequences

- Image loading needs a custom decrypting fetcher.
- Backup and share must read and write through the media layer.
- Secure erase is not claimed.
