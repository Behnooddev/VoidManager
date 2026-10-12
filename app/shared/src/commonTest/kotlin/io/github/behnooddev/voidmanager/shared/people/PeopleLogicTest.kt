package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.FieldPart
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleRouteTest {
    @Test
    fun everyRouteSurvivesEncodingAndParsing() {
        val routes =
            listOf(
                PeopleRoute.Home,
                PeopleRoute.QuickAdd,
                PeopleRoute.Trash,
                PeopleRoute.Profile("018f-aa"),
                PeopleRoute.Rename("018f-aa"),
                PeopleRoute.FieldEditor("018f-aa", "018f-bb"),
                PeopleRoute.FieldEditor("018f-aa", null),
            )
        for (route in routes) assertEquals(route, PeopleRoute.parse(route.encode()))
    }

    @Test
    fun unknownOrMalformedTextIsNotARoute() {
        for (text in listOf("", "nowhere", "profile", "profile/", "field/a", "field//new", "field/a/b/c")) {
            assertNull(PeopleRoute.parse(text), "'$text'")
        }
    }
}

class FieldInputTest {
    @Test
    fun emptyTextIsRejectedForEveryType() {
        for (type in FieldDataType.entries) assertEquals(FieldIssue.Empty, FieldInput.validate(type, "   "))
    }

    @Test
    fun phoneNumbersAcceptSeparatorsAndAnyDigitScript() {
        assertNull(FieldInput.validate(FieldDataType.Phone, "+98 912 345 6789"))
        assertNull(FieldInput.validate(FieldDataType.Phone, "(021) 555-0100"))
        assertNull(FieldInput.validate(FieldDataType.Phone, "۰۹۱۲۳۴۵۶۷۸۹"))
        assertEquals(FieldIssue.InvalidPhone, FieldInput.validate(FieldDataType.Phone, "12"))
        assertEquals(FieldIssue.InvalidPhone, FieldInput.validate(FieldDataType.Phone, "call me"))
    }

    @Test
    fun emailsNeedOneAtSignAndADottedDomain() {
        assertNull(FieldInput.validate(FieldDataType.Email, "someone@example.org"))
        for (bad in listOf("someone", "@example.org", "a@b", "a@@b.c", "a b@c.d", "a@.c", "a@b.")) {
            assertEquals(FieldIssue.InvalidEmail, FieldInput.validate(FieldDataType.Email, bad), bad)
        }
    }

    @Test
    fun datesMustBeRealCalendarDays() {
        assertNull(FieldInput.validate(FieldDataType.Date, "2024-02-29"))
        assertEquals(FieldIssue.InvalidDate, FieldInput.validate(FieldDataType.Date, "2023-02-29"))
        assertEquals(FieldIssue.InvalidDate, FieldInput.validate(FieldDataType.Date, "2024-13-01"))
        assertEquals(FieldIssue.InvalidDate, FieldInput.validate(FieldDataType.Date, "2024-04-31"))
        assertEquals(FieldIssue.InvalidDate, FieldInput.validate(FieldDataType.Date, "24-1-1"))
        assertNull(FieldInput.validate(FieldDataType.Date, "1900-02-28"))
        assertEquals(FieldIssue.InvalidDate, FieldInput.validate(FieldDataType.Date, "1900-02-29"))
        assertNull(FieldInput.validate(FieldDataType.Date, "2000-02-29"))
    }

    @Test
    fun numbersAreDigitsOnlyAndBounded() {
        assertNull(FieldInput.validate(FieldDataType.Number, "42"))
        assertEquals(FieldIssue.InvalidNumber, FieldInput.validate(FieldDataType.Number, "4 2"))
        assertEquals(FieldIssue.InvalidNumber, FieldInput.validate(FieldDataType.Number, "1".repeat(19)))
    }

    @Test
    fun urlsCannotContainSpaces() {
        assertNull(FieldInput.validate(FieldDataType.Url, "https://example.org/a"))
        assertEquals(FieldIssue.InvalidUrl, FieldInput.validate(FieldDataType.Url, "example .org"))
    }

    @Test
    fun compositeFieldsAreEditablePartByPart() {
        for (key in listOf("card", "account", "address", "work", "education", "phone")) {
            val definition = FieldRegistry.infoBySystemKey(key)
            assertTrue(definition != null && FieldInput.isEditable(definition), key)
        }
    }

    @Test
    fun aSingleValueFieldDisappearsFromTheAddListOnceItHasAValue() {
        val before = FieldInput.addable(FieldRegistry.infos, emptyList()).map { it.systemKey }
        assertTrue("birthday" in before && "phone" in before)

        val birthday = value("v1", FieldDefinition.idForSystemKey("birthday"), "2000-01-01")
        val phone = value("v2", FieldDefinition.idForSystemKey("phone"), "5550100")
        val after = FieldInput.addable(FieldRegistry.infos, listOf(birthday, phone)).map { it.systemKey }
        assertFalse("birthday" in after)
        assertTrue("phone" in after)
    }
}

class FieldCustomizationTest {
    private fun definitions(vararg changes: (FieldDefinitionInfo) -> FieldDefinitionInfo): List<FieldDefinitionInfo> =
        FieldRegistry.infos.map { info -> changes.fold(info) { acc, change -> change(acc) } }

    @Test
    fun aHiddenFieldIsLeftOutOfTheAddListButItsValuesStayOnTheProfile() {
        val hidden = definitions({ if (it.systemKey == "birthday") it.copy(isHidden = true) else it })
        assertFalse("birthday" in FieldInput.addable(hidden, emptyList()).map { it.systemKey })

        val saved = value("v1", FieldDefinition.idForSystemKey("birthday"), "2000-01-01")
        val entity = Entity("e1", EntityKind.Person, "Synthetic Person", false, null, 1L, 1L, null)
        val view = ProfileBuilder.build(entity, listOf(saved), hidden)
        assertEquals(listOf("v1"), view.sections.flatMap { it.rows }.map { it.valueId })
    }

    @Test
    fun aCustomFieldIsOfferedAndShownUnderItsOwnLabelInTheCustomSection() {
        val custom =
            FieldDefinitionInfo(
                id = "usr.1",
                systemKey = null,
                section = FieldSection.Custom,
                dataType = FieldDataType.Text,
                defaultLabel = "Locker code",
                labelOverride = null,
                defaultSensitivity = Sensitivity.Secret,
                allowsMultiple = false,
                parts = emptyList(),
                isUserDefined = true,
                isHidden = false,
                sortOrder = 99,
            )
        val all = FieldRegistry.infos + custom
        assertTrue("usr.1" in FieldInput.addable(all, emptyList()).map { it.id })

        val entity = Entity("e1", EntityKind.Person, "Synthetic Person", false, null, 1L, 1L, null)
        val view = ProfileBuilder.build(entity, listOf(value("v1", "usr.1", "4821", protected = true)), all)
        val section = view.sections.single()
        assertEquals(FieldSection.Custom, section.section)
        assertEquals(
            "Locker code",
            section.rows
                .single()
                .definition
                ?.label,
        )
        assertFalse(
            "usr.1" in FieldInput.addable(all, listOf(value("v1", "usr.1", "4821"))).map { it.id },
            "single value",
        )
    }

    @Test
    fun aRenamedFieldShowsItsOverrideAndABlankOverrideFallsBack() {
        val phone = FieldRegistry.infoBySystemKey("phone")!!
        assertEquals("Mobile", phone.copy(labelOverride = "Mobile").label)
        assertEquals("Phone", phone.copy(labelOverride = "  ").label)
        assertEquals("Phone", phone.label)
    }

    @Test
    fun aValueCanBeRaisedButNeverLoweredByTheScreens() {
        assertEquals(
            listOf(Sensitivity.Personal, Sensitivity.Sensitive, Sensitivity.Secret),
            ProtectionLevels.choices(Sensitivity.Personal, null),
        )
        assertEquals(
            listOf(Sensitivity.Sensitive, Sensitivity.Secret),
            ProtectionLevels.choices(Sensitivity.Sensitive, Sensitivity.Sensitive),
        )
        assertEquals(listOf(Sensitivity.Secret), ProtectionLevels.choices(Sensitivity.Personal, Sensitivity.Secret))
        assertEquals(Sensitivity.entries, ProtectionLevels.choices(Sensitivity.Public, null))
    }

    @Test
    fun aCustomFieldNeedsALabelOfReasonableLength() {
        assertFalse(CustomFieldForm.canSave("   "))
        assertTrue(CustomFieldForm.canSave(" Locker "))
        assertTrue(CustomFieldForm.canSave("x".repeat(60)))
        assertFalse(CustomFieldForm.canSave("x".repeat(61)))
        assertTrue(CustomFieldForm.TYPES.none { it.name in setOf("Card", "Account", "Address", "Work", "Education") })
    }
}

class QuickAddTest {
    @Test
    fun aNameAloneIsEnough() {
        assertTrue(QuickAdd.canSave(QuickAddInput(name = "Synthetic Person")))
        assertTrue(QuickAdd.values(QuickAddInput(name = "Synthetic Person")).isEmpty())
    }

    @Test
    fun aMissingNameBlocksSaving() {
        assertEquals(listOf(QuickAddIssue.NameMissing), QuickAdd.check(QuickAddInput(name = "  ")))
    }

    @Test
    fun filledOptionalFieldsMustBeValid() {
        val issues = QuickAdd.check(QuickAddInput(name = "A", phone = "x", email = "y"))
        assertEquals(listOf(QuickAddIssue.PhoneInvalid, QuickAddIssue.EmailInvalid), issues)
    }

    @Test
    fun valuesAreTrimmedAndPhoneAndEmailArePrimary() {
        val values = QuickAdd.values(QuickAddInput(name = "A", phone = " 5550100 ", email = " a@b.co ", note = " hi "))
        assertEquals(listOf("sys.phone", "sys.email", "sys.note"), values.map { it.definitionId })
        assertEquals(listOf("5550100", "a@b.co", "hi"), values.map { it.value })
        assertEquals(listOf(true, true, false), values.map { it.isPrimary })
    }
}

class ProfileBuilderTest {
    private val entity = Entity("e1", EntityKind.Person, "Synthetic Person", false, null, 1L, 1L, null)

    @Test
    fun valuesAreGroupedBySectionInRegistryOrderWithPrimaryFirst() {
        val values =
            listOf(
                value("v1", "sys.note", "a note", sort = 0),
                value("v2", "sys.phone", "5550101", sort = 1),
                value("v3", "sys.phone", "5550100", sort = 2, primary = true),
                value("v4", "sys.birthday", "2000-01-01", sort = 0),
            )
        val view = ProfileBuilder.build(entity, values, FieldRegistry.infos)
        assertEquals(
            listOf(FieldSection.Contact, FieldSection.Dates, FieldSection.Notes),
            view.sections.map { it.section },
        )
        assertEquals(
            listOf("v3", "v2"),
            view.sections
                .first()
                .rows
                .map { it.valueId },
        )
    }

    @Test
    fun aProtectedValueStaysMaskedUntilItIsRevealedOnPurpose() {
        val secret = value("v1", "sys.national_id", "0123456789", protected = true)
        val row =
            ProfileBuilder
                .build(entity, listOf(secret), FieldRegistry.infos)
                .sections
                .single()
                .rows
                .single()
        assertTrue(row.isProtected)
        assertEquals(FieldText.MASK, row.text.toString())
        assertEquals("0123456789", row.text?.reveal())
    }

    private fun account(): FieldValue =
        value("v1", "sys.account", null).copy(
            parts =
                listOf(
                    FieldPart("password", FieldText("hunter2", true), Sensitivity.Secret),
                    FieldPart("username", FieldText("someone", false), Sensitivity.Personal),
                    FieldPart("service", FieldText("Mail", false), Sensitivity.Personal),
                ),
        )

    @Test
    fun aCompositeValueListsItsPartsInDefinitionOrderAndKeepsProtectedPartsMasked() {
        val row =
            ProfileBuilder
                .build(entity, listOf(account()), FieldRegistry.infos)
                .sections
                .single()
                .rows
                .single()
        assertNull(row.text)
        assertEquals(listOf("service", "username", "password"), row.parts.map { it.key })
        assertTrue(row.isProtected)
        assertEquals(
            FieldText.MASK,
            row.parts
                .last()
                .text
                .toString(),
        )
        assertEquals(
            "hunter2",
            row.parts
                .last()
                .text
                .reveal(),
        )
        assertTrue(row.canEdit)
    }

    @Test
    fun aRowCanBeStoredAgainWithOnlyThePrimaryMarkChanged() {
        val row =
            ProfileBuilder
                .build(entity, listOf(account()), FieldRegistry.infos)
                .sections
                .single()
                .rows
                .single()
        val input = row.toInput(isPrimary = true)
        assertTrue(input.isPrimary)
        assertNull(input.value)
        assertEquals(mapOf("service" to "Mail", "username" to "someone", "password" to "hunter2"), input.parts)
        assertEquals(row.sensitivity, input.sensitivity)

        val scalar =
            ProfileBuilder
                .build(
                    entity,
                    listOf(value("v2", "sys.phone", "5550100")),
                    FieldRegistry.infos,
                ).sections
                .single()
                .rows
                .single()
        val scalarInput = scalar.toInput(isPrimary = false)
        assertEquals("5550100", scalarInput.value)
        assertTrue(scalarInput.parts.isEmpty())
    }

    @Test
    fun anEmptyProfileHasNoSectionsButOffersFieldsToAdd() {
        val view = ProfileBuilder.build(entity, emptyList(), FieldRegistry.infos)
        assertTrue(view.isEmpty)
        assertTrue(view.addable.isNotEmpty())
    }
}

internal fun value(
    id: String,
    definitionId: String,
    text: String?,
    sort: Int = 0,
    primary: Boolean = false,
    protected: Boolean = false,
): FieldValue =
    FieldValue(
        id = id,
        entityId = "e1",
        definitionId = definitionId,
        label = null,
        value = text?.let { FieldText(it, protected) },
        note = null,
        isPrimary = primary,
        sortOrder = sort,
        sensitivity = if (protected) Sensitivity.Sensitive else Sensitivity.Personal,
        sharingPolicy = SharingPolicy.Inherit,
        metadata = null,
        parts = emptyList(),
        createdAt = sort.toLong(),
        updatedAt = sort.toLong(),
    )
