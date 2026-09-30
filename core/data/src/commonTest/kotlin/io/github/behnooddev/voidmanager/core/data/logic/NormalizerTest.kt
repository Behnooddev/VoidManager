package io.github.behnooddev.voidmanager.core.data.logic

import io.github.behnooddev.voidmanager.core.model.FieldDataType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NormalizerTest {
    @Test
    fun foldsCaseAndWhitespace() {
        assertEquals("ali rezaei", TextNormalizer.normalize("  ALI   Rezaei \n"))
    }

    @Test
    fun foldsLatinAccents() {
        assertEquals("jose garcia", TextNormalizer.normalize("Jos\u00E9 Garc\u00EDa"))
        assertEquals("strasse", TextNormalizer.normalize("Stra\u00DFe"))
        assertEquals("zoe", TextNormalizer.normalize("Zo\u00EB"))
    }

    @Test
    fun mapsArabicLettersToPersianForms() {
        val arabic = "\u0639\u0644\u064A \u0643\u0631\u064A\u0645"
        val persian = "\u0639\u0644\u06CC \u06A9\u0631\u06CC\u0645"

        assertEquals(TextNormalizer.normalize(persian), TextNormalizer.normalize(arabic))
    }

    @Test
    fun ignoresZeroWidthNonJoinerInPersianNames() {
        val withJoiner = "\u0639\u0644\u06CC\u200C\u0631\u0636\u0627"
        val without = "\u0639\u0644\u06CC\u0631\u0636\u0627"

        assertEquals(TextNormalizer.normalize(without), TextNormalizer.normalize(withJoiner))
    }

    @Test
    fun removesDiacriticMarksAndTatweel() {
        val marked = "\u0645\u064F\u062D\u064E\u0645\u0651\u064E\u062F"
        val plain = "\u0645\u062D\u0645\u062F"
        val stretched = "\u0645\u062D\u0640\u0640\u0645\u062F"

        assertEquals(plain, TextNormalizer.normalize(marked))
        assertEquals(plain, TextNormalizer.normalize(stretched))
    }

    @Test
    fun mapsPersianAndArabicIndicDigitsToAscii() {
        assertEquals(
            "09123456789",
            TextNormalizer.normalize("\u06F0\u06F9\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9"),
        )
        assertEquals("2026", TextNormalizer.normalize("\u0662\u0660\u0662\u0666"))
    }

    @Test
    fun searchInputAndStoredTextMeetInTheSameForm() {
        val stored = TextNormalizer.normalize("Jos\u00E9 Mar\u00EDa")
        val query = TextNormalizer.normalize("JOSE MARIA")

        assertEquals(stored, query)
    }

    @Test
    fun emptyAndBlankInputNormalizeToEmpty() {
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("  \n\t "))
    }

    @Test
    fun phoneDigitsIgnoreFormatting() {
        assertEquals("989123456789", PhoneNormalizer.digits("+98 (912) 345-6789"))
        assertEquals(
            "09123456789",
            PhoneNormalizer.digits("\u06F0\u06F9\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9"),
        )
    }

    @Test
    fun phonesWithDifferentCountryPrefixesAreProbablyTheSame() {
        assertTrue(PhoneNormalizer.probablySame("+98 912 345 6789", "09123456789"))
        assertTrue(PhoneNormalizer.probablySame("+1 (555) 010-1234", "5550101234"))
    }

    @Test
    fun differentNumbersAreNotTheSame() {
        assertFalse(PhoneNormalizer.probablySame("09123456789", "09123456780"))
        assertFalse(PhoneNormalizer.probablySame("", "09123456789"))
        assertFalse(PhoneNormalizer.probablySame("12345", "9912345"))
    }

    @Test
    fun likeEscapingNeutralizesWildcards() {
        assertEquals("100\\%\\_\\\\", LikeEscaper.escape("100%_\\"))
        assertEquals("%a\\%b%", LikeEscaper.contains("a%b"))
    }

    @Test
    fun fieldNormalizerPicksTheFormByType() {
        assertEquals("989123456789", FieldNormalizer.normalizedFor(FieldDataType.Phone, "+98 912 345 6789"))
        assertEquals("ali", FieldNormalizer.normalizedFor(FieldDataType.Text, " ALI "))
        assertNull(FieldNormalizer.normalizedFor(FieldDataType.Number, "42"))
        assertNull(FieldNormalizer.normalizedFor(FieldDataType.Text, "   "))
    }
}
