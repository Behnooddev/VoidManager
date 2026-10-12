package io.github.behnooddev.voidmanager.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Formatted output of the value types must never contain the text they hold. */
class LoggingTest {
    private val secret = "correct horse battery staple 8841"

    @Test
    fun aFieldValueInputPrintsNoValueNoteOrPart() {
        val input =
            FieldValueInput(
                definitionId = "sys.account",
                label = "Mail",
                value = secret,
                note = secret,
                parts = mapOf("password" to secret, "username" to secret),
            )
        val text = input.toString()
        assertFalse(secret in text, text)
        assertTrue("sys.account" in text)
        assertTrue("password" in text, "the part names are not secret")
    }

    @Test
    fun aProtectedFieldTextPrintsAMaskInsideAnyContainer() {
        val text = FieldText(secret, isProtected = true)
        assertFalse(secret in text.toString())
        assertFalse(secret in listOf(text).toString())
        assertFalse(secret in mapOf("password" to text).toString())
        assertFalse(secret in "value: $text")
    }

    @Test
    fun aFieldValueWithProtectedPartsPrintsNoSecret() {
        val value =
            FieldValue(
                id = "v1",
                entityId = "e1",
                definitionId = "sys.account",
                label = null,
                value = null,
                note = FieldText(secret, true),
                isPrimary = false,
                sortOrder = 0,
                sensitivity = Sensitivity.Secret,
                sharingPolicy = SharingPolicy.Inherit,
                metadata = null,
                parts = listOf(FieldPart("password", FieldText(secret, true), Sensitivity.Secret)),
                createdAt = 1L,
                updatedAt = 1L,
            )
        assertFalse(secret in value.toString(), value.toString())
    }
}
