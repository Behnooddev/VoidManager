package io.github.behnooddev.voidmanager.core.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MiniJsonTest {
    @Test
    fun outputIsCanonicalWithSortedKeysAndNoWhitespace() {
        val text =
            MiniJson.write(
                mapOf("b" to 1, "a" to listOf(true, null, "x"), "c" to mapOf("z" to 2L, "y" to false)),
            )
        assertEquals("""{"a":[true,null,"x"],"b":1,"c":{"y":false,"z":2}}""", text)
    }

    @Test
    fun valuesSurviveAWriteAndParse() {
        val value =
            mapOf(
                "name" to "علی رضایی",
                "quote" to "He said \"hi\"\n\ttab \\ slash",
                "control" to "\u0001\u001f",
                "emoji" to "\uD83D\uDE00",
                "n" to -42L,
                "big" to Long.MAX_VALUE,
                "list" to listOf(1L, "two", listOf<Any?>()),
                "none" to null,
            )
        assertEquals(value, MiniJson.parse(MiniJson.write(value)))
    }

    @Test
    fun whitespaceBetweenTokensIsAccepted() {
        assertEquals(mapOf("a" to listOf(1L, 2L)), MiniJson.parse(" { \"a\" : [ 1 , 2 ] } \n"))
    }

    @Test
    fun unicodeEscapesAreDecoded() {
        assertEquals("A\u00e9", MiniJson.parse("\"\\u0041\\u00e9\""))
    }

    @Test
    fun malformedInputIsRejected() {
        val bad =
            listOf(
                "",
                "{",
                "[1,]",
                "{\"a\":}",
                "{\"a\" 1}",
                "{a:1}",
                "\"unterminated",
                "tru",
                "nul",
                "01",
                "1.5",
                "1e3",
                "-",
                "+1",
                "{\"a\":1,\"a\":2}",
                "[1] x",
                "\"bad \\q escape\"",
                "\"\\u12\"",
                "\"\\uZZZZ\"",
                "\"raw\ncontrol\"",
                "99999999999999999999",
            )
        for (text in bad) assertFailsWith<BackupFormatException>("'$text'") { MiniJson.parse(text) }
    }

    @Test
    fun deepNestingIsRejected() {
        val deep = "[".repeat(100) + "]".repeat(100)
        assertFailsWith<BackupFormatException> { MiniJson.parse(deep) }
    }

    @Test
    fun onlyKnownValueTypesCanBeWritten() {
        assertFailsWith<IllegalArgumentException> { MiniJson.write(1.5) }
        assertFailsWith<IllegalArgumentException> { MiniJson.write(object {}) }
    }

    @Test
    fun nullAndLiteralsParse() {
        assertNull(MiniJson.parse("null"))
        assertEquals(true, MiniJson.parse("true"))
        assertEquals(false, MiniJson.parse("false"))
    }
}
