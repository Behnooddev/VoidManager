# Open decisions

Items that are pending, need confirmation, or are not covered by the specification. Resolved items move to an ADR.

| # | Decision | State | Notes |
| --- | --- | --- | --- |
| 1 | Technology stack | Resolved | Kotlin and Compose Multiplatform. ADR-0001 |
| 2 | Android `minSdk` | Proposed 26 | Excludes older devices. See platform-strategy.md |
| 3 | Desktop database encryption approach | Open | Phase 2 spike. Blocks desktop release. ADR-0002 |
| 4 | First share transport | Open | Local network, Nearby Connections, or encrypted file. Nearby needs Google Play services |
| 5 | HTTPS app links for share | Open | Needs a verified domain and hosted file; no domain is committed |
| 6 | Sender signing key (trust on first use) | Open | Not in the specification; expands scope |
| 7 | Recovery key for the vault | Open | Not in the specification; affects the key hierarchy |
| 8 | UI languages and right-to-left support | Open | Affects layout and resources from Phase 1 |
| 9 | Gender field | Open | The specification allows it only if the approved design includes it; affects reciprocal relationships |
| 10 | Password generator and strength meter | Open | Not in the specification. A strength estimate for the master and backup passwords is planned as a security control |
| 11 | Android package identifier | Open | Needs an identifier that the project owns |
| 12 | Backup file extension | Open | `.vmbk` is the working name; collision check incomplete |
| 13 | License | Pending | No license selected |
| 14 | Security reporting channel | Pending | No contact exists yet; SECURITY.md is written after this is decided |
| 15 | XLSX and PDF libraries | Open | To be chosen in Phase 7 with license and Android compatibility review |
| 16 | Navigation library | Open | AndroidX Navigation Compose multiplatform, confirmed by a Phase 1 spike |
| 17 | In-app QR scanning implementation | Open | Prefer a scanner without Google services dependency; decided in Phase 8 |
