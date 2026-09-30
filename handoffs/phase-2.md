# Handoff: Phase 2 (database, data model, repositories)

Date: 2026-09-29

## Scope

Schema version 1, field registry, relationship types, encrypted database drivers for Android and desktop, vault opening and migration planning, repositories for people, field values, relationships, trash and search, and the desktop encryption spike.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| CI fix: wrapper permission, `.gitattributes` | Implemented | Not yet confirmed on GitHub |
| `core:model` (entities, sensitivity, registry, relationship types, UUIDv7) | Tested | 20 tests ran outside Gradle |
| `core:data` logic (normalizers, phone matching, sensitivity and sharing policy) | Tested | 20 tests ran outside Gradle |
| `core:database` pure code (migration planner, key format, encryption check) | Tested | 10 tests ran outside Gradle |
| SQL schema v1 and all 42 queries | Tested | Executed against SQLite (Python `sqlite3`), 29 behavior checks passed. Not yet through the SQLDelight compiler |
| Desktop database encryption | Tested (Linux only) | Real encrypted file, wrong key, FTS5, foreign keys. See ADR-0002 |
| `DriverFactory`, `VaultDatabaseOpener`, JVM and Android drivers | Scaffolded | Not compiled. Android driver not run |
| Repositories and `VaultRepositories`, `VaultSeeder` | Scaffolded | Not compiled |
| JVM tests for the opener and the repositories (about 35 tests) | Scaffolded | Written, never run |

Crypto is not implemented. Repositories depend on the `ValueCipher` interface; tests use a fake that only obscures bytes and enforces context binding. The real implementation and the key hierarchy are Phase 3.

## Decisions taken

- SQLDelight 2.4.0 (the current release; 2.3.2 added AGP 9 compatibility) with the SQLite 3.38 dialect.
- Desktop: `io.github.willena:sqlite-jdbc` 3.53.4.0 replaces the stock driver; the stock artifact is excluded in the root build.
- Database key is a raw 256-bit key (`x'<hex>'`), no password derivation at open time.
- Schema version lives in `vault_meta`, not `PRAGMA user_version`.
- Search is `LIKE` over normalized columns; FTS5 deferred.
- The encryption of the file is verified after every open, because a driver that ignores its key parameter creates a plain database silently.
- Repositories are blocking; no coroutines yet.
- Parts of a composite value take the higher of their own default sensitivity and the value's effective sensitivity.
- Gendered relationship types use neutral reciprocals; the caller can choose another reciprocal.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 49 pure tests (model, data logic, database pure code) | Kotlin 2.4.20 compiler, JDK 21, outside Gradle | 49 passed |
| Schema DDL, 42 queries, constraints and cascades | SQLite via Python | Passed |
| SQLCipher-compatible encryption on the JVM | Willena JDBC 3.53.4.0, Linux | Passed |
| ktlint 1.5.0 | Authoring environment | Passed |
| Syntax scan of Kotlin that needs SQLDelight-generated code | Compiler without the generated classes | No syntax errors; type errors expected |

## Not verified and known risks

1. The SQLDelight Gradle plugin has not run. Generated class names are assumed: `VoidManagerDatabase`, `entityQueries`, `fieldDefinitionQueries`, `fieldValueQueries`, `relationshipQueries`, `miscQueries`. Repositories use mapper lambdas with positional parameters, so column property names do not matter, but the number and order of columns do. The first compile will show any mismatch.
2. The SQLDelight plugin with the Android-KMP library plugin and AGP 9 is only known to work from the SQLDelight release notes and its own repository. If configuration fails, check the SQLDelight issue tracker for the plugin versions in use.
3. The Android driver code follows the SQLCipher for Android and SQLDelight APIs from documentation and has not been compiled. `System.loadLibrary("sqlcipher")` and the `SupportOpenHelperFactory` constructor are the likeliest points of difference.
4. Versions `net.zetetic:sqlcipher-android` 4.6.1 and `androidx.sqlite` 2.4.0 are best guesses and were not looked up; correct them on the first resolution error.
5. Windows and macOS desktop encryption are untested.
6. Searching a stored local number with a full international query does not match; a query with a local prefix finds an international number. Match suggestions during merge use `PhoneNormalizer.probablySame`, which handles both.
7. Purging an entity does not delete media files yet; media storage arrives in Phase 3 and its purge path must be added then.
8. Migration steps do not exist yet. Schema version 1 has none; the framework is exercised only by the newer-schema refusal test.

## How to continue

1. Push the tree and read the CI run. Fix compile errors first in `core:database`, then `core:data`.
2. Run `./gradlew :core:model:jvmTest :core:data:jvmTest :core:database:jvmTest` locally. The JVM tests use a real encrypted file.
3. When the tests pass, update this handoff with the result.
4. Phase 3 starts with the crypto and key management foundation (real `ValueCipher`, key hierarchy, app lock), then People and Personal screens.

## Open items

`docs/open-decisions.md`, items 13 to 21.

## Entry criteria for Phase 3

- CI green on `main`, including the JVM tests of `core:database` and `core:data`.
- Desktop app and Android debug APK still start.
- Brand assets integration decided (done or postponed).

## File map

- `core/model`: domain types, registry, relationship types, id generator
- `core/database`: SQL schema, opener, drivers, key type
- `core/data`: normalizers, policy, repositories, seeder, `ValueCipher`
- `docs/database.md`, `docs/adr/0002-local-database.md`, `docs/testing.md`
