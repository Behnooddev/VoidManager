<p align="center">
  <img src="assets/brand/banner.png" alt="VoidManager" width="100%">
</p>

# VoidManager

VoidManager is a private, local-first application for organizing personal information: people, contact details, accounts, financial data, relationships, notes, photos and attachments, plus a personal vault for the user's own data. It stores everything on the device, works offline and does not require an account or a server.

Android is the first target platform. Desktop (Windows first, then Linux and macOS) shares the same core through Kotlin Multiplatform.

Repository: https://github.com/Behnooddev/VoidManager

## Status

Phase 2 (data layer). The repository has an application shell, a design system, and a data layer: schema, encrypted database drivers and repositories. The screens do not use them yet, and the encryption of stored values (Phase 3) is not implemented. There are no features visible to a user yet. `docs/` describes the intended design; none of it is implemented unless a document says so, and `handoffs/` records what each phase delivered and verified.

## Requirements

JDK 17 or newer and the Android SDK. See [docs/development-setup.md](docs/development-setup.md).

```
./gradlew :app:desktop:run
./gradlew :app:android:assembleDebug
./gradlew :core:common:jvmTest :core:model:jvmTest :core:data:jvmTest :core:database:jvmTest
./gradlew spotlessCheck
```

## Repository layout

- `app/` Android app, desktop app and the shared UI shell
- `core/` shared modules
- `docs/` architecture, security design, formats, decisions
- `handoffs/` per-phase handoff documents
- `assets/brand/` logo and icon masters

## Documentation

The documentation map is in [docs/README.md](docs/README.md).

## License

MIT. See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2026 Behnood Shafiei (Behnooddev) (VoidRoot)
