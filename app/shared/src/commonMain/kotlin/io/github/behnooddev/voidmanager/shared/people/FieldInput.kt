package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldValue

enum class FieldIssue {
    Empty,
    InvalidPhone,
    InvalidEmail,
    InvalidNumber,
    InvalidDate,
    InvalidUrl,
}

/** Which fields the editor offers and whether typed text is acceptable. */
object FieldInput {
    private val EDITABLE_TYPES =
        setOf(
            FieldDataType.Text,
            FieldDataType.Multiline,
            FieldDataType.Number,
            FieldDataType.Date,
            FieldDataType.Url,
            FieldDataType.Phone,
            FieldDataType.Email,
            FieldDataType.Username,
        )

    private const val MIN_PHONE_DIGITS = 3
    private const val MAX_NUMBER_DIGITS = 18
    private const val DATE_LENGTH = 10
    private const val FEBRUARY = 2
    private const val LEAP_FEBRUARY_DAYS = 29
    private val PHONE_EXTRAS = setOf(' ', '+', '-', '(', ')', '.')
    private val DAYS_IN_MONTH = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    /** Composite fields (address, card, account, work, education) are not editable yet. */
    fun isEditable(definition: FieldDefinition): Boolean =
        !definition.isComposite && definition.dataType in EDITABLE_TYPES

    /** Fields that can be added now: editable ones, minus single-value fields that already have a value. */
    fun addable(existing: List<FieldValue>): List<FieldDefinition> {
        val used = existing.mapTo(HashSet()) { it.definitionId }
        return FieldRegistry.definitions.filter { isEditable(it) && (it.allowsMultiple || it.id !in used) }
    }

    fun validate(
        type: FieldDataType,
        text: String,
    ): FieldIssue? {
        val value = text.trim()
        if (value.isEmpty()) return FieldIssue.Empty
        return when (type) {
            FieldDataType.Phone -> if (isPhone(value)) null else FieldIssue.InvalidPhone
            FieldDataType.Email -> if (isEmail(value)) null else FieldIssue.InvalidEmail
            FieldDataType.Number ->
                if (value.length <= MAX_NUMBER_DIGITS &&
                    value.all { it.isDigit() }
                ) {
                    null
                } else {
                    FieldIssue.InvalidNumber
                }
            FieldDataType.Date -> if (isDate(value)) null else FieldIssue.InvalidDate
            FieldDataType.Url -> if (value.none { it.isWhitespace() }) null else FieldIssue.InvalidUrl
            else -> null
        }
    }

    /** Digits in any script, with the usual separators. Persian and Arabic-Indic digits are accepted. */
    fun isPhone(text: String): Boolean =
        text.all { it.isDigit() || it in PHONE_EXTRAS } && text.count { it.isDigit() } >= MIN_PHONE_DIGITS

    fun isEmail(text: String): Boolean {
        if (text.any { it.isWhitespace() }) return false
        val at = text.indexOf('@')
        if (at <= 0 || at != text.lastIndexOf('@')) return false
        val domain = text.substring(at + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
    }

    /** `YYYY-MM-DD` with a real calendar day. */
    fun isDate(text: String): Boolean {
        if (text.length != DATE_LENGTH || text[4] != '-' || text[7] != '-') return false
        val year = text.substring(0, 4).toIntOrNull() ?: return false
        val month = text.substring(5, 7).toIntOrNull() ?: return false
        val day = text.substring(8, 10).toIntOrNull() ?: return false
        if (year < 1 || month !in 1..DAYS_IN_MONTH.size) return false
        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val days = if (month == FEBRUARY && leap) LEAP_FEBRUARY_DAYS else DAYS_IN_MONTH[month - 1]
        return day in 1..days
    }
}
