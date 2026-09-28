# Database and data model

Status: Proposed.

## Goals

- One model for People and for the Personal profile.
- Fields are data, not columns. A field value is an instance of a field definition.
- Multiple values per field type, each with its own label, note, primary flag, sensitivity and sharing policy.
- Sensitivity is stored per value and enforced in the data layer.
- Trash, relationships, photos and attachments are part of the model from the first schema version.

## Storage

SQLite through SQLDelight, encrypted with SQLCipher. Values of sensitivity level 2 and 3 are additionally encrypted per value with the vault key hierarchy (see `security/key-management.md`). Rationale and open risk for desktop are in ADR-0002.

App settings that are not personal data (theme, timeouts, default share policies) are stored separately from the vault database so they are readable before unlock.

## Identifiers and time

- Primary keys are UUIDv7 (time-ordered), generated on device.
- Timestamps are UTC epoch milliseconds.
- Every table that holds user data has `created_at` and `updated_at`.

## Entities

### entity

A person or the Personal profile.

| Column | Notes |
| --- | --- |
| id | UUIDv7 |
| kind | `PERSON` or `SELF`. Exactly one `SELF` row exists. |
| display_name | Denormalized for lists and sorting. Level 0. |
| sort_key | Normalized name for ordering and search |
| is_favorite | |
| primary_photo_id | Nullable reference to `media_asset` |
| deleted_at | Nullable. Non-null means the entity is in Trash. |
| created_at, updated_at | |

### field_definition

The registry. Ships with the application and can be extended by the user.

| Column | Notes |
| --- | --- |
| id | |
| system_key | Stable identifier such as `phone`, `email`, `card`, `account`. Null for user-defined fields. Never shown to the user and never renamed. |
| section | identity, contact, social, accounts, financial, address, work, education, dates, relationships, notes, custom |
| data_type | text, multiline, number, date, time, url, phone, email, username, boolean, selection, address, card, account, file, ... |
| default_label | Localizable default |
| default_sensitivity | 0 to 3 |
| allows_multiple | |
| is_composite | True for types stored as parts (address, card, account, education, work) |
| is_user_defined | |
| schema_version | Version of the type's part layout |

Labels shown to the user come from `field_value.label` when set, otherwise the definition's default label. Renaming a label never touches `system_key`.

### field_value

One instance of a field on an entity.

| Column | Notes |
| --- | --- |
| id | |
| entity_id | |
| definition_id | |
| label | User-visible label override, nullable |
| value_plain | Scalar value for level 0 and 1, nullable |
| value_cipher | Ciphertext for level 2 and 3 scalars, nullable |
| note_plain / note_cipher | Same rule as the value |
| normalized | Search-normalized form for level 0 and 1 (digits-only phone, folded text). Null for level 2 and 3. |
| is_primary | |
| sort_order | |
| sensitivity | 0 to 3. Defaults from the definition, can be raised by the user, lowering below the definition default is restricted. |
| sharing_policy | `INHERIT`, `ALLOW`, `ASK`, `NEVER` |
| metadata | JSON, non-sensitive presentation data only |
| created_at, updated_at, deleted_at | |

A CHECK constraint enforces that exactly one of `value_plain` and `value_cipher` is set for scalar types, and that `value_cipher` is used whenever `sensitivity >= 2`.

### field_part

Composite types (address, card, account, education, work) are stored as parts, not as one JSON blob. Each part has its own sensitivity, so a card's holder name is not encrypted the same way as its CVV2, and only the searchable parts are indexed.

| Column | Notes |
| --- | --- |
| id | |
| field_value_id | |
| part_key | e.g. `street`, `city`, `number`, `cvv2`, `pin`, `expiry`, `iban`, `service`, `username`, `password`, `website` |
| value_plain / value_cipher | Same rule as `field_value` |
| sensitivity | Per part |

Default part sensitivities:

| Type | Part | Level |
| --- | --- | --- |
| account | service, website, username | 1 |
| account | password, recovery data | 3 |
| card | holder, bank | 2 |
| card | number, iban, expiry | 2 |
| card | cvv2, pin | 3 |
| address | all parts | 1 |
| national ID | value | 2 |

A single JSON document per composite value was rejected: it cannot carry per-part sensitivity, cannot be indexed selectively, and makes partial sharing harder.

### relationship_type

| Column | Notes |
| --- | --- |
| id, key, label | |
| reciprocal_type_id | Type used for the opposite direction |
| is_symmetric | Friend, colleague, spouse |
| gender_variants | Optional map used only when the linked person has an explicit gender value |

### relationship

Stored as two rows sharing a `pair_id`, one per direction, created and deleted in one transaction.

| Column | Notes |
| --- | --- |
| id, pair_id | |
| from_entity_id, to_entity_id | |
| type_id | |
| note, metadata | |
| created_at, updated_at, deleted_at | |

Reciprocal type resolution: if the type has a gender variant and the other person's gender is known, the variant is used; otherwise the neutral reciprocal type applies (for example Sibling). Whether the gender field exists at all is an open decision.

Creating a relationship to a person who does not exist offers to create that person. Existing people are linked, never duplicated.

### Tags, groups

`tag`, `entity_tag`, `person_group`, `entity_group`. Tags and groups carry a name, a sort order and timestamps.

### media_asset

| Column | Notes |
| --- | --- |
| id, entity_id | |
| kind | `PROFILE`, `GALLERY`, `ATTACHMENT` |
| file_ref | Relative path in encrypted media storage |
| mime, size_bytes, sha256 | Hash of the plaintext, stored inside the encrypted database |
| title, note | Level 1 |
| taken_at | Optional |
| sensitivity | Attachments default to 2 |
| deleted_at | |

Changing the primary photo updates `entity.primary_photo_id` only. Gallery rows are untouched.

### share_origin

Remembers that a local entity came from a share, so a later share from the same source can be offered as an update.

| Column | Notes |
| --- | --- |
| entity_id | |
| source_key_id | Identifier of the sender's share key, if signing is adopted |
| source_entity_id | Sender's entity id |
| last_received_at | |

### schema_meta

Schema version, creation version of the app, last successful migration record.

## Trash

Deleting sets `deleted_at` on the entity. Field values, media and relationships of a trashed entity are hidden by the entity state and are not individually marked. Restoring clears the flag. Permanent deletion removes rows and deletes encrypted files, in one operation with a confirmation. No automatic purge is defined in the specification and none is planned.

## Search

- FTS5 index over level 0 and 1 normalized text: names, phones (digits form), emails, usernames, social handles, tags, notes, custom text, address parts, service names.
- Level 2 and 3 values are never indexed and never returned as search text.
- Normalization: Unicode NFKC, case folding, diacritic folding, mapping of Arabic and Persian letter variants to one form, mapping of Persian and Arabic-Indic digits to ASCII digits, digits-only phone form with libphonenumber parsing where the region can be determined.
- Search results carry only the entity id, matched field label and a masked snippet. Level 2 and 3 fields never appear as snippets.

## Field registry and customization

- Built-in definitions can be hidden, reordered and relabeled per vault. These are stored as per-vault overrides keyed by `system_key`, not by editing the definition.
- User-defined definitions have `system_key` null and a generated id.
- Deleting a user-defined definition that has values is blocked until the values are moved or removed.

## Consistency rules

- Foreign keys enforced.
- All multi-row operations (create entity with fields, relationship pair, merge) run in one transaction.
- Repositories are the only writers. No feature module accesses SQL directly.
