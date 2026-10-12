package io.github.behnooddev.voidmanager.core.data

import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldDefinitionRepositoryTest {
    private val vault = TestVault()

    private fun <T> Outcome<T>.value(): T = (this as Outcome.Success).value

    private fun failureOf(outcome: Outcome<*>): FailureReason = (outcome as Outcome.Failure).reason

    private fun custom(
        label: String,
        type: FieldDataType = FieldDataType.Text,
        level: Sensitivity = Sensitivity.Personal,
        multiple: Boolean = false,
    ): FieldDefinitionInfo = vault.definitions.createCustom(label, type, level, multiple).value()

    @Test
    fun theListHoldsEveryBuiltInFieldWithItsPartsAndNoOverrides() {
        val all = vault.definitions.list()
        assertEquals(FieldRegistry.definitions.size, all.size)
        val card = all.single { it.systemKey == "card" }
        assertTrue(card.isComposite)
        assertEquals(FieldRegistry.bySystemKey("card")!!.parts.map { it.key }, card.parts.map { it.key })
        assertTrue(all.none { it.isHidden || it.labelOverride != null || it.isUserDefined })
    }

    @Test
    fun aCustomFieldIsStoredInTheCustomSectionAndListedLast() {
        val created = custom("Locker number", FieldDataType.Number, Sensitivity.Sensitive, multiple = true)
        assertTrue(created.isUserDefined)
        assertNull(created.systemKey)
        assertEquals(FieldSection.Custom, created.section)
        assertEquals(Sensitivity.Sensitive, created.defaultSensitivity)
        assertTrue(created.allowsMultiple)
        assertEquals(
            created.id,
            vault.definitions
                .list()
                .last()
                .id,
        )
    }

    @Test
    fun aValueCanBeSavedUnderACustomFieldAndKeepsItsLevel() {
        val field = custom("Locker code", level = Sensitivity.Secret)
        val person = vault.entities.createPerson("Synthetic Person").value()

        val saved = vault.fields.add(person.id, FieldValueInput(field.id, value = "4821")).value()

        assertEquals(Sensitivity.Secret, saved.sensitivity)
        assertEquals(
            "4821",
            vault.fields
                .listForEntity(person.id)
                .single()
                .value!!
                .reveal(),
        )
    }

    @Test
    fun customFieldsRejectBadLabelsTypesAndDuplicates() {
        assertEquals(
            FailureReason.InvalidInput,
            failureOf(vault.definitions.createCustom("   ", FieldDataType.Text, Sensitivity.Personal, false)),
        )
        assertEquals(
            FailureReason.InvalidInput,
            failureOf(vault.definitions.createCustom("x".repeat(61), FieldDataType.Text, Sensitivity.Personal, false)),
        )
        assertEquals(
            FailureReason.InvalidInput,
            failureOf(vault.definitions.createCustom("Mixed", FieldDataType.Card, Sensitivity.Personal, false)),
        )

        custom("Locker number")
        assertEquals(
            FailureReason.Conflict,
            failureOf(
                vault.definitions.createCustom(" locker NUMBER ", FieldDataType.Text, Sensitivity.Personal, false),
            ),
        )
        assertEquals(
            FailureReason.Conflict,
            failureOf(vault.definitions.createCustom("Phone", FieldDataType.Text, Sensitivity.Personal, false)),
            "a built-in label counts too",
        )
    }

    @Test
    fun hidingAFieldKeepsItsValues() {
        val person = vault.entities.createPerson("Synthetic Person").value()
        vault.fields.add(person.id, FieldValueInput("sys.birthday", value = "2000-01-01"))

        val hidden = vault.definitions.setHidden("sys.birthday", true).value()

        assertTrue(hidden.isHidden)
        assertEquals(1, vault.fields.listForEntity(person.id).size)
        assertFalse(
            vault.definitions
                .setHidden("sys.birthday", false)
                .value()
                .isHidden,
        )
    }

    @Test
    fun aBuiltInLabelIsOverriddenAndRestored() {
        val renamed = vault.definitions.setLabel("sys.phone", "Mobile").value()
        assertEquals("Mobile", renamed.label)
        assertEquals("Phone", renamed.defaultLabel)

        assertEquals(
            "Phone",
            vault.definitions
                .setLabel("sys.phone", null)
                .value()
                .label,
        )
        assertNull(
            vault.definitions
                .list()
                .single { it.id == "sys.phone" }
                .labelOverride,
        )
    }

    @Test
    fun aLabelThatMatchesAnotherFieldIsRefused() {
        assertEquals(FailureReason.Conflict, failureOf(vault.definitions.setLabel("sys.phone", "Email")))
    }

    @Test
    fun aCustomFieldCanBeRenamedButNotBlanked() {
        val field = custom("Locker")
        assertEquals(
            "Gym locker",
            vault.definitions
                .setLabel(field.id, "Gym locker")
                .value()
                .label,
        )
        assertEquals(FailureReason.InvalidInput, failureOf(vault.definitions.setLabel(field.id, "  ")))
    }

    @Test
    fun aCustomFieldWithValuesCannotBeDeletedUntilTheyAreGone() {
        val field = custom("Locker")
        val person = vault.entities.createPerson("Synthetic Person").value()
        val value = vault.fields.add(person.id, FieldValueInput(field.id, value = "12")).value()

        assertEquals(FailureReason.Conflict, failureOf(vault.definitions.deleteCustom(field.id)))

        vault.fields.delete(value.id).value()
        vault.definitions.deleteCustom(field.id).value()
        assertTrue(vault.definitions.list().none { it.id == field.id })
    }

    @Test
    fun aBuiltInFieldCannotBeDeletedAndAMissingOneIsNotFound() {
        assertEquals(FailureReason.NotAllowed, failureOf(vault.definitions.deleteCustom("sys.phone")))
        assertEquals(FailureReason.NotFound, failureOf(vault.definitions.deleteCustom("usr.nothing")))
        assertEquals(FailureReason.NotFound, failureOf(vault.definitions.setHidden("usr.nothing", true)))
    }

    @Test
    fun overridesAndCustomFieldsSurviveTheSeederRunningAgain() {
        val field = custom("Locker")
        vault.definitions.setLabel("sys.phone", "Mobile").value()
        vault.definitions.setHidden("sys.birthday", true).value()

        VaultSeeder.seed(vault.database)

        val all = vault.definitions.list()
        assertEquals("Mobile", all.single { it.id == "sys.phone" }.label)
        assertTrue(all.single { it.id == "sys.birthday" }.isHidden)
        assertTrue(all.any { it.id == field.id })
    }
}
