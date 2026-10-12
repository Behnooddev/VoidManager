# Handoff: Phase 4b (field customization, protection level per value)

Date: 2026-10-11. Version 0.8.0.

## Scope

Hide, rename and add fields, delete custom fields, and choose a protection level for a value. Reordering, composite custom fields and lowering a level are not part of this phase.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `FieldDefinitionInfo` and `FieldRegistry.infos` (`core:model`) | Compiled and used by tests outside Gradle | The registry entry plus the vault's overrides |
| `FieldDefinition.sq` queries, `SqlFieldDefinitionRepository`, `VaultRepositories.fieldDefinitions` | Written, not built | SQLDelight generates the query functions, which cannot be run outside Gradle |
| `FieldDefinitionRepositoryTest` (10 tests) | Written, not run | Runs in CI on Linux, Windows and macOS |
| `FieldInput.addable`, `ProfileBuilder`, `CompositeInput` on `FieldDefinitionInfo`, `ProtectionLevels`, `CustomFieldForm` | Tested | |
| `PeopleModel` definition operations and problems (`LabelTaken`, `FieldInUse`) | Tested | Against in-memory repositories |
| Fields screen, level picker, Settings entry, editor changes | Written, not built | Compose |
| CI job for the encryption tests on Windows and macOS | Written, not run | Answers open decision 13 once it passes |

## Decisions taken

- Overrides live in the existing columns of `field_definition` (`is_hidden`, `label_override`, `sort_order`). The earlier text in `docs/database.md` described a separate override store keyed by `system_key`; the schema never had one, and the seeder already left these columns alone, so the doc was changed instead.
- A hidden field leaves the add list only. Hiding it from saved values would make data disappear without the user knowing why.
- Custom fields are scalar and use the Custom section. Composite types need a part layout, so they are not offered.
- Labels are unique across built-in and custom fields, ignoring case, so the add list is never ambiguous.
- The screens only raise a protection level. Lowering a level that was set on purpose looks like an accident or an attack more often than a wish, and the repository still allows it for a later explicit feature.
- The Windows and macOS job runs the existing JVM tests (`core:database`, `core:data`, `core:crypto`) rather than new ones, so it measures the same code the Linux job does.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 126 `app:shared` and `core:model` tests together (10 new customization tests plus updates) | Kotlin 2.4.20 compiler, outside Gradle | 126 passed |
| Every string used is defined and imported; no unused strings | Script over the sources | No gaps |
| ktlint 1.5.0 on every file | Authoring environment | Passed |

## Not verified and known risks

1. The SQL and the repository have not been compiled or run. SQLDelight names the generated parameters from the named placeholders; a mismatch is a compile error in CI. `FieldDefinitionRepositoryTest` is the first real exercise of them.
2. 0.6.0 and 0.7.0 have not been seen by CI either, so the first run may report errors from any of three releases. The file name in each error says which.
3. The Windows and macOS job has never run; a failure there may come from the runner setup rather than the code.
4. Compose screens: the Fields screen is one long scrolling column; very long field lists and the keyboard have not been looked at on a device.
5. Deleting a custom field relies on the foreign key from `field_value`; the count check runs first, so the message is friendly, but a race with another writer is not handled (there is only one writer).

## Next

Push 0.6.0, 0.7.0 and 0.8.0 together if they have not been pushed, and send the CI log. Then device checks for 0.7.0 (clipboard, re-authentication) and 0.8.0 (create a custom Secret field, hide a built-in field, rename one). After that, the order of phases 6, 7 and 8 and the desktop track are for the project owner to confirm.
