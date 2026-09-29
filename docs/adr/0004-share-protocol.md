# ADR-0004: Share protocol

Status: Proposed

## Context

Sharing must work without a server and without placing personal data or usable secrets in a QR code, and the recipient must approve everything before import.

## Decision

Three layers: encrypted package, session bootstrapped by a QR code carrying only ephemeral public material and a single-use token, and a pluggable transport. Key agreement through HPKE with the token as pre-shared key. Sender approves each connecting device. Details in `docs/sharing.md`.

## Alternatives

- Put the encrypted package and its key in the QR code: limited to small payloads, and anyone who captures the image can decrypt. Rejected as the default.
- Encrypted package in the QR with a passcode shown separately: a short passcode allows offline guessing from a captured QR. Rejected.
- A relay server: contradicts the no-backend requirement. Rejected.
- HTTPS app links: require a verified domain and hosted files. Deferred (see open decisions).

## Consequences

- A transport is required to complete a share. The first one is an open decision.
- Custom-scheme links can be claimed by other apps; the design assumes the link is observable.
- Protocol version 1 is not frozen until implemented and tested.

## Amendment (2026-09-29)

The developer selected two user-selectable share methods: QR code and Nearby. The QR method uses the local network as its transport; the Nearby method uses Nearby Connections and requires Google Play services, so it is optional and hidden where unavailable. The decision on the first transport is closed. Branded QR rules are in `docs/qr-branding.md`.
