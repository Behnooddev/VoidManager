package io.github.behnooddev.voidmanager.shared.vault

import io.github.behnooddev.voidmanager.core.security.PasswordIssue
import io.github.behnooddev.voidmanager.core.security.PasswordPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupFormTest {
    private val policy = PasswordPolicy()
    private val good = "synthetic horse battery staple 42"

    @Test
    fun aStrongMatchingAcknowledgedPasswordCanBeCreated() {
        assertTrue(SetupForm.check(good, good, acknowledged = true, policy = policy).canCreate)
    }

    @Test
    fun theAcknowledgementIsRequired() {
        assertFalse(SetupForm.check(good, good, acknowledged = false, policy = policy).canCreate)
    }

    @Test
    fun aMismatchBlocksCreationAndIsReportedOnlyAfterTyping() {
        val untouched = SetupForm.check(good, "", acknowledged = true, policy = policy)
        assertFalse(untouched.canCreate)
        assertFalse(untouched.showMismatch)

        val typed = SetupForm.check(good, "$good!", acknowledged = true, policy = policy)
        assertFalse(typed.canCreate)
        assertTrue(typed.showMismatch)
    }

    @Test
    fun aWeakPasswordIsRefusedEvenWhenItMatches() {
        val short = SetupForm.check("short", "short", acknowledged = true, policy = policy)
        assertFalse(short.canCreate)
        assertTrue(PasswordIssue.TooShort in short.assessment.issues)

        val common = SetupForm.check("changeme", "changeme", acknowledged = true, policy = policy)
        assertFalse(common.canCreate)
    }

    @Test
    fun anEmptyPasswordIsNeverAccepted() {
        assertFalse(SetupForm.check("", "", acknowledged = true, policy = policy).canCreate)
    }
}
