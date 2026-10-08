# Handoff: Phase 3b-2 (People, Personal, profiles, Trash)

Date: 2026-10-08. Version 0.5.0.

## Scope

The first screens that work with data: the People list, Quick Add, the profile, the field editor, the Personal profile and Trash. Relationships, photos, tags, composite field editing and custom fields belong to later phases (4 to 6).

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `PeopleRoute` | Tested | Routes are saved across rotation as strings holding identifiers only |
| `FieldInput` (validation, addable fields) | Tested | Phone accepts any digit script; dates are checked against the real calendar |
| `QuickAdd` | Tested | |
| `ProfileBuilder` / `ProfileView` | Tested | Protected values stay in `FieldText`, which prints as a mask |
| `PeopleModel` | Tested | Against in-memory repositories; a locked vault ends in a storage message, not an exception |
| `BackDispatcher` and Android back wiring | Written, not run | |
| `VmListItem` | Written, not built | |
| People, Quick Add, Profile, Field editor, Rename and Trash screens, `PeopleHost` | Written, not built | Compose; first compile is CI |

## Decisions taken

- One `PeopleModel` per unlocked vault, created in the app shell and dropped on lock. The People and Personal tabs share it; a screen shows the profile only if it belongs to the entity that screen asked for.
- Every repository call runs on a worker and is wrapped, because the vault can lock (and close the database) while a call is in flight.
- Quick Add creates the person first, then the values, one call each. If a value fails the person stays and the screen says so (`PartiallySaved`); there is no transaction across the entity and field repositories.
- Protected values are masked in the profile and revealed per row on request; the reveal state is plain `remember`, so it is forgotten when the screen or the vault goes away. Password and value text in forms is never saved to instance state.
- Editing a value preserves its sensitivity, sharing policy, sort order and metadata; only value, label and note change.
- Only scalar, non-composite field types can be added or edited. Composite values are listed read-only.
- Typing counts as activity (`VaultController.onInteraction`), which closes the open item from the 3b-1 handoff.
- Back is handled through `BackDispatcher` instead of Compose's back handler, because the common back-handler API differs between Compose Multiplatform versions.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 29 tests for routes, validation, quick add, profile building and the model | Kotlin 2.4.20 compiler, outside Gradle, with a stand-in for Compose state | 29 passed |
| All 69 `app:shared` tests together | Same | 69 passed |
| Every string resource used is defined and imported | Script over the sources | No gaps |
| ktlint 1.5.0 on every file | Authoring environment | Passed |

## Not verified and known risks

1. The screens, `PeopleHost`, `VmListItem`, `BackDispatcher` wiring and the Android activity change have not been compiled; Compose cannot be built outside Gradle. Expect the first CI run to find something, as 0.4.1 did.
2. Nothing has run on a device. Check: add a person with Quick Add, open the profile, add and edit a value, mark a favorite, rename, move to Trash, restore, delete forever; open a National ID value and confirm it is masked until Show; lock and unlock and confirm nothing from the profile remains on screen; press the system back button inside a profile and confirm it returns to the list.
3. `LazyColumn` rows and the search field have not been checked for keyboard and focus behavior on desktop.
4. Search is `LIKE` over normalized text (see `docs/database.md`); results for Persian text depend on the normalization in `core:data` and are tested there, not here.
5. Names are shown as typed. Right-to-left names inside left-to-right rows have not been looked at.
6. The screens use English text only; the string resources are in place for translation.

## Next

Push 0.5.0 and send the CI log. After a green run and a device check, phase 4 (custom fields, multiple values, composite editing, field customization).
