# QR code branding

Status: Proposed. Implemented in Phase 8. Nothing in this document exists as code yet.

## Requirements

1. The QR data stays standards-compliant (ISO/IEC 18004).
2. The centred logo never contains sensitive data.
3. Branded QR codes use an appropriate error correction level.
4. Enough clear modules are preserved around the logo.
5. The required quiet zone is preserved.
6. The logo stays recognizable without preventing reliable scanning.
7. The generator validates the QR after the logo is applied.
8. When a branded QR is unreliable, the generator reduces the logo size, and falls back to a plain QR if needed.
9. QR patterns are never faked or edited by hand.
10. The QR contains only the intended share payload or reference: never plaintext passwords or sensitive profile data.
11. A VoidManager-branded dark and light QR presentation is supported.
12. The same official logo mark is used for the app icon, QR sharing, website, exports and application branding.

## What the QR contains

Only the share bootstrap described in [sharing.md](sharing.md): protocol version, ephemeral public key, single-use token, expiry and transport descriptor. Nothing about the person being shared, and no field values.

## Construction

The module matrix comes from a standard encoder (ZXing core, Apache-2.0) and is never modified. The logo is drawn as a separate layer over the rendered image. The encoded payload is therefore identical with and without the logo, and recovery of the covered modules relies on Reed-Solomon error correction, not on any change to the matrix.

| Parameter | Value |
| --- | --- |
| Error correction level | H (recovers about 30% of codewords) for branded codes. Plain fallback codes may use a lower level if the payload needs it, but the same level rules apply to validation. |
| Quiet zone | 4 modules on every side, filled with the plate colour, never transparent |
| Logo start size | 22% of the symbol width |
| Reduction step | 2 percentage points |
| Logo floor | 12% of the symbol width; below this the code is rendered plain |
| Clear border around the logo | The plate extends at least 1 module beyond the logo on every side and is aligned to the module grid |

Rules for what the logo may cover:

- Never covered: finder patterns and their separators, timing patterns, format information, version information.
- May be covered when validation passes: alignment patterns and data modules.

The values above are starting points. They are confirmed or changed by the validation tests on real devices in Phase 8.

## Validation

After rendering, the generator decodes its own output with an independent decoder path (ZXing reader) and requires an exact byte match with the intended payload at:

1. the rendered size,
2. half size,
3. quarter size,
4. the rendered size with a light box blur, to approximate a soft camera image.

If any check fails, the logo size is reduced by one step and all checks run again. At the floor size the generator produces a plain QR and validates that. If the plain QR fails as well, no QR is shown and the user gets an error that names the problem. A code that has not been validated is never displayed.

## Presentations

| Presentation | Modules | Plate | Use |
| --- | --- | --- | --- |
| Light (default) | Navy `#0D1220` | White `#FFFFFF` | Standard polarity. Read by every scanner. In the dark theme the plate appears as a bright card. |
| Dark | Light `#E8ECF5` | Deep navy `#07090F` | Inverted polarity. Matches the dark theme. Some scanners, including some system camera apps, do not read inverted codes; the in-app scanner does. |

Rules for both:

- Contrast between modules and plate must be at least 7:1.
- The dark presentation is validated with the reader's inverted-image mode enabled.
- The default is Light. The choice is a setting under Sharing, and the dark presentation shows a short note about scanner compatibility.
- Accent colours and glow are allowed on the card around the plate, never on the modules or the quiet zone.

## Logo asset

- One source: `assets/brand/logo-mark.svg`. The app icon, QR logo, website, PDF exports and in-app branding are derived from it. See `assets/brand/README.md`.
- The QR uses the same mark shape. A single-colour version of the same shape is used when the full-colour version lacks contrast on the plate.
- The logo is a bundled resource. It is never user-supplied and never a profile photo.

## Implementation notes

- `core:sharing` defines the payload and a `QrImageRenderer` interface in common code.
- The renderer and validator are JVM code shared by Android and desktop.
- The screen that shows the QR uses the validated image only.
