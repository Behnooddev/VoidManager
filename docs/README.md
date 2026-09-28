# Documentation

| Document | Contents |
| --- | --- |
| [architecture.md](architecture.md) | Layers, modules, data flow, dependency rules |
| [platform-strategy.md](platform-strategy.md) | Android and desktop plans, platform-specific code |
| [database.md](database.md) | Data model, field system, relationships, trash, search |
| [security/key-management.md](security/key-management.md) | Key hierarchy, unlock paths, lock behavior, recovery limits |
| [security/password-manager.md](security/password-manager.md) | Accounts, secrets, reveal and clipboard handling |
| [security/threat-model.md](security/threat-model.md) | Assets, adversaries, mitigations, known limits |
| [sharing.md](sharing.md) | Share package, QR bootstrap, transports, incoming share, merge |
| [backup-format.md](backup-format.md) | Encrypted backup container and restore rules |
| [media-storage.md](media-storage.md) | Photo and attachment storage |
| [testing.md](testing.md) | Test layers and required coverage |
| [ci-cd.md](ci-cd.md) | Pipelines, checks, release artifacts |
| [versioning.md](versioning.md) | Independent version numbers and the compatibility matrix |
| [migrations.md](migrations.md) | Database, backup and share migration rules |
| [roadmap.md](roadmap.md) | Phases, exit criteria, sequencing |
| [repository-layout.md](repository-layout.md) | Planned repository tree |
| [open-decisions.md](open-decisions.md) | Decisions pending or needing confirmation |
| [adr/](adr) | Architecture decision records |

Documents are updated in the same change that alters the behavior they describe.

Status labels used in the documents:

- Proposed: a design that has not been implemented.
- Accepted: a decision that has been approved.
- Implemented: code exists.
- Tested: automated tests cover it.
