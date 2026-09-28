# Photo and attachment storage

Status: Proposed.

## Storage

- Files are stored in the app's private storage, one file per asset, named by asset id. No files are written to shared storage or the media provider.
- Each file is encrypted with the streaming AEAD construction under the key derived from the vault key for media. File name and path reveal nothing about the content.
- The database stores metadata only: mime type, size, plaintext hash, title, note, dates.

## Import

- Photos come from the system photo picker on Android and a file dialog on desktop.
- On import the image is decoded off the main thread, downscaled to a bounded maximum dimension, re-encoded, and location metadata (EXIF GPS) is removed by default. The maximum dimension and the option to keep the original are decided in Phase 3.
- Attachments other than images are stored as they are, subject to a size limit.

## Display

- A custom Coil fetcher decrypts to memory and decodes there. Decrypted files are not written to disk.
- Thumbnails are generated at import, encrypted the same way, and used in lists. Full images are decoded only when opened.
- Large images are sampled, never decoded at full size for a list.

## Lifecycle

- Replacing the primary photo changes `entity.primary_photo_id` only; the previous photo stays in the gallery unless the user removes it.
- Moving an entity to Trash keeps its files. Permanent deletion deletes the files and overwrites nothing beyond normal file removal; secure erase is not claimed.
- Orphaned files are found by a maintenance check that compares the media directory with the `media_asset` table.

## Sharing and export

Photos are included in shares and exports only when selected. PDF export embeds the primary photo only if the user includes it.
