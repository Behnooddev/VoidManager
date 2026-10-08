package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldPart
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
    fun compositeFieldsAreNotEditableYet() {
        val card =
            io.github.behnooddev.voidmanager.core.model.FieldRegistry
                .bySystemKey("card")
        assertTrue(card != null && !FieldInput.isEditable(card))
        val phone =
            io.github.behnooddev.voidmanager.core.model.FieldRegistry
                .bySystemKey("phone")
        assertTrue(phone != null && FieldInput.isEditable(phone))
    }

    @Test
    fun aSingleValueFieldDisappearsFromTheAddListOnceItHasAValue() {
        val before = FieldInput.addable(emptyList()).map { it.systemKey }
        assertTrue("birthday" in before && "phone" in before)

        val birthday = value("v1", FieldDefinition.idForSystemKey("birthday"), "2000-01-01")
        val phone = value("v2", FieldDefinition.idForSystemKey("phone"), "5550100")
        val after = FieldInput.addable(listOf(birthday, phone)).map { it.systemKey }
        assertFalse("birthday" in after)
        assertTrue("phone" in after)
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
        val view = ProfileBuilder.build(entity, values)
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
                .build(entity, listOf(secret))
                .sections
                .single()
                .rows
                .single()
        assertTrue(row.isProtected)
        assertEquals(FieldText.MASK, row.text.toString())
        assertEquals("0123456789", row.text?.reveal())
    }

    @Test
    fun aCompositeValueShowsOnlyItsUnprotectedPartsAndCountsTheRest() {
        val account =
            value("v1", "sys.account", null).copy(
                parts =
                    listOf(
                        FieldPart("service", FieldText("Mail", false), Sensitivity.Personal),
                        FieldPart("username", FieldText("someone", false), Sensitivity.Personal),
                        FieldPart("password", FieldText("hunter2", true), Sensitivity.Secret),
                    ),
            )
        val row =
            ProfileBuilder
                .build(entity, listOf(account))
                .sections
                .single()
                .rows
                .single()
        assertEquals("Mail, someone", row.text?.reveal())
        assertEquals(1, row.hiddenParts)
        assertFalse(row.canEdit)
    }

    @Test
    fun anEmptyProfileHasNoSectionsButOffersFieldsToAdd() {
        val view = ProfileBuilder.build(entity, emptyList())
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
