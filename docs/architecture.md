# Architecture

Status: Proposed (stack accepted, see ADR-0001).

## Principles

- Local-first. All core features work with no network access and no VoidManager server.
- Security is enforced in the data layer. Hiding a value in the UI is not a control.
- The data model is fixed before large UI work starts.
- Platform code is small and isolated behind interfaces. Product logic lives in shared Kotlin.

## Layers

```
app (android, desktop)         entry points, DI wiring, platform manifests
feature:*                      screens, view models, navigation per feature
core:data                      repositories, use cases, field registry, search,
                               relationship engine, trash, merge engine
core:security                  lock state, sensitivity policy, secret handling
core:crypto                    KDF, AEAD, key hierarchy, secure buffers
core:database                  schema, migrations, database factory
core:media                     encrypted photo and attachment storage
core:backup / core:sharing / core:export
core:platform                  expect/actual APIs (clipboard, intents, biometrics, ...)
core:designsystem              tokens and components
core:model / core:common       pure domain types and utilities
```

Dependency rule: arrows point downward only. `core:model` depends on nothing. `feature:*` modules never depend on each other; they communicate through navigation contracts declared in a small `core:navigation` surface.

## Presentation

- Compose Multiplatform for all UI.
- Unidirectional data flow: a screen renders an immutable UI state and emits events to a view model. View models come from the AndroidX lifecycle KMP artifacts.
- Mobile and desktop use separate layout compositions that share state holders. Desktop is not a scaled mobile layout: navigation rail, multi-pane profile view, keyboard shortcuts.
- Navigation: type-safe routes with the AndroidX Navigation Compose multiplatform artifact. To be confirmed by a Phase 1 spike against desktop back-stack behavior; Decompose is the fallback.
- Dependency injection: Koin. Constructor injection everywhere; the container is used only at the composition root.
- All user-visible strings live in resources. No string literals in composables. This keeps RTL and localization possible without a rewrite (see open decisions).

## Concurrency

- Kotlin coroutines and Flow.
- Argon2id, bulk AEAD, image decoding and export generation run on a dedicated background dispatcher, never on the main thread.
- Repositories are blocking in Phase 2. Callers dispatch them off the main thread. Suspend and Flow wrappers are added with the first feature that needs them.

## Error model

- Domain operations return a sealed result type. Exceptions are reserved for programmer errors.
- User-facing errors state what happened, whether data was saved, and what the user can do next. Stack traces are never shown.

## Logging

- One logging facade in `core:common`. Direct use of platform logging or `println` is blocked by a lint rule.
- Sensitive values are carried in wrapper types whose `toString` returns a fixed mask. Logging a secret by accident produces the mask.
- Production builds log at warning level and above, without record contents.

## Dependency policy

New dependencies must be recorded with license, maintenance status and reason. Cryptographic primitives come from maintained libraries (see `security/key-management.md`); no custom cryptography.
