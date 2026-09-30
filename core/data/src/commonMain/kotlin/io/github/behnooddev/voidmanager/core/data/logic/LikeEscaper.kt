package io.github.behnooddev.voidmanager.core.data.logic

/** Escapes user input for use in `LIKE ... ESCAPE '\'` patterns. */
object LikeEscaper {
    fun escape(input: String): String {
        val out = StringBuilder(input.length + 4)
        for (c in input) {
            if (c == '\\' || c == '%' || c == '_') out.append('\\')
            out.append(c)
        }
        return out.toString()
    }

    fun contains(input: String): String = "%" + escape(input) + "%"
}
