# Platform strategy

Status: Proposed.

## Android (primary)

- Kotlin, Compose Multiplatform UI, single-activity app.
- Proposed `minSdk` 26. Rationale: `java.time` without desugaring, mature Android Keystore behavior, and StrongBox where present. Devices below API 26 are not supported. Pending confirmation.
- `targetSdk` follows the latest stable API level and is set in Phase 1.
- Must run on devices without Google Play services. No hard dependency on Google Play services libraries anywhere in core flows.
- Photo import uses the system photo picker, so the app requests no broad storage permission.
- Sensitive screens set `FLAG_SECURE`. The recent-apps thumbnail is hidden while locked.
- Clipboard: secrets are marked sensitive with `ClipDescription.EXTRA_IS_SENSITIVE` on API 33 and above, and cleared after a timeout with `ClipboardManager.clearPrimaryClip` on API 28 and above. On older versions the app cannot guarantee clearing and the settings screen says so.
- Deep link: custom scheme `voidmanager://` declared as an intent filter. See `sharing.md`.
- Biometric unlock: AndroidX Biometric with a Keystore key that requires user authentication.
- Distribution artifacts: signed APK and AAB. Store publication is out of scope for Phase 0.

## Desktop (later)

- Compose Desktop on the JVM, distributed as native installers built with the Compose Gradle plugin (jpackage under the hood). Windows first, then Linux and macOS.
- Shared modules are the same as Android: `core:model`, `core:data`, `core:crypto`, `core:security`, `core:database`, `core:backup`, `core:sharing`, `core:export`.
- Desktop-specific work:
  - Layout: navigation rail, multi-pane person profile, keyboard shortcuts, resizable windows.
  - Secure storage of the device-bound key: Windows Credential Manager or DPAPI, macOS Keychain, Linux Secret Service. Accessed from JVM through a native-access library; the choice is made when the desktop phase starts.
  - Unlock: master password. Biometric unlock on desktop is not planned initially.
  - Protocol handler registration for `voidmanager://` is done by the installer per operating system. Behavior of the packaging tooling for this must be verified before the desktop phase.
- Gate: desktop ships only after full-database encryption is validated on the JVM (see ADR-0002). The search index lives inside the database, so an unencrypted desktop database would leave indexed names and phone numbers in plaintext on disk.

## iOS

Not planned. The choice of Kotlin Multiplatform keeps the option open. Crypto and platform interfaces are defined in `commonMain` so an iOS implementation can be added without changing callers.

## Platform-specific interfaces (`core:platform`)

Defined in common code, implemented per platform:

- clipboard (with sensitive flag and clear-after)
- external actions: call, compose email, open URL, open maps, open Telegram
- file picker and file save
- share sheet
- deep link intake
- biometric prompt and device-bound key store
- secure window flag
- app lifecycle (foreground/background) signal for auto-lock
- QR scanning (Phase 8)
