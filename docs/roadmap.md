# Roadmap

Status: Proposed. Phases follow the project specification; changes to the order are recorded in this file.

Each phase ends with a report stating, per feature, whether it is designed, scaffolded, implemented, tested or production-ready. A phase is closed only when lint, formatting and tests pass and documentation is updated.

| Phase | Content | Exit criteria |
| --- | --- | --- |
| 0 | Architecture, stack, security design, formats, threat model outline | Documents reviewed; open decisions resolved or scheduled |
| 1 | Repository foundation, build system, CI skeleton, design system, version consistency checks | Empty app builds on Android and desktop in CI; lint and format gates active; design tokens and base components documented |
| 1.1 | Brand assets integration | Logo, banner and icon masters added under `assets/brand`; launcher icon and desktop icons wired; README banner renders |
| 2 | Database, data model, field registry, repositories, migrations framework | Schema v1 with migration tests; repository tests on encrypted database; desktop encryption approach validated (gates the desktop track) |
| 3 | Crypto and key management foundation, app lock, People, Personal, Quick Add and detailed profile, trash | Key hierarchy tests, lock behavior tests, create/edit/trash flows tested |
| 4 | Custom fields, multiple values, field customization, advanced profile editing | Field registry overrides and custom types tested |
| 5 | Password manager, sensitivity enforcement, clipboard and reveal handling | Level 3 handling tests, threat model review complete |
| 6 | Search, relationships, smart actions | Search normalization tests, reciprocal relationship tests |
| 7 | Export (CSV, XLSX, PDF), import, encrypted backup and restore | Backup fixtures restored in CI, export field selection tested |
| 8 | Sharing: package, session, QR and Nearby methods, branded QR, deep link, incoming share, merge | Protocol tests, tamper tests, branded QR validation tests, instrumented deep link tests |
| 9 | Polish, accessibility, performance | Accessibility checks, performance budgets met on a reference device |
| 10 | CI/CD completion, release process, documentation, website | Reproducible release, documentation complete |

Sequencing notes:

- Crypto and key management move ahead of People because encrypted storage shapes every repository. This matches the implementation order in the specification.
- Trash is part of the data model from schema v1 even though the Trash screens ship with People, to avoid a later schema change.
- Desktop UI work starts after Phase 2 validates database encryption on the JVM and after the Android track has a stable people and profile flow.
