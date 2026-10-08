package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldValueInput

data class QuickAddInput(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val note: String = "",
)

enum class QuickAddIssue { NameMissing, PhoneInvalid, EmailInvalid }

/** The fast path for saving a person: a name, and optionally a phone number, an email and a note. */
object QuickAdd {
    private val PHONE_ID = FieldDefinition.idForSystemKey("phone")
    private val EMAIL_ID = FieldDefinition.idForSystemKey("email")
    private val NOTE_ID = FieldDefinition.idForSystemKey("note")

    /** Empty optional fields are fine; a field that has text must be valid. */
    fun check(input: QuickAddInput): List<QuickAddIssue> =
        buildList {
            if (input.name.isBlank()) add(QuickAddIssue.NameMissing)
            if (input.phone.isNotBlank() &&
                FieldInput.validate(FieldDataType.Phone, input.phone) != null
            ) {
                add(QuickAddIssue.PhoneInvalid)
            }
            if (input.email.isNotBlank() &&
                FieldInput.validate(FieldDataType.Email, input.email) != null
            ) {
                add(QuickAddIssue.EmailInvalid)
            }
        }

    fun canSave(input: QuickAddInput): Boolean = check(input).isEmpty()

    /** The values to store next to the person. The phone and email become the primary ones. */
    fun values(input: QuickAddInput): List<FieldValueInput> =
        buildList {
            if (input.phone.isNotBlank()) add(FieldValueInput(PHONE_ID, value = input.phone.trim(), isPrimary = true))
            if (input.email.isNotBlank()) add(FieldValueInput(EMAIL_ID, value = input.email.trim(), isPrimary = true))
            if (input.note.isNotBlank()) add(FieldValueInput(NOTE_ID, value = input.note.trim()))
        }
}
