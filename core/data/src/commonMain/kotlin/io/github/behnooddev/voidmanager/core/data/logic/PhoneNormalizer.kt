package io.github.behnooddev.voidmanager.core.data.logic

/**
 * Digit-level handling of phone numbers. The original text is always kept as entered; this
 * produces the comparison form only. The suffix comparison is a heuristic that is used for match
 * suggestions and never to merge records automatically.
 */
object PhoneNormalizer {
    private const val SUFFIX_LENGTH = 10
    private const val MIN_COMPARABLE_DIGITS = 7

    fun digits(raw: String): String {
        val out = StringBuilder(raw.length)
        for (c in raw) TextNormalizer.asciiDigit(c)?.let { out.append(it) }
        return out.toString()
    }

    /** True when two numbers probably identify the same line, ignoring formatting, country code and trunk prefix. */
    fun probablySame(
        a: String,
        b: String,
    ): Boolean {
        val da = digits(a)
        val db = digits(b)
        if (da.isEmpty() || db.isEmpty()) return false
        if (da == db) return true
        if (da.length < MIN_COMPARABLE_DIGITS || db.length < MIN_COMPARABLE_DIGITS) return false
        return da.takeLast(SUFFIX_LENGTH) == db.takeLast(SUFFIX_LENGTH)
    }
}
