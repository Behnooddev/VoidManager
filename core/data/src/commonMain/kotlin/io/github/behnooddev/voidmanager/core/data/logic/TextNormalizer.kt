package io.github.behnooddev.voidmanager.core.data.logic

/**
 * Produces the form of a text that is stored for search and compared against search input.
 * Both sides go through the same function, so case, accents, Persian and Arabic letter variants,
 * digit systems and spacing do not prevent a match.
 *
 * The Latin accent table covers Latin-1 Supplement and Latin Extended-A letters. Other scripts are
 * not decomposed.
 */
object TextNormalizer {
    fun normalize(input: String): String {
        val out = StringBuilder(input.length)
        var lastWasSpace = true
        for (raw in input) {
            val mapped = mapChar(raw)
            when {
                mapped == null -> Unit
                mapped.isEmpty() -> Unit
                mapped[0].isWhitespace() -> {
                    if (!lastWasSpace) out.append(' ')
                    lastWasSpace = true
                }
                else -> {
                    out.append(mapped)
                    lastWasSpace = false
                }
            }
        }
        while (out.isNotEmpty() && out.last() == ' ') out.deleteAt(out.length - 1)
        return out.toString()
    }

    /** Maps digits from Persian, Arabic-Indic and fullwidth forms to ASCII digits. */
    fun asciiDigit(c: Char): Char? =
        when (c) {
            in '0'..'9' -> c
            in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
            in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
            in '\uFF10'..'\uFF19' -> '0' + (c - '\uFF10')
            else -> null
        }

    private fun mapChar(c: Char): String? {
        asciiDigit(c)?.let { return it.toString() }
        return when (c) {
            // Zero-width joiners split words in Persian; users type names with and without them.
            '\u200C', '\u200D', '\u200E', '\u200F', '\u0640' -> ""
            in '\u064B'..'\u065F', '\u0670' -> ""
            '\u064A', '\u0649' -> "\u06CC"
            '\u0643' -> "\u06A9"
            '\u0629' -> "\u0647"
            '\u0623', '\u0625', '\u0671' -> "\u0627"
            '\u00DF' -> "ss"
            '\u00E6', '\u00C6' -> "ae"
            '\u0153', '\u0152' -> "oe"
            else -> foldLatin(c)
        }
    }

    private fun foldLatin(c: Char): String {
        val lower = c.lowercaseChar()
        val folded = LATIN_FOLD[lower]
        return (folded ?: lower).toString()
    }

    private val LATIN_FOLD: Map<Char, Char> =
        buildMap {
            val groups =
                mapOf(
                    'a' to "\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u0101\u0103\u0105",
                    'c' to "\u00E7\u0107\u0109\u010B\u010D",
                    'd' to "\u010F\u0111",
                    'e' to "\u00E8\u00E9\u00EA\u00EB\u0113\u0115\u0117\u0119\u011B",
                    'g' to "\u011D\u011F\u0121\u0123",
                    'h' to "\u0125\u0127",
                    'i' to "\u00EC\u00ED\u00EE\u00EF\u0129\u012B\u012D\u012F\u0131",
                    'j' to "\u0135",
                    'k' to "\u0137",
                    'l' to "\u013A\u013C\u013E\u0140\u0142",
                    'n' to "\u00F1\u0144\u0146\u0148",
                    'o' to "\u00F2\u00F3\u00F4\u00F5\u00F6\u00F8\u014D\u014F\u0151",
                    'r' to "\u0155\u0157\u0159",
                    's' to "\u015B\u015D\u015F\u0161",
                    't' to "\u0163\u0165\u0167",
                    'u' to "\u00F9\u00FA\u00FB\u00FC\u0169\u016B\u016D\u016F\u0171\u0173",
                    'w' to "\u0175",
                    'y' to "\u00FD\u00FF\u0177",
                    'z' to "\u017A\u017C\u017E",
                )
            for ((base, variants) in groups) {
                for (v in variants) put(v, base)
            }
        }
}
