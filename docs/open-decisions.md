# Decisions

## Resolved

| # | Decision | Outcome | Date |
| --- | --- | --- | --- |
| 1 | Technology stack | Kotlin and Compose Multiplatform (ADR-0001) | 2026-09-29 |
| 2 | Android `minSdk` | 26 | 2026-09-29 |
| 3 | Share methods | QR code and Nearby, selectable by the user (`sharing.md`) | 2026-09-29 |
| 4 | Deep links | Custom scheme `voidmanager://` only in version 1. HTTPS app links are not planned | 2026-09-29 |
| 5 | Sender signing key | Not adopted in version 1. Sender name stays self-asserted | 2026-09-29 |
| 6 | Vault recovery key | Not adopted. Backups are the recovery path | 2026-09-29 |
| 7 | Gender field | Not included. Gendered relationship types fall back to neutral reciprocal types unless the user picks another type | 2026-09-29 |
| 8 | Password generator | Not included. A strength estimate for the master and backup passwords is included as a security control | 2026-09-29 |
| 9 | Android package identifier | `io.github.behnooddev.voidmanager` | 2026-09-29 |
| 10 | License | MIT | 2026-09-29 |
| 11 | Security reporting channel | Email listed in `SECURITY.md` | 2026-09-29 |
| 12 | UI language | English strings in resources. Layouts are right-to-left capable. Additional languages are added later | 2026-09-29 |

## Open

| # | Decision | Notes |
| --- | --- | --- |
| 13 | Desktop database encryption on Windows and macOS | Approach chosen and verified on Linux (ADR-0002). Windows and macOS runs are still needed before a desktop release |
| 14 | Backup file extension | `.vmbk` is the working name. Collision check is incomplete (`backup-format.md`) |
| 15 | XLSX and PDF libraries | Chosen in Phase 7 after a license and Android compatibility review |
| 16 | Navigation library | Phase 1 uses plain state. Chosen when nested navigation is needed (Phase 3): AndroidX Navigation Compose or Navigation 3 |
| 17 | In-app QR scanning implementation | Prefer a scanner that does not depend on Google services. Decided in Phase 8 |
| 18 | Typeface | Needs a licensed family that can be bundled. Platform default until then |
| 19 | Brand assets | Logo mark, banner and icon masters are needed under `assets/brand` (Phase 1.1) |
| 20 | Dependency verification metadata | Generated after the first successful build |
| 21 | Static analysis beyond Android lint | detekt is deferred until a release compatible with the Kotlin version is confirmed |
