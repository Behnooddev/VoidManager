package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.FieldDefinition

/**
 * Rules for the parts of a composite value (address, account, card, work, education).
 * Every part is optional, but a value needs at least one filled part, and a part that has text must
 * be well formed. The checks are about typing mistakes, not about whether a card or an IBAN exists.
 */
object CompositeInput {
    private const val MIN_CARD_DIGITS = 12
    private const val MAX_CARD_DIGITS = 19
    private const val MIN_CVV_DIGITS = 3
    private const val MAX_CVV_DIGITS = 4
    private const val MIN_PIN_DIGITS = 4
    private const val MAX_PIN_DIGITS = 12
    private const val MIN_IBAN_LENGTH = 15
    private const val MAX_IBAN_LENGTH = 34
    private const val EXPIRY_LENGTH = 5
    private const val YEAR_LENGTH = 4
    private const val YEAR_MONTH_LENGTH = 7
    private const val MONTHS = 12
    private val CARD_SEPARATORS = setOf(' ', '-')

    fun hasContent(parts: Map<String, String>): Boolean = parts.values.any { it.isNotBlank() }

    /** One entry per filled part that is not well formed. Blank parts are never reported. */
    fun issues(
        definition: FieldDefinition,
        parts: Map<String, String>,
    ): Map<String, FieldIssue> =
        buildMap {
            for (part in definition.parts) {
                val issue = validatePart(part.key, parts[part.key].orEmpty())
                if (issue != null) put(part.key, issue)
            }
        }

    fun canSave(
        definition: FieldDefinition,
        parts: Map<String, String>,
    ): Boolean = hasContent(parts) && issues(definition, parts).isEmpty()

    /** The parts to store: only the keys the definition knows, trimmed, without blanks. */
    fun cleaned(
        definition: FieldDefinition,
        parts: Map<String, String>,
    ): Map<String, String> =
        buildMap {
            for (part in definition.parts) {
                val text = parts[part.key].orEmpty().trim()
                if (text.isNotEmpty()) put(part.key, text)
            }
        }

    fun validatePart(
        key: String,
        raw: String,
    ): FieldIssue? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        return when (key) {
            "website" -> if (text.none { it.isWhitespace() }) null else FieldIssue.InvalidUrl
            "email" -> if (FieldInput.isEmail(text)) null else FieldIssue.InvalidEmail
            "phone" -> if (FieldInput.isPhone(text)) null else FieldIssue.InvalidPhone
            "number" -> if (isCardNumber(text)) null else FieldIssue.InvalidCardNumber
            "expiry" -> if (isExpiry(text)) null else FieldIssue.InvalidExpiry
            "iban" -> if (isIban(text)) null else FieldIssue.InvalidIban
            "cvv2" -> if (isDigits(text, MIN_CVV_DIGITS, MAX_CVV_DIGITS)) null else FieldIssue.InvalidCvv
            "pin" -> if (isDigits(text, MIN_PIN_DIGITS, MAX_PIN_DIGITS)) null else FieldIssue.InvalidPin
            "start_date", "end_date" -> if (isPartialDate(text)) null else FieldIssue.InvalidPartialDate
            else -> null
        }
    }

    private fun isDigits(
        text: String,
        min: Int,
        max: Int,
    ): Boolean = text.length in min..max && text.all { it.isDigit() }

    /** 12 to 19 digits, optionally grouped with spaces or dashes. */
    fun isCardNumber(text: String): Boolean =
        text.all { it.isDigit() || it in CARD_SEPARATORS } &&
            text.count { it.isDigit() } in MIN_CARD_DIGITS..MAX_CARD_DIGITS

    /** `MM/YY`. */
    fun isExpiry(text: String): Boolean {
        if (text.length != EXPIRY_LENGTH || text[2] != '/') return false
        val month = text.substring(0, 2).toIntOrNull() ?: return false
        return month in 1..MONTHS && text.substring(3).all { it.isDigit() }
    }

    /** Two letters followed by letters and digits, 15 to 34 characters; spaces are ignored. The check digits are not verified. */
    fun isIban(text: String): Boolean {
        val compact = text.filterNot { it == ' ' }
        return compact.length in MIN_IBAN_LENGTH..MAX_IBAN_LENGTH &&
            compact.take(2).all { it.isLetter() } &&
            compact.all { it.isLetterOrDigit() }
    }

    /** `YYYY`, `YYYY-MM` or `YYYY-MM-DD`. */
    fun isPartialDate(text: String): Boolean =
        when (text.length) {
            YEAR_LENGTH -> text.all { it.isDigit() }
            YEAR_MONTH_LENGTH ->
                text.substring(0, YEAR_LENGTH).all { it.isDigit() } &&
                    text[YEAR_LENGTH] == '-' &&
                    (text.substring(YEAR_LENGTH + 1).toIntOrNull() ?: 0) in 1..MONTHS
            else -> FieldInput.isDate(text)
        }
}
