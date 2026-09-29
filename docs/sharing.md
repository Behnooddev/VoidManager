# Sharing

Status: Proposed. Share protocol version 1 is a draft; it is not frozen until implemented and tested.

## Requirements carried over

- Selective: the sender chooses fields per share, after a review step.
- The QR code never contains personal data or secrets.
- The recipient reviews before anything is stored, and decides between create, update and merge.
- No VoidManager server. No account.

## Share methods

The sender picks the method on the share screen, after the review step. Two methods are supported.

| Method | How the devices connect | Availability |
| --- | --- | --- |
| QR code | The sender shows a QR code and the recipient scans it. The data moves over the local network: the same Wi-Fi network, or a hotspot started on either phone. | Android and desktop. Does not need Google Play services. |
| Nearby | The devices find each other through Nearby Connections. No QR code is involved. Both screens show a confirmation code that the two users compare before the sender approves. | Android only, and only on devices with Google Play services. The option is hidden on other devices, with a short explanation. |

Both methods use the same package format, the same approval step and the same validation on receive. A method consists of a bootstrap (how the recipient learns how to connect) and a `ShareTransport` (how bytes move). Adding a method later does not change the package or the incoming share screen.

Nearby permissions are requested when the user starts a Nearby share, not at install or first launch. The permission list is taken from the current Nearby Connections documentation when the method is implemented.

The QR code is rendered with the VoidManager logo in the centre. The rules for that are in [qr-branding.md](qr-branding.md).

## Layers

1. Package: a versioned, encrypted container holding the selected data.
2. Session: how two devices agree on a key and confirm each other, bootstrapped by a QR code.
3. Transport: how bytes move between the devices. Pluggable.

## Package (v1)

Contents:

- protocol version and schema version
- sender display name and creation time (self-asserted)
- entity payload: selected field values with labels, notes, sensitivity, part structure
- selected photos and files
- manifest with per-item SHA-256
- limits: maximum decompressed size, item count and per-item size, enforced on read

Level 3 values are never included. Level 2 values are included only when the sender allowed them for this share.

The package is compressed, then encrypted with AEAD under the session key. Integrity comes from the AEAD tags plus the manifest hashes.

## Session (v1)

In the QR method the QR code carries:

- protocol version
- an ephemeral public key of the sender
- a random single-use token of at least 128 bits
- an expiry time (default a few minutes)
- a transport descriptor (transport id and connection hints)

It carries no names, field counts or categories.

Handshake:

1. Sender creates the QR after the review step and waits.
2. Recipient scans the QR and opens `voidmanager://share/v1/...`; the app parses and validates it.
3. Recipient generates an ephemeral key pair and connects through the transport, presenting its public key and proof of the token.
4. Sender shows the connecting device and a short confirmation code derived from the handshake transcript. The sender approves.
5. Sender encrypts the package with HPKE to the recipient's ephemeral key, using the token as pre-shared key, and sends it.
6. Recipient decrypts into memory only and shows Incoming Share.
7. The token is invalidated after first use or expiry.

The exact HPKE mode and encoding are confirmed in a Phase 8 spike against the Tink API and against test vectors.

## Deep link

- Scheme: `voidmanager://share/...` declared in the Android manifest and registered by the desktop installer.
- The link contains only what the QR contains.
- A custom scheme can be claimed by other apps. The design assumes the link is observable: it holds no secret that is sufficient on its own, and the sender approves each connecting device.
- HTTPS app links would need a verified web domain and a hosted assets file. Nothing is hosted for now. See `open-decisions.md`.
- When the app is not installed, scanning has no effect. No fallback server is planned.

## Transports

| Transport | Used by | Notes |
| --- | --- | --- |
| Local network | QR method | The sender listens on an ephemeral port on its Wi-Fi or hotspot address, which is placed in the transport descriptor. Both devices must be on the same network. When the address is not reachable, the screen tells the user to start a hotspot on one of the phones. |
| Nearby Connections | Nearby method | Discovery, connection and byte stream come from Nearby Connections. Its own authentication is not relied on: the session below runs on top of it and produces the confirmation code. |

In the Nearby method there is no QR code and therefore no token. The pre-shared key is absent and the ephemeral key exchange is authenticated by the users comparing the confirmation code on both screens, which is derived from the handshake transcript. The sender cannot approve until the code is shown.

An encrypted share file (a package sent through any channel the user chooses) stays a possible later method. It is not part of version 1.

## Incoming Share screen

Shows: sender name, person name, number of fields, whether photos are included, whether account or social data is included, sensitive categories present, and the security status of the session. Actions: Review, Ignore. Nothing is written to the database before the user confirms a choice.

Review shows every field with its label and value. Level 2 values are masked until the user chooses to display them.

## Match and merge

Candidate matching on receive:

- exact normalized phone or email match: strong
- `share_origin` match from an earlier share: strong
- normalized name equality or high similarity: weak, shown as a suggestion only

Choices: Create new, Update existing, Review and merge. There is no silent merge.

For each conflicting field (same field type, different value) the user picks incoming, existing or keep both. Fields present only on one side are added. Every merge result is previewed before commit and applied in one transaction.

## Sender identity

Sender name in the package is self-asserted. An optional per-install signing key (trust on first use) would let a recipient recognize repeated senders. It expands scope and is listed in `open-decisions.md`.

## Validation on receive

Protocol version supported, schema known, AEAD verified, sizes and counts within limits, field types known, media type and size checks, hashes match, text is treated as plain text and never rendered as markup, links are shown but never opened automatically.
