package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositeInputTest {
    private val card = checkNotNull(FieldRegistry.bySystemKey("card"))
    private val work = checkNotNull(FieldRegistry.bySystemKey("work"))
    private val education = checkNotNull(FieldRegistry.bySystemKey("education"))
    private val account = checkNotNull(FieldRegistry.bySystemKey("account"))

    @Test
    fun aValueNeedsAtLeastOneFilledPart() {
        assertFalse(CompositeInput.canSave(account, emptyMap()))
        assertFalse(CompositeInput.canSave(account, mapOf("service" to "   ")))
        assertTrue(CompositeInput.canSave(account, mapOf("service" to "Mail")))
    }

    @Test
    fun blankPartsAreNeverReportedAsInvalid() {
        assertTrue(CompositeInput.issues(card, mapOf("number" to "", "cvv2" to " ", "expiry" to "")).isEmpty())
    }

    @Test
    fun cardNumbersAreTwelveToNineteenDigitsWithOptionalGrouping() {
        assertNull(CompositeInput.validatePart("number", "6037 9912 3456 7890"))
        assertNull(CompositeInput.validatePart("number", "6037-9912-3456-7890"))
        assertNull(CompositeInput.validatePart("number", "۶۰۳۷۹۹۱۲۳۴۵۶۷۸۹۰"))
        assertEquals(FieldIssue.InvalidCardNumber, CompositeInput.validatePart("number", "1234"))
        assertEquals(FieldIssue.InvalidCardNumber, CompositeInput.validatePart("number", "6037 9912 3456 789x"))
        assertEquals(FieldIssue.InvalidCardNumber, CompositeInput.validatePart("number", "1".repeat(20)))
    }

    @Test
    fun expiryIsMonthSlashYear() {
        assertNull(CompositeInput.validatePart("expiry", "04/29"))
        assertNull(CompositeInput.validatePart("expiry", "12/30"))
        for (bad in listOf("13/29", "00/29", "4/29", "04-29", "04/2029", "ab/cd")) {
            assertEquals(FieldIssue.InvalidExpiry, CompositeInput.validatePart("expiry", bad), bad)
        }
    }

    @Test
    fun securityCodesAreDigitsOfTheRightLength() {
        assertNull(CompositeInput.validatePart("cvv2", "123"))
        assertNull(CompositeInput.validatePart("cvv2", "1234"))
        assertEquals(FieldIssue.InvalidCvv, CompositeInput.validatePart("cvv2", "12"))
        assertEquals(FieldIssue.InvalidCvv, CompositeInput.validatePart("cvv2", "12345"))
        assertNull(CompositeInput.validatePart("pin", "1234"))
        assertEquals(FieldIssue.InvalidPin, CompositeInput.validatePart("pin", "123"))
        assertEquals(FieldIssue.InvalidPin, CompositeInput.validatePart("pin", "12a4"))
    }

    @Test
    fun anIbanIsTwoLettersThenLettersAndDigits() {
        assertNull(CompositeInput.validatePart("iban", "IR06 2960 0000 0010 0324 2000 01"))
        assertNull(CompositeInput.validatePart("iban", "GB82WEST12345698765432"))
        assertEquals(FieldIssue.InvalidIban, CompositeInput.validatePart("iban", "12345678901234567"))
        assertEquals(FieldIssue.InvalidIban, CompositeInput.validatePart("iban", "IR12"))
        assertEquals(FieldIssue.InvalidIban, CompositeInput.validatePart("iban", "IR06-2960-0000-0010-0324-2000-01"))
    }

    @Test
    fun workPartsReuseThePhoneEmailAndUrlRules() {
        assertNull(CompositeInput.validatePart("email", "someone@example.org"))
        assertEquals(FieldIssue.InvalidEmail, CompositeInput.validatePart("email", "someone"))
        assertNull(CompositeInput.validatePart("phone", "+98 21 5550100"))
        assertEquals(FieldIssue.InvalidPhone, CompositeInput.validatePart("phone", "ext 5"))
        assertEquals(FieldIssue.InvalidUrl, CompositeInput.validatePart("website", "example .org"))
        assertEquals(
            mapOf("email" to FieldIssue.InvalidEmail),
            CompositeInput.issues(
                work,
                mapOf(
                    "company" to "Example",
                    "email" to "x",
                ),
            ),
        )
    }

    @Test
    fun educationDatesMayBeAYearAMonthOrADay() {
        for (ok in listOf(
            "2015",
            "2015-09",
            "2015-09-23",
        )) {
            assertNull(CompositeInput.validatePart("start_date", ok), ok)
        }
        for (bad in listOf("15", "2015-13", "2015-9", "2015-02-30", "september")) {
            assertEquals(FieldIssue.InvalidPartialDate, CompositeInput.validatePart("end_date", bad), bad)
        }
        assertTrue(
            CompositeInput
                .issues(
                    education,
                    mapOf("institution" to "Example University", "start_date" to "2015"),
                ).isEmpty(),
        )
    }

    @Test
    fun cleanedKeepsOnlyKnownTrimmedNonBlankParts() {
        val cleaned =
            CompositeInput.cleaned(
                account,
                mapOf(
                    "service" to " Mail ",
                    "username" to "  ",
                    "bogus" to "x",
                    "password" to "pw",
                ),
            )
        assertEquals(mapOf("service" to "Mail", "password" to "pw"), cleaned)
    }
}
