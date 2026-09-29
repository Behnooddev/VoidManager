# Brand assets

`logo-mark.svg` is the single source for the VoidManager mark. Every other use is derived from it: launcher icons, desktop icons, the QR logo, PDF exports, the website and in-app branding. The mark is not redrawn per use.

## Files

| File | Purpose | Requirements |
| --- | --- | --- |
| `logo-mark.svg` | Master, full colour | Square viewBox, transparent background, no text, no embedded raster images, no external references |
| `logo-mark-mono.svg` | Single-colour version of the same shape | Same geometry as the master. Used for the Android themed icon, notification icon and the QR logo when contrast requires it |
| `app-icon-foreground.svg` | Android adaptive icon foreground | 108 x 108 unit canvas, mark inside the central 66 unit circle |
| `app-icon-1024.png` | Master for desktop icons | 1024 x 1024, opaque background |
| `banner.png` | README banner and GitHub social preview | 1280 x 640 (2:1) |
| `logo-wordmark.svg` | Mark with the name, optional | Text converted to outlines so no font is needed |

## Derived files

Generated from the masters and committed with the module that uses them:

- Android: vector drawables for the adaptive icon (foreground and monochrome layers) and the background colour.
- Desktop: `.ico` (16, 32, 48, 64, 128, 256), `.icns` and a 512 px `.png`.
- Website: favicon set and social image.

## Rules

- Do not use a photo, a user image or any personal data as a logo.
- The mark keeps a clear space of at least one quarter of its height on every side.
- The mark is not stretched, recoloured outside the palette in `docs/design-system.md`, or given effects.
