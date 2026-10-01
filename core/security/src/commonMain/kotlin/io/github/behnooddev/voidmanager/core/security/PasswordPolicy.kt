package io.github.behnooddev.voidmanager.core.security

import kotlin.math.log2

enum class PasswordIssue {
    TooShort,
    CommonPassword,
    SingleRepeatedCharacter,
    SimpleSequence,
}

enum class PasswordStrength { Weak, Fair, Good, Strong }

data class PasswordAssessment(
    val issues: List<PasswordIssue>,
    val strength: PasswordStrength,
    val estimatedBits: Int,
) {
    val isAcceptable: Boolean get() = issues.isEmpty()
}

/**
 * Rules for the master password and the backup password.
 *
 * The strength value is a heuristic estimate of guessing effort, not a guarantee. It counts the
 * character pool and length, and discounts obvious patterns. It cannot know whether a password
 * has appeared in a breach.
 */
class PasswordPolicy(
    val minLength: Int = DEFAULT_MIN_LENGTH,
) {
    init {
        require(minLength >= ABSOLUTE_MIN_LENGTH) { "The minimum length cannot be below $ABSOLUTE_MIN_LENGTH" }
    }

    fun assess(password: CharSequence): PasswordAssessment {
        val text = password.toString()
        val issues =
            buildList {
                if (text.length < minLength) add(PasswordIssue.TooShort)
                if (text.lowercase() in COMMON) add(PasswordIssue.CommonPassword)
                if (text.length > 1 && text.all { it == text[0] }) add(PasswordIssue.SingleRepeatedCharacter)
                if (isSimpleSequence(text)) add(PasswordIssue.SimpleSequence)
            }
        val bits = estimateBits(text)
        val strength =
            when {
                issues.isNotEmpty() && bits < FAIR_BITS -> PasswordStrength.Weak
                bits < FAIR_BITS -> PasswordStrength.Weak
                bits < GOOD_BITS -> PasswordStrength.Fair
                bits < STRONG_BITS -> PasswordStrength.Good
                else -> PasswordStrength.Strong
            }
        return PasswordAssessment(issues, strength, bits)
    }

    private fun estimateBits(text: String): Int {
        if (text.isEmpty()) return 0
        var pool = 0
        if (text.any { it in 'a'..'z' }) pool += LOWER
        if (text.any { it in 'A'..'Z' }) pool += UPPER
        if (text.any { it in '0'..'9' }) pool += DIGITS
        if (text.any { !it.isLetterOrDigit() && it.code < ASCII_LIMIT }) pool += SYMBOLS
        if (text.any { it.code >= ASCII_LIMIT }) pool += OTHER
        if (pool == 0) return 0
        // Repeated characters add little: count each distinct character once at full weight and repeats at a fraction.
        val distinct = text.toSet().size
        val repeats = text.length - distinct
        val effectiveLength = distinct + repeats * REPEAT_WEIGHT
        var bits = effectiveLength * log2(pool.toDouble())
        if (isSimpleSequence(text)) bits /= SEQUENCE_DIVISOR
        if (text.lowercase() in COMMON) bits = minOf(bits, COMMON_CAP_BITS)
        return bits.toInt()
    }

    private fun isSimpleSequence(text: String): Boolean {
        if (text.length < SEQUENCE_MIN) return false
        val step = text[1].code - text[0].code
        if (step != 1 && step != -1) return false
        return (1 until text.length).all { text[it].code - text[it - 1].code == step }
    }

    companion object {
        const val DEFAULT_MIN_LENGTH = 12
        private const val ABSOLUTE_MIN_LENGTH = 8
        private const val LOWER = 26
        private const val UPPER = 26
        private const val DIGITS = 10
        private const val SYMBOLS = 33
        private const val OTHER = 64
        private const val ASCII_LIMIT = 128
        private const val REPEAT_WEIGHT = 0.25
        private const val SEQUENCE_MIN = 4
        private const val SEQUENCE_DIVISOR = 4.0
        private const val COMMON_CAP_BITS = 20.0
        private const val FAIR_BITS = 45
        private const val GOOD_BITS = 65
        private const val STRONG_BITS = 85

        private val COMMON =
            setOf(
                "password",
                "password1",
                "password123",
                "passw0rd",
                "123456",
                "12345678",
                "123456789",
                "1234567890",
                "qwerty",
                "qwertyuiop",
                "qwerty123",
                "abc123",
                "letmein",
                "welcome",
                "iloveyou",
                "admin",
                "administrator",
                "monkey",
                "dragon",
                "football",
                "baseball",
                "111111",
                "000000",
                "123123",
                "1q2w3e4r",
                "zaq12wsx",
                "changeme",
                "voidmanager",
            )
    }
}
