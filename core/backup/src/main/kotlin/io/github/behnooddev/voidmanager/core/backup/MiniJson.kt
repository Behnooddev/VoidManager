package io.github.behnooddev.voidmanager.core.backup

/**
 * The small subset of JSON the backup format uses: objects, arrays, strings, integers, booleans and
 * null. Output is canonical (keys sorted, no whitespace), so equal data gives equal bytes. Numbers
 * with a fraction or an exponent, duplicate keys, trailing data and deep nesting are rejected, which
 * keeps the parser short and the input surface small.
 */
object MiniJson {
    private const val MAX_DEPTH = 32
    private const val HEX_RADIX = 16
    private const val UNICODE_ESCAPE_LENGTH = 4
    private const val FIRST_PRINTABLE = 0x20

    fun write(value: Any?): String = buildString { writeValue(this, value, 0) }

    /** Returns a `Map<String, Any?>`, `List<Any?>`, `String`, `Long`, `Boolean` or null. */
    fun parse(text: String): Any? = Parser(text).document()

    private fun writeValue(
        out: StringBuilder,
        value: Any?,
        depth: Int,
    ) {
        require(depth <= MAX_DEPTH) { "JSON nested too deeply" }
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value.toString())
            is Int -> out.append(value.toString())
            is Long -> out.append(value.toString())
            is String -> writeString(out, value)
            is Map<*, *> -> {
                out.append('{')
                val keys = value.keys.map { it as String }.sorted()
                keys.forEachIndexed { index, key ->
                    if (index > 0) out.append(',')
                    writeString(out, key)
                    out.append(':')
                    writeValue(out, value[key], depth + 1)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                value.forEachIndexed { index, item ->
                    if (index > 0) out.append(',')
                    writeValue(out, item, depth + 1)
                }
                out.append(']')
            }
            else -> throw IllegalArgumentException("Unsupported JSON value")
        }
    }

    private fun writeString(
        out: StringBuilder,
        text: String,
    ) {
        out.append('"')
        for (char in text) {
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char.code < FIRST_PRINTABLE ->
                    out
                        .append(
                            "\\u",
                        ).append(char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_LENGTH, '0'))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    private class Parser(
        private val text: String,
    ) {
        private var position = 0

        fun document(): Any? {
            skipWhitespace()
            val value = value(0)
            skipWhitespace()
            if (position != text.length) fail("Data after the JSON value")
            return value
        }

        private fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) fail("JSON nested too deeply")
            if (position >= text.length) fail("Unexpected end of JSON")
            return when (val char = text[position]) {
                '{' -> obj(depth)
                '[' -> array(depth)
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (char == '-' || char.isAsciiDigit()) integer() else fail("Unexpected character in JSON")
            }
        }

        private fun obj(depth: Int): Map<String, Any?> {
            position++
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                position++
                return result
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Expected a key")
                val key = string()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                if (result.containsKey(key)) fail("Duplicate key")
                result[key] = value(depth + 1)
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    '}' -> return result
                    else -> fail("Expected a comma or a closing brace")
                }
            }
        }

        private fun array(depth: Int): List<Any?> {
            position++
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                position++
                return result
            }
            while (true) {
                skipWhitespace()
                result += value(depth + 1)
                skipWhitespace()
                when (next()) {
                    ',' -> continue
                    ']' -> return result
                    else -> fail("Expected a comma or a closing bracket")
                }
            }
        }

        private fun string(): String {
            position++
            val out = StringBuilder()
            while (true) {
                if (position >= text.length) fail("Unterminated string")
                val char = text[position++]
                when {
                    char == '"' -> return out.toString()
                    char == '\\' -> out.append(escape())
                    char.code < FIRST_PRINTABLE -> fail("Control character in string")
                    else -> out.append(char)
                }
            }
        }

        private fun escape(): Char {
            if (position >= text.length) fail("Unterminated escape")
            return when (val code = text[position++]) {
                '"', '\\', '/' -> code
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    if (position + UNICODE_ESCAPE_LENGTH > text.length) fail("Short unicode escape")
                    val digits = text.substring(position, position + UNICODE_ESCAPE_LENGTH)
                    position += UNICODE_ESCAPE_LENGTH
                    if (!digits.all { it.isAsciiHex() }) fail("Bad unicode escape")
                    digits.toInt(HEX_RADIX).toChar()
                }
                else -> fail("Unknown escape")
            }
        }

        private fun integer(): Long {
            val start = position
            if (peek() == '-') position++
            val digitsStart = position
            while (position < text.length && text[position].isAsciiDigit()) position++
            val digits = position - digitsStart
            if (digits == 0) fail("Bad number")
            if (digits > 1 && text[digitsStart] == '0') fail("Leading zero in number")
            if (position < text.length && (text[position] == '.' || text[position] == 'e' || text[position] == 'E')) {
                fail("Only whole numbers are allowed")
            }
            return text.substring(start, position).toLongOrNull() ?: fail("Number out of range")
        }

        private fun literal(
            word: String,
            result: Any?,
        ): Any? {
            if (!text.startsWith(word, position)) fail("Unexpected word in JSON")
            position += word.length
            return result
        }

        private fun skipWhitespace() {
            while (position < text.length && text[position] in " \t\r\n") position++
        }

        private fun peek(): Char? = if (position < text.length) text[position] else null

        private fun next(): Char? = if (position < text.length) text[position++] else null

        private fun expect(char: Char) {
            if (next() != char) fail("Expected '$char'")
        }

        private fun fail(message: String): Nothing = throw BackupFormatException("$message (offset $position)")

        private fun Char.isAsciiDigit() = this in '0'..'9'

        private fun Char.isAsciiHex() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
