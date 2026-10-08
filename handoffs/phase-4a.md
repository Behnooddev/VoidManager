# Handoff: Phase 4a (composite values and primary choice)

Date: 2026-10-09. Version 0.6.0.

## Scope

Editing of the five composite field types (address, account, bank card, work, education) and choosing the primary value among several. Custom field definitions, hiding or renaming built-in fields, and raising a value's protection level are phase 4b.

## Delivered

| Item | Status | Notes |
| --- | --- | --- |
| `CompositeInput` | Tested | Part rules and the at-least-one-part rule |
| `PartRow`, parts on `ProfileRow`, `ProfileRow.toInput` | Tested | A row can be stored again with only the primary mark changed |
| `PeopleModel.makePrimary` and composite saves | Tested | Against in-memory repositories |
| Part editor, per-part masking, part labels, Make primary button | Written, not built | Compose; first compile is CI |

## Decisions taken

- A composite value has no single text; the profile lists its parts in the order of the field definition. Protected parts are masked together per value (one Show or Hide button), not per part.
- Parts at the highest protection level are typed into a masked field; the others are plain fields. Parts at level 2 (card number, expiry, IBAN, holder, bank) are visible while editing.
- Validation catches typing mistakes only. The card number is not checked with the Luhn algorithm and the IBAN check digits are not verified, so that cards and accounts from any country and issuer can be saved.
- Making a value primary stores it again unchanged except for the mark; the repository clears the mark on the other values of that field.
- The note of a composite value follows the sensitivity of the value, as for any value.

## Verification performed

| Check | Where | Result |
| --- | --- | --- |
| 41 `app:shared` people tests, 81 `app:shared` tests together | Kotlin 2.4.20 compiler, outside Gradle | 81 passed |
| Every string used is defined and imported; no unused strings | Script over the sources | No gaps |
| ktlint 1.5.0 on every changed file | Authoring environment | Passed |

## Not verified and known risks

1. The Compose changes (profile cards, the part editor, the Make primary button) have not been compiled; the previous three Compose releases compiled on the first or second CI run.
2. Nothing of this release has run on a device. Check: add an account with a password, confirm the password field is masked and the profile shows it masked until Show; add a bank card with an expiry typed wrongly and confirm the message; save a card, edit it, and confirm CVV2 and PIN still hold their values; add two phone numbers and use Make primary.
3. The part editor shows one field per part of the definition; a value that was stored with a part the definition does not know (for example from a future version) keeps that part on screen in the profile but cannot edit it, and saving drops it. No such data exists yet.
4. Editing a value reveals its protected parts in the editor form. They stay in plain `remember` state and disappear with the screen.

## Next

Push 0.6.0 and send the CI log. Then 4b, or the first password-manager work (phase 5), whichever is wanted first.
